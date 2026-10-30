package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object SMSClassifier {

    private val UTR_REGEX = Pattern.compile("(?i)(?:upi\\s*ref(?:\\s*no)?|utr|ref\\s*no|reference(?:\\s*no)?|txn\\s*id|rrn)[:\\s-]*([A-Za-z0-9]{10,22})")
    private val STANDALONE_12_DIGIT_REGEX = Pattern.compile("\\b(\\d{12})\\b")
    private val ACCOUNT_ENDING_REGEX = Pattern.compile("(?i)(?:a/c|acct|account|card)\\s*(?:no\\.?)?\\s*(?:ending\\s+(?:with\\s+)?)?[*xX]*(\\d{4})")

    // Outgoing Debit Patterns: matches "debited by Rs 450", "paid Rs 200", "spent Rs 1,299", "sent Rs.500", "Debit INR 33015.00"
    private val DEBIT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:debited\\s+(?:by|with|for)?|debit\\s+of|debit|spent\\s+at|spent\\s+for|spent|paid|sent|transferred|withdrawn|txn\\s+of|payment\\s+of|charged|deducted)\\s*(?:towards\\s+[A-Za-z0-9&.\\s]+?\\s+for|to\\s+[A-Za-z0-9&.\\s]+?)?\\s*(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s+(?:debited|spent|paid|sent|transferred|withdrawn|deducted|has\\s+been\\s+debited)"),
        Pattern.compile("(?i)(?:debited|spent)\\s+towards\\s+[A-Za-z0-9&.\\s]+?\\s+for\\s*(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        Pattern.compile("(?i)(?:debited\\s+by|spent\\s+at|transferred)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:on|to|for|via|ref|at)")
    )

    // Incoming Credit Patterns: matches "credited with Rs 45,000", "received Rs 500", "refund of Rs 350"
    private val CREDIT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:credited\\s+(?:with|by|to|for)?|received|refund\\s+of|cashback\\s+of)\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s+(?:credited|received|refunded|deposited|has\\s+been\\s+credited)"),
        Pattern.compile("(?i)(?:credited\\s+with|received)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:on|from|via|ref|by)")
    )

    // General fallback amount pattern (only if debit/credit context was verified)
    private val GENERAL_AMOUNT_PATTERN = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)")

    // Balance pattern to avoid mistaking Avl Bal or Avbl Limit for transaction amount
    private val BALANCE_PATTERN = Pattern.compile("(?i)(?:avl(?:\\.|\\s+bal|\\s+lmt|\\s+limit)?|available\\s+(?:balance|limit|bal)|avbl(?:\\.|\\s+limit|\\s+lmt)?|credit\\s+limit|total\\s+limit|bal(?:ance)?|limit|lmt)\\s*(?:is)?[:\\s-]*(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{1,2})?)")

    /**
     * Executes the dedicated classification pipeline:
     * SMS -> Normalize Text -> Detect Noise (OTP/Promo/Bill/KYC/Investment/Order) ->
     * Detect Financial Event -> Extract Amount -> Determine Type/Status/Refund ->
     * Calculate Confidence -> Extract Merchant & UTR -> Deduplicate & Link -> Assign Category.
     */
    fun classify(
        sender: String,
        rawContent: String,
        messageTimestampMs: Long = System.currentTimeMillis(),
        availableCategories: List<Category> = emptyList(),
        existingTransactions: List<Transaction> = emptyList()
    ): ClassificationResult {
        // Step 1: Normalize Text
        val normalizedText = normalizeText(rawContent)
        val normalizedSender = sender.trim()
        val lower = normalizedText.lowercase(Locale.ENGLISH)

        // Step 2: Hard Exclusions / Noise Detection
        // 2a. OTP & Authentication
        if (isOtpMessage(lower)) {
            return ClassificationResult(
                isFinancialTransaction = false,
                confidence = ClassificationConfidence.LOW,
                noiseCategory = NoiseCategory.OTP_AUTHENTICATION,
                rejectionReason = "OTP or authentication security message"
            )
        }

        // 2b. Promotional Offers & Loan Spam
        if (isPromotionalOrLoanOffer(lower)) {
            return ClassificationResult(
                isFinancialTransaction = false,
                confidence = ClassificationConfidence.LOW,
                noiseCategory = NoiseCategory.PROMOTIONAL_OFFER,
                rejectionReason = "Promotional offer, loan solicitation, or credit limit increase notice"
            )
        }

        // 2c. Bill & EMI Reminders / CC Bill Repayments (Settlement)
        if (isBillReminderOrSettlement(lower)) {
            return ClassificationResult(
                isFinancialTransaction = false,
                confidence = ClassificationConfidence.LOW,
                noiseCategory = NoiseCategory.BILL_PAYMENT_REMINDER,
                rejectionReason = "Bill payment reminder or credit card payment settlement"
            )
        }

        // 2d. KYC / Account / Security Notifications
        if (isKycOrSecurityAlert(lower)) {
            return ClassificationResult(
                isFinancialTransaction = false,
                confidence = ClassificationConfidence.LOW,
                noiseCategory = NoiseCategory.KYC_ACCOUNT_SECURITY,
                rejectionReason = "KYC, account dispatch, or login alert notification"
            )
        }

        // 2e. Investment / Financial Information (NAV, Stock, Portfolio)
        if (isInvestmentInfo(lower)) {
            return ClassificationResult(
                isFinancialTransaction = false,
                confidence = ClassificationConfidence.LOW,
                noiseCategory = NoiseCategory.INVESTMENT_INFO,
                rejectionReason = "Mutual fund NAV, portfolio valuation, or scheduled SIP notice"
            )
        }

        // 2f. Delivery / Order Notifications
        if (isOrderDeliveryNotification(lower)) {
            return ClassificationResult(
                isFinancialTransaction = false,
                confidence = ClassificationConfidence.LOW,
                noiseCategory = NoiseCategory.ORDER_DELIVERY_NOTIFICATION,
                rejectionReason = "Order delivery or shipment update without completed payment"
            )
        }

        // Step 3: Failed / Reversed / Declined Checks
        val isFailed = isFailedTransaction(lower)
        val isReversed = isReversalTransaction(lower)

        // Step 4: Refund Detection & Refund States
        val (isRefund, refundState) = detectRefund(lower)

        // Step 5: Cashback Detection
        val isCashback = detectCashback(lower)

        // Step 6: Self-Transfer Detection
        val isSelfTransfer = detectSelfTransfer(lower)

        // Step 7: Financial Event Verification & Direction Determination
        val hasDebitVerb = hasDebitEventVerb(lower)
        val hasCreditVerb = hasCreditEventVerb(lower)

        if (!hasDebitVerb && !hasCreditVerb && !isRefund && !isCashback && !isReversed && !isFailed) {
            return ClassificationResult(
                isFinancialTransaction = false,
                confidence = ClassificationConfidence.LOW,
                rejectionReason = "No financial event verb or transaction activity found"
            )
        }

        val direction = when {
            isRefund || isCashback || isReversed -> TransactionDirection.INCOMING
            hasCreditVerb && !hasDebitVerb -> TransactionDirection.INCOMING
            else -> TransactionDirection.OUTGOING
        }

        // Step 8: Amount Extraction (distinguishing from balance & limits)
        val balancePaise = extractBalance(normalizedText)
        val amountPaise = extractTransactionAmount(normalizedText, direction, balancePaise)
        if (amountPaise == null || amountPaise <= 0L) {
            return ClassificationResult(
                isFinancialTransaction = false,
                confidence = ClassificationConfidence.LOW,
                rejectionReason = "Could not extract valid transaction amount"
            )
        }

        // Step 9: Multiple Signals Verification & Confidence Calculation
        val confidence = computeConfidence(
            sender = normalizedSender,
            text = normalizedText,
            lower = lower,
            amountPaise = amountPaise,
            hasEventVerb = hasDebitVerb || hasCreditVerb || isRefund || isCashback || isReversed || isFailed
        )

        // Precision > Recall Rule: Drop LOW confidence candidates
        if (confidence == ClassificationConfidence.LOW) {
            return ClassificationResult(
                isFinancialTransaction = false,
                confidence = ClassificationConfidence.LOW,
                rejectionReason = "Low confidence: Insufficient corroborating signals"
            )
        }

        // Step 10: Merchant Extraction & Normalization
        val rawCounterparty = extractCounterparty(normalizedText, direction, isRefund)
        val normalizedMerchant = MerchantNormalizer.normalize(rawCounterparty)

        // Step 11: Transaction ID / UPI Reference / Account Ending / Date
        val utr = extractUtr(normalizedText)
        val upiVpa = extractUpiVpa(normalizedText)
        val accountEnding = extractAccountEnding(normalizedText)
        val eventTimestampMs = extractDateFromText(normalizedText) ?: messageTimestampMs
        val paymentApp = detectPaymentApp(normalizedSender, normalizedText)

        // Determine Transaction Status
        val status = when {
            isFailed -> TransactionStatus.FAILED
            isReversed -> TransactionStatus.REVERSED
            refundState == RefundState.REFUND_INITIATED -> TransactionStatus.PENDING
            else -> TransactionStatus.COMPLETED
        }

        // Determine Transaction Type
        val type = when {
            isRefund -> TransactionType.REFUND
            isCashback -> TransactionType.CASHBACK
            isReversed -> TransactionType.REVERSAL
            isSelfTransfer -> TransactionType.TRANSFER_SELF
            direction == TransactionDirection.INCOMING -> {
                if (lower.contains("salary") || lower.contains("payroll")) TransactionType.INCOME else TransactionType.INCOME
            }
            else -> TransactionType.EXPENSE
        }

        // Step 12: Link Refund / Reversal to Original Transaction (if available)
        var relatedTxId: Long? = null
        if (isRefund || isReversed) {
            val matchedOriginal = findMatchingOriginalTransaction(
                amountPaise = amountPaise,
                merchant = normalizedMerchant,
                utr = utr,
                timestampMs = eventTimestampMs,
                existingTransactions = existingTransactions
            )
            relatedTxId = matchedOriginal?.id
        }

        // Step 13: Category Resolution
        val (categoryId, categoryName, categoryColor) = resolveCategory(
            type = type,
            counterparty = normalizedMerchant,
            direction = direction,
            content = normalizedText,
            availableCategories = availableCategories
        )

        val paymentMethod = when {
            normalizedText.contains("atm", ignoreCase = true) -> "ATM"
            normalizedText.contains("card", ignoreCase = true) -> "Card"
            normalizedText.contains("neft", ignoreCase = true) -> "NEFT"
            normalizedText.contains("imps", ignoreCase = true) -> "IMPS"
            normalizedText.contains("rtgs", ignoreCase = true) -> "RTGS"
            else -> "UPI"
        }

        val transaction = Transaction(
            id = 0L,
            amountPaise = amountPaise,
            type = type,
            direction = direction,
            status = status,
            title = normalizedMerchant,
            counterpartyName = normalizedMerchant,
            counterpartyUpiId = upiVpa,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryColorHex = categoryColor,
            timestampMs = eventTimestampMs,
            paymentMethod = paymentMethod,
            paymentApp = paymentApp,
            bankReferenceNum = utr,
            notes = if (accountEnding != null) "A/c ending **$accountEnding" else null,
            source = TransactionSource.SMS_IMPORT,
            rawIdentifier = "SMS_${eventTimestampMs}_${amountPaise}_${utr ?: normalizedSender}",
            relatedTransactionId = relatedTxId
        )

        return ClassificationResult(
            isFinancialTransaction = true,
            confidence = confidence,
            noiseCategory = NoiseCategory.NONE,
            transaction = transaction,
            refundState = refundState,
            accountEnding = accountEnding,
            availableBalancePaise = balancePaise
        )
    }

    // -------------------------------------------------------------
    // Hard Exclusions & Noise Detection
    // -------------------------------------------------------------

    fun isOtpMessage(lower: String): Boolean {
        // Matches OTP, secret passwords, verification codes
        val hasOtpKeyword = lower.contains("otp") ||
                lower.contains("one-time password") ||
                lower.contains("one time password") ||
                lower.contains("verification code") ||
                lower.contains("verification pin") ||
                lower.contains("login code") ||
                lower.contains("auth code") ||
                lower.contains("secret code") ||
                lower.contains("security code") ||
                lower.contains("password reset")

        val hasOtpWarning = lower.contains("do not share") ||
                lower.contains("never share") ||
                lower.contains("valid for") ||
                lower.contains("pls do not share") ||
                lower.contains("valid till") ||
                lower.contains("confidential")

        val hasCredential = lower.contains("cvv") ||
                lower.contains("mpin") ||
                lower.contains("upi pin") ||
                lower.contains("atm pin")

        val startsWithCode = lower.matches(Regex("^\\s*\\d{4,8}\\s+is\\s+(?:your\\s+)?(?:secret\\s+)?(?:otp|code|one-time).*"))

        return (hasOtpKeyword && (hasOtpWarning || startsWithCode || lower.contains("txn of") || lower.contains("transaction of"))) ||
                hasCredential || startsWithCode
    }

    fun isPromotionalOrLoanOffer(lower: String): Boolean {
        // Loan & Credit Lines
        if (lower.contains("pre-approved") || lower.contains("pre approved") || lower.contains("preapproved") ||
            lower.contains("instant loan") || lower.contains("personal loan") || lower.contains("cred cash") ||
            lower.contains("credit line") || lower.contains("apply now") || lower.contains("eligible for") ||
            lower.contains("instant cash") || lower.contains("funds ready") || lower.contains("funds of") ||
            lower.contains("funds are available") || lower.contains("are available and") || lower.contains("is available and") ||
            lower.contains("disbursement") || lower.contains("disbursal") || lower.contains("access loan") ||
            lower.contains("get a loan") || lower.contains("claim loan") || lower.contains("loan up to") ||
            lower.contains("require consent") || lower.contains("requiring consent") || lower.contains("providing consent") ||
            lower.contains("with your consent") || lower.contains("consent to continue") || lower.contains("consent for") ||
            lower.contains("give consent") || lower.contains("service alert:\nfunds") || lower.contains("service alert: funds")
        ) {
            return true
        }

        // Promotional Cashback / Discounts Bait (NOT completed cashback)
        val hasPromoCashback = (lower.contains("get ₹") || lower.contains("get rs") || lower.contains("get inr") ||
                lower.contains("win cashback") || lower.contains("earn cashback") || lower.contains("chance to win") ||
                lower.contains("upto ₹") || lower.contains("upto rs") || lower.contains("flat ₹") || lower.contains("flat rs") ||
                lower.contains("get 20%") || lower.contains("get 10%") || lower.contains("get 50%") || lower.contains("use code") ||
                lower.contains("discount of") || lower.contains("voucher worth") || lower.contains("gift voucher")) &&
                (lower.contains("cashback") || lower.contains("purchase") || lower.contains("order") || lower.contains("recharge") || lower.contains("offer"))

        val isCompletedCashback = lower.contains("credited") || lower.contains("received")
        if (hasPromoCashback && !isCompletedCashback) {
            return true
        }

        // Credit Card Limit & Conversion Offers
        if (lower.contains("credit limit has been increased") || lower.contains("limit enhanced") ||
            lower.contains("increase your limit") || lower.contains("upgrade your card") ||
            lower.contains("special offer on your credit card") || lower.contains("credit limit increased to") ||
            lower.contains("limit preferences have been updated") || lower.contains("convert your") ||
            lower.contains("convert to emi") || lower.contains("converting outstanding") ||
            lower.contains("converted into emi") || lower.contains("can be converted into") ||
            lower.contains("into emis") || lower.contains("emi tenure") || lower.contains("cibil score") ||
            lower.contains("credit report") || lower.contains("special offer") || lower.contains("exclusive offer") ||
            lower.contains("claim now") || lower.contains("avail now") || lower.contains("spin the wheel")
        ) {
            return true
        }

        return false
    }

    fun isBillReminderOrSettlement(lower: String): Boolean {
        // Credit card bill payment acknowledgment (account settlement, not daily expense)
        if ((lower.contains("towards your") || lower.contains("towards")) &&
            (lower.contains("credit card") || lower.contains("card no.") || lower.contains("card xx") || lower.contains("card ending")) &&
            (lower.contains("payment") || lower.contains("received") || lower.contains("thank you"))
        ) {
            return true
        }

        // Pending bills & statements where payment has NOT completed
        val hasDueLanguage = lower.contains("is due") || lower.contains("due date") ||
                lower.contains("due on") || lower.contains("due by") || lower.contains("due tomorrow") ||
                lower.contains("pay before") || lower.contains("is pending") || lower.contains("pending payment") ||
                lower.contains("total due") || lower.contains("min due") || lower.contains("min. due") ||
                lower.contains("minimum due") || lower.contains("bill for your") ||
                lower.contains("pay your emi") || lower.contains("pay your bill") ||
                lower.contains("estatement") || lower.contains("e-statement") || lower.contains("statement for your") ||
                lower.contains("to be debited on") || lower.contains("scheduled for debit") ||
                (lower.contains("before") && (lower.contains("bill") || lower.contains("emi") || lower.contains("pay ")))

        val hasCompletedDebit = lower.contains("has been debited") || lower.contains("debited with") ||
                lower.contains("debited by") || lower.contains("debited towards") ||
                lower.contains("payment successful") || lower.contains("paid successfully")

        return hasDueLanguage && !hasCompletedDebit
    }

    fun isKycOrSecurityAlert(lower: String): Boolean {
        return lower.contains("kyc update") || lower.contains("update kyc") ||
                lower.contains("kyc suspended") || lower.contains("kyc pending") ||
                lower.contains("update your pan") || lower.contains("pan linked") ||
                lower.contains("account has been successfully verified") || lower.contains("account was logged in") ||
                lower.contains("new login detected") || lower.contains("login alert") ||
                lower.contains("debit card is ready") || lower.contains("card has been dispatched") ||
                lower.contains("cheque book has been dispatched") || lower.contains("cheque book issued") ||
                lower.contains("account statement is available") || lower.contains("download statement") ||
                lower.contains("service notification") || lower.contains("has gone below minimum limit") ||
                lower.contains("yesterday's bal") || lower.contains("minimum limit of")
    }

    fun isInvestmentInfo(lower: String): Boolean {
        if (lower.contains("mutual fund nav") || lower.contains("nav is") ||
            lower.contains("stock price is") || lower.contains("portfolio value is") ||
            lower.contains("sensex") || lower.contains("nifty")
        ) {
            return true
        }
        if (lower.contains("sip of") && (lower.contains("scheduled for tomorrow") || lower.contains("is scheduled on"))) {
            return true
        }
        return false
    }

    fun isOrderDeliveryNotification(lower: String): Boolean {
        val hasOrderLanguage = (lower.contains("order") && (lower.contains("confirmed") || lower.contains("delivered") || lower.contains("shipped") || lower.contains("dispatched") || lower.contains("prepared") || lower.contains("out for delivery"))) ||
                lower.contains("will be delivered") || lower.contains("has been shipped") ||
                lower.contains("out for delivery") || lower.contains("delivered successfully") ||
                lower.contains("arriving today") || lower.contains("item has been dispatched")

        val hasActualDebit = lower.contains("debited") || lower.contains("paid") ||
                lower.contains("card was used") || lower.contains("charged") ||
                lower.contains("payment successful")

        return hasOrderLanguage && !hasActualDebit
    }

    fun isFailedTransaction(lower: String): Boolean {
        return lower.contains("transaction failed") || lower.contains("payment failed") ||
                lower.contains("upi transaction declined") || lower.contains("payment declined") ||
                lower.contains("failed due to") || lower.contains("unsuccessful") ||
                lower.contains("was not completed") || lower.contains("insufficient balance") ||
                lower.contains("insufficient funds") || lower.contains("declined") ||
                lower.contains("txn failed")
    }

    fun isReversalTransaction(lower: String): Boolean {
        return lower.contains("transaction reversed") || lower.contains("amount reversed") ||
                lower.contains("payment reversed") || lower.contains("amount credited back") ||
                lower.contains("debit reversed") || lower.contains("has been reversed")
    }

    fun detectRefund(lower: String): Pair<Boolean, RefundState> {
        val isRefund = lower.contains("refund") || lower.contains("refunded") ||
                lower.contains("reversed to your account")

        if (!isRefund) return false to RefundState.NOT_A_REFUND

        val isInitiated = lower.contains("refund initiated") || lower.contains("has been initiated") ||
                lower.contains("refund has been processed") || lower.contains("refund is in process") ||
                lower.contains("will be credited") || lower.contains("within 5-7 days") ||
                lower.contains("within 3-5 days") || lower.contains("within 7 days")

        val isCompleted = lower.contains("refund credited") || lower.contains("refund received") ||
                lower.contains("has refunded") || lower.contains("credited as refund") ||
                lower.contains("amount has been refunded") || lower.contains("credited back")

        return when {
            isCompleted -> true to RefundState.REFUND_COMPLETED
            isInitiated -> true to RefundState.REFUND_INITIATED
            else -> true to RefundState.REFUND_COMPLETED
        }
    }

    fun detectCashback(lower: String): Boolean {
        return lower.contains("cashback credited") || lower.contains("cashback of") && lower.contains("credited") ||
                lower.contains("credited") && lower.contains("as cashback") ||
                lower.contains("received cashback") || lower.contains("cashback received")
    }

    fun detectSelfTransfer(lower: String): Boolean {
        return lower.contains("transfer to own account") || lower.contains("transferred to own account") ||
                lower.contains("transfer to your own") || lower.contains("transferred to your own") ||
                lower.contains("self transfer") || lower.contains("between your accounts") ||
                lower.contains("transfer to linked account") || lower.contains("to your other a/c") ||
                lower.contains("to your own a/c")
    }

    private fun hasDebitEventVerb(lower: String): Boolean {
        return lower.contains("debited") || lower.contains("debit of") || lower.contains("debit") ||
                lower.contains("spent") || lower.contains("paid") || lower.contains("payment") ||
                lower.contains("transferred") || lower.contains("withdrawn") ||
                lower.contains("deducted") || lower.contains("charged") || lower.contains("purchase completed") ||
                lower.contains("payment successful") || lower.contains("was used") || lower.contains("card used") ||
                lower.contains("sent rs") || lower.contains("sent inr") || lower.contains("sent ₹") ||
                lower.contains("atm cash") || lower.contains("emi debited") || lower.contains("nach-dr") ||
                lower.contains("ach-dr") || lower.contains("upi/dr")
    }

    private fun hasCreditEventVerb(lower: String): Boolean {
        return lower.contains("credited") || lower.contains("credit alert") || lower.contains("credit of") ||
                lower.contains("has been credited") || lower.contains("received") ||
                lower.contains("deposited") || lower.contains("salary") || lower.contains("payroll") ||
                lower.contains("interest credited") || lower.contains("nach-cr") || lower.contains("ach-cr") ||
                lower.contains("upi/cr") || lower.contains("neft") || lower.contains("imps credit")
    }

    // -------------------------------------------------------------
    // Confidence Computation (Multiple Signals)
    // -------------------------------------------------------------

    private fun computeConfidence(
        sender: String,
        text: String,
        lower: String,
        amountPaise: Long,
        hasEventVerb: Boolean
    ): ClassificationConfidence {
        var signalScore = 0

        // 1. Valid non-zero Amount
        if (amountPaise > 0L) signalScore++

        // 2. Financial Event Verb
        if (hasEventVerb) signalScore++

        // 3. Account / Card / Payment Context
        val hasAccountContext = lower.contains("a/c") || lower.contains("acct") ||
                lower.contains("account") || lower.contains("card ending") ||
                lower.contains("card xx") || lower.contains("card x") ||
                lower.contains("upi") || lower.contains("vpa") ||
                lower.contains("imps") || lower.contains("neft") ||
                lower.contains("rtgs") || lower.contains("atm") ||
                lower.contains("pos") || lower.contains("wallet")
        if (hasAccountContext) signalScore++

        // 4. Completion Language
        val hasCompletion = lower.contains("successful") || lower.contains("completed") ||
                lower.contains("done") || lower.contains("debited") || lower.contains("credited") ||
                lower.contains("spent") || lower.contains("paid")
        if (hasCompletion) signalScore++

        // 5. Sender Verification
        val lowerSender = sender.lowercase(Locale.ENGLISH)
        val isRecognizedBankSender = lowerSender.contains("hdfc") || lowerSender.contains("sbi") ||
                lowerSender.contains("icici") || lowerSender.contains("axis") ||
                lowerSender.contains("kotak") || lowerSender.contains("idfc") ||
                lowerSender.contains("yes") || lowerSender.contains("pnb") ||
                lowerSender.contains("bob") || lowerSender.contains("paytm") ||
                lowerSender.contains("phonepe") || lowerSender.contains("gpay") ||
                lowerSender.contains("cred") || lowerSender.contains("bank") ||
                lowerSender.contains("bnk") || lowerSender.contains("upi") ||
                lowerSender.contains("indus") || lowerSender.contains("canara") ||
                sender.matches(Regex("^[A-Za-z]{2}-[A-Za-z0-9]{4,}"))
        if (isRecognizedBankSender) signalScore++

        return when {
            signalScore >= 4 -> ClassificationConfidence.HIGH
            signalScore == 3 && hasEventVerb -> ClassificationConfidence.MEDIUM
            else -> ClassificationConfidence.LOW
        }
    }

    // -------------------------------------------------------------
    // Data Extraction Helpers
    // -------------------------------------------------------------

    private fun normalizeText(text: String): String {
        return text.replace('\u00A0', ' ')
            .replace('\u200B', ' ')
            .replace(Regex("[\\r\\n]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractTransactionAmount(content: String, direction: TransactionDirection, balancePaise: Long?): Long? {
        val patterns = if (direction == TransactionDirection.OUTGOING) DEBIT_PATTERNS else CREDIT_PATTERNS

        for (pattern in patterns) {
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val amtStr = matcher.group(1) ?: continue
                val parsed = parseAmountStringToPaise(amtStr)
                if (parsed != null && parsed > 0L && parsed != balancePaise) {
                    return parsed
                }
            }
        }

        // Fallback: general currency pattern
        val generalMatcher = GENERAL_AMOUNT_PATTERN.matcher(content)
        while (generalMatcher.find()) {
            val amtStr = generalMatcher.group(1) ?: continue
            val parsed = parseAmountStringToPaise(amtStr)
            if (parsed != null && parsed > 0L && parsed != balancePaise) {
                return parsed
            }
        }

        return null
    }

    private fun extractBalance(content: String): Long? {
        val matcher = BALANCE_PATTERN.matcher(content)
        return if (matcher.find()) {
            matcher.group(1)?.let { parseAmountStringToPaise(it) }
        } else {
            null
        }
    }

    private fun extractCounterparty(content: String, direction: TransactionDirection, isRefund: Boolean): String {
        // 1. Try @UPI_MERCHANT or @MERCHANT (common in YES Bank, IndusInd, e.g. "@UPI_ZOMATO MEDIA PRIVA")
        val atMatcher = Pattern.compile("(?i)@(?:UPI_)?([A-Za-z0-9&.\\s]+?)(?:\\s+\\d{1,2}[-/]|\\s+on|\\s+via|\\s+using|\\.|$)").matcher(content)
        if (atMatcher.find()) {
            val name = atMatcher.group(1)?.trim()
            if (!name.isNullOrBlank()) {
                val cleaned = cleanExtractedMerchant(name)
                if (cleaned.isNotBlank() && cleaned.length >= 2) return cleaned
            }
        }

        // 2. UPI Narration patterns
        // Format A & B: UPI/(CR|DR)/<ref>/<merchant>/<bank>
        val upiCrDrMatcher = Pattern.compile("(?i)UPI/(?:CR|DR)/[A-Za-z0-9]+/([A-Za-z0-9&.\\s]+?)/").matcher(content)
        if (upiCrDrMatcher.find()) {
            val name = upiCrDrMatcher.group(1)?.trim()
            if (!name.isNullOrBlank()) {
                val cleaned = cleanExtractedMerchant(name)
                if (cleaned.isNotBlank() && cleaned.length >= 2) return cleaned
            }
        }

        // Format C: UPI/(P2A|P2M)/<ref>/<merchant>
        val upiP2Matcher = Pattern.compile("(?i)UPI/(?:P2A|P2M)/[A-Za-z0-9]+/([A-Za-z0-9&.\\s]+?)(?:/|\\r?\\n|\\s+not\\s+you|\\s+sms|\\.|$)").matcher(content)
        if (upiP2Matcher.find()) {
            val name = upiP2Matcher.group(1)?.trim()
            if (!name.isNullOrBlank()) {
                val cleaned = cleanExtractedMerchant(name)
                if (cleaned.isNotBlank() && cleaned.length >= 2) return cleaned
            }
        }

        // Format D: UPI/<ref>/<merchant>/
        val upiGeneralMatcher = Pattern.compile("(?i)UPI/[A-Za-z0-9]+/([A-Za-z0-9&.\\s]+?)/").matcher(content)
        if (upiGeneralMatcher.find()) {
            val name = upiGeneralMatcher.group(1)?.trim()
            if (!name.isNullOrBlank()) {
                val cleaned = cleanExtractedMerchant(name)
                if (cleaned.isNotBlank() && cleaned.length >= 2) return cleaned
            }
        }

        // 3. For Refunds: "refunded by Amazon", "refund of Rs 850 from Flipkart", "Swiggy has refunded"
        if (isRefund) {
            val refundFromMatcher = Pattern.compile("(?i)(?:from|by|for)\\s+([A-Za-z0-9&.\\s]+?)(?:\\s+on|\\s+via|\\s+credited|\\s+using|\\.|$)").matcher(content)
            if (refundFromMatcher.find()) {
                val name = refundFromMatcher.group(1)?.trim()
                if (!name.isNullOrBlank()) {
                    val cleaned = cleanExtractedMerchant(name)
                    if (cleaned.isNotBlank() && cleaned.length >= 2) return cleaned
                }
            }
            val refundHasMatcher = Pattern.compile("(?i)([A-Za-z0-9&.\\s]+?)\\s+has\\s+refunded").matcher(content)
            if (refundHasMatcher.find()) {
                val name = refundHasMatcher.group(1)?.trim()
                if (!name.isNullOrBlank()) {
                    val cleaned = cleanExtractedMerchant(name)
                    if (cleaned.isNotBlank() && cleaned.length >= 2) return cleaned
                }
            }
        }

        // 4. Try Preposition patterns: "to SWIGGY", "at AMAZON", "towards Google", "from RAJESH"
        val prepPattern = if (direction == TransactionDirection.OUTGOING) {
            Pattern.compile("(?i)(?:towards|to|at|info:|for)\\s+([A-Za-z0-9&.\\s]+?)(?:\\s+for\\s+(?:rs|inr|₹)|\\s+on\\s+\\d{1,2}[-/]|\\s+via|\\s+using|\\s+upi|\\s+ref|\\s+avl|\\s+bal|\\.|$)")
        } else {
            Pattern.compile("(?i)(?:from|by)\\s+([A-Za-z0-9&.\\s]+?)(?:\\s+on|\\s+via|\\s+using|\\s+upi|\\s+ref|\\s+avl|\\s+bal|\\.|$)")
        }

        val prepMatcher = prepPattern.matcher(content)
        if (prepMatcher.find()) {
            val raw = prepMatcher.group(1)?.trim() ?: ""
            val cleaned = cleanExtractedMerchant(raw)
            if (cleaned.isNotBlank() && cleaned.length >= 2) {
                return cleaned
            }
        }

        // 5. Fallback: Match Known Indian Merchants in text
        val knownMerchants = listOf(
            "Swiggy", "Zomato", "Starbucks", "Blinkit", "Zepto", "Instamart", "Bigbasket",
            "Amazon", "Flipkart", "Myntra", "Meesho", "Nykaa", "Tata Cliq", "Ajio", "Zara",
            "Uber", "Ola", "Rapido", "Metro", "Shell", "Indian Oil", "HPCL", "BPCL", "IRCTC", "MakeMyTrip",
            "Bescom", "Electricity", "Airtel", "Jio", "Vodafone", "Tata Play", "Billdesk",
            "Netflix", "Spotify", "BookMyShow", "PVR", "Inox", "Disney Hotstar", "YouTube", "Google",
            "Apollo", "PharmEasy", "1mg", "Medplus",
            "Zerodha", "Groww", "CRED"
        )

        for (merchant in knownMerchants) {
            if (content.contains(merchant, ignoreCase = true)) {
                return merchant
            }
        }

        return if (direction == TransactionDirection.INCOMING) "Money Received" else "UPI Payment"
    }

    private fun cleanExtractedMerchant(raw: String): String {
        val cleaned = raw.replace(Pattern.compile("(?i)^(?:a/c|vpa|info|vpa:|upi:|upi_|@upi_|@|upi\\s+)\\s*").toRegex(), "")
            .replace(Pattern.compile("(?i)\\s+(?:not\\s+you|sms\\s+block|blockupi|cust\\s+id|ref|utr|rrn|no|media\\s+priva|pvt\\s+ltd|ltd).*$").toRegex(), "")
            .trim()

        if (cleaned.matches(Regex("^[0-9+\\s-]+$"))) return ""
        val lower = cleaned.lowercase(Locale.ENGLISH)
        if (lower in listOf("queries", "customer", "help", "support", "dispute", "information", "details", "request", "account", "card", "credit card")) {
            return ""
        }
        if (lower.startsWith("within ") || lower.startsWith("download for")) {
            return ""
        }
        return cleaned
    }

    private fun extractUtr(content: String): String? {
        val matcher = UTR_REGEX.matcher(content)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        val standalone = STANDALONE_12_DIGIT_REGEX.matcher(content)
        if (standalone.find()) {
            return standalone.group(1)?.trim()
        }
        return null
    }

    private fun extractUpiVpa(content: String): String? {
        val matcher = Pattern.compile("([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)").matcher(content)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }

    private fun extractAccountEnding(content: String): String? {
        val matcher = ACCOUNT_ENDING_REGEX.matcher(content)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun detectPaymentApp(sender: String, content: String): String {
        val combined = (sender + " " + content).lowercase(Locale.ENGLISH)
        return when {
            combined.contains("gpay") || combined.contains("google pay") || combined.contains("@okhdfc") || combined.contains("@okaxis") || combined.contains("@oksbi") || combined.contains("@okicici") -> "Google Pay"
            combined.contains("phonepe") || combined.contains("@ybl") || combined.contains("@ibl") || combined.contains("@axl") -> "PhonePe"
            combined.contains("paytm") || combined.contains("@paytm") -> "Paytm"
            combined.contains("cred") -> "CRED"
            combined.contains("bhim") -> "BHIM UPI"
            combined.contains("yes") || combined.contains("yesbnk") -> "YES Bank"
            combined.contains("indus") || combined.contains("indusb") -> "IndusInd Bank"
            combined.contains("idfc") -> "IDFC FIRST Bank"
            combined.contains("hdfc") -> "HDFC Bank"
            combined.contains("sbi") -> "SBI"
            combined.contains("icici") -> "ICICI Bank"
            combined.contains("axis") -> "Axis Bank"
            combined.contains("kotak") -> "Kotak Bank"
            combined.contains("union") -> "Union Bank"
            combined.contains("pnb") -> "PNB"
            else -> "Google Pay"
        }
    }

    private fun extractDateFromText(content: String): Long? {
        val datePatterns = listOf(
            "dd-MM-yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm:ss",
            "dd-MMM-yy HH:mm:ss",
            "dd-MMM-yyyy",
            "dd-MMM-yy",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd/MM/yy",
            "dd-MM-yy"
        )

        val dateMatcher = Pattern.compile("(?i)\\b(\\d{1,2}[-/](?:[a-z]{3}|\\d{2})[-/]\\d{2,4}(?:\\s+\\d{2}:\\d{2}:\\d{2})?)\\b").matcher(content)
        if (dateMatcher.find()) {
            val dateStr = dateMatcher.group(1) ?: return null
            val now = System.currentTimeMillis()
            for (pattern in datePatterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }
                    val parsed = sdf.parse(dateStr)
                    if (parsed != null && parsed.time <= now && parsed.time >= now - (365L * 24 * 60 * 60 * 1000L)) {
                        return parsed.time
                    }
                } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun findMatchingOriginalTransaction(
        amountPaise: Long,
        merchant: String,
        utr: String?,
        timestampMs: Long,
        existingTransactions: List<Transaction>
    ): Transaction? {
        // 1. Match by Bank Reference / UTR
        if (!utr.isNullOrBlank()) {
            val utrMatch = existingTransactions.find {
                it.bankReferenceNum != null && it.bankReferenceNum.equals(utr, ignoreCase = true)
            }
            if (utrMatch != null) return utrMatch
        }

        // 2. Match by normalized merchant and amount within 60 days
        val maxDiffMs = 60L * 24 * 60 * 60 * 1000L
        val normCandidate = MerchantNormalizer.normalize(merchant)

        return existingTransactions.find { existing ->
            val normExisting = MerchantNormalizer.normalize(existing.counterpartyName)
            val sameMerchant = normCandidate.equals(normExisting, ignoreCase = true)
            val timeDiff = timestampMs - existing.timestampMs
            val reasonableTime = timeDiff >= 0 && timeDiff <= maxDiffMs
            val matchesAmount = existing.amountPaise >= amountPaise // Allows full or partial refund match

            existing.type == TransactionType.EXPENSE && sameMerchant && matchesAmount && reasonableTime
        }
    }

    private fun resolveCategory(
        type: TransactionType,
        counterparty: String,
        direction: TransactionDirection,
        content: String,
        availableCategories: List<Category>
    ): Triple<Long, String, String> {
        fun findCat(name: String, fallbackId: Long, fallbackColor: String): Triple<Long, String, String> {
            val found = availableCategories.find { it.name.contains(name, ignoreCase = true) }
            return Triple(
                found?.id ?: fallbackId,
                found?.name ?: name,
                found?.colorHex ?: fallbackColor
            )
        }

        if (type == TransactionType.REFUND) {
            return findCat("Refund", 8L, "#26A69A")
        }

        if (type == TransactionType.CASHBACK) {
            return findCat("Cashback", 8L, "#FFA726")
        }

        if (type == TransactionType.TRANSFER_SELF || type == TransactionType.TRANSFER) {
            return findCat("Transfer", 8L, "#7E57C2")
        }

        if (direction == TransactionDirection.INCOMING) {
            val incomeCat = availableCategories.find { it.type == "INCOME" || it.id == 7L }
            return Triple(
                incomeCat?.id ?: 7L,
                incomeCat?.name ?: "Salary",
                incomeCat?.colorHex ?: "#00E676"
            )
        }

        val text = (counterparty + " " + content).lowercase(Locale.ENGLISH)

        return when {
            text.contains("swiggy") || text.contains("zomato") || text.contains("starbucks") ||
                    text.contains("mcdonald") || text.contains("burger") || text.contains("pizza") ||
                    text.contains("cafe") || text.contains("restaurant") || text.contains("chai") ||
                    text.contains("dining") || text.contains("food") || text.contains("bakery") ||
                    text.contains("blinkit") || text.contains("zepto") || text.contains("instamart") ||
                    text.contains("pan shop") || text.contains("fruits") || text.contains("groceries") ->
                findCat("Food & Dining", 1L, "#FF7A00")

            text.contains("amazon") || text.contains("flipkart") || text.contains("myntra") ||
                    text.contains("meesho") || text.contains("zara") || text.contains("ajio") ||
                    text.contains("nykaa") || text.contains("shopping") || text.contains("retail") ||
                    text.contains("store") ->
                findCat("Shopping", 2L, "#7C5CFC")

            text.contains("uber") || text.contains("ola") || text.contains("rapido") ||
                    text.contains("metro") || text.contains("petrol") || text.contains("fuel") ||
                    text.contains("shell") || text.contains("indian oil") || text.contains("hpcl") ||
                    text.contains("bpcl") || text.contains("flight") || text.contains("irctc") ||
                    text.contains("makemytrip") || text.contains("indigo") ->
                findCat("Travel", 3L, "#2979FF")

            text.contains("bescom") || text.contains("electricity") || text.contains("airtel") ||
                    text.contains("jio") || text.contains("vi") || text.contains("vodafone") ||
                    text.contains("broadband") || text.contains("water") || text.contains("gas") ||
                    text.contains("bill") || text.contains("recharge") || text.contains("tata play") ||
                    text.contains("dth") || text.contains("hdfc bank") || text.contains("nach") ||
                    text.contains("ach") || text.contains("cred") || text.contains("medical") ->
                findCat("Bills & Utilities", 4L, "#00C853")

            text.contains("netflix") || text.contains("spotify") || text.contains("bookmyshow") ||
                    text.contains("cinema") || text.contains("prime") || text.contains("youtube") ||
                    text.contains("pvr") || text.contains("inox") || text.contains("hotstar") ||
                    text.contains("movie") || text.contains("game") ->
                findCat("Entertainment", 5L, "#E040FB")

            text.contains("zerodha") || text.contains("groww") || text.contains("mutual") ||
                    text.contains("fund") || text.contains("sip") || text.contains("upstox") ||
                    text.contains("coin") ->
                findCat("Investment", 6L, "#00B0FF")

            else ->
                findCat("Others", 8L, "#546E7A")
        }
    }

    fun parseAmountStringToPaise(raw: String): Long? {
        val clean = raw.replace(",", "").replace("₹", "").trim()
        if (clean.isBlank()) return null
        return try {
            val bigDecimal = BigDecimal(clean)
            bigDecimal.multiply(BigDecimal(100)).toLong()
        } catch (_: Exception) {
            null
        }
    }
}

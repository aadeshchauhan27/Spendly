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

data class ParsedSmsResult(
    val transaction: Transaction,
    val sender: String,
    val rawBody: String,
    val accountEnding: String? = null,
    val availableBalancePaise: Long? = null
)

object SMSTransactionParser {

    private val UTR_REGEX = Pattern.compile("(?i)(?:upi\\s*ref(?:\\s*no)?|utr|ref\\s*no|reference(?:\\s*no)?|txn\\s*id|rrn)[:\\s-]*([A-Za-z0-9]{10,22})")
    private val STANDALONE_12_DIGIT_REGEX = Pattern.compile("\\b(\\d{12})\\b")
    private val ACCOUNT_ENDING_REGEX = Pattern.compile("(?i)(?:a/c|acct|account|card)\\s*(?:no\\.?)?\\s*(?:ending\\s+(?:with\\s+)?)?[*xX]*(\\d{4})")

    // Outgoing Debit Patterns: matches "debited by Rs 450", "paid Rs 200", "spent Rs 1,299", "sent Rs.500", "Debit INR 33015.00"
    private val DEBIT_AMOUNT_PATTERNS = listOf(
        // Pattern 1: debited/debit/spent/paid/sent followed by optional merchant then currency and amount
        Pattern.compile("(?i)(?:debited\\s+(?:by|with|for)?|debit|spent|paid|sent|transferred|withdrawn|txn\\s+of|payment\\s+of)\\s*(?:towards\\s+[A-Za-z0-9&.\\s]+?\\s+for|to\\s+[A-Za-z0-9&.\\s]+?)?\\s*(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        // Pattern 2: Currency and amount followed by debited/spent/paid/sent
        Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s+(?:debited|spent|paid|sent|transferred|withdrawn|has\\s+been\\s+debited)"),
        // Pattern 3: debited towards Merchant for INR 489.00
        Pattern.compile("(?i)(?:debited|spent)\\s+towards\\s+[A-Za-z0-9&.\\s]+?\\s+for\\s*(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        // Pattern 4: "debited by 450.00" without currency symbol
        Pattern.compile("(?i)(?:debited\\s+by|spent\\s+at|transferred)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:on|to|for|via|ref|at)")
    )

    // Incoming Credit Patterns: matches "credited with Rs 45,000", "received Rs 500", "refund of Rs 350"
    private val CREDIT_AMOUNT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:credited\\s+(?:with|by|to|for)?|received|refund\\s+of|cashback\\s+of)\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s+(?:credited|received|refunded|deposited|has\\s+been\\s+credited)"),
        Pattern.compile("(?i)(?:credited\\s+with|received)\\s+([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:on|from|via|ref|by)")
    )

    // General fallback amount pattern (only if debit/credit context was verified)
    private val GENERAL_AMOUNT_PATTERN = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)")

    // Balance pattern to avoid mistaking Avl Bal or Avbl Limit for transaction amount
    private val BALANCE_PATTERN = Pattern.compile("(?i)(?:avl(?:\\.|\\s+bal|\\s+lmt)?|available\\s+(?:balance|limit)|avbl(?:\\.|\\s+limit|\\s+lmt)?|credit\\s+limit|total\\s+limit|bal|limit|lmt)\\s*(?:is)?[:\\s-]*(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d{1,2})?)")

    /**
     * Checks if an SMS message is a promotional spam message, loan offer,
     * consent disbursement request, or non-transaction notice.
     */
    fun isSpamOrOffer(content: String): Boolean {
        val lower = content.lowercase(Locale.ENGLISH)
        return lower.contains("require consent") ||
                lower.contains("requiring consent") ||
                lower.contains("providing consent") ||
                lower.contains("with your consent") ||
                lower.contains("consent to continue") ||
                lower.contains("consent for") ||
                lower.contains("consent here") ||
                lower.contains("give consent") ||
                lower.contains("disbursement") ||
                lower.contains("disbursal") ||
                lower.contains("disburse") ||
                lower.contains("funds ready") ||
                lower.contains("funds of") ||
                lower.contains("funds are available") ||
                lower.contains("are available and") ||
                lower.contains("is available and") ||
                lower.contains("loan of") ||
                lower.contains("access loan") ||
                lower.contains("process for the loan") ||
                lower.contains("pre-approved") ||
                lower.contains("pre approved") ||
                lower.contains("preapproved") ||
                lower.contains("instant loan") ||
                lower.contains("personal loan") ||
                lower.contains("cred cash") ||
                lower.contains("credit line") ||
                lower.contains("apply now") ||
                lower.contains("eligible for") ||
                lower.contains("instant cash") ||
                lower.contains("voucher worth") ||
                lower.contains("authorize the transfer") ||
                lower.contains("authorize the") ||
                lower.contains("reminder authorize") ||
                lower.contains("convert your") ||
                lower.contains("convert to emi") ||
                lower.contains("converting outstanding") ||
                lower.contains("converted into emi") ||
                lower.contains("can be converted into") ||
                lower.contains("into emis") ||
                lower.contains("option available for converting") ||
                lower.contains("emi tenure") ||
                lower.contains("credit report") ||
                lower.contains("cibil score") ||
                lower.contains("limit preferences have been updated") ||
                lower.contains("limit enhanced") ||
                lower.contains("increase your limit") ||
                lower.contains("upgrade your card") ||
                lower.contains("special offer") ||
                lower.contains("exclusive offer") ||
                lower.contains("claim now") ||
                lower.contains("avail now") ||
                lower.contains("service alert:\nfunds") ||
                lower.contains("service alert: funds") ||
                lower.contains("service notification")
    }

    /**
     * Parses an incoming or inbox SMS message.
     * Returns null if the SMS does not contain a financial transaction.
     */
    fun parseSms(
        sender: String,
        content: String,
        messageTimestampMs: Long = System.currentTimeMillis(),
        availableCategories: List<Category> = emptyList(),
        includeSpam: Boolean = false
    ): Transaction? {
        val parsedResult = parseSmsDetailed(sender, content, messageTimestampMs, availableCategories)
        val tx = parsedResult?.transaction ?: return null
        if (tx.type == TransactionType.SPAM && !includeSpam) {
            return null
        }
        return tx
    }

    /**
     * Detailed SMS parser returning metadata like account ending and raw body.
     */
    fun parseSmsDetailed(
        sender: String,
        content: String,
        messageTimestampMs: Long = System.currentTimeMillis(),
        availableCategories: List<Category> = emptyList()
    ): ParsedSmsResult? {
        val trimmedContent = content.trim()
        val trimmedSender = sender.trim()

        val isSpam = isSpamOrOffer(trimmedContent)
        if (!isFinancialSms(trimmedContent, trimmedSender) && !isSpam) {
            return null
        }

        // 9. Extract Date if embedded in text, else fallback to SMS arrival timestamp
        val eventTimestampMs = extractDateFromText(trimmedContent) ?: messageTimestampMs

        // If detected as loan offer / promotional spam, classify into Spam category and ignore
        if (isSpam) {
            val amountPaise = extractTransactionAmount(trimmedContent, TransactionDirection.OUTGOING) ?: 0L
            val spamCategoryId = availableCategories.find { it.name.equals("Spam", ignoreCase = true) }?.id ?: 9L
            val transaction = Transaction(
                id = 0L,
                amountPaise = amountPaise,
                type = TransactionType.SPAM,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Loan Offer / Spam (Ignored)",
                counterpartyName = "Promotional Offer",
                counterpartyUpiId = null,
                categoryId = spamCategoryId,
                categoryName = "Spam",
                categoryColorHex = "#78909C",
                timestampMs = eventTimestampMs,
                paymentMethod = "Offer",
                paymentApp = null,
                bankReferenceNum = null,
                notes = "Filtered promotional loan offer - Ignored from spending",
                source = TransactionSource.SMS_IMPORT,
                rawIdentifier = "SPAM_${eventTimestampMs}_${amountPaise}_$trimmedSender"
            )
            return ParsedSmsResult(
                transaction = transaction,
                sender = trimmedSender,
                rawBody = trimmedContent,
                accountEnding = null,
                availableBalancePaise = null
            )
        }

        // 1. Determine Direction and Type
        val (direction, type) = determineDirectionAndType(trimmedContent)

        // 2. Extract Transaction Amount (distinguishing from balance)
        val amountPaise = extractTransactionAmount(trimmedContent, direction)
            ?: return null

        if (amountPaise <= 0L) {
            return null
        }

        // 3. Extract Merchant / Counterparty
        val counterparty = extractCounterparty(trimmedContent, direction)
        val normalizedMerchant = MerchantNormalizer.normalize(counterparty)

        // 4. Extract UPI Ref / UTR
        val utr = extractUtr(trimmedContent)

        // 5. Extract UPI VPA
        val upiVpa = extractUpiVpa(trimmedContent)

        // 6. Detect Payment App or Bank Channel
        val paymentApp = detectPaymentApp(trimmedSender, trimmedContent)

        // 7. Extract Account Ending (last 4 digits)
        val accountEnding = extractAccountEnding(trimmedContent)

        // 8. Extract Available Balance (if present)
        val balancePaise = extractBalance(trimmedContent)

        // 10. Category Resolution
        val (categoryId, categoryName, categoryColor) = resolveCategory(
            counterparty = normalizedMerchant,
            direction = direction,
            content = trimmedContent,
            availableCategories = availableCategories
        )

        val transaction = Transaction(
            id = 0L,
            amountPaise = amountPaise,
            type = type,
            direction = direction,
            status = TransactionStatus.COMPLETED,
            title = normalizedMerchant,
            counterpartyName = normalizedMerchant,
            counterpartyUpiId = upiVpa,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryColorHex = categoryColor,
            timestampMs = eventTimestampMs,
            paymentMethod = if (trimmedContent.contains("card", ignoreCase = true)) "Card" else "UPI",
            paymentApp = paymentApp,
            bankReferenceNum = utr,
            notes = if (accountEnding != null) "A/c ending **$accountEnding" else null,
            source = TransactionSource.SMS_IMPORT,
            rawIdentifier = "SMS_${eventTimestampMs}_${amountPaise}_${utr ?: trimmedSender}"
        )

        return ParsedSmsResult(
            transaction = transaction,
            sender = trimmedSender,
            rawBody = trimmedContent,
            accountEnding = accountEnding,
            availableBalancePaise = balancePaise
        )
    }

    private fun isFinancialSms(content: String, sender: String): Boolean {
        val lower = content.lowercase(Locale.ENGLISH)

        // 1. Strict skip: OTPs, logins, passwords, fraud verification codes
        if (lower.contains("otp") || lower.contains("one time password") ||
            lower.contains("secret otp") || lower.contains("verification code") ||
            lower.contains("do not share") || lower.contains("valid for")
        ) {
            return false
        }

        // 2. Strict skip: Loan offers, CRED cash, pre-approved limits, disbursals, promotions
        if (isSpamOrOffer(content)) {
            return false
        }

        // 3. Strict skip: Bill statements, due reminders, and standing instruction notifications
        if (lower.contains("estatement") || lower.contains("e-statement") ||
            lower.contains("statement for") || lower.contains("is generated") ||
            lower.contains("has been generated") || lower.contains("bill for your") ||
            lower.contains("total due") || lower.contains("min due") || lower.contains("min. due") ||
            lower.contains("minimum due") || lower.contains("due date") || lower.contains("due on") ||
            lower.contains("due by") || lower.contains("payment is due") ||
            lower.contains("to be debited") || lower.contains("scheduled for debit") ||
            lower.contains("standing instruction") || lower.contains("standing instructions") ||
            lower.contains("convert your") || lower.contains("convert to emi") ||
            lower.contains("emi tenure")
        ) {
            return false
        }

        // 4. Strict skip: Credit card repayment acknowledgments (paying CC bill is an account settlement, not an expense or income)
        if ((lower.contains("towards your") || lower.contains("towards")) &&
            (lower.contains("credit card") || lower.contains("card no.") || lower.contains("card xx") || lower.contains("card ending")) &&
            (lower.contains("payment") || lower.contains("received") || lower.contains("thank you"))
        ) {
            return false
        }

        // 5. Strict skip: Account balance, threshold, and valuation updates
        if (lower.contains("has gone below minimum limit") || lower.contains("yesterday's bal") ||
            lower.contains("investment value in") || lower.contains("minimum limit of")
        ) {
            return false
        }

        // 6. Strict skip: Failed, declined, returned, or mandate setups
        if (lower.contains("failed") || lower.contains("declined") ||
            lower.contains("unsuccessful") || lower.contains("was not completed") ||
            lower.contains("insufficient funds") || lower.contains("mandate set") ||
            lower.contains("mandate has been")
        ) {
            return false
        }

        // Must contain currency or debit/credit indicator
        val hasCurrency = lower.contains("rs") || lower.contains("inr") || lower.contains("₹")
        val hasDebit = lower.contains("debited") || lower.contains("debit of") || lower.contains("spent") ||
                lower.contains("paid") || lower.contains("transferred") || lower.contains("withdrawn") ||
                lower.contains("payment of") || lower.contains("sent rs") || lower.contains("sent inr")
        val hasCredit = lower.contains("credited") || lower.contains("credit alert") || lower.contains("credit of") ||
                lower.contains("has been credited") || lower.contains("received from") || lower.contains("received rs") ||
                lower.contains("deposited") || lower.contains("refund") || lower.contains("cashback")

        if (!hasCurrency && !hasDebit && !hasCredit) {
            return false
        }

        // Sender check: Indian Telecom Sender IDs are alphanumeric (e.g. "AD-HDFCBK", "VK-SBIINB", "VA-YESBNK")
        val lowerSender = sender.lowercase(Locale.ENGLISH)
        val hasBankSender = lowerSender.contains("hdfc") || lowerSender.contains("sbi") ||
                lowerSender.contains("icici") || lowerSender.contains("axis") ||
                lowerSender.contains("kotak") || lowerSender.contains("pnb") ||
                lowerSender.contains("bob") || lowerSender.contains("paytm") ||
                lowerSender.contains("phonepe") || lowerSender.contains("gpay") ||
                lowerSender.contains("cred") || lowerSender.contains("bank") ||
                lowerSender.contains("bnk") || lowerSender.contains("upi") ||
                lowerSender.contains("canara") || lowerSender.contains("union") ||
                lowerSender.contains("indus") || lowerSender.contains("yes") ||
                lowerSender.contains("idfc")

        return (hasDebit || hasCredit) && (hasCurrency || hasBankSender)
    }

    private fun determineDirectionAndType(content: String): Pair<TransactionDirection, TransactionType> {
        val lower = content.lowercase(Locale.ENGLISH)

        // Check refund & reversal
        if (lower.contains("refund") || lower.contains("reversed") || lower.contains("reversal")) {
            return TransactionDirection.INCOMING to TransactionType.REFUND
        }

        // Check salary
        if (lower.contains("salary") || lower.contains("payroll")) {
            return TransactionDirection.INCOMING to TransactionType.INCOME
        }

        // Check genuine incoming credit (do not match bare "credit" which matches "credit card"!)
        val hasActualCredit = lower.contains("credited") ||
                lower.contains("received from") || lower.contains("received rs") ||
                lower.contains("deposited") || lower.contains("cashback")

        val hasDebit = lower.contains("debited") || lower.contains("debit") ||
                lower.contains("spent") || lower.contains("paid") ||
                lower.contains("sent rs") || lower.contains("transferred to") ||
                lower.contains("withdrawn")

        return if (hasActualCredit && !hasDebit) {
            TransactionDirection.INCOMING to TransactionType.INCOME
        } else {
            TransactionDirection.OUTGOING to TransactionType.EXPENSE
        }
    }

    private fun extractTransactionAmount(content: String, direction: TransactionDirection): Long? {
        val patterns = if (direction == TransactionDirection.OUTGOING) {
            DEBIT_AMOUNT_PATTERNS
        } else {
            CREDIT_AMOUNT_PATTERNS
        }

        // Try directional patterns first
        for (pattern in patterns) {
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val amtStr = matcher.group(1) ?: continue
                val parsed = parseAmountStringToPaise(amtStr)
                if (parsed != null && parsed > 0L) {
                    return parsed
                }
            }
        }

        // Fallback: general currency pattern (make sure it doesn't match the Avl Bal)
        val generalMatcher = GENERAL_AMOUNT_PATTERN.matcher(content)
        val balancePaise = extractBalance(content)

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

    private fun extractCounterparty(content: String, direction: TransactionDirection): String {
        // 1. Try @UPI_MERCHANT or @MERCHANT (common in YES Bank, IndusInd, etc., e.g. "@UPI_ZOMATO MEDIA PRIVA")
        val atMatcher = Pattern.compile("(?i)@(?:UPI_)?([A-Za-z0-9&.\\s]+?)(?:\\s+\\d{1,2}[-/]|\\s+on|\\s+via|\\s+using|\\.|$)").matcher(content)
        if (atMatcher.find()) {
            val name = atMatcher.group(1)?.trim()
            if (!name.isNullOrBlank()) {
                val cleaned = cleanExtractedMerchant(name)
                if (cleaned.isNotBlank() && cleaned.length >= 2) return cleaned
            }
        }

        // 2. Try UPI narration patterns:
        // Format A & B: UPI/(CR|DR)/<ref>/<merchant>/<bank> (e.g. "UPI/CR/429812345679/AMAZON/ICICI")
        val upiCrDrMatcher = Pattern.compile("(?i)UPI/(?:CR|DR)/[A-Za-z0-9]+/([A-Za-z0-9&.\\s]+?)/").matcher(content)
        if (upiCrDrMatcher.find()) {
            val name = upiCrDrMatcher.group(1)?.trim()
            if (!name.isNullOrBlank()) {
                val cleaned = cleanExtractedMerchant(name)
                if (cleaned.isNotBlank() && cleaned.length >= 2) return cleaned
            }
        }

        // Format C & D: UPI/(P2A|P2M)/<ref>/<merchant> (e.g. "UPI/P2A/629971948458/ANGAD BIHARI")
        val upiP2Matcher = Pattern.compile("(?i)UPI/(?:P2A|P2M)/[A-Za-z0-9]+/([A-Za-z0-9&.\\s]+?)(?:/|\\r?\\n|\\.|$)").matcher(content)
        if (upiP2Matcher.find()) {
            val name = upiP2Matcher.group(1)?.trim()
            if (!name.isNullOrBlank()) {
                val cleaned = cleanExtractedMerchant(name)
                if (cleaned.isNotBlank() && cleaned.length >= 2) return cleaned
            }
        }

        // Format E: UPI/<ref>/<merchant>/ (e.g. "UPI/429812/SWIGGY/")
        val upiGeneralMatcher = Pattern.compile("(?i)UPI/[A-Za-z0-9]+/([A-Za-z0-9&.\\s]+?)/").matcher(content)
        if (upiGeneralMatcher.find()) {
            val name = upiGeneralMatcher.group(1)?.trim()
            if (!name.isNullOrBlank()) {
                val cleaned = cleanExtractedMerchant(name)
                if (cleaned.isNotBlank() && cleaned.length >= 2) return cleaned
            }
        }

        val upiDashMatcher = Pattern.compile("(?i)UPI-([A-Za-z0-9&.\\s]+?)-").matcher(content)
        if (upiDashMatcher.find()) {
            val name = upiDashMatcher.group(1)?.trim()
            if (!name.isNullOrBlank()) return name
        }

        // 3. Try ACH / NACH pattern (e.g. "ACH-DR-HDFC BANK LTD-48116" or "towards HDFC BANK LTD")
        val achMatcher = Pattern.compile("(?i)(?:ACH|NACH)-(?:DR|CR)-([A-Za-z0-9&.\\s]+?)-\\d+").matcher(content)
        if (achMatcher.find()) {
            val name = achMatcher.group(1)?.trim()
            if (!name.isNullOrBlank()) return cleanExtractedMerchant(name)
        }

        // 3b. Try 'at' merchant location (e.g. "at UPI BLINKIT", "at FLIPKART PA", "at ZOMATO LTD")
        val atLocationMatcher = Pattern.compile("(?i)\\bat\\s+(?:UPI[\\s_]+)?([A-Za-z0-9&.\\s]+?)(?:\\s+on\\s+|\\s+via|\\s+using|\\s+avl|\\s+bal|\\.|$)").matcher(content)
        if (atLocationMatcher.find()) {
            val name = atLocationMatcher.group(1)?.trim()
            if (!name.isNullOrBlank()) {
                val cleaned = cleanExtractedMerchant(name)
                if (cleaned.isNotBlank() && cleaned.length >= 2 && !cleaned.lowercase(Locale.ENGLISH).contains("card")) {
                    return cleaned
                }
            }
        }

        // 3c. Try 'towards' or 'NACH debit towards' (e.g. "towards HDFC BANK LTD", "towards Google")
        val towardsMatcher = Pattern.compile("(?i)(?:towards|NACH debit towards)\\s+([A-Za-z0-9&.\\s]+?)(?:\\s+for\\s+(?:rs|inr|₹)|\\s+for\\s+|\\s+with\\s+|\\s+on\\s+|\\.|$)").matcher(content)
        if (towardsMatcher.find()) {
            val name = towardsMatcher.group(1)?.trim()
            if (!name.isNullOrBlank()) {
                val cleaned = cleanExtractedMerchant(name)
                if (cleaned.isNotBlank() && cleaned.length >= 2) return cleaned
            }
        }

        // 3d. For incoming NEFT credits
        if (direction == TransactionDirection.INCOMING) {
            val neftMatcher = Pattern.compile("(?i)Ref:\\s*NEFT/[^/]+/([A-Za-z0-9&.\\s]+?)(?:\\.|$)").matcher(content)
            if (neftMatcher.find()) {
                val name = neftMatcher.group(1)?.trim()
                if (!name.isNullOrBlank()) {
                    val cleaned = cleanExtractedMerchant(name)
                    if (cleaned.isNotBlank() && cleaned.length >= 2) return cleaned
                }
            }
            if (content.contains("credited to", ignoreCase = true) && content.contains("neft", ignoreCase = true)) {
                return "Salary / NEFT Credit"
            }
        }

        // 4. Try preposition patterns: "towards Google for INR 489", "to SWIGGY", "at STARBUCKS", "from RAJESH"
        val prepPattern = if (direction == TransactionDirection.OUTGOING) {
            Pattern.compile("(?i)(?:towards|to|at|info:|for|on)\\s+([A-Za-z0-9&.\\s]+?)(?:\\s+for\\s+(?:rs|inr|₹)|\\s+on\\s+\\d{1,2}[-/]|\\s+via|\\s+using|\\s+upi|\\s+ref|\\s+avl|\\s+bal|\\.|$)")
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

        // 5. Fallback: Check known Indian merchants & services in text
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
            .replace(Pattern.compile("(?i)\\s+(?:ref|utr|rrn|no|media\\s+priva|pvt\\s+ltd|ltd).*$").toRegex(), "")
            .trim()

        // Reject if it is a phone number, digit sequence, or generic stopword
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

    private fun resolveCategory(
        counterparty: String,
        direction: TransactionDirection,
        content: String,
        availableCategories: List<Category>
    ): Triple<Long, String, String> {
        if (direction == TransactionDirection.INCOMING) {
            val incomeCat = availableCategories.find { it.type == "INCOME" || it.id == 7L }
            return Triple(
                incomeCat?.id ?: 7L,
                incomeCat?.name ?: "Salary",
                incomeCat?.colorHex ?: "#00E676"
            )
        }

        val text = (counterparty + " " + content).lowercase(Locale.ENGLISH)

        fun findCat(name: String, fallbackId: Long, fallbackColor: String): Triple<Long, String, String> {
            val found = availableCategories.find { it.name.contains(name, ignoreCase = true) }
            return Triple(
                found?.id ?: fallbackId,
                found?.name ?: name,
                found?.colorHex ?: fallbackColor
            )
        }

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

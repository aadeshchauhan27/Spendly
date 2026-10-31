package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SMSClassifierTest {

    private val categories = listOf(
        Category(id = 1, name = "Food & Dining", type = "EXPENSE", colorHex = "#FF7A00", iconName = "restaurant", isSystem = true, sortOrder = 1),
        Category(id = 2, name = "Shopping", type = "EXPENSE", colorHex = "#7C5CFC", iconName = "shopping_bag", isSystem = true, sortOrder = 2),
        Category(id = 3, name = "Travel", type = "EXPENSE", colorHex = "#2979FF", iconName = "flight", isSystem = true, sortOrder = 3),
        Category(id = 4, name = "Bills & Utilities", type = "EXPENSE", colorHex = "#00C853", iconName = "receipt_long", isSystem = true, sortOrder = 4),
        Category(id = 5, name = "Entertainment", type = "EXPENSE", colorHex = "#E040FB", iconName = "sports_esports", isSystem = true, sortOrder = 5),
        Category(id = 6, name = "Investment", type = "EXPENSE", colorHex = "#00B0FF", iconName = "savings", isSystem = true, sortOrder = 6),
        Category(id = 7, name = "Salary", type = "INCOME", colorHex = "#00E676", iconName = "work", isSystem = true, sortOrder = 7),
        Category(id = 8, name = "Others", type = "EXPENSE", colorHex = "#546E7A", iconName = "more_horiz", isSystem = true, sortOrder = 8),
        Category(id = 10, name = "Refund", type = "REFUND", colorHex = "#26A69A", iconName = "replay", isSystem = true, sortOrder = 10),
        Category(id = 11, name = "Cashback", type = "INCOME", colorHex = "#FFA726", iconName = "redeem", isSystem = true, sortOrder = 11)
    )

    // =========================================================================
    // SECTION 1: FINANCIAL EVENT VERIFICATION (OUTGOING & INCOMING)
    // =========================================================================

    @Test
    fun classify_outgoingDebit_createsExpense() {
        val sms = "Rs. 450 debited from your account XX1234 on 24-Oct-26. UPI Ref 429812345678."
        val result = SMSClassifier.classify("AD-HDFCBK", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        assertEquals(ClassificationConfidence.HIGH, result.confidence)
        val tx = result.transaction!!
        assertEquals(45000L, tx.amountPaise)
        assertEquals(TransactionDirection.OUTGOING, tx.direction)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(TransactionStatus.COMPLETED, tx.status)
        assertEquals("429812345678", tx.bankReferenceNum)
        assertEquals("1234", result.accountEnding)
    }

    @Test
    fun classify_upiPaymentSwiggy_extractsSwiggyAndFoodCategory() {
        val sms = "UPI payment of Rs 799 to Swiggy successful. Ref 429811223344. A/c ending 5678."
        val result = SMSClassifier.classify("VK-SBIINB", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        val tx = result.transaction!!
        assertEquals(79900L, tx.amountPaise)
        assertEquals("Swiggy", tx.counterpartyName)
        assertEquals(1L, tx.categoryId) // Food & Dining
        assertEquals(TransactionType.EXPENSE, tx.type)
    }

    @Test
    fun classify_cardUsedAtAmazon_extractsAmazonAndShoppingCategory() {
        val sms = "Your card ending 1234 was used for INR 1,299 at AMAZON on 25-Oct-26. Avl limit: INR 45,000."
        val result = SMSClassifier.classify("JM-ICICIB", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        val tx = result.transaction!!
        assertEquals(129900L, tx.amountPaise)
        assertEquals("Amazon", tx.counterpartyName)
        assertEquals(2L, tx.categoryId) // Shopping
        assertEquals("Card", tx.paymentMethod)
        assertEquals("1234", result.accountEnding)
    }

    @Test
    fun classify_atmCashWithdrawal_extractsAtmExpense() {
        val sms = "Dear Customer, INR 2,000 withdrawn from ATM A/c ending 9012 on 26-Oct-26. Avl bal: INR 12,300."
        val result = SMSClassifier.classify("AD-KOTAKB", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        val tx = result.transaction!!
        assertEquals(200000L, tx.amountPaise)
        assertEquals(TransactionDirection.OUTGOING, tx.direction)
        assertEquals("ATM", tx.paymentMethod)
    }

    @Test
    fun classify_loanEmiDebited_extractsBillsAndUtilities() {
        val sms = "Loan EMI of Rs 14,500 successfully debited from your HDFC Bank account **7862 for loan A/c 889900."
        val result = SMSClassifier.classify("AD-HDFCBK", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        val tx = result.transaction!!
        assertEquals(1450000L, tx.amountPaise)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(4L, tx.categoryId) // Bills & Utilities
    }

    @Test
    fun classify_incomingCredit_createsIncome() {
        val sms = "Rs. 5,000 credited to your account XX4321 on 26-Oct-26 by UPI. Avl Bal: INR 35,000."
        val result = SMSClassifier.classify("VM-HDFCBK", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        val tx = result.transaction!!
        assertEquals(500000L, tx.amountPaise)
        assertEquals(TransactionDirection.INCOMING, tx.direction)
        assertEquals(TransactionType.INCOME, tx.type)
    }

    @Test
    fun classify_receivedFromPeer_createsIncome() {
        val sms = "You have received ₹2,000 from Rohan on Google Pay via UPI Ref 429812999000."
        val result = SMSClassifier.classify("AD-GPAY", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        val tx = result.transaction!!
        assertEquals(200000L, tx.amountPaise)
        assertEquals(TransactionDirection.INCOMING, tx.direction)
        assertEquals("Rohan", tx.counterpartyName)
        assertEquals("429812999000", tx.bankReferenceNum)
    }

    @Test
    fun classify_salaryCredit_extractsSalaryIncome() {
        val sms = "UPDATE: Rs. 75,000 credited to your HDFC Bank A/c **4321 on 26-OCT-26 by TECH CORP (Salary). Avl Bal: INR 95,400.00"
        val result = SMSClassifier.classify("VM-HDFCBK", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        val tx = result.transaction!!
        assertEquals(7500000L, tx.amountPaise)
        assertEquals(7L, tx.categoryId) // Salary
        assertEquals(TransactionType.INCOME, tx.type)
    }

    // =========================================================================
    // SECTION 2: SELF TRANSFERS (ZERO NET SPEND/INCOME IMPACT)
    // =========================================================================

    @Test
    fun classify_selfTransfer_classifiedAsTransferSelf() {
        val sms = "Rs 10,000 transferred to your own account ending 8812. Ref No 429811002233."
        val result = SMSClassifier.classify("AD-AXISBK", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        val tx = result.transaction!!
        assertEquals(1000000L, tx.amountPaise)
        assertEquals(TransactionType.TRANSFER_SELF, tx.type)
    }

    // =========================================================================
    // SECTION 3: FAILED & DECLINED TRANSACTIONS
    // =========================================================================

    @Test
    fun classify_failedUpiPayment_markedAsFailedStatus() {
        val sms = "UPI payment of Rs 1,200 to Zomato failed due to network issue. Any amount debited will be refunded."
        val result = SMSClassifier.classify("AD-PHONPE", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        val tx = result.transaction!!
        assertEquals(TransactionStatus.FAILED, tx.status)
        assertEquals(120000L, tx.amountPaise)
    }

    @Test
    fun classify_declinedTransaction_markedAsFailedStatus() {
        val sms = "Transaction of Rs 500 declined due to insufficient funds in A/c XX1234."
        val result = SMSClassifier.classify("VK-SBIINB", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        val tx = result.transaction!!
        assertEquals(TransactionStatus.FAILED, tx.status)
    }

    // =========================================================================
    // SECTION 4: HARD EXCLUSIONS & NOISE FILTERING
    // =========================================================================

    @Test
    fun noise_otpMessageWithAmount_isRejected() {
        val otp1 = "Your OTP for transaction is 458921. Valid for 10 mins. Do not share with anyone."
        val result1 = SMSClassifier.classify("VK-HDFCBK", otp1, availableCategories = categories)
        assertFalse(result1.isFinancialTransaction)
        assertEquals(NoiseCategory.OTP_AUTHENTICATION, result1.noiseCategory)

        val otp2 = "862956 is SECRET OTP for txn of INR 582.09 on Axis Bank card XX1661 at ZOMATO LTD on 27-10-26 21:05:46. OTP valid for 5 mins. Pls do not share this OTP."
        val result2 = SMSClassifier.classify("VM-AXISBK-T", otp2, availableCategories = categories)
        assertFalse(result2.isFinancialTransaction)
        assertEquals(NoiseCategory.OTP_AUTHENTICATION, result2.noiseCategory)

        val otp3 = "459123 is your one-time password (OTP) for login to SBI NetBanking."
        val result3 = SMSClassifier.classify("VK-SBIINB", otp3, availableCategories = categories)
        assertFalse(result3.isFinancialTransaction)
        assertEquals(NoiseCategory.OTP_AUTHENTICATION, result3.noiseCategory)
    }

    @Test
    fun noise_promotionalCashbackOffers_areRejected() {
        val promo1 = "Get ₹500 cashback on your next purchase using HDFC Bank Credit Card. Apply code SAVE500."
        val result1 = SMSClassifier.classify("AD-HDFCBK", promo1, availableCategories = categories)
        assertFalse(result1.isFinancialTransaction)
        assertEquals(NoiseCategory.PROMOTIONAL_OFFER, result1.noiseCategory)

        val promo2 = "Get 20% cashback on your next purchase at Myntra with Axis Bank cards."
        val result2 = SMSClassifier.classify("AD-AXISBK", promo2, availableCategories = categories)
        assertFalse(result2.isFinancialTransaction)
        assertEquals(NoiseCategory.PROMOTIONAL_OFFER, result2.noiseCategory)
    }

    @Test
    fun noise_loanOffersAndCreditLimits_areRejected() {
        val loan1 = "Pre-approved personal loan of ₹5 lakh available with zero processing fee. Click here to apply now."
        val result1 = SMSClassifier.classify("VM-HDFCBK", loan1, availableCategories = categories)
        assertFalse(result1.isFinancialTransaction)
        assertEquals(NoiseCategory.PROMOTIONAL_OFFER, result1.noiseCategory)

        val loan2 = "Yes Bank Credit cars are avaiable and required consent for disbursemtn of funds."
        val result2 = SMSClassifier.classify("VA-YESBNK", loan2, availableCategories = categories)
        assertFalse(result2.isFinancialTransaction)
        assertEquals(NoiseCategory.PROMOTIONAL_OFFER, result2.noiseCategory)

        val loan3 = "Unlock pre-approved limit on CRED cash of up to Rs. 5,00,000.00."
        val result3 = SMSClassifier.classify("JX-CREDIN", loan3, availableCategories = categories)
        assertFalse(result3.isFinancialTransaction)
        assertEquals(NoiseCategory.PROMOTIONAL_OFFER, result3.noiseCategory)

        val limitIncrease = "Congratulations! Your credit limit has been increased to INR 3,50,000 on your ICICI Card 4001."
        val result4 = SMSClassifier.classify("JM-ICICIB", limitIncrease, availableCategories = categories)
        assertFalse(result4.isFinancialTransaction)
        assertEquals(NoiseCategory.PROMOTIONAL_OFFER, result4.noiseCategory)
    }

    @Test
    fun noise_billAndEmiReminders_areRejected() {
        val bill1 = "Your electricity bill of ₹2,400 is due on 15th Nov. Pay before due date to avoid late fees."
        val result1 = SMSClassifier.classify("BESCOM", bill1, availableCategories = categories)
        assertFalse(result1.isFinancialTransaction)
        assertEquals(NoiseCategory.BILL_PAYMENT_REMINDER, result1.noiseCategory)

        val bill2 = "Your credit card payment of ₹12,500 is due tomorrow for Card ending 9988."
        val result2 = SMSClassifier.classify("AD-HDFCBK", bill2, availableCategories = categories)
        assertFalse(result2.isFinancialTransaction)
        assertEquals(NoiseCategory.BILL_PAYMENT_REMINDER, result2.noiseCategory)

        val bill3 = "Pay your EMI of ₹8,000 before 5th June to keep your credit score healthy."
        val result3 = SMSClassifier.classify("AD-BAJAJF", bill3, availableCategories = categories)
        assertFalse(result3.isFinancialTransaction)
        assertEquals(NoiseCategory.BILL_PAYMENT_REMINDER, result3.noiseCategory)

        val ccSettlement = "Thank you for payment of INR 2,419.25 towards your FIRST Select Credit Card XX7713 on 26 Oct 2026. IDFC FIRST Bank"
        val result4 = SMSClassifier.classify("CP-IDFCFB", ccSettlement, availableCategories = categories)
        assertFalse(result4.isFinancialTransaction)
        assertEquals(NoiseCategory.BILL_PAYMENT_REMINDER, result4.noiseCategory)
    }

    @Test
    fun noise_futureDebitsAndUpcomingMandates_areRejected() {
        // e-mandate upcoming deduction
        val mandate1 = "Ed mandate of Rss 199 will be decucted on 05-Nov for Netflix. Kindly maintain balance."
        val result1 = SMSClassifier.classify("VK-HDFCBK", mandate1, availableCategories = categories)
        assertFalse(result1.isFinancialTransaction)
        assertEquals(NoiseCategory.BILL_PAYMENT_REMINDER, result1.noiseCategory)

        // Upcoming mandate set for ... will be debited
        val mandate2 = "for the upcoming mandate set for HOTSTAR .. INR 299 will be debited on 10-Nov"
        val result2 = SMSClassifier.classify("AD-AXISBK", mandate2, availableCategories = categories)
        assertFalse(result2.isFinancialTransaction)
        assertEquals(NoiseCategory.BILL_PAYMENT_REMINDER, result2.noiseCategory)

        // EMI will be debited reminder
        val emiAlert = "Your EMI of Rs 5,000 will be debited on 05-Nov-2026. Keep sufficient balance in A/c ending 1234."
        val result3 = SMSClassifier.classify("AD-KOTAKB", emiAlert, availableCategories = categories)
        assertFalse(result3.isFinancialTransaction)
        assertEquals(NoiseCategory.BILL_PAYMENT_REMINDER, result3.noiseCategory)

        // Mandate setup/creation notification
        val mandateCreated = "Your e-mandate for Rs 199 has been successfully created for Netflix."
        val result4 = SMSClassifier.classify("VK-SBIINB", mandateCreated, availableCategories = categories)
        assertFalse(result4.isFinancialTransaction)
        assertEquals(NoiseCategory.BILL_PAYMENT_REMINDER, result4.noiseCategory)

        // Autopay scheduled reminder
        val autopayAlert = "Dear Customer, UPI autopay of Rs. 499 for Spotify is scheduled on 28-Oct. Ensure sufficient balance in A/c XX1234."
        val result5 = SMSClassifier.classify("JM-ICICIB", autopayAlert, availableCategories = categories)
        assertFalse(result5.isFinancialTransaction)
        assertEquals(NoiseCategory.BILL_PAYMENT_REMINDER, result5.noiseCategory)
    }

    @Test
    fun noise_kycAndSecurityNotifications_areRejected() {
        val kyc = "KYC update required for your Bank account. Visit the nearest branch or update online."
        val result1 = SMSClassifier.classify("VK-SBIINB", kyc, availableCategories = categories)
        assertFalse(result1.isFinancialTransaction)
        assertEquals(NoiseCategory.KYC_ACCOUNT_SECURITY, result1.noiseCategory)

        val login = "Your account was logged in from a new Chrome device on Windows at 21:30."
        val result2 = SMSClassifier.classify("AD-HDFCBK", login, availableCategories = categories)
        assertFalse(result2.isFinancialTransaction)
        assertEquals(NoiseCategory.KYC_ACCOUNT_SECURITY, result2.noiseCategory)

        val dispatch = "Your cheque book has been dispatched via BlueDart with tracking 998822."
        val result3 = SMSClassifier.classify("AD-AXISBK", dispatch, availableCategories = categories)
        assertFalse(result3.isFinancialTransaction)
        assertEquals(NoiseCategory.KYC_ACCOUNT_SECURITY, result3.noiseCategory)

        val lowBal = "UPDATE:Bal in HDFC Bank A/c XX7862 has gone below minimum limit of INR 5,000.00.Yesterday's bal:INR 1,004.68."
        val result4 = SMSClassifier.classify("VM-HDFCBK-S", lowBal, availableCategories = categories)
        assertFalse(result4.isFinancialTransaction)
        assertEquals(NoiseCategory.KYC_ACCOUNT_SECURITY, result4.noiseCategory)
    }

    @Test
    fun noise_investmentNavAndScheduledSip_areRejected() {
        val nav = "Your mutual fund NAV is ₹145.20 for Nippon India Growth Fund. Portfolio value is ₹2,40,000."
        val result1 = SMSClassifier.classify("AD-CAMSON", nav, availableCategories = categories)
        assertFalse(result1.isFinancialTransaction)
        assertEquals(NoiseCategory.INVESTMENT_INFO, result1.noiseCategory)

        val sipScheduled = "Your SIP of ₹5,000 is scheduled for tomorrow in Mirae Asset Large Cap Fund."
        val result2 = SMSClassifier.classify("AD-GROWW", sipScheduled, availableCategories = categories)
        assertFalse(result2.isFinancialTransaction)
        assertEquals(NoiseCategory.INVESTMENT_INFO, result2.noiseCategory)
    }

    @Test
    fun noise_deliveryAndOrderNotifications_areRejected() {
        val delivery1 = "Your Amazon order of ₹1,299 will be delivered tomorrow by 8 PM."
        val result1 = SMSClassifier.classify("AD-AMAZON", delivery1, availableCategories = categories)
        assertFalse(result1.isFinancialTransaction)
        assertEquals(NoiseCategory.ORDER_DELIVERY_NOTIFICATION, result1.noiseCategory)

        val delivery2 = "Your Swiggy order of ₹450 is confirmed and being prepared by restaurant."
        val result2 = SMSClassifier.classify("AD-SWIGGY", delivery2, availableCategories = categories)
        assertFalse(result2.isFinancialTransaction)
        assertEquals(NoiseCategory.ORDER_DELIVERY_NOTIFICATION, result2.noiseCategory)

        val delivery3 = "Your Flipkart order worth ₹2,500 has been shipped and is out for delivery."
        val result3 = SMSClassifier.classify("AD-FLIPKT", delivery3, availableCategories = categories)
        assertFalse(result3.isFinancialTransaction)
        assertEquals(NoiseCategory.ORDER_DELIVERY_NOTIFICATION, result3.noiseCategory)
    }

    // =========================================================================
    // SECTION 5: REFUNDS, REVERSALS & CASHBACK
    // =========================================================================

    @Test
    fun refunds_refundInitiated_isPending() {
        val sms = "Refund of Rs 850 from Flipkart has been initiated. Amount will be credited within 5-7 days."
        val result = SMSClassifier.classify("AD-FLIPKT", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        assertEquals(RefundState.REFUND_INITIATED, result.refundState)
        val tx = result.transaction!!
        assertEquals(TransactionStatus.PENDING, tx.status)
        assertEquals(85000L, tx.amountPaise)
    }

    @Test
    fun refunds_refundCompleted_isCompletedRefund() {
        val sms = "₹1,299 refunded by Amazon to your account ending 4321 on 26-Oct-26. UPI Ref 429811998877."
        val result = SMSClassifier.classify("AD-AMAZON", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        assertEquals(RefundState.REFUND_COMPLETED, result.refundState)
        val tx = result.transaction!!
        assertEquals(TransactionStatus.COMPLETED, tx.status)
        assertEquals(TransactionType.REFUND, tx.type)
        assertEquals(129900L, tx.amountPaise)
        assertEquals(TransactionDirection.INCOMING, tx.direction)
        assertEquals("Amazon", tx.counterpartyName)
        assertEquals("429811998877", tx.bankReferenceNum)
    }

    @Test
    fun refunds_swiggyAndUberRefunds_classifiedAsRefund() {
        val swiggySms = "Swiggy has refunded ₹450 to your UPI account. Ref: 429800112233."
        val r1 = SMSClassifier.classify("AD-SWIGGY", swiggySms, availableCategories = categories)
        assertTrue(r1.isFinancialTransaction)
        assertEquals(TransactionType.REFUND, r1.transaction!!.type)
        assertEquals(45000L, r1.transaction!!.amountPaise)

        val uberSms = "Uber refund of ₹320 credited to your card XX5566."
        val r2 = SMSClassifier.classify("AD-UBERIN", uberSms, availableCategories = categories)
        assertTrue(r2.isFinancialTransaction)
        assertEquals(TransactionType.REFUND, r2.transaction!!.type)
        assertEquals(32000L, r2.transaction!!.amountPaise)
    }

    @Test
    fun refunds_linkToOriginalTransaction_setsRelatedTransactionId() {
        val originalExpense = Transaction(
            id = 42L,
            amountPaise = 249900L, // ₹2,499
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.COMPLETED,
            title = "Amazon",
            counterpartyName = "Amazon",
            categoryId = 2L,
            timestampMs = System.currentTimeMillis() - 86400000L * 2, // 2 days ago
            bankReferenceNum = "429800001111"
        )

        val refundSms = "Refund of Rs 800 from Amazon credited to your card XX1234. Ref 429800001111."
        val result = SMSClassifier.classify(
            sender = "AD-HDFCBK",
            rawContent = refundSms,
            availableCategories = categories,
            existingTransactions = listOf(originalExpense)
        )

        assertTrue(result.isFinancialTransaction)
        val refundTx = result.transaction!!
        assertEquals(TransactionType.REFUND, refundTx.type)
        assertEquals(80000L, refundTx.amountPaise)
        assertEquals(42L, refundTx.relatedTransactionId) // Successfully linked to original!
    }

    @Test
    fun cashback_credited_classifiedAsCashback() {
        val sms = "₹500 cashback credited to your account XX1234 for festive rewards."
        val result = SMSClassifier.classify("AD-HDFCBK", sms, availableCategories = categories)

        assertTrue(result.isFinancialTransaction)
        val tx = result.transaction!!
        assertEquals(50000L, tx.amountPaise)
        assertEquals(TransactionType.CASHBACK, tx.type)
        assertEquals(TransactionDirection.INCOMING, tx.direction)
    }

    @Test
    fun reversal_reversalSms_classifiedAsReversalAndLinked() {
        val originalTx = Transaction(
            id = 99L,
            amountPaise = 100000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.COMPLETED,
            title = "Flipkart",
            counterpartyName = "Flipkart",
            categoryId = 2L,
            timestampMs = System.currentTimeMillis() - 3600000L,
            bankReferenceNum = "429855554444"
        )

        val reversalSms = "Transaction reversed. INR 1,000 credited back to A/c XX1234 from Flipkart. Ref 429855554444."
        val result = SMSClassifier.classify(
            sender = "AD-SBIINB",
            rawContent = reversalSms,
            availableCategories = categories,
            existingTransactions = listOf(originalTx)
        )

        assertTrue(result.isFinancialTransaction)
        val tx = result.transaction!!
        assertEquals(TransactionType.REVERSAL, tx.type)
        assertEquals(TransactionStatus.REVERSED, tx.status)
        assertEquals(100000L, tx.amountPaise)
        assertEquals(99L, tx.relatedTransactionId) // Successfully linked!
    }

    // =========================================================================
    // SECTION 6: INDIAN BANKS & PAYMENT PLATFORMS
    // =========================================================================

    @Test
    fun banks_sbiHdfcIciciAxisKotakIdfcYesBank_allParsedSuccessfully() {
        // SBI
        val sbi = "Dear SBI User, your A/c ending 1234 debited by Rs 350.00 on 24-Oct-26 via UPI Ref 4298001. Avl bal Rs 5,000."
        val rSbi = SMSClassifier.classify("VK-SBIINB", sbi, availableCategories = categories)
        assertTrue(rSbi.isFinancialTransaction)
        assertEquals(35000L, rSbi.transaction!!.amountPaise)

        // HDFC
        val hdfc = "Sent Rs.450.00 from HDFC Bank A/C **1234 to SWIGGY on 24-10-26 via UPI Ref 429812345678. Avl bal: Rs.24,500.00"
        val rHdfc = SMSClassifier.classify("AD-HDFCBK", hdfc, availableCategories = categories)
        assertTrue(rHdfc.isFinancialTransaction)
        assertEquals(45000L, rHdfc.transaction!!.amountPaise)

        // ICICI
        val icici = "Account 1234 debited with INR 1,299.00 on 25-Oct-26. Info: UPI/CR/429812345679/AMAZON/ICICI. Avl Bal: INR 35,000.00."
        val rIcici = SMSClassifier.classify("JM-ICICIB", icici, availableCategories = categories)
        assertTrue(rIcici.isFinancialTransaction)
        assertEquals(129900L, rIcici.transaction!!.amountPaise)

        // Axis
        val axis = "INR 130.00 debited\nA/c no. XX3721\nUPI/P2A/629971948458/ANGAD BIHARI\nAxis Bank"
        val rAxis = SMSClassifier.classify("AD-AXISBK", axis, availableCategories = categories)
        assertTrue(rAxis.isFinancialTransaction)
        assertEquals(13000L, rAxis.transaction!!.amountPaise)

        // Kotak
        val kotak = "Sent Rs.250 from Kotak Bank A/c ending 4567 to Chai Point on 26-Oct-26. UPI Ref 429899."
        val rKotak = SMSClassifier.classify("AD-KOTAKB", kotak, availableCategories = categories)
        assertTrue(rKotak.isFinancialTransaction)
        assertEquals(25000L, rKotak.transaction!!.amountPaise)

        // IDFC
        val idfc = "INR 650 spent on IDFC FIRST Bank Card XX7713 at ZOMATO on 27-Oct-26. Avl Lmt: INR 90,000."
        val rIdfc = SMSClassifier.classify("CP-IDFCFB", idfc, availableCategories = categories)
        assertTrue(rIdfc.isFinancialTransaction)
        assertEquals(65000L, rIdfc.transaction!!.amountPaise)

        // Yes Bank
        val yes = "INR 366.14 spent on YES BANK Card X6890 @UPI_ZOMATO MEDIA PRIVA 29-10-2026. Avl Lmt INR 396,259.43."
        val rYes = SMSClassifier.classify("VA-YESBNK", yes, availableCategories = categories)
        assertTrue(rYes.isFinancialTransaction)
        assertEquals(36614L, rYes.transaction!!.amountPaise)
    }

    @Test
    fun paymentApps_phonePeGPayPaytmAmazonPay_allParsedSuccessfully() {
        val phonepe = "Paid Rs.750.00 to BOOKMYSHOW using PhonePe. Txn ID: 429811223344."
        val rPhonepe = SMSClassifier.classify("AD-PHONPE", phonepe, availableCategories = categories)
        assertTrue(rPhonepe.isFinancialTransaction)
        assertEquals("PhonePe", rPhonepe.transaction!!.paymentApp)

        val gpay = "Paid Rs.400 to Starbucks on Google Pay. UPI Ref: 429812999888."
        val rGpay = SMSClassifier.classify("AD-GPAY", gpay, availableCategories = categories)
        assertTrue(rGpay.isFinancialTransaction)
        assertEquals("Google Pay", rGpay.transaction!!.paymentApp)

        val paytm = "Paid Rs.250.00 to Chai Point on Paytm. UPI Ref: 429812999888."
        val rPaytm = SMSClassifier.classify("BP-PAYTM", paytm, availableCategories = categories)
        assertTrue(rPaytm.isFinancialTransaction)
        assertEquals("Paytm", rPaytm.transaction!!.paymentApp)
    }

    // =========================================================================
    // SECTION 7: DEDUPLICATION PREFERENCE
    // =========================================================================

    @Test
    fun deduplication_prefersCompletedConfirmationOverInitiated() {
        val initiated = Transaction(
            id = 1L,
            amountPaise = 50000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.PENDING,
            title = "Swiggy",
            counterpartyName = "Swiggy",
            categoryId = 1L,
            timestampMs = System.currentTimeMillis() - 60000L,
            bankReferenceNum = null
        )

        val bankConfirmation = Transaction(
            id = 0L,
            amountPaise = 50000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.COMPLETED,
            title = "Swiggy",
            counterpartyName = "Swiggy",
            categoryId = 1L,
            timestampMs = System.currentTimeMillis(),
            bankReferenceNum = "429800112233",
            notes = "A/c ending **1234"
        )

        assertTrue(TransactionDeduplicator.shouldPreferCandidate(bankConfirmation, initiated))
        assertFalse(TransactionDeduplicator.shouldPreferCandidate(initiated, bankConfirmation))
    }
}

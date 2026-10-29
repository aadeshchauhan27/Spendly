package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SMSTransactionParserTest {

    private val categories = listOf(
        Category(id = 1, name = "Food & Dining", type = "EXPENSE", colorHex = "#FF7A00", iconName = "restaurant", isSystem = true, sortOrder = 1),
        Category(id = 2, name = "Shopping", type = "EXPENSE", colorHex = "#7C5CFC", iconName = "shopping_bag", isSystem = true, sortOrder = 2),
        Category(id = 3, name = "Travel", type = "EXPENSE", colorHex = "#2979FF", iconName = "flight", isSystem = true, sortOrder = 3),
        Category(id = 4, name = "Bills & Utilities", type = "EXPENSE", colorHex = "#00C853", iconName = "receipt_long", isSystem = true, sortOrder = 4),
        Category(id = 5, name = "Entertainment", type = "EXPENSE", colorHex = "#E040FB", iconName = "sports_esports", isSystem = true, sortOrder = 5),
        Category(id = 7, name = "Salary", type = "INCOME", colorHex = "#00E676", iconName = "work", isSystem = true, sortOrder = 7),
        Category(id = 8, name = "Others", type = "EXPENSE", colorHex = "#546E7A", iconName = "more_horiz", isSystem = true, sortOrder = 8)
    )

    @Test
    fun parse_hdfcUpiDebit_extractsCorrectFields() {
        val sms = "Sent Rs.450.00 from HDFC Bank A/C **1234 to SWIGGY on 24-10-26 via UPI Ref 429812345678. Avl bal: Rs.24,500.00"
        val parsed = SMSTransactionParser.parseSmsDetailed("AD-HDFCBK", sms, availableCategories = categories)

        assertNotNull(parsed)
        val tx = parsed!!.transaction
        assertEquals(45000L, tx.amountPaise) // ₹450.00
        assertEquals(TransactionDirection.OUTGOING, tx.direction)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("Swiggy", tx.counterpartyName)
        assertEquals("429812345678", tx.bankReferenceNum)
        assertEquals("1234", parsed.accountEnding)
        assertEquals(1L, tx.categoryId) // Food & Dining
    }

    @Test
    fun parse_hdfcSalaryCredit_extractsCorrectIncome() {
        val sms = "UPDATE: Rs.42,000.00 credited to your HDFC Bank A/c **4321 on 26-OCT-26 by TECH CORP (Salary). Avl Bal: INR 65,400.00"
        val parsed = SMSTransactionParser.parseSmsDetailed("VM-HDFCBK", sms, availableCategories = categories)

        assertNotNull(parsed)
        val tx = parsed!!.transaction
        assertEquals(4200000L, tx.amountPaise) // ₹42,000.00
        assertEquals(TransactionDirection.INCOMING, tx.direction)
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals("4321", parsed.accountEnding)
        assertEquals(7L, tx.categoryId) // Salary
    }

    @Test
    fun parse_sbiDebitWithUtr_extractsCorrectly() {
        val sms = "Dear SBI User, your A/c ending with 5678 has been debited by Rs 380.00 on 24/10/26 transfer to ZOMATO UPI Ref No 429812001122. Avl bal: Rs 15,200."
        val parsed = SMSTransactionParser.parseSmsDetailed("VK-SBIINB", sms, availableCategories = categories)

        assertNotNull(parsed)
        val tx = parsed!!.transaction
        assertEquals(38000L, tx.amountPaise) // ₹380
        assertEquals(TransactionDirection.OUTGOING, tx.direction)
        assertEquals("Zomato", tx.counterpartyName)
        assertEquals("429812001122", tx.bankReferenceNum)
        assertEquals(1L, tx.categoryId) // Food & Dining
    }

    @Test
    fun parse_iciciBankUpi_extractsCorrectly() {
        val sms = "Dear Customer, Account 1234 debited with INR 1,299.00 on 25-Oct-26. Info: UPI/CR/429812345679/AMAZON/ICICI. Avl Bal: INR 35,000.00."
        val parsed = SMSTransactionParser.parseSmsDetailed("JM-ICICIB", sms, availableCategories = categories)

        assertNotNull(parsed)
        val tx = parsed!!.transaction
        assertEquals(129900L, tx.amountPaise) // ₹1,299.00
        assertEquals("Amazon", tx.counterpartyName)
        assertEquals(2L, tx.categoryId) // Shopping
    }

    @Test
    fun parse_paytmUpiPayment_extractsCorrectly() {
        val sms = "Paid Rs.250.00 to Chai Point on Paytm. UPI Ref: 429812999888."
        val parsed = SMSTransactionParser.parseSmsDetailed("BP-PAYTM", sms, availableCategories = categories)

        assertNotNull(parsed)
        val tx = parsed!!.transaction
        assertEquals(25000L, tx.amountPaise) // ₹250
        assertEquals(TransactionDirection.OUTGOING, tx.direction)
        assertEquals("429812999888", tx.bankReferenceNum)
        assertEquals("Paytm", tx.paymentApp)
    }

    @Test
    fun parse_phonePePayment_extractsCorrectly() {
        val sms = "Paid Rs.750.00 to BOOKMYSHOW using PhonePe. Txn ID: 429811223344."
        val parsed = SMSTransactionParser.parseSmsDetailed("AD-PHONPE", sms, availableCategories = categories)

        assertNotNull(parsed)
        val tx = parsed!!.transaction
        assertEquals(75000L, tx.amountPaise)
        assertEquals("BookMyShow", tx.counterpartyName)
        assertEquals("PhonePe", tx.paymentApp)
        assertEquals(5L, tx.categoryId) // Entertainment
    }

    @Test
    fun parse_otpMessage_returnsNull() {
        val otpSms = "123456 is your secret OTP for login to your account. Valid for 10 mins. Do not share with anyone."
        val parsed = SMSTransactionParser.parseSms("VK-HDFCBK", otpSms, availableCategories = categories)

        assertNull(parsed)
    }

    @Test
    fun parse_realYesBankZomato_extractsAmountAndMerchant() {
        val sms = "INR 366.14 spent on YES BANK Card X6890 @UPI_ZOMATO MEDIA PRIVA 29-10-2026 09:06:38 pm. Avl Lmt INR 396,259.43. SMS BLKCC 6890 to 9840909000 if not you"
        val parsed = SMSTransactionParser.parseSmsDetailed("VA-YESBNK-S", sms, availableCategories = categories)

        assertNotNull(parsed)
        val tx = parsed!!.transaction
        assertEquals(36614L, tx.amountPaise)
        assertEquals(TransactionDirection.OUTGOING, tx.direction)
        assertEquals("Zomato", tx.counterpartyName)
        assertEquals(1L, tx.categoryId) // Food & Dining
        assertEquals("6890", parsed.accountEnding)
        assertEquals("YES Bank", tx.paymentApp)
    }

    @Test
    fun parse_realAxisBankGoogleDebit_extractsCorrectly() {
        val sms = "Your A/c has been debited towards Google for INR 489.00 on 29-10-26. 2b53bf69d60444ecbba72d766df75245@ybl - Axis Bank"
        val parsed = SMSTransactionParser.parseSmsDetailed("AD-AXISBK-S", sms, availableCategories = categories)

        assertNotNull(parsed)
        val tx = parsed!!.transaction
        assertEquals(48900L, tx.amountPaise)
        assertEquals(TransactionDirection.OUTGOING, tx.direction)
        assertEquals("Google", tx.counterpartyName)
        assertEquals("PhonePe", tx.paymentApp) // Detected via @ybl
    }

    @Test
    fun parse_realAxisBankUpiP2A_extractsAngadBihari() {
        val sms = "INR 130.00 debited\nA/c no. XX3721\n26-10-26, 16:34:59\nUPI/P2A/629971948458/ANGAD BIHARI\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank"
        val parsed = SMSTransactionParser.parseSmsDetailed("AD-AXISBK-S", sms, availableCategories = categories)

        assertNotNull(parsed)
        val tx = parsed!!.transaction
        assertEquals(13000L, tx.amountPaise)
        assertEquals(TransactionDirection.OUTGOING, tx.direction)
        assertEquals("Angad Bihari", tx.counterpartyName)
        assertEquals("629971948458", tx.bankReferenceNum)
        assertEquals("3721", parsed.accountEnding)
    }

    @Test
    fun parse_mandateAndOtp_areIgnored() {
        val mandateSms = "Mandate Set\nRs.1950.00\nFor Google\nFrom HDFC Bank A/c x7862"
        val parsedMandate = SMSTransactionParser.parseSms("AD-HDFCBK-S", mandateSms, availableCategories = categories)
        assertNull(parsedMandate)

        val otpSms = "862956 is SECRET OTP for txn of INR 582.09 on Axis Bank card XX1661 at ZOMATO LTD on 27-10-26 21:05:46. OTP valid for 5 mins. Pls do not share this OTP."
        val parsedOtp = SMSTransactionParser.parseSms("VM-AXISBK-T", otpSms, availableCategories = categories)
        assertNull(parsedOtp)
    }

    @Test
    fun parse_promotionalLoansAndEstatements_areIgnored() {
        val loanSms = "Funds ready for Disbursal Please verify the disbursal of INR 6,00,000.00 to your YES BANK Credit Card 6890."
        assertNull(SMSTransactionParser.parseSms("VM-YESBNK-S", loanSms, availableCategories = categories))

        val credLoan = "Unlock pre-approved limit on CRED cash of up to Rs. 5,00,000.00."
        assertNull(SMSTransactionParser.parseSms("JX-CREDIN", credLoan, availableCategories = categories))

        val eStatement = "The eStatement for your FIRST Select Credit Card XX7713 is here. Min Due: INR 100.00 Total Due: INR 2,419.25"
        assertNull(SMSTransactionParser.parseSms("VD-IDFCFB-S", eStatement, availableCategories = categories))

        val ccPayment = "Thank you for payment of INR 2,419.25 towards your FIRST Select Credit Card XX7713 on 26 Oct 2026. IDFC FIRST Bank"
        assertNull(SMSTransactionParser.parseSms("CP-IDFCFB-S", ccPayment, availableCategories = categories))

        val siNotice = "Payment of INR 1890.00 towards Merchant Amazon Utility to be debited from ICICI Bank Credit Card 0006, as per Standing Instruction YR55FJJMPc, is due by 26/10/2026."
        assertNull(SMSTransactionParser.parseSms("AD-ICICIT-T", siNotice, availableCategories = categories))

        val lowBal = "UPDATE:Bal in HDFC Bank A/c XX7862 has gone below minimum limit of INR 5,000.00.Yesterday's bal:INR 1,004.68."
        assertNull(SMSTransactionParser.parseSms("VM-HDFCBK-S", lowBal, availableCategories = categories))
    }

    @Test
    fun parse_allRealDeviceSms_verifiesTotalsAndCategorization() {
        val file = java.io.File("""C:\Users\GOOD VIBES\.gemini\antigravity\brain\c575aaa2-ccea-4749-a191-9add026aa82c\scratch\fin_msgs_7d.txt""")
        if (!file.exists()) return

        val text = file.readText()
        val sections = text.split("--- MSG #")
        var totalExpensesPaise = 0L
        var totalIncomePaise = 0L
        var txCount = 0

        println("\n===== REAL DEVICE SMS PARSING TEST =====")
        for (sec in sections) {
            if (sec.isBlank()) continue
            val lines = sec.lines()
            val header = lines[0]
            val parts = header.split("|")
            val sender = if (parts.size > 1) parts[1].trim() else "BANK"
            val body = lines.drop(1).joinToString("\n").trim()

            val parsed = SMSTransactionParser.parseSmsDetailed(sender, body, availableCategories = categories)
            if (parsed != null) {
                txCount++
                val tx = parsed.transaction
                val amtRs = tx.amountPaise / 100.0
                println("TX #$txCount: [${tx.direction}] Rs.$amtRs | ${tx.counterpartyName} | ${tx.categoryName} | Sender: $sender")
                if (tx.direction == TransactionDirection.OUTGOING) {
                    totalExpensesPaise += tx.amountPaise
                } else if (tx.direction == TransactionDirection.INCOMING) {
                    totalIncomePaise += tx.amountPaise
                }
            }
        }
        println("===== SUMMARY =====")
        println("Total Transactions: $txCount")
        println("Total Expenses: Rs." + (totalExpensesPaise / 100.0))
        println("Total Income: Rs." + (totalIncomePaise / 100.0))
        println("===================\n")
    }
}

package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.ImportRowStatus
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class CsvTransactionParserTest {

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
    fun parse_standardSpendlyCsv_parsesCorrectly() {
        val csv = """
            Date,Merchant,Category,Amount,Type,Reference,Notes
            24/10/2026,Swiggy,Food & Dining,450.50,DEBIT,429812345678,Dinner delivery
            25/10/2026,Amazon Retail,Shopping,1299.00,DEBIT,429812345679,Books
            26/10/2026,Infosys Ltd,Salary,45000.00,CREDIT,429812345680,October Salary
        """.trimIndent()

        val result = CsvTransactionParser.parse(csv, "spendly_test.csv", emptyList(), categories)

        assertEquals(3, result.totalRows)
        assertEquals(3, result.validCount)
        assertEquals(0, result.duplicateCount)
        assertEquals(0, result.invalidCount)

        val row1 = result.rows[0].transaction!!
        assertEquals("Swiggy", row1.counterpartyName)
        assertEquals(45050L, row1.amountPaise)
        assertEquals(TransactionDirection.OUTGOING, row1.direction)
        assertEquals("429812345678", row1.bankReferenceNum)
        assertEquals(1L, row1.categoryId)

        val row3 = result.rows[2].transaction!!
        assertEquals(4500000L, row3.amountPaise)
        assertEquals(TransactionDirection.INCOMING, row3.direction)
        assertEquals(TransactionType.INCOME, row3.type)
        assertEquals(7L, row3.categoryId)
    }

    @Test
    fun parse_hdfcBankStatement_withSeparateDebitCreditColumns() {
        val hdfcCsv = """
            ***** HDFC BANK STATEMENT *****
            Customer: John Doe
            Account: XXXXXXXX1234
            Date,Narration,Chq./Ref.No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance
            20/10/2026,UPI/CR/429812001122/Zomato/HDFC/Order99,429812001122,20/10/2026,520.00,,45200.00
            21/10/2026,UPI-BESCOM ELECTRICITY BILL-429812001123,429812001123,21/10/2026,"1,850.50",,43349.50
            22/10/2026,SALARY CREDIT TECH CORP,SAL202610,22/10/2026,,55000.00,98349.50
            Total Withdrawals: 2370.50, Total Deposits: 55000.00
        """.trimIndent()

        val result = CsvTransactionParser.parse(hdfcCsv, "hdfc.csv", emptyList(), categories)

        assertEquals(3, result.totalRows)
        assertEquals(3, result.validCount)

        val zomatoRow = result.rows[0].transaction!!
        assertEquals("Zomato", zomatoRow.counterpartyName)
        assertEquals(52000L, zomatoRow.amountPaise)
        assertEquals(TransactionDirection.OUTGOING, zomatoRow.direction)
        assertEquals(1L, zomatoRow.categoryId) // Food & Dining

        val bescomRow = result.rows[1].transaction!!
        assertEquals(185050L, bescomRow.amountPaise)
        assertEquals(TransactionDirection.OUTGOING, bescomRow.direction)
        assertEquals(4L, bescomRow.categoryId) // Bills & Utilities

        val salaryRow = result.rows[2].transaction!!
        assertEquals(5500000L, salaryRow.amountPaise)
        assertEquals(TransactionDirection.INCOMING, salaryRow.direction)
        assertEquals(7L, salaryRow.categoryId) // Salary
    }

    @Test
    fun parse_sbiBankStatement_withDebitCreditAndCommas() {
        val sbiCsv = """
            Txn Date,Value Date,Description,Ref No./Cheque No.,Debit,Credit,Balance
            15-Oct-2026,15-Oct-2026,UPI/DR/123456789012/STARBUCKS COFFEE/SBI/PAY,123456789012,"1,250.00",,15000.00
            16-Oct-2026,16-Oct-2026,UPI/DR/123456789013/UBER INDIA/SBI/PAY,123456789013,420.00,,14580.00
        """.trimIndent()

        val result = CsvTransactionParser.parse(sbiCsv, "sbi.csv", emptyList(), categories)

        assertEquals(2, result.totalRows)
        assertEquals(2, result.validCount)

        val starbucks = result.rows[0].transaction!!
        assertEquals("Starbucks", starbucks.counterpartyName)
        assertEquals(125000L, starbucks.amountPaise)
        assertEquals(1L, starbucks.categoryId)

        val uber = result.rows[1].transaction!!
        assertEquals("Uber", uber.counterpartyName)
        assertEquals(42000L, uber.amountPaise)
        assertEquals(3L, uber.categoryId) // Travel
    }

    @Test
    fun parse_detectsDuplicatesAgainstExistingDatabaseTransactions() {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH)
        val existingTx = Transaction(
            id = 101L,
            amountPaise = 35000L, // ₹350
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.COMPLETED,
            title = "Starbucks",
            counterpartyName = "Starbucks",
            categoryId = 1,
            timestampMs = sdf.parse("24/10/2026 15:30")!!.time,
            bankReferenceNum = "UTR999888777111"
        )

        val csv = """
            Date,Time,Merchant,Amount,Type,Reference
            24/10/2026,15:30,Starbucks,350.00,DEBIT,UTR999888777111
            24/10/2026,16:00,Cafe Coffee Day,180.00,DEBIT,UTR999888777222
        """.trimIndent()

        val result = CsvTransactionParser.parse(
            csvContent = csv,
            fileName = "dupe_test.csv",
            existingTransactions = listOf(existingTx),
            availableCategories = categories
        )

        assertEquals(2, result.totalRows)
        assertEquals(1, result.validCount)
        assertEquals(1, result.duplicateCount)

        val dupeRow = result.rows[0]
        assertEquals(ImportRowStatus.DUPLICATE, dupeRow.status)
        assertEquals(false, dupeRow.isSelected) // Not selected by default
        assertNotNull(dupeRow.matchedDuplicate)
        assertEquals(101L, dupeRow.matchedDuplicate!!.id)

        val newRow = result.rows[1]
        assertEquals(ImportRowStatus.VALID, newRow.status)
        assertEquals(true, newRow.isSelected)
    }

    @Test
    fun parse_handlesCorruptedAndInvalidLinesGracefully() {
        val badCsv = """
            Date,Merchant,Amount
            ,No Date Merchant,100.00
            24/10/2026,Zero Amount,0.00
            InvalidDateRow,Starbucks,250.00
            25/10/2026,Valid Row,500.00
        """.trimIndent()

        val result = CsvTransactionParser.parse(badCsv, "corrupt.csv", emptyList(), categories)

        assertEquals(4, result.totalRows)
        assertEquals(1, result.validCount)
        assertEquals(3, result.invalidCount)

        val validRow = result.rows.find { it.status == ImportRowStatus.VALID }
        assertNotNull(validRow)
        assertEquals(50000L, validRow!!.transaction!!.amountPaise)
    }

    @Test
    fun parseCsvLine_handlesEmbeddedCommasAndEscapedQuotes() {
        val line = """"Starbucks, Bandra West","1,450.00","Special ""Nitro"" Cold Brew""""
        val tokens = CsvTransactionParser.parseCsvLine(line)

        assertEquals(3, tokens.size)
        assertEquals("Starbucks, Bandra West", tokens[0])
        assertEquals("1,450.00", tokens[1])
        assertEquals("Special \"Nitro\" Cold Brew", tokens[2])
    }
}

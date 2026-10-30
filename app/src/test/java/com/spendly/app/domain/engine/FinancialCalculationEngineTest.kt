package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FinancialCalculationEngineTest {

    private val categories = listOf(
        Category(id = 1, name = "Food & Dining", colorHex = "#FF7A00", iconName = "restaurant"),
        Category(id = 2, name = "Shopping", colorHex = "#7C5CFC", iconName = "shopping_bag"),
        Category(id = 7, name = "Salary", colorHex = "#00E676", iconName = "work")
    )

    @Test
    fun calculateSummary_correctNetSpendingAndRefunds() {
        val now = System.currentTimeMillis()
        val transactions = listOf(
            // Expense 1: ₹500 (50,000 paise)
            Transaction(
                id = 1,
                amountPaise = 50000L,
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Swiggy",
                counterpartyName = "Swiggy",
                categoryId = 1,
                timestampMs = now
            ),
            // Expense 2: ₹1,000 (100,000 paise)
            Transaction(
                id = 2,
                amountPaise = 100000L,
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Amazon",
                counterpartyName = "Amazon",
                categoryId = 2,
                timestampMs = now
            ),
            // Refund: ₹200 (20,000 paise) for Swiggy
            Transaction(
                id = 3,
                amountPaise = 20000L,
                type = TransactionType.REFUND,
                direction = TransactionDirection.INCOMING,
                status = TransactionStatus.COMPLETED,
                title = "Swiggy Refund",
                counterpartyName = "Swiggy",
                categoryId = 1,
                timestampMs = now + 1000
            ),
            // Income: Salary ₹5,000 (500,000 paise)
            Transaction(
                id = 4,
                amountPaise = 500000L,
                type = TransactionType.INCOME,
                direction = TransactionDirection.INCOMING,
                status = TransactionStatus.COMPLETED,
                title = "Salary",
                counterpartyName = "Employer",
                categoryId = 7,
                timestampMs = now
            )
        )

        val summary = FinancialCalculationEngine.calculateSummary(transactions, emptyList(), categories)

        // Gross = ₹1,500 (150,000 paise)
        assertEquals(150000L, summary.grossSpendingPaise)
        // Refunds = ₹200 (20,000 paise)
        assertEquals(20000L, summary.refundsPaise)
        // Net Spending = ₹1,500 - ₹200 = ₹1,300 (130,000 paise)
        assertEquals(130000L, summary.netSpendingPaise)
        // Qualifying Income = ₹5,000 (500,000 paise)
        assertEquals(500000L, summary.qualifyingIncomePaise)
        // Total Received = Salary + Refund = ₹5,200 (520,000 paise)
        assertEquals(520000L, summary.totalReceivedPaise)
        // Net Cash Flow = Total Received (5,200) - Gross Spending (1,500) = ₹3,700 (370,000 paise)
        assertEquals(370000L, summary.netCashFlowPaise)
    }

    @Test
    fun calculateSummary_failedAndSelfTransfers_excludedFromCalculations() {
        val now = System.currentTimeMillis()
        val transactions = listOf(
            // Completed Expense: ₹300
            Transaction(
                id = 1,
                amountPaise = 30000L,
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Chai",
                counterpartyName = "Chai",
                categoryId = 1,
                timestampMs = now
            ),
            // Failed Expense: ₹1,000 (Must NOT be counted)
            Transaction(
                id = 2,
                amountPaise = 100000L,
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.FAILED,
                title = "Failed Transfer",
                counterpartyName = "Merchant",
                categoryId = 2,
                timestampMs = now
            ),
            // Pending Expense: ₹500 (Must NOT be counted in completed)
            Transaction(
                id = 3,
                amountPaise = 50000L,
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.PENDING,
                title = "Pending Tx",
                counterpartyName = "Pending",
                categoryId = 2,
                timestampMs = now
            ),
            // Self Transfer: ₹10,000 (Own account transfer has 0 net impact)
            Transaction(
                id = 4,
                amountPaise = 1000000L,
                type = TransactionType.TRANSFER_SELF,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Transfer to Self Account",
                counterpartyName = "Self Bank",
                categoryId = 8,
                timestampMs = now
            )
        )

        val summary = FinancialCalculationEngine.calculateSummary(transactions, emptyList(), categories)

        // Only the ₹300 completed expense must be counted
        assertEquals(30000L, summary.grossSpendingPaise)
        assertEquals(30000L, summary.netSpendingPaise)
        assertEquals(0L, summary.qualifyingIncomePaise)
    }

    @Test
    fun calculateSummary_periodComparison_computesPercentage() {
        val now = System.currentTimeMillis()
        val current = listOf(
            Transaction(
                id = 1,
                amountPaise = 880000L, // ₹8,800
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Shopping",
                counterpartyName = "Store",
                categoryId = 2,
                timestampMs = now
            )
        )
        val previous = listOf(
            Transaction(
                id = 2,
                amountPaise = 1000000L, // ₹10,000
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Shopping Prev",
                counterpartyName = "Store",
                categoryId = 2,
                timestampMs = now - 100000
            )
        )

        val summary = FinancialCalculationEngine.calculateSummary(current, previous, categories)

        // (8800 - 10000) / 10000 = -12.0%
        assertNotNull(summary.spendingChangePercent)
        assertEquals(-12.0f, summary.spendingChangePercent!!, 0.01f)
    }

    @Test
    fun formatPaiseAdaptive_correctLakhsAndCrores() {
        // 13.59 Lakhs (₹13,59,286.45)
        val incomePaise = 135928645L
        assertEquals("₹13.59L", com.spendly.app.domain.model.formatPaiseAdaptive(incomePaise))
        assertEquals("₹13.59 Lakhs", com.spendly.app.domain.model.formatPaiseLakhsSubtitle(incomePaise))

        // 1.71 Lakhs
        val flowPaise = 17082846L
        assertEquals("₹1.71L", com.spendly.app.domain.model.formatPaiseAdaptive(flowPaise))

        // Negative 1.71 Lakhs
        assertEquals("-₹1.71L", com.spendly.app.domain.model.formatPaiseAdaptive(-flowPaise))

        // 28,600 (under 1 Lakh shows full number with commas)
        val shoppingPaise = 2860000L
        assertEquals("₹28,600", com.spendly.app.domain.model.formatPaiseAdaptive(shoppingPaise))
        assertNull(com.spendly.app.domain.model.formatPaiseLakhsSubtitle(shoppingPaise))

        // 6,060
        val foodPaise = 606000L
        assertEquals("₹6,060", com.spendly.app.domain.model.formatPaiseAdaptive(foodPaise))

        // 1.25 Crore
        val crorePaise = 1250000000L
        assertEquals("₹1.25 Cr", com.spendly.app.domain.model.formatPaiseAdaptive(crorePaise))
        assertEquals("₹1.25 Crores", com.spendly.app.domain.model.formatPaiseLakhsSubtitle(crorePaise))

        // Zero
        assertEquals("₹0", com.spendly.app.domain.model.formatPaiseAdaptive(0L))
    }

    @Test
    fun formatPaiseToRupees_includeDecimalsFlag() {
        val amount = 118931698L // ₹11,89,316.98 (11 Lakhs, 89 Thousand, 316)
        assertEquals("₹11,89,316.98", com.spendly.app.domain.model.formatPaiseToRupees(amount, includeDecimals = true))
        assertEquals("₹11,89,316", com.spendly.app.domain.model.formatPaiseToRupees(amount, includeDecimals = false))
    }
}

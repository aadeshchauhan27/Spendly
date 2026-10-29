package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsEngineTest {

    @Test
    fun generateInsights_emptyTransactions_returnsEmptyList() {
        val insights = InsightsEngine.generateInsights(
            currentTransactions = emptyList(),
            previousTransactions = emptyList(),
            budgets = emptyList(),
            daysInPeriod = 30
        )
        assertTrue(insights.isEmpty())
    }

    @Test
    fun generateInsights_detectsTopSpendingCategory() {
        val txs = listOf(
            Transaction(
                id = 1,
                amountPaise = 500000L, // ₹5,000
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Swiggy",
                counterpartyName = "Swiggy",
                categoryId = 1,
                categoryName = "Food & Dining",
                timestampMs = System.currentTimeMillis()
            ),
            Transaction(
                id = 2,
                amountPaise = 200000L, // ₹2,000
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Uber",
                counterpartyName = "Uber",
                categoryId = 3,
                categoryName = "Travel",
                timestampMs = System.currentTimeMillis()
            )
        )

        val insights = InsightsEngine.generateInsights(
            currentTransactions = txs,
            previousTransactions = emptyList(),
            budgets = emptyList(),
            daysInPeriod = 30
        )

        val topCatInsight = insights.find { it.id == "top_category" }
        org.junit.Assert.assertNotNull(topCatInsight)
        assertTrue(topCatInsight!!.description.contains("Food & Dining"))
        assertTrue(topCatInsight.description.contains("₹5000"))
        assertTrue(topCatInsight.description.contains("71%"))
    }

    @Test
    fun generateInsights_detectsBudgetWarningWhenApproachingLimit() {
        val overallBudget = Budget(
            id = 1,
            categoryId = null,
            amountPaise = 1000000L, // ₹10,000
            spentPaise = 850000L,  // ₹8,500 (85%)
            periodMonth = 10,
            periodYear = 2026
        )

        val insights = InsightsEngine.generateInsights(
            currentTransactions = emptyList(),
            previousTransactions = emptyList(),
            budgets = listOf(overallBudget),
            daysInPeriod = 30
        )

        val budgetInsight = insights.find { it.id == "budget_warning" }
        org.junit.Assert.assertNotNull(budgetInsight)
        assertEquals(InsightType.WARNING, budgetInsight!!.type)
        assertTrue(budgetInsight.description.contains("85%"))
    }

    @Test
    fun generateInsights_calculatesDailyAverageCorrectly() {
        val txs = listOf(
            Transaction(
                id = 1,
                amountPaise = 300000L, // ₹3,000
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Groceries",
                counterpartyName = "Zepto",
                categoryId = 1,
                categoryName = "Food & Dining",
                timestampMs = System.currentTimeMillis()
            )
        )

        val insights = InsightsEngine.generateInsights(
            currentTransactions = txs,
            previousTransactions = emptyList(),
            budgets = emptyList(),
            daysInPeriod = 10
        )

        val dailyInsight = insights.find { it.id == "daily_average" }
        org.junit.Assert.assertNotNull(dailyInsight)
        // ₹3,000 / 10 days = ₹300/day
        assertTrue(dailyInsight!!.description.contains("₹300"))
    }
}

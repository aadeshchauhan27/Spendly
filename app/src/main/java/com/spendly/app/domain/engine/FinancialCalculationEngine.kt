package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.CategorySpending
import com.spendly.app.domain.model.DashboardSummary
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType

object FinancialCalculationEngine {

    /**
     * Calculates the dashboard summary according to documented financial policies:
     * - Gross Spending: Sum of completed EXPENSE
     * - Refunds & Reversals: Sum of completed REFUND and REVERSAL
     * - Net Spending: Gross Spending - Refunds & Reversals
     * - Qualifying Income: Sum of completed INCOME (excludes self transfers and refunds)
     * - Total Received: Qualifying Income + Refunds + incoming transfers
     * - Net Cash Flow: Total Received - Gross Spending
     */
    fun calculateSummary(
        currentPeriodTransactions: List<Transaction>,
        previousPeriodTransactions: List<Transaction> = emptyList(),
        categories: List<Category> = emptyList()
    ): DashboardSummary {
        val completedCurrent = currentPeriodTransactions.filter { it.status == TransactionStatus.COMPLETED }

        var grossSpendingPaise = 0L
        var refundsPaise = 0L
        var qualifyingIncomePaise = 0L
        var totalReceivedPaise = 0L

        val categorySpendingMap = mutableMapOf<Long, Long>()

        completedCurrent.forEach { tx ->
            when (tx.type) {
                TransactionType.EXPENSE -> {
                    grossSpendingPaise += tx.amountPaise
                    val currentCatTotal = categorySpendingMap.getOrDefault(tx.categoryId, 0L)
                    categorySpendingMap[tx.categoryId] = currentCatTotal + tx.amountPaise
                }
                TransactionType.REFUND, TransactionType.REVERSAL -> {
                    refundsPaise += tx.amountPaise
                    totalReceivedPaise += tx.amountPaise
                    // Deduct from category if related
                    val currentCatTotal = categorySpendingMap.getOrDefault(tx.categoryId, 0L)
                    categorySpendingMap[tx.categoryId] = (currentCatTotal - tx.amountPaise).coerceAtLeast(0L)
                }
                TransactionType.INCOME -> {
                    qualifyingIncomePaise += tx.amountPaise
                    totalReceivedPaise += tx.amountPaise
                }
                TransactionType.TRANSFER_OTHER -> {
                    if (tx.direction == TransactionDirection.INCOMING) {
                        totalReceivedPaise += tx.amountPaise
                    } else {
                        grossSpendingPaise += tx.amountPaise
                    }
                }
                TransactionType.TRANSFER_SELF, TransactionType.SPAM -> {
                    // Own-account transfers and spam offers have zero net impact on spending or income
                }
            }
        }

        val netSpendingPaise = (grossSpendingPaise - refundsPaise).coerceAtLeast(0L)
        val netCashFlowPaise = totalReceivedPaise - grossSpendingPaise

        // Calculate comparison with previous period
        val spendingChangePercent = if (previousPeriodTransactions.isNotEmpty()) {
            val completedPrevious = previousPeriodTransactions.filter { it.status == TransactionStatus.COMPLETED }
            val prevGross = completedPrevious.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
            val prevRefunds = completedPrevious.filter { it.type == TransactionType.REFUND || it.type == TransactionType.REVERSAL }.sumOf { it.amountPaise }
            val prevNet = (prevGross - prevRefunds).coerceAtLeast(0L)

            if (prevNet > 0L) {
                val diff = netSpendingPaise.toDouble() - prevNet.toDouble()
                ((diff / prevNet.toDouble()) * 100.0).toFloat()
            } else {
                null
            }
        } else {
            null
        }

        // Build Category Spendings
        val categoryMap = categories.associateBy { it.id }
        val categorySpendings = categorySpendingMap.map { (catId, totalPaise) ->
            val cat = categoryMap[catId]
            val pct = if (netSpendingPaise > 0L) (totalPaise.toFloat() / netSpendingPaise.toFloat()) * 100f else 0f
            CategorySpending(
                categoryId = catId,
                categoryName = cat?.name ?: "Others",
                colorHex = cat?.colorHex ?: "#546E7A",
                iconName = cat?.iconName ?: "more_horiz",
                totalAmountPaise = totalPaise,
                percentage = pct
            )
        }.sortedByDescending { it.totalAmountPaise }

        // Build Trend Data Points (sub-intervals across period)
        val trendPoints = generateTrendPoints(completedCurrent)

        return DashboardSummary(
            grossSpendingPaise = grossSpendingPaise,
            refundsPaise = refundsPaise,
            netSpendingPaise = netSpendingPaise,
            qualifyingIncomePaise = qualifyingIncomePaise,
            totalReceivedPaise = totalReceivedPaise,
            netCashFlowPaise = netCashFlowPaise,
            spendingChangePercent = spendingChangePercent,
            trendDataPoints = trendPoints,
            topCategories = categorySpendings,
            recentTransactions = completedCurrent.filter { it.type != TransactionType.SPAM }.sortedByDescending { it.timestampMs }.take(10)
        )
    }

    private fun generateTrendPoints(transactions: List<Transaction>, buckets: Int = 8): List<Float> {
        val expenseTx = transactions.filter { it.type == TransactionType.EXPENSE }
        if (expenseTx.isEmpty()) return listOf(0f, 0f)

        val minTime = expenseTx.minOf { it.timestampMs }
        val maxTime = expenseTx.maxOf { it.timestampMs }

        if (minTime == maxTime) {
            return listOf(expenseTx.sumOf { it.amountPaise } / 100f)
        }

        val step = (maxTime - minTime) / buckets.coerceAtLeast(1)
        val bucketAmounts = FloatArray(buckets) { 0f }

        expenseTx.forEach { tx ->
            val bucketIndex = (((tx.timestampMs - minTime) / step).toInt()).coerceIn(0, buckets - 1)
            bucketAmounts[bucketIndex] += (tx.amountPaise / 100f)
        }

        return bucketAmounts.toList()
    }
}

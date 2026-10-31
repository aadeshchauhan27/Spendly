package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.CategorySpending
import com.spendly.app.domain.model.DashboardSummary
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.TrendPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        categories: List<Category> = emptyList(),
        startMs: Long? = null,
        endMs: Long? = null
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
                TransactionType.CASHBACK -> {
                    // Cashback is a financial reward/credit, recorded as incoming cash flow and qualifying income
                    qualifyingIncomePaise += tx.amountPaise
                    totalReceivedPaise += tx.amountPaise
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
                TransactionType.TRANSFER, TransactionType.TRANSFER_SELF, TransactionType.SPAM -> {
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

        // Build Trend Data Points for every day in the selected period window
        val trendPoints = generateTrendPoints(
            transactions = completedCurrent,
            startMs = startMs,
            endMs = endMs
        )

        return DashboardSummary(
            grossSpendingPaise = grossSpendingPaise,
            refundsPaise = refundsPaise,
            netSpendingPaise = netSpendingPaise,
            qualifyingIncomePaise = qualifyingIncomePaise,
            totalReceivedPaise = totalReceivedPaise,
            netCashFlowPaise = netCashFlowPaise,
            spendingChangePercent = spendingChangePercent,
            trendPoints = trendPoints,
            trendDataPoints = trendPoints.map { it.amountRupees },
            topCategories = categorySpendings,
            recentTransactions = completedCurrent.filter { it.type != TransactionType.SPAM }.sortedByDescending { it.timestampMs }
        )
    }

    private fun generateTrendPoints(
        transactions: List<Transaction>,
        startMs: Long? = null,
        endMs: Long? = null
    ): List<TrendPoint> {
        val expenseTx = transactions.filter {
            it.type == TransactionType.EXPENSE && it.status == TransactionStatus.COMPLETED
        }
        val dateFormat = SimpleDateFormat("dd MMM", Locale("en", "IN"))
        val fullDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("en", "IN"))
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val now = endMs ?: System.currentTimeMillis()
        val start = startMs ?: if (expenseTx.isNotEmpty()) expenseTx.minOf { it.timestampMs } else (now - 6 * 86400000L)

        // Normalize calendar start to 00:00:00.000
        val calStart = java.util.Calendar.getInstance().apply {
            timeInMillis = start
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        // Normalize calendar end to 23:59:59.999
        val calEnd = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }

        val diffDays = (((calEnd.timeInMillis - calStart.timeInMillis) / 86400000L) + 1).toInt().coerceIn(1, 365)

        // Sum completed expenses per calendar day
        val dailySums = mutableMapOf<String, Float>()
        expenseTx.forEach { tx ->
            val dayKey = dayKeyFormat.format(Date(tx.timestampMs))
            val cur = dailySums.getOrDefault(dayKey, 0f)
            dailySums[dayKey] = cur + (tx.amountPaise / 100f)
        }

        val cursorCal = java.util.Calendar.getInstance().apply { timeInMillis = calStart.timeInMillis }
        val points = mutableListOf<TrendPoint>()

        for (i in 0 until diffDays) {
            val dayDate = cursorCal.time
            val dayKey = dayKeyFormat.format(dayDate)
            val dayStartMs = cursorCal.timeInMillis
            val amount = dailySums[dayKey] ?: 0f

            points.add(
                TrendPoint(
                    amountRupees = amount,
                    dateLabel = dateFormat.format(dayDate),
                    fullDate = fullDateFormat.format(dayDate),
                    timestampMs = dayStartMs
                )
            )
            cursorCal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }

        return if (points.size >= 2) points else {
            val p = points.firstOrNull() ?: TrendPoint(0f, dateFormat.format(Date(now)), fullDateFormat.format(Date(now)), now)
            listOf(p, p)
        }
    }
}

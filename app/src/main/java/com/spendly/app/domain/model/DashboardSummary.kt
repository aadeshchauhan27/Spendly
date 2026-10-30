package com.spendly.app.domain.model

data class TrendPoint(
    val amountRupees: Float,
    val dateLabel: String,
    val fullDate: String = "",
    val timestampMs: Long = 0L
)

data class DashboardSummary(
    val grossSpendingPaise: Long = 0L,
    val refundsPaise: Long = 0L,
    val netSpendingPaise: Long = 0L,
    val qualifyingIncomePaise: Long = 0L,
    val totalReceivedPaise: Long = 0L,
    val netCashFlowPaise: Long = 0L,
    val spendingChangePercent: Float? = null, // e.g. -12.0f for 12% decrease
    val trendPoints: List<TrendPoint> = emptyList(),
    val trendDataPoints: List<Float> = emptyList(),
    val topCategories: List<CategorySpending> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList()
) {
    val formattedNetSpending: String
        get() = formatPaiseToRupees(netSpendingPaise, includeDecimals = false)

    val formattedNetSpendingFull: String
        get() = formatPaiseToRupees(netSpendingPaise, includeDecimals = true)

    val formattedNetSpendingLakhs: String?
        get() = formatPaiseLakhsSubtitle(netSpendingPaise)

    val formattedIncome: String
        get() = formatPaiseAdaptive(qualifyingIncomePaise)

    val formattedIncomeFull: String
        get() = formatPaiseToRupees(qualifyingIncomePaise)

    val formattedReceived: String
        get() = formatPaiseAdaptive(totalReceivedPaise)

    val formattedReceivedFull: String
        get() = formatPaiseToRupees(totalReceivedPaise)

    val formattedCashFlow: String
        get() {
            val sign = if (netCashFlowPaise > 0) "+" else ""
            return "$sign${formatPaiseAdaptive(netCashFlowPaise)}"
        }

    val formattedCashFlowFull: String
        get() {
            val sign = if (netCashFlowPaise > 0) "+" else ""
            return "$sign${formatPaiseToRupees(netCashFlowPaise)}"
        }
}

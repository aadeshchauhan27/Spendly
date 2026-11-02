package com.spendly.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.domain.engine.FinancialInsight
import com.spendly.app.domain.engine.InsightsEngine
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.TrendPoint
import com.spendly.app.domain.model.formatPaiseAdaptive
import com.spendly.app.domain.model.formatPaiseLakhsSubtitle
import com.spendly.app.domain.model.formatPaiseToRupees
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySpendBreakdown(
    val categoryName: String,
    val colorHex: String,
    val amountPaise: Long,
    val percentage: Int,
    val transactions: List<Transaction> = emptyList()
) {
    val formattedRupees: String
        get() = formatPaiseToRupees(amountPaise, includeSymbol = false, includeDecimals = false)

    val formattedAdaptive: String
        get() = formatPaiseAdaptive(amountPaise)

    val formattedFull: String
        get() = formatPaiseToRupees(amountPaise)
}

data class AnalyticsUiState(
    val period: String = "30D",
    val customRangeStartMs: Long? = null,
    val customRangeEndMs: Long? = null,
    val customRangeLabel: String? = null,
    val totalSpendingPaise: Long = 0L,
    val totalIncomePaise: Long = 0L,
    val spendChangePercent: Int = 0,
    val trendDataPoints: List<Float> = emptyList(),
    val trendPoints: List<TrendPoint> = emptyList(),
    val trendAxisDates: List<String> = emptyList(),
    val categoryBreakdowns: List<CategorySpendBreakdown> = emptyList(),
    val insights: List<FinancialInsight> = emptyList(),
    val isLoading: Boolean = false
) {
    val formattedTotalSpending: String
        get() = formatPaiseToRupees(totalSpendingPaise, includeDecimals = false)

    val formattedTotalSpendingLakhs: String?
        get() = formatPaiseLakhsSubtitle(totalSpendingPaise)
}

class AnalyticsViewModel(
    private val repository: SpendlyRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow("30D")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        _selectedPeriod,
        _customDateRange
    ) { period, customRange ->
        Pair(period, customRange)
    }.flatMapLatest { (period, customRange) ->
        val window = computePeriodTimestamps(period, customRange)
        val startMs = window.startMs
        val endMs = window.endMs
        val prevStartMs = window.prevStartMs
        val prevEndMs = window.prevEndMs
        val daysInPeriod = window.days

        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH) + 1

        val customLabel = if (period == "Custom" && customRange != null) {
            val sdf = SimpleDateFormat("dd MMM", Locale("en", "IN"))
            "${sdf.format(Date(customRange.first))} - ${sdf.format(Date(customRange.second))}"
        } else null

        combine(
            repository.getTransactionsBetweenFlow(startMs, endMs),
            repository.getTransactionsBetweenFlow(prevStartMs, prevEndMs),
            repository.getBudgetsForPeriodFlow(curYear, curMonth)
        ) { currentTx, prevTx, budgets ->
            val expenses = currentTx.filter {
                it.type == TransactionType.EXPENSE && it.status == TransactionStatus.COMPLETED
            }
            val income = currentTx.filter {
                it.type == TransactionType.INCOME && it.status == TransactionStatus.COMPLETED
            }

            val totalSpend = expenses.sumOf { it.amountPaise }
            val totalIncome = income.sumOf { it.amountPaise }

            val prevSpend = prevTx.filter {
                it.type == TransactionType.EXPENSE && it.status == TransactionStatus.COMPLETED
            }.sumOf { it.amountPaise }

            val changePercent = if (prevSpend > 0L) {
                (((totalSpend - prevSpend).toDouble() / prevSpend) * 100).toInt()
            } else {
                0
            }

            // Category breakdown
            val categoryBreakdowns = if (totalSpend > 0L) {
                expenses.groupBy { it.categoryName.ifBlank { "Others" } to it.categoryColorHex }
                    .map { (key, txList) ->
                        val catSpend = txList.sumOf { it.amountPaise }
                        val pct = ((catSpend.toDouble() / totalSpend) * 100).toInt()
                        CategorySpendBreakdown(
                            categoryName = key.first,
                            colorHex = key.second,
                            amountPaise = catSpend,
                            percentage = pct,
                            transactions = txList.sortedByDescending { it.timestampMs }
                        )
                    }
                    .sortedByDescending { it.amountPaise }
            } else {
                emptyList()
            }

            // Generate trend line points & axis dates
            val (trendPointsList, dates) = computeTrendLine(expenses, startMs, endMs, daysInPeriod)

            // Generate Insights
            val insights = InsightsEngine.generateInsights(
                currentTransactions = currentTx,
                previousTransactions = prevTx,
                budgets = budgets,
                daysInPeriod = daysInPeriod
            )

            AnalyticsUiState(
                period = period,
                customRangeStartMs = customRange?.first,
                customRangeEndMs = customRange?.second,
                customRangeLabel = customLabel,
                totalSpendingPaise = totalSpend,
                totalIncomePaise = totalIncome,
                spendChangePercent = changePercent,
                trendDataPoints = trendPointsList.map { it.amountRupees },
                trendPoints = trendPointsList,
                trendAxisDates = dates,
                categoryBreakdowns = categoryBreakdowns,
                insights = insights,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState(isLoading = true)
    )

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun setCustomDateRange(startMs: Long, endMs: Long) {
        _customDateRange.value = Pair(startMs, endMs)
        _selectedPeriod.value = "Custom"
    }

    private data class PeriodWindow(
        val startMs: Long,
        val endMs: Long,
        val prevStartMs: Long,
        val prevEndMs: Long,
        val days: Int
    )

    private fun computePeriodTimestamps(period: String, customRange: Pair<Long, Long>? = null): PeriodWindow {
        if (period == "Custom" && customRange != null) {
            val startMs = customRange.first
            val endMs = customRange.second
            val durationMs = (endMs - startMs).coerceAtLeast(86400000L)
            val days = (durationMs / 86400000L).toInt().coerceAtLeast(1)
            val prevStartMs = startMs - durationMs
            val prevEndMs = startMs
            return PeriodWindow(startMs, endMs, prevStartMs, prevEndMs, days)
        }

        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val days = when (period) {
            "7D" -> 7
            "30D" -> 30
            "90D", "3M" -> 90
            "180D", "6M" -> 180
            "365D", "1Y" -> 365
            else -> 30
        }
        val durationMs = days * oneDayMs
        val calStart = Calendar.getInstance().apply {
            timeInMillis = now - durationMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = calStart.timeInMillis
        val prevStartMs = startMs - durationMs
        val prevEndMs = startMs
        return PeriodWindow(startMs, now, prevStartMs, prevEndMs, days)
    }

    private fun computeTrendLine(
        expenses: List<Transaction>,
        startMs: Long,
        endMs: Long,
        days: Int
    ): Pair<List<TrendPoint>, List<String>> {
        val dateFormat = SimpleDateFormat("dd MMM", Locale("en", "IN"))
        val fullDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("en", "IN"))
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val calStart = Calendar.getInstance().apply {
            timeInMillis = startMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calEnd = Calendar.getInstance().apply {
            timeInMillis = endMs
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val diffDays = (((calEnd.timeInMillis - calStart.timeInMillis) / 86400000L) + 1).toInt().coerceIn(1, 365)

        val dailySums = mutableMapOf<String, Float>()
        expenses.forEach { tx ->
            val dayKey = dayKeyFormat.format(Date(tx.timestampMs))
            val cur = dailySums.getOrDefault(dayKey, 0f)
            dailySums[dayKey] = cur + (tx.amountPaise / 100f)
        }

        val cursorCal = Calendar.getInstance().apply { timeInMillis = calStart.timeInMillis }
        val points = mutableListOf<TrendPoint>()
        val dates = mutableListOf<String>()

        for (i in 0 until diffDays) {
            val dayDate = cursorCal.time
            val dayKey = dayKeyFormat.format(dayDate)
            val dayStartMs = cursorCal.timeInMillis
            val amount = dailySums[dayKey] ?: 0f
            val dateLabel = dateFormat.format(dayDate)

            points.add(
                TrendPoint(
                    amountRupees = amount,
                    dateLabel = dateLabel,
                    fullDate = fullDateFormat.format(dayDate),
                    timestampMs = dayStartMs
                )
            )
            dates.add(dateLabel)
            cursorCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val resultPoints = if (points.size >= 2) points else {
            val p = points.firstOrNull() ?: TrendPoint(0f, dateFormat.format(Date(endMs)), fullDateFormat.format(Date(endMs)), endMs)
            listOf(p, p)
        }

        return resultPoints to dates
    }

    class Factory(private val repository: SpendlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
                return AnalyticsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

package com.spendly.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.formatPaiseToRupees
import com.spendly.app.domain.model.rupeesToPaise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TransactionGroup(
    val dateLabel: String,
    val totalAmountFormatted: String,
    val transactions: List<Transaction>
)

data class TransactionDateRange(
    val startMs: Long,
    val endMs: Long,
    val label: String
)

sealed interface TransactionsUiState {
    object Loading : TransactionsUiState
    data class Success(
        val searchQuery: String,
        val selectedCategory: String,
        val availableCategories: List<Category>,
        val groupedTransactions: List<TransactionGroup>,
        val dateRange: TransactionDateRange? = null,
        val isEmpty: Boolean
    ) : TransactionsUiState
    data class Error(val message: String) : TransactionsUiState
}

class TransactionsViewModel(
    private val repository: SpendlyRepository
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCategoryFlow = MutableStateFlow("All")
    private val dateRangeFlow = MutableStateFlow<TransactionDateRange?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.getAllTransactionsFlow(),
        repository.getAllCategoriesFlow(),
        searchQueryFlow,
        selectedCategoryFlow,
        dateRangeFlow
    ) { allTransactions, categories, query, categoryFilter, dateRange ->
        val filtered = allTransactions.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    tx.counterpartyName.contains(query, ignoreCase = true) ||
                    (tx.notes?.contains(query, ignoreCase = true) == true) ||
                    (tx.bankReferenceNum?.contains(query, ignoreCase = true) == true)

            val matchesCategory = if (categoryFilter == "All") {
                tx.type != TransactionType.SPAM
            } else {
                tx.categoryName.equals(categoryFilter, ignoreCase = true)
            }

            val matchesDate = if (dateRange != null) {
                tx.timestampMs in dateRange.startMs..dateRange.endMs
            } else {
                true
            }

            matchesQuery && matchesCategory && matchesDate
        }

        val grouped = groupTransactionsByDay(filtered)

        TransactionsUiState.Success(
            searchQuery = query,
            selectedCategory = categoryFilter,
            availableCategories = categories,
            groupedTransactions = grouped,
            dateRange = dateRange,
            isEmpty = allTransactions.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState.Loading
    )

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
    }

    fun onCategorySelect(category: String) {
        selectedCategoryFlow.value = category
    }

    fun setDateRange(startMs: Long, endMs: Long) {
        val dateFormat = SimpleDateFormat("dd MMM", Locale("en", "IN"))
        val label = "${dateFormat.format(Date(startMs))} - ${dateFormat.format(Date(endMs))}"
        dateRangeFlow.value = TransactionDateRange(startMs, endMs, label)
    }

    fun filterByDay(timestampMs: Long) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestampMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis
        val dateFormat = SimpleDateFormat("dd MMM", Locale("en", "IN"))
        dateRangeFlow.value = TransactionDateRange(startMs, endMs, dateFormat.format(Date(startMs)))
    }

    fun filterByPeriod(days: Int) {
        val now = System.currentTimeMillis()
        val startMs = now - (days * 86400000L)
        val dateFormat = SimpleDateFormat("dd MMM", Locale("en", "IN"))
        val label = "${days}D"
        dateRangeFlow.value = TransactionDateRange(startMs, now, label)
    }

    fun filterByThisYear() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        val now = System.currentTimeMillis()
        dateRangeFlow.value = TransactionDateRange(startMs, now, "This Year")
    }

    fun clearDateRange() {
        dateRangeFlow.value = null
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun addManualTransaction(
        title: String,
        amountRupees: Double,
        categoryId: Long,
        isExpense: Boolean,
        notes: String?
    ) {
        viewModelScope.launch {
            val amountPaise = rupeesToPaise(amountRupees)
            val newTx = Transaction(
                amountPaise = amountPaise,
                type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                direction = if (isExpense) TransactionDirection.OUTGOING else TransactionDirection.INCOMING,
                status = TransactionStatus.COMPLETED,
                title = title.trim(),
                counterpartyName = title.trim(),
                categoryId = categoryId,
                timestampMs = System.currentTimeMillis(),
                paymentMethod = "UPI",
                paymentApp = "Manual Entry",
                notes = notes?.trim()?.ifBlank { null },
                source = TransactionSource.MANUAL
            )
            repository.insertTransaction(newTx)
        }
    }

    private fun groupTransactionsByDay(transactions: List<Transaction>): List<TransactionGroup> {
        val nowCal = Calendar.getInstance()
        val txCal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("en", "IN"))

        val map = linkedMapOf<String, MutableList<Transaction>>()

        transactions.sortedByDescending { it.timestampMs }.forEach { tx ->
            txCal.timeInMillis = tx.timestampMs

            val label = when {
                isSameDay(nowCal, txCal) -> "Today"
                isYesterday(nowCal, txCal) -> "Yesterday"
                else -> dateFormat.format(Date(tx.timestampMs))
            }

            map.getOrPut(label) { mutableListOf() }.add(tx)
        }

        return map.map { (dateLabel, txList) ->
            // Day total sum of expenses
            val dayExpensesPaise = txList
                .filter { it.type == TransactionType.EXPENSE && it.status == TransactionStatus.COMPLETED }
                .sumOf { it.amountPaise }
            val formattedTotal = formatPaiseToRupees(dayExpensesPaise)

            TransactionGroup(
                dateLabel = dateLabel,
                totalAmountFormatted = formattedTotal,
                transactions = txList
            )
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = cal1.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return yesterday.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    class Factory(
        private val repository: SpendlyRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionsViewModel(repository) as T
        }
    }
}

package com.spendly.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.components.DonutSegment
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.CategorySpending
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.formatPaiseToRupees
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategoryDateRange(
    val startMs: Long,
    val endMs: Long,
    val label: String
)

sealed interface CategoriesUiState {
    object Loading : CategoriesUiState
    data class Success(
        val isSpending: Boolean,
        val totalAmountFormatted: String,
        val donutSegments: List<DonutSegment>,
        val categoriesList: List<CategorySpending>,
        val dateRange: CategoryDateRange? = null,
        val isEmpty: Boolean
    ) : CategoriesUiState
}

class CategoriesViewModel(
    private val repository: SpendlyRepository
) : ViewModel() {

    private val isSpendingFlow = MutableStateFlow(true)
    private val dateRangeFlow = MutableStateFlow<CategoryDateRange?>(null)

    val uiState: StateFlow<CategoriesUiState> = combine(
        isSpendingFlow,
        dateRangeFlow,
        repository.getAllTransactionsFlow(),
        repository.getAllCategoriesFlow()
    ) { isSpending, dateRange, transactions, categories ->
        val inRangeTransactions = if (dateRange != null) {
            transactions.filter { it.timestampMs in dateRange.startMs..dateRange.endMs }
        } else {
            transactions
        }

        val filtered = if (isSpending) {
            inRangeTransactions.filter { tx ->
                tx.status == TransactionStatus.COMPLETED &&
                (tx.direction == TransactionDirection.OUTGOING || tx.type == TransactionType.EXPENSE) &&
                tx.type != TransactionType.SPAM &&
                tx.type != TransactionType.TRANSFER_SELF
            }
        } else {
            inRangeTransactions.filter { tx ->
                tx.status == TransactionStatus.COMPLETED &&
                (tx.direction == TransactionDirection.INCOMING || tx.type == TransactionType.INCOME || tx.type == TransactionType.CASHBACK || tx.type == TransactionType.REFUND) &&
                tx.type != TransactionType.SPAM &&
                tx.type != TransactionType.TRANSFER_SELF
            }
        }

        val categorySpendingMap = mutableMapOf<String, Long>()
        var totalPaise = 0L

        filtered.forEach { tx ->
            val catName = tx.categoryName.ifBlank { if (isSpending) "Others" else "Income" }
            val amount = tx.amountPaise
            totalPaise += amount
            categorySpendingMap[catName] = (categorySpendingMap[catName] ?: 0L) + amount
        }

        val categoriesByName = categories.associateBy { it.name.lowercase() }

        val categoriesList = categorySpendingMap.map { (catName, amountPaise) ->
            val category = categoriesByName[catName.lowercase()]
            val colorHex = category?.colorHex ?: if (isSpending) "#546E7A" else "#10B981"
            val iconName = category?.iconName ?: "category"
            val percentage = if (totalPaise > 0) ((amountPaise.toDouble() / totalPaise) * 100.0).toFloat() else 0f
            CategorySpending(
                categoryId = category?.id ?: 0L,
                categoryName = catName,
                colorHex = colorHex,
                iconName = iconName,
                totalAmountPaise = amountPaise,
                percentage = percentage
            )
        }.sortedByDescending { it.totalAmountPaise }

        val segments = categoriesList.map { cat ->
            val color = try {
                Color(android.graphics.Color.parseColor(cat.colorHex))
            } catch (_: Exception) {
                if (isSpending) Color(0xFF546E7A) else Color(0xFF10B981)
            }
            DonutSegment(
                name = cat.categoryName,
                value = (cat.totalAmountPaise / 100).toFloat(),
                color = color
            )
        }

        val totalFormatted = formatPaiseToRupees(totalPaise)

        CategoriesUiState.Success(
            isSpending = isSpending,
            totalAmountFormatted = totalFormatted,
            donutSegments = segments,
            categoriesList = categoriesList,
            dateRange = dateRange,
            isEmpty = filtered.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState.Loading
    )

    fun setTab(spending: Boolean) {
        isSpendingFlow.value = spending
    }

    fun setDateRange(startMs: Long, endMs: Long) {
        val dateFormat = SimpleDateFormat("dd MMM", Locale("en", "IN"))
        val label = "${dateFormat.format(Date(startMs))} - ${dateFormat.format(Date(endMs))}"
        dateRangeFlow.value = CategoryDateRange(startMs, endMs, label)
    }

    fun filterByPeriod(days: Int) {
        val now = System.currentTimeMillis()
        val startMs = now - (days * 86400000L)
        dateRangeFlow.value = CategoryDateRange(startMs, now, "${days}D")
    }

    fun filterByThisMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        val now = System.currentTimeMillis()
        dateRangeFlow.value = CategoryDateRange(startMs, now, "This Month")
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
        dateRangeFlow.value = CategoryDateRange(startMs, now, "This Year")
    }

    fun clearDateRange() {
        dateRangeFlow.value = null
    }

    fun addCustomCategory(
        name: String,
        type: String = "EXPENSE",
        colorHex: String = "#FF7A00",
        iconName: String = "category"
    ) {
        viewModelScope.launch {
            val newCat = Category(
                name = name,
                type = type,
                colorHex = colorHex,
                iconName = iconName,
                isSystem = false,
                sortOrder = 99
            )
            repository.insertCategory(newCat)
        }
    }

    class Factory(
        private val repository: SpendlyRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CategoriesViewModel(repository) as T
        }
    }
}

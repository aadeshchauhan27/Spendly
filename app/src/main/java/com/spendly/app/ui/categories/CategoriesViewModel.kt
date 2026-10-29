package com.spendly.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.components.DonutSegment
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.domain.engine.FinancialCalculationEngine
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.CategorySpending
import com.spendly.app.domain.model.formatPaiseToRupees
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

sealed interface CategoriesUiState {
    object Loading : CategoriesUiState
    data class Success(
        val isSpending: Boolean,
        val totalAmountFormatted: String,
        val donutSegments: List<DonutSegment>,
        val categoriesList: List<CategorySpending>,
        val isEmpty: Boolean
    ) : CategoriesUiState
}

class CategoriesViewModel(
    private val repository: SpendlyRepository
) : ViewModel() {

    private val isSpendingFlow = MutableStateFlow(true)

    val uiState: StateFlow<CategoriesUiState> = combine(
        isSpendingFlow,
        repository.getAllTransactionsFlow(),
        repository.getAllCategoriesFlow()
    ) { isSpending, transactions, categories ->
        val summary = FinancialCalculationEngine.calculateSummary(
            currentPeriodTransactions = transactions,
            categories = categories
        )

        val segments = summary.topCategories.map { cat ->
            val color = try {
                Color(android.graphics.Color.parseColor(cat.colorHex))
            } catch (e: Exception) {
                Color(0xFF546E7A)
            }
            DonutSegment(
                name = cat.categoryName,
                value = (cat.totalAmountPaise / 100).toFloat(),
                color = color
            )
        }

        val totalFormatted = formatPaiseToRupees(summary.netSpendingPaise)

        CategoriesUiState.Success(
            isSpending = isSpending,
            totalAmountFormatted = totalFormatted,
            donutSegments = segments,
            categoriesList = summary.topCategories,
            isEmpty = transactions.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState.Loading
    )

    fun setTab(spending: Boolean) {
        isSpendingFlow.value = spending
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

package com.spendly.app.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class BudgetsUiState(
    val currentYear: Int,
    val currentMonth: Int,
    val overallBudget: Budget? = null,
    val categoryBudgets: List<Budget> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class BudgetsViewModel(
    private val repository: SpendlyRepository
) : ViewModel() {

    private val cal = Calendar.getInstance()
    private val _period = MutableStateFlow(cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1))
    val period: StateFlow<Pair<Int, Int>> = _period.asStateFlow()

    val uiState: StateFlow<BudgetsUiState> = combine(
        _period.flatMapLatest { (year, month) -> repository.getBudgetsForPeriodFlow(year, month) },
        repository.getAllCategoriesFlow(),
        _period
    ) { budgets, categories, (year, month) ->
        val overall = budgets.find { it.categoryId == null }
        val categoryBudgets = budgets.filter { it.categoryId != null }
        BudgetsUiState(
            currentYear = year,
            currentMonth = month,
            overallBudget = overall,
            categoryBudgets = categoryBudgets,
            availableCategories = categories.filter { it.type == "EXPENSE" },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState(
            currentYear = cal.get(Calendar.YEAR),
            currentMonth = cal.get(Calendar.MONTH) + 1,
            isLoading = true
        )
    )

    fun setPeriod(year: Int, month: Int) {
        _period.value = year to month
    }

    fun upsertBudget(
        categoryId: Long?,
        amountRupees: Double,
        existingBudgetId: Long = 0L
    ) {
        viewModelScope.launch {
            val amountPaise = (amountRupees * 100).toLong()
            val (year, month) = _period.value
            val budget = Budget(
                id = existingBudgetId,
                categoryId = categoryId,
                amountPaise = amountPaise,
                periodMonth = month,
                periodYear = year,
                isActive = true
            )
            repository.upsertBudget(budget)
        }
    }

    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            repository.deleteBudget(budgetId)
        }
    }

    class Factory(private val repository: SpendlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BudgetsViewModel::class.java)) {
                return BudgetsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

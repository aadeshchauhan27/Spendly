package com.spendly.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.domain.model.SavingsGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoalsUiState(
    val goals: List<SavingsGoal> = emptyList(),
    val totalSavedPaise: Long = 0L,
    val totalTargetPaise: Long = 0L,
    val isLoading: Boolean = false
)

class GoalsViewModel(
    private val repository: SpendlyRepository
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = repository.getAllGoalsFlow()
        .map { list ->
            GoalsUiState(
                goals = list,
                totalSavedPaise = list.sumOf { it.savedAmountPaise },
                totalTargetPaise = list.sumOf { it.targetAmountPaise },
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GoalsUiState(isLoading = true)
        )

    fun createGoal(
        name: String,
        targetRupees: Double,
        iconName: String = "plant",
        colorHex: String = "#C8FF5A"
    ) {
        viewModelScope.launch {
            val targetPaise = (targetRupees * 100).toLong()
            val newGoal = SavingsGoal(
                name = name,
                targetAmountPaise = targetPaise,
                savedAmountPaise = 0L,
                iconName = iconName,
                colorHex = colorHex
            )
            repository.insertGoal(newGoal)
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
        }
    }

    fun addContribution(
        goalId: Long,
        amountRupees: Double,
        isDeposit: Boolean,
        note: String?
    ) {
        viewModelScope.launch {
            val amountPaise = (amountRupees * 100).toLong() * (if (isDeposit) 1 else -1)
            repository.addGoalContribution(goalId, amountPaise, note)
        }
    }

    class Factory(private val repository: SpendlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GoalsViewModel::class.java)) {
                return GoalsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

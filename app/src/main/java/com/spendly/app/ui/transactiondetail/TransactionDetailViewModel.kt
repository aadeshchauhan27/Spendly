package com.spendly.app.ui.transactiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TransactionDetailUiState {
    object Loading : TransactionDetailUiState
    data class Success(
        val transaction: Transaction,
        val categories: List<Category>
    ) : TransactionDetailUiState
    object Deleted : TransactionDetailUiState
    data class Error(val message: String) : TransactionDetailUiState
}

class TransactionDetailViewModel(
    private val transactionId: Long,
    private val repository: SpendlyRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)
    private val isDeleted = MutableStateFlow(false)

    val uiState: StateFlow<TransactionDetailUiState> = combine(
        refreshTrigger,
        isDeleted,
        repository.getAllCategoriesFlow()
    ) { _, deleted, categories ->
        if (deleted) {
            TransactionDetailUiState.Deleted
        } else {
            val tx = repository.getTransactionById(transactionId)
            if (tx != null) {
                TransactionDetailUiState.Success(
                    transaction = tx,
                    categories = categories
                )
            } else {
                TransactionDetailUiState.Error("Transaction not found")
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionDetailUiState.Loading
    )

    fun updateNotes(newNotes: String) {
        viewModelScope.launch {
            val current = repository.getTransactionById(transactionId) ?: return@launch
            val updated = current.copy(notes = newNotes.trim().ifBlank { null })
            repository.updateTransaction(updated)
            refreshTrigger.value++
        }
    }

    fun updateCategory(newCategoryId: Long) {
        viewModelScope.launch {
            val current = repository.getTransactionById(transactionId) ?: return@launch
            val updated = current.copy(categoryId = newCategoryId)
            repository.updateTransaction(updated)
            refreshTrigger.value++
        }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            isDeleted.value = true
        }
    }

    class Factory(
        private val transactionId: Long,
        private val repository: SpendlyRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionDetailViewModel(transactionId, repository) as T
        }
    }
}

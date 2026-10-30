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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecategorizePrompt(
    val newCategoryId: Long,
    val newCategoryName: String,
    val merchantName: String,
    val similarCount: Int
)

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
    private val _recategorizePrompt = MutableStateFlow<RecategorizePrompt?>(null)
    val recategorizePrompt: StateFlow<RecategorizePrompt?> = _recategorizePrompt.asStateFlow()

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

    fun requestCategoryChange(newCategory: Category) {
        viewModelScope.launch {
            val current = repository.getTransactionById(transactionId) ?: return@launch
            if (current.categoryId == newCategory.id) return@launch

            val merchant = current.counterpartyName.ifBlank { current.title }
            val similarCount = repository.countSimilarTransactions(
                excludeId = current.id,
                counterpartyName = current.counterpartyName,
                title = current.title
            )

            if (similarCount > 0 && merchant.isNotBlank()) {
                _recategorizePrompt.value = RecategorizePrompt(
                    newCategoryId = newCategory.id,
                    newCategoryName = newCategory.name,
                    merchantName = merchant,
                    similarCount = similarCount
                )
            } else {
                applyCategoryChange(newCategory.id, applyToAll = false)
            }
        }
    }

    fun confirmRecategorization(applyToAll: Boolean) {
        val prompt = _recategorizePrompt.value ?: return
        _recategorizePrompt.value = null
        applyCategoryChange(prompt.newCategoryId, applyToAll = applyToAll)
    }

    fun dismissRecategorizePrompt() {
        _recategorizePrompt.value = null
    }

    private fun applyCategoryChange(newCategoryId: Long, applyToAll: Boolean) {
        viewModelScope.launch {
            val current = repository.getTransactionById(transactionId) ?: return@launch
            if (applyToAll) {
                repository.updateCategoryForSimilarTransactions(
                    counterpartyName = current.counterpartyName,
                    title = current.title,
                    newCategoryId = newCategoryId
                )
            }
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

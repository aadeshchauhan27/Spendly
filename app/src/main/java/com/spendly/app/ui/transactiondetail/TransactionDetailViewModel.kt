package com.spendly.app.ui.transactiondetail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
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

    fun createAndAssignCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val newCategory = Category(
                name = name.trim(),
                type = "EXPENSE",
                iconName = iconName,
                colorHex = colorHex
            )
            val result = repository.insertCategory(newCategory)
            val newId = result.getOrNull()
            if (newId != null) {
                applyCategoryChange(newId, applyToAll = false)
            }
        }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            isDeleted.value = true
        }
    }

    suspend fun getExactSmsBody(context: Context, tx: Transaction): String {
        // 1. Direct match from transaction tag
        if (!tx.tag.isNullOrBlank()) {
            return tx.tag
        }

        // 2. Query device SMS inbox if permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val resolver = context.contentResolver
                val uri = Telephony.Sms.Inbox.CONTENT_URI
                val projection = arrayOf(
                    Telephony.Sms.Inbox.BODY,
                    Telephony.Sms.Inbox.DATE,
                    Telephony.Sms.Inbox.ADDRESS
                )

                val timeWindowMs = 10 * 60 * 1000L // 10 minutes window
                val minDate = tx.timestampMs - timeWindowMs
                val maxDate = tx.timestampMs + timeWindowMs

                val selection = "${Telephony.Sms.Inbox.DATE} >= ? AND ${Telephony.Sms.Inbox.DATE} <= ?"
                val selectionArgs = arrayOf(minDate.toString(), maxDate.toString())
                val sortOrder = "${Telephony.Sms.Inbox.DATE} DESC"

                resolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                    val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
                    while (cursor.moveToNext()) {
                        val body = cursor.getString(bodyIndex) ?: continue
                        if (tx.bankReferenceNum != null && body.contains(tx.bankReferenceNum)) {
                            repository.updateTransaction(tx.copy(tag = body))
                            return body
                        }
                        val rupees = (tx.amountPaise / 100).toString()
                        if (body.contains(rupees)) {
                            repository.updateTransaction(tx.copy(tag = body))
                            return body
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore query exception and fallback
            }
        }

        // 3. Fallback formatted representation
        return buildString {
            append("Transaction: ${tx.title}\n")
            append("Amount: ${tx.formattedRupees}\n")
            append("Status: ${tx.status}\n")
            if (tx.bankReferenceNum != null) append("Reference / UTR: ${tx.bankReferenceNum}\n")
            if (tx.counterpartyUpiId != null) append("UPI VPA: ${tx.counterpartyUpiId}\n")
            if (!tx.notes.isNullOrBlank()) append("Notes: ${tx.notes}\n")
            append("Source: ${tx.source}")
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

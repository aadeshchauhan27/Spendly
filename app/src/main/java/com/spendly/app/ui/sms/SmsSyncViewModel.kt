package com.spendly.app.ui.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.domain.engine.SMSTransactionParser
import com.spendly.app.domain.engine.TransactionDeduplicator
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.sms.DetectedSmsItem
import com.spendly.app.sms.SmsReader
import com.spendly.app.sms.SmsScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SmsSyncUiState {
    data object Idle : SmsSyncUiState
    data object Scanning : SmsSyncUiState
    data class Preview(val result: SmsScanResult) : SmsSyncUiState
    data class Syncing(val totalCount: Int) : SmsSyncUiState
    data class Success(val count: Int, val totalSpentPaise: Long, val totalIncomePaise: Long) : SmsSyncUiState
    data object PermissionRequired : SmsSyncUiState
    data class Error(val message: String) : SmsSyncUiState
}

class SmsSyncViewModel(
    private val repository: SpendlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SmsSyncUiState>(SmsSyncUiState.Idle)
    val uiState: StateFlow<SmsSyncUiState> = _uiState.asStateFlow()

    fun checkAndScan(context: Context) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _uiState.value = SmsSyncUiState.PermissionRequired
        } else {
            scanInbox(context)
        }
    }

    fun scanInbox(context: Context) {
        viewModelScope.launch {
            _uiState.value = SmsSyncUiState.Scanning
            try {
                val existing = repository.getAllTransactionsFlow().first()
                val categories = repository.getAllCategoriesFlow().first()
                val scanResult = SmsReader.scanInbox(context, existing, categories, maxDays = 365)

                if (scanResult.items.isEmpty()) {
                    _uiState.value = SmsSyncUiState.Error("No transaction SMS messages found in the last 365 days.")
                } else {
                    _uiState.value = SmsSyncUiState.Preview(scanResult)
                }
            } catch (e: SecurityException) {
                _uiState.value = SmsSyncUiState.PermissionRequired
            } catch (e: Exception) {
                _uiState.value = SmsSyncUiState.Error("Failed to scan messages: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun toggleItem(smsId: Long) {
        val current = _uiState.value
        if (current is SmsSyncUiState.Preview) {
            val updated = current.result.items.map { item ->
                if (item.smsId == smsId && !item.isDuplicate) {
                    item.copy(isSelected = !item.isSelected)
                } else {
                    item
                }
            }
            _uiState.value = SmsSyncUiState.Preview(current.result.copy(items = updated))
        }
    }

    fun syncSelected() {
        val current = _uiState.value as? SmsSyncUiState.Preview ?: return
        val selectedTransactions = current.result.items
            .filter { it.isSelected && it.transaction != null }
            .mapNotNull { it.transaction }

        if (selectedTransactions.isEmpty()) {
            _uiState.value = SmsSyncUiState.Error("No transactions selected for sync.")
            return
        }

        viewModelScope.launch {
            _uiState.value = SmsSyncUiState.Syncing(selectedTransactions.size)
            try {
                val summary = repository.batchInsertTransactions(
                    transactions = selectedTransactions,
                    fileName = "SMS_Inbox_Sync",
                    source = "SMS",
                    duplicateCount = current.result.duplicateCount,
                    errorCount = 0
                )
                val totalSpent = selectedTransactions.filter { !it.isIncoming }.sumOf { it.amountPaise }
                val totalIncome = selectedTransactions.filter { it.isIncoming }.sumOf { it.amountPaise }
                _uiState.value = SmsSyncUiState.Success(
                    count = summary.importedCount,
                    totalSpentPaise = totalSpent,
                    totalIncomePaise = totalIncome
                )
            } catch (e: Exception) {
                _uiState.value = SmsSyncUiState.Error("Sync failed: ${e.localizedMessage ?: "Database error"}")
            }
        }
    }

    /**
     * Test / Simulation helper: parses any given SMS and injects it into preview state
     */
    fun simulateSms(sender: String, content: String) {
        viewModelScope.launch {
            _uiState.value = SmsSyncUiState.Scanning
            val existing = repository.getAllTransactionsFlow().first()
            val categories = repository.getAllCategoriesFlow().first()

            val transaction = SMSTransactionParser.parseSms(
                sender = sender,
                content = content,
                messageTimestampMs = System.currentTimeMillis(),
                availableCategories = categories,
                existingTransactions = existing
            )

            if (transaction == null) {
                _uiState.value = SmsSyncUiState.Error("SMS text did not match any known bank or UPI transaction pattern.")
                return@launch
            }

            val duplicate = TransactionDeduplicator.findDuplicate(transaction, existing)
            val item = DetectedSmsItem(
                smsId = System.currentTimeMillis(),
                sender = sender,
                body = content,
                dateMs = System.currentTimeMillis(),
                transaction = transaction,
                isDuplicate = duplicate != null,
                matchedDuplicate = duplicate,
                isSelected = (duplicate == null)
            )

            val scanResult = SmsScanResult(
                totalScanned = 1,
                totalTransactionsFound = 1,
                validNewCount = if (duplicate == null) 1 else 0,
                duplicateCount = if (duplicate != null) 1 else 0,
                totalSpentPaise = if (!transaction.isIncoming) transaction.amountPaise else 0L,
                totalIncomePaise = if (transaction.isIncoming) transaction.amountPaise else 0L,
                items = listOf(item)
            )

            _uiState.value = SmsSyncUiState.Preview(scanResult)
        }
    }

    fun reset() {
        _uiState.value = SmsSyncUiState.Idle
    }

    class Factory(private val repository: SpendlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SmsSyncViewModel::class.java)) {
                return SmsSyncViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

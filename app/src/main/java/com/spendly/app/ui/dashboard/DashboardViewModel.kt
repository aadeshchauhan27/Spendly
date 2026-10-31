package com.spendly.app.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.preferences.UserPreferences
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.domain.model.DashboardSummary
import com.spendly.app.sms.SmsReader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Syncing(val message: String) : DashboardUiState
    data class Success(
        val summary: DashboardSummary,
        val selectedPeriod: String,
        val userName: String,
        val isEmpty: Boolean,
        val isSyncing: Boolean = false
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: SpendlyRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val selectedPeriodFlow = MutableStateFlow("7D")
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow("Analyzing UPI Transactions...")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDemoData()
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        selectedPeriodFlow,
        _isSyncing,
        _syncMessage
    ) { period, isSyncing, syncMessage ->
        Triple(period, isSyncing, syncMessage)
    }.flatMapLatest { (period, isSyncing, syncMessage) ->
        val now = System.currentTimeMillis()
        val durationMs = when (period) {
            "7D" -> 7 * 24 * 60 * 60 * 1000L
            "30D" -> 30 * 24 * 60 * 60 * 1000L
            "90D", "3M" -> 90 * 24 * 60 * 60 * 1000L
            "365D", "1Y" -> 365 * 24 * 60 * 60 * 1000L
            else -> 7 * 24 * 60 * 60 * 1000L
        }
        val startMs = now - durationMs

        combine(
            repository.getDashboardSummaryFlow(startMs, now),
            userPreferences.userNameFlow,
            repository.getAllTransactionsFlow()
        ) { summary, userName, allTransactions ->
            if (isSyncing && allTransactions.isEmpty()) {
                DashboardUiState.Syncing(syncMessage)
            } else {
                DashboardUiState.Success(
                    summary = summary,
                    selectedPeriod = period,
                    userName = userName,
                    isEmpty = allTransactions.isEmpty(),
                    isSyncing = isSyncing
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    fun setPeriod(period: String) {
        selectedPeriodFlow.value = period
    }

    fun syncTransactionsFromSms(context: Context, force: Boolean = false) {
        viewModelScope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true
            _syncMessage.value = "Reading SMS transactions from phone..."
            try {
                val categories = repository.getAllCategoriesFlow().first()
                val existing = repository.getAllTransactionsFlow().first()
                val deleted = repository.getAllDeletedTransactions()

                _syncMessage.value = "Parsing and categorizing bank messages..."
                val scanResult = SmsReader.scanInbox(context, existing, categories, deletedTransactions = deleted, maxDays = 365)
                val newTransactions = scanResult.items
                    .filter { it.isSelected && it.transaction != null }
                    .mapNotNull { it.transaction }

                if (newTransactions.isNotEmpty()) {
                    _syncMessage.value = "Saving ${newTransactions.size} transactions..."
                    repository.batchInsertTransactions(
                        transactions = newTransactions,
                        fileName = "SMS_Inbox_Sync",
                        source = "SMS",
                        duplicateCount = scanResult.duplicateCount,
                        errorCount = 0
                    )
                }
            } catch (e: Exception) {
                // Silently finish
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            repository.seedDemoData()
            userPreferences.setDemoDataSeeded(true)
        }
    }

    class Factory(
        private val repository: SpendlyRepository,
        private val userPreferences: UserPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository, userPreferences) as T
        }
    }
}

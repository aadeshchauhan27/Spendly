package com.spendly.app.ui.upi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.formatPaiseAdaptive
import com.spendly.app.domain.model.formatPaiseLakhsSubtitle
import com.spendly.app.domain.model.formatPaiseToRupees
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

data class BankSpendBreakdown(
    val bankName: String,
    val totalAmountPaise: Long,
    val count: Int,
    val percentage: Int,
    val colorHex: String,
    val transactions: List<Transaction>
) {
    val formattedRupees: String
        get() = formatPaiseToRupees(totalAmountPaise, includeSymbol = false, includeDecimals = false)

    val formattedAdaptive: String
        get() = formatPaiseAdaptive(totalAmountPaise)

    val formattedFull: String
        get() = formatPaiseToRupees(totalAmountPaise)

    val shortLabel: String
        get() = when {
            bankName.contains("Axis", ignoreCase = true) -> "Axis"
            bankName.contains("HDFC", ignoreCase = true) -> "HDFC"
            bankName.contains("YES", ignoreCase = true) -> "YES"
            bankName.contains("ICICI", ignoreCase = true) -> "ICICI"
            bankName.contains("IndusInd", ignoreCase = true) -> "Indus"
            bankName.contains("Kiwi", ignoreCase = true) -> "Kiwi"
            bankName.contains("SBI", ignoreCase = true) -> "SBI"
            bankName.contains("Kotak", ignoreCase = true) -> "Kotak"
            bankName.contains("Google", ignoreCase = true) -> "GPay"
            bankName.contains("PhonePe", ignoreCase = true) -> "PhonePe"
            bankName.contains("Paytm", ignoreCase = true) -> "Paytm"
            bankName.contains("CRED", ignoreCase = true) -> "CRED"
            else -> bankName.take(5)
        }
}

data class UpiTransactionsUiState(
    val selectedPeriod: String = "7D",
    val totalSpendPaise: Long = 0L,
    val totalTxCount: Int = 0,
    val bankBreakdowns: List<BankSpendBreakdown> = emptyList(),
    val selectedBankName: String? = null,
    val displayedTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false
) {
    val formattedTotalSpend: String
        get() = formatPaiseToRupees(totalSpendPaise, includeDecimals = false)

    val formattedTotalSpendLakhs: String?
        get() = formatPaiseLakhsSubtitle(totalSpendPaise)
}

@OptIn(ExperimentalCoroutinesApi::class)
class UpiTransactionsViewModel(
    private val repository: SpendlyRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow("7D")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _selectedBankFilter = MutableStateFlow<String?>(null)
    val selectedBankFilter: StateFlow<String?> = _selectedBankFilter.asStateFlow()

    val uiState: StateFlow<UpiTransactionsUiState> = combine(
        _selectedPeriod,
        _selectedBankFilter
    ) { period, selectedBank ->
        period to selectedBank
    }.flatMapLatest { (period, selectedBank) ->
        val now = System.currentTimeMillis()
        val durationMs = when (period) {
            "7D" -> 7 * 24 * 60 * 60 * 1000L
            "30D" -> 30 * 24 * 60 * 60 * 1000L
            "90D", "3M" -> 90 * 24 * 60 * 60 * 1000L
            "365D", "1Y" -> 365 * 24 * 60 * 60 * 1000L
            else -> 7 * 24 * 60 * 60 * 1000L
        }
        val startMs = now - durationMs

        repository.getTransactionsBetweenFlow(startMs, now).combine(MutableStateFlow(Unit)) { txList, _ ->
            val upiTxList = txList.filter {
                it.status == TransactionStatus.COMPLETED &&
                it.direction == TransactionDirection.OUTGOING &&
                it.type == TransactionType.EXPENSE
            }

            val totalSpend = upiTxList.sumOf { it.amountPaise }
            val totalCount = upiTxList.size

            // Group transactions by Bank / App Source
            val grouped = upiTxList.groupBy { tx ->
                resolveBankOrApp(tx)
            }

            val breakdowns = grouped.map { (bankName, list) ->
                val bankSpend = list.sumOf { it.amountPaise }
                val pct = if (totalSpend > 0L) {
                    ((bankSpend.toDouble() / totalSpend) * 100).toInt()
                } else 0

                val color = getBankColor(bankName)

                BankSpendBreakdown(
                    bankName = bankName,
                    totalAmountPaise = bankSpend,
                    count = list.size,
                    percentage = pct,
                    colorHex = color,
                    transactions = list.sortedByDescending { it.timestampMs }
                )
            }.sortedByDescending { it.totalAmountPaise }

            val displayed = if (selectedBank != null) {
                breakdowns.find { it.bankName == selectedBank }?.transactions ?: emptyList()
            } else {
                upiTxList.sortedByDescending { it.timestampMs }
            }

            UpiTransactionsUiState(
                selectedPeriod = period,
                totalSpendPaise = totalSpend,
                totalTxCount = totalCount,
                bankBreakdowns = breakdowns,
                selectedBankName = selectedBank,
                displayedTransactions = displayed,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UpiTransactionsUiState(isLoading = true)
    )

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun selectBank(bankName: String?) {
        _selectedBankFilter.value = if (_selectedBankFilter.value == bankName) null else bankName
    }

    companion object {
        fun resolveBankOrApp(tx: Transaction): String {
            val notes = (tx.notes ?: "").lowercase(Locale.ENGLISH)
            val app = (tx.paymentApp ?: "").lowercase(Locale.ENGLISH)
            val rawId = (tx.rawIdentifier ?: "").lowercase(Locale.ENGLISH)
            val title = tx.title.lowercase(Locale.ENGLISH)

            return when {
                app.contains("axis") || notes.contains("3721") || notes.contains("6946") || notes.contains("1661") || rawId.contains("axis") -> "Axis Bank"
                app.contains("hdfc") || notes.contains("7862") || rawId.contains("hdfc") -> "HDFC Bank"
                app.contains("icici") || notes.contains("0006") || rawId.contains("icici") -> "ICICI Bank"
                app.contains("yes") || notes.contains("6890") || rawId.contains("yes") -> "YES Bank"
                app.contains("indus") || notes.contains("9526") || rawId.contains("indus") -> "IndusInd Bank"
                app.contains("kiwi") || notes.contains("kiwi") || rawId.contains("kiwi") -> "Kiwi"
                app.contains("sbi") || notes.contains("sbi") || rawId.contains("sbi") -> "SBI"
                app.contains("kotak") || rawId.contains("kotak") -> "Kotak Bank"
                app.contains("cred") || title.contains("cred") -> "CRED Club"
                app.contains("phonepe") -> "PhonePe"
                app.contains("paytm") -> "Paytm"
                app.contains("google pay") || app.contains("gpay") -> "Google Pay"
                !tx.paymentApp.isNullOrBlank() -> tx.paymentApp
                else -> "UPI Account"
            }
        }

        fun getBankColor(bankName: String): String {
            return when {
                bankName.contains("Axis", ignoreCase = true) -> "#97144D" // Axis Maroon
                bankName.contains("HDFC", ignoreCase = true) -> "#004C8F" // HDFC Navy
                bankName.contains("ICICI", ignoreCase = true) -> "#F58220" // ICICI Orange
                bankName.contains("YES", ignoreCase = true) -> "#002D62" // YES Blue
                bankName.contains("IndusInd", ignoreCase = true) -> "#8B181B" // IndusInd Crimson
                bankName.contains("Kiwi", ignoreCase = true) -> "#C8FF5A" // Kiwi Neon Lime
                bankName.contains("SBI", ignoreCase = true) -> "#280071" // SBI Blue
                bankName.contains("Kotak", ignoreCase = true) -> "#ED1B24" // Kotak Red
                bankName.contains("CRED", ignoreCase = true) -> "#2B2D42" // CRED Dark
                bankName.contains("PhonePe", ignoreCase = true) -> "#5F259F" // PhonePe Purple
                bankName.contains("Paytm", ignoreCase = true) -> "#00BAF2" // Paytm Cyan
                bankName.contains("Google", ignoreCase = true) -> "#4285F4" // Google Blue
                else -> "#7C5CFC"
            }
        }
    }

    class Factory(
        private val repository: SpendlyRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UpiTransactionsViewModel(repository) as T
        }
    }
}

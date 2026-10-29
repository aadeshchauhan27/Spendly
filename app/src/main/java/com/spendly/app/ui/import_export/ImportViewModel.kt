package com.spendly.app.ui.import_export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.domain.engine.CsvTransactionParser
import com.spendly.app.domain.model.ImportParseResult
import com.spendly.app.domain.model.ImportResultSummary
import com.spendly.app.domain.model.ImportRowStatus
import com.spendly.app.domain.model.ImportSession
import com.spendly.app.domain.model.ParsedImportRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data class Parsing(val fileName: String) : ImportUiState
    data class Preview(val parseResult: ImportParseResult) : ImportUiState
    data class Importing(val totalCount: Int) : ImportUiState
    data class Success(val summary: ImportResultSummary) : ImportUiState
    data class Error(val message: String) : ImportUiState
}

enum class SampleBankType(val displayName: String) {
    HDFC_UPI("HDFC Bank UPI Statement"),
    SBI_UPI("SBI Bank Statement"),
    GENERIC_UPI("Unified UPI Statement")
}

class ImportViewModel(
    private val repository: SpendlyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    val importHistory: StateFlow<List<ImportSession>> = repository.getAllImportSessionsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun parseCsvContent(content: String, fileName: String) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Parsing(fileName)
            try {
                val result = withContext(Dispatchers.Default) {
                    val existingTransactions = repository.getAllTransactionsFlow().first()
                    val categories = repository.getAllCategoriesFlow().first()
                    CsvTransactionParser.parse(
                        csvContent = content,
                        fileName = fileName,
                        existingTransactions = existingTransactions,
                        availableCategories = categories
                    )
                }

                if (result.rows.isEmpty()) {
                    _uiState.value = ImportUiState.Error("No transactions found in '$fileName'. Please check CSV formatting.")
                } else {
                    _uiState.value = ImportUiState.Preview(result)
                }
            } catch (e: Exception) {
                _uiState.value = ImportUiState.Error("Error parsing file: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun toggleRowSelection(rowIndex: Int) {
        val currentState = _uiState.value
        if (currentState is ImportUiState.Preview) {
            val updatedRows = currentState.parseResult.rows.map { row ->
                if (row.rowIndex == rowIndex) {
                    row.copy(isSelected = !row.isSelected)
                } else {
                    row
                }
            }
            _uiState.value = ImportUiState.Preview(
                currentState.parseResult.copy(rows = updatedRows)
            )
        }
    }

    fun selectAllMatching(status: ImportRowStatus, select: Boolean) {
        val currentState = _uiState.value
        if (currentState is ImportUiState.Preview) {
            val updatedRows = currentState.parseResult.rows.map { row ->
                if (row.status == status) {
                    row.copy(isSelected = select)
                } else {
                    row
                }
            }
            _uiState.value = ImportUiState.Preview(
                currentState.parseResult.copy(rows = updatedRows)
            )
        }
    }

    fun confirmImport() {
        val currentState = _uiState.value as? ImportUiState.Preview ?: return
        val selectedRows = currentState.parseResult.rows.filter { it.isSelected && it.transaction != null }

        if (selectedRows.isEmpty()) {
            _uiState.value = ImportUiState.Error("No valid transactions selected for import.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ImportUiState.Importing(selectedRows.size)
            try {
                val transactionsToImport = selectedRows.mapNotNull { it.transaction }
                val summary = repository.batchInsertTransactions(
                    transactions = transactionsToImport,
                    fileName = currentState.parseResult.fileName,
                    source = "CSV",
                    duplicateCount = currentState.parseResult.duplicateCount,
                    errorCount = currentState.parseResult.invalidCount
                )
                _uiState.value = ImportUiState.Success(summary)
            } catch (e: Exception) {
                _uiState.value = ImportUiState.Error("Import failed: ${e.localizedMessage ?: "Database error"}")
            }
        }
    }

    fun reset() {
        _uiState.value = ImportUiState.Idle
    }

    fun loadSample(sampleType: SampleBankType) {
        val (csv, fileName) = when (sampleType) {
            SampleBankType.HDFC_UPI -> {
                val data = """
                    ***** HDFC BANK STATEMENT *****
                    Date,Narration,Chq./Ref.No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance
                    24/10/2026,UPI/CR/429812999001/Swiggy/HDFC/Dinner,429812999001,24/10/2026,450.00,,32550.00
                    24/10/2026,UPI/CR/429812999002/Starbucks Mumbai/HDFC/Coffee,429812999002,24/10/2026,380.00,,32170.00
                    25/10/2026,UPI/CR/429812999003/Amazon Retail/HDFC/Headphones,429812999003,25/10/2026,"2,499.00",,29671.00
                    25/10/2026,UPI-BESCOM ELECTRICITY BILL-429812999004,429812999004,25/10/2026,1280.00,,28391.00
                    26/10/2026,UPI/CR/429812999005/Uber Rides/HDFC/Trip,429812999005,26/10/2026,340.00,,28051.00
                    26/10/2026,SALARY CREDIT TECH CORP,SALOCT2026,26/10/2026,,48000.00,76051.00
                """.trimIndent()
                data to "HDFC_Statement_Oct2026.csv"
            }
            SampleBankType.SBI_UPI -> {
                val data = """
                    Txn Date,Value Date,Description,Ref No./Cheque No.,Debit,Credit,Balance
                    20-Oct-2026,20-Oct-2026,UPI/DR/123456789101/BLINKIT GROCERY/SBI/PAY,123456789101,620.00,,18400.00
                    21-Oct-2026,21-Oct-2026,UPI/DR/123456789102/NETFLIX INDIA/SBI/PAY,123456789102,649.00,,17751.00
                    22-Oct-2026,22-Oct-2026,UPI/DR/123456789103/SHELL PETROL/SBI/PAY,123456789103,"1,500.00",,16251.00
                    23-Oct-2026,23-Oct-2026,UPI/DR/123456789104/BOOKMYSHOW CINEMA/SBI/PAY,123456789104,750.00,,15501.00
                """.trimIndent()
                data to "SBI_Statement_Oct2026.csv"
            }
            SampleBankType.GENERIC_UPI -> {
                val data = """
                    Date,Merchant,Category,Amount,Type,Reference,Notes
                    24/10/2026,Zomato,Food & Dining,520.00,DEBIT,UTR4298111,Lunch order
                    25/10/2026,Myntra,Shopping,1890.00,DEBIT,UTR4298112,Denim jeans
                    26/10/2026,Airtel Prepaid,Bills & Utilities,299.00,DEBIT,UTR4298113,Monthly recharge
                """.trimIndent()
                data to "Spendly_UPI_Export.csv"
            }
        }
        parseCsvContent(csv, fileName)
    }

    class Factory(private val repository: SpendlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ImportViewModel::class.java)) {
                return ImportViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

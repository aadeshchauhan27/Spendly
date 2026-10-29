package com.spendly.app.domain.model

enum class ImportRowStatus {
    VALID,
    DUPLICATE,
    INVALID
}

data class ParsedImportRow(
    val rowIndex: Int,
    val transaction: Transaction?,
    val rawLine: String,
    val status: ImportRowStatus,
    val reason: String? = null,
    val matchedDuplicate: Transaction? = null,
    val isSelected: Boolean = (status == ImportRowStatus.VALID)
)

data class ImportParseResult(
    val fileName: String,
    val totalRows: Int,
    val validCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int,
    val rows: List<ParsedImportRow>
) {
    val selectedRows: List<ParsedImportRow>
        get() = rows.filter { it.isSelected && it.transaction != null }

    val selectedCount: Int
        get() = selectedRows.size
}

data class ImportResultSummary(
    val sessionId: Long,
    val fileName: String,
    val totalRows: Int,
    val importedCount: Int,
    val duplicateCount: Int,
    val errorCount: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

data class ImportSession(
    val id: Long = 0L,
    val source: String,
    val fileName: String,
    val totalRows: Int,
    val importedCount: Int,
    val duplicateCount: Int,
    val errorCount: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

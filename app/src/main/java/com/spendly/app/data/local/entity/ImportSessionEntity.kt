package com.spendly.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_sessions")
data class ImportSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "source")
    val source: String, // CSV, STATEMENT, OCR

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "total_rows")
    val totalRows: Int,

    @ColumnInfo(name = "imported_count")
    val importedCount: Int,

    @ColumnInfo(name = "duplicate_count")
    val duplicateCount: Int,

    @ColumnInfo(name = "error_count")
    val errorCount: Int,

    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long = System.currentTimeMillis()
)

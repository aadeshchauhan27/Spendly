package com.spendly.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deleted_transactions",
    indices = [
        Index(value = ["bank_reference_num"]),
        Index(value = ["raw_identifier"]),
        Index(value = ["timestamp_ms"])
    ]
)
data class DeletedTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "bank_reference_num")
    val bankReferenceNum: String? = null,

    @ColumnInfo(name = "raw_identifier")
    val rawIdentifier: String? = null,

    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long = 0L,

    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long = 0L,

    @ColumnInfo(name = "counterparty_name")
    val counterpartyName: String = "",

    @ColumnInfo(name = "sms_body")
    val smsBody: String? = null,

    @ColumnInfo(name = "deleted_at_ms")
    val deletedAtMs: Long = System.currentTimeMillis()
)

package com.spendly.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp_ms"]),
        Index(value = ["category_id"]),
        Index(value = ["raw_identifier"], unique = false),
        Index(value = ["bank_reference_num"]),
        Index(value = ["type", "status"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long, // Exact integer in paise (₹1.00 = 100 paise)

    @ColumnInfo(name = "type")
    val type: String, // EXPENSE, INCOME, REFUND, REVERSAL, TRANSFER_SELF, TRANSFER_OTHER

    @ColumnInfo(name = "direction")
    val direction: String, // OUTGOING, INCOMING

    @ColumnInfo(name = "status")
    val status: String, // COMPLETED, PENDING, FAILED

    @ColumnInfo(name = "title")
    val title: String, // Swiggy, Amazon, etc.

    @ColumnInfo(name = "counterparty_name")
    val counterpartyName: String,

    @ColumnInfo(name = "counterparty_upi_id")
    val counterpartyUpiId: String? = null,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long,

    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = "UPI",

    @ColumnInfo(name = "payment_app")
    val paymentApp: String? = "Google Pay",

    @ColumnInfo(name = "bank_reference_num")
    val bankReferenceNum: String? = null, // UPI 12-digit UTR

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "tag")
    val tag: String? = null,

    @ColumnInfo(name = "source")
    val source: String = "MANUAL", // MANUAL, CSV_IMPORT, STATEMENT_IMPORT, OCR, NOTIFICATION

    @ColumnInfo(name = "import_session_id")
    val importSessionId: Long? = null,

    @ColumnInfo(name = "raw_identifier")
    val rawIdentifier: String? = null,

    @ColumnInfo(name = "related_transaction_id")
    val relatedTransactionId: Long? = null,

    @ColumnInfo(name = "is_demo")
    val isDemo: Boolean = false,

    @ColumnInfo(name = "created_at_ms")
    val createdAtMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at_ms")
    val updatedAtMs: Long = System.currentTimeMillis()
)

package com.spendly.app.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME,
    REFUND,
    REVERSAL,
    TRANSFER_SELF,
    TRANSFER_OTHER,
    SPAM
}

enum class TransactionDirection {
    OUTGOING,
    INCOMING
}

enum class TransactionStatus {
    COMPLETED,
    PENDING,
    FAILED
}

enum class TransactionSource {
    MANUAL,
    CSV_IMPORT,
    STATEMENT_IMPORT,
    OCR,
    NOTIFICATION,
    SMS_IMPORT
}

data class Transaction(
    val id: Long = 0L,
    val amountPaise: Long,
    val type: TransactionType,
    val direction: TransactionDirection,
    val status: TransactionStatus,
    val title: String,
    val counterpartyName: String,
    val counterpartyUpiId: String? = null,
    val categoryId: Long,
    val categoryName: String = "",
    val categoryColorHex: String = "#546E7A",
    val timestampMs: Long,
    val paymentMethod: String = "UPI",
    val paymentApp: String? = "Google Pay",
    val bankReferenceNum: String? = null,
    val notes: String? = null,
    val tag: String? = null,
    val source: TransactionSource = TransactionSource.MANUAL,
    val importSessionId: Long? = null,
    val rawIdentifier: String? = null,
    val relatedTransactionId: Long? = null,
    val isDemo: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    val isIncoming: Boolean
        get() = direction == TransactionDirection.INCOMING

    val formattedRupees: String
        get() = formatPaiseToRupees(amountPaise)
}

fun formatPaiseToRupees(amountPaise: Long, includeSymbol: Boolean = true): String {
    val isNegative = amountPaise < 0
    val absPaise = Math.abs(amountPaise)
    val rupees = absPaise / 100
    val paise = absPaise % 100

    val rupeeString = java.text.NumberFormat.getIntegerInstance(java.util.Locale("en", "IN")).format(rupees)
    val formatted = if (paise == 0L) {
        rupeeString
    } else {
        "$rupeeString.${paise.toString().padStart(2, '0')}"
    }

    val symbol = if (includeSymbol) "₹" else ""
    return if (isNegative) "-$symbol$formatted" else "$symbol$formatted"
}

fun rupeesToPaise(rupees: Double): Long {
    return Math.round(rupees * 100.0)
}

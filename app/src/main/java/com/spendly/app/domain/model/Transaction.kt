package com.spendly.app.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    TRANSFER_SELF,
    TRANSFER_OTHER,
    REFUND,
    CASHBACK,
    REVERSAL,
    SPAM
}

enum class TransactionDirection {
    OUTGOING,
    INCOMING
}

enum class TransactionStatus {
    COMPLETED,
    PENDING,
    FAILED,
    REVERSED
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
    val categoryIconName: String = "",
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

    val formattedAdaptive: String
        get() = formatPaiseAdaptive(amountPaise)
}

fun formatIndianNumber(number: Long): String {
    val isNegative = number < 0
    val absStr = Math.abs(number).toString()
    if (absStr.length <= 3) {
        return if (isNegative) "-$absStr" else absStr
    }
    val last3 = absStr.substring(absStr.length - 3)
    val rem = absStr.substring(0, absStr.length - 3)
    val sb = StringBuilder()
    var count = 0
    for (i in rem.length - 1 downTo 0) {
        sb.append(rem[i])
        count++
        if (count % 2 == 0 && i != 0) {
            sb.append(',')
        }
    }
    val grouped = sb.reverse().toString()
    val formatted = "$grouped,$last3"
    return if (isNegative) "-$formatted" else formatted
}

fun formatPaiseToRupees(
    amountPaise: Long,
    includeSymbol: Boolean = true,
    includeDecimals: Boolean = true
): String {
    val isNegative = amountPaise < 0
    val absPaise = Math.abs(amountPaise)
    val rupees = absPaise / 100
    val paise = absPaise % 100

    val rupeeString = formatIndianNumber(rupees)
    val formatted = if (paise == 0L || !includeDecimals) {
        rupeeString
    } else {
        "$rupeeString.${paise.toString().padStart(2, '0')}"
    }

    val symbol = if (includeSymbol) "₹" else ""
    return if (isNegative) "-$symbol$formatted" else "$symbol$formatted"
}

/**
 * Formats paise adaptively for space-constrained UI elements:
 * - If >= 1 Crore: e.g. "₹1.36 Cr"
 * - If >= 1 Lakh: e.g. "₹2.25L"
 * - If < 1 Lakh: Indian formatted number with commas: e.g. "₹71,753", "₹6,060", "₹450"
 */
fun formatPaiseAdaptive(
    amountPaise: Long,
    compactIfOverLakh: Boolean = true,
    includeSymbol: Boolean = true
): String {
    val absPaise = Math.abs(amountPaise)
    val isNegative = amountPaise < 0
    val prefix = if (isNegative) "-" else ""
    val sym = if (includeSymbol) "₹" else ""
    val rupees = absPaise / 100.0

    if (compactIfOverLakh) {
        when {
            absPaise >= 1_00_00_000_00L -> {
                val cr = rupees / 1_00_00_000.0
                return String.format(java.util.Locale.US, "$prefix$sym%.2f Cr", cr)
            }
            absPaise >= 1_00_000_00L -> {
                val lakhs = rupees / 1_00_000.0
                return String.format(java.util.Locale.US, "$prefix$sym%.2fL", lakhs)
            }
        }
    }

    val rupeeInt = (absPaise / 100)
    val formatted = formatIndianNumber(rupeeInt)
    return "$prefix$sym$formatted"
}

/**
 * Returns formatted Lakhs/Crores string if >= 1 Lakh, or null if below 1 Lakh.
 * e.g. 118931698 -> "₹11.89 Lakhs"
 */
fun formatPaiseLakhsSubtitle(amountPaise: Long, includeSymbol: Boolean = true): String? {
    val absPaise = Math.abs(amountPaise)
    val isNegative = amountPaise < 0
    val prefix = if (isNegative) "-" else ""
    val sym = if (includeSymbol) "₹" else ""
    val rupees = absPaise / 100.0

    return when {
        absPaise >= 1_00_00_000_00L -> {
            val cr = rupees / 1_00_00_000.0
            String.format(java.util.Locale.US, "$prefix$sym%.2f Crores", cr)
        }
        absPaise >= 1_00_000_00L -> {
            val lakhs = rupees / 1_00_000.0
            String.format(java.util.Locale.US, "$prefix$sym%.2f Lakhs", lakhs)
        }
        else -> null
    }
}

fun rupeesToPaise(rupees: Double): Long {
    return Math.round(rupees * 100.0)
}

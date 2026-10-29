package com.spendly.app.domain.model

data class Budget(
    val id: Long = 0L,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val categoryColorHex: String? = null,
    val amountPaise: Long,
    val spentPaise: Long = 0L,
    val periodMonth: Int,
    val periodYear: Int,
    val isActive: Boolean = true,
    val thresholdPercent: Int = 80,
    val alertTriggered: Boolean = false,
    val isDemo: Boolean = false
) {
    val remainingPaise: Long
        get() = (amountPaise - spentPaise).coerceAtLeast(0L)

    val progress: Float
        get() = if (amountPaise > 0L) (spentPaise.toFloat() / amountPaise.toFloat()).coerceAtLeast(0f) else 0f

    val percentUsed: Int
        get() = Math.round(progress * 100f).coerceAtLeast(0)

    val formattedBudget: String
        get() = formatPaiseToRupees(amountPaise)

    val formattedTarget: String
        get() = formattedBudget

    val formattedSpent: String
        get() = formatPaiseToRupees(spentPaise)

    val formattedRemaining: String
        get() = formatPaiseToRupees(remainingPaise)

    val usedPercent: Int
        get() = percentUsed

    val progressFraction: Float
        get() = progress.coerceIn(0f, 1f)
}

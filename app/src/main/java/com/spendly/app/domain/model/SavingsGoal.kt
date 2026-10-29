package com.spendly.app.domain.model

data class SavingsGoal(
    val id: Long = 0L,
    val name: String,
    val targetAmountPaise: Long,
    val savedAmountPaise: Long,
    val deadlineMs: Long? = null,
    val iconName: String = "savings",
    val colorHex: String = "#C8FF5A",
    val isCompleted: Boolean = false,
    val isDemo: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
) {
    val remainingPaise: Long
        get() = (targetAmountPaise - savedAmountPaise).coerceAtLeast(0L)

    val progress: Float
        get() = if (targetAmountPaise > 0L) (savedAmountPaise.toFloat() / targetAmountPaise.toFloat()).coerceIn(0f, 1f) else 0f

    val percentSaved: Int
        get() = Math.round(progress * 100f).coerceIn(0, 100)

    val progressPercent: Int
        get() = percentSaved

    val progressFraction: Float
        get() = progress

    val formattedSaved: String
        get() = formatPaiseToRupees(savedAmountPaise)

    val formattedTarget: String
        get() = formatPaiseToRupees(targetAmountPaise)

    val formattedRemaining: String
        get() = formatPaiseToRupees(remainingPaise)
}

data class GoalContribution(
    val id: Long = 0L,
    val goalId: Long,
    val amountPaise: Long,
    val note: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
) {
    val formattedRupees: String
        get() = formatPaiseToRupees(amountPaise)
}

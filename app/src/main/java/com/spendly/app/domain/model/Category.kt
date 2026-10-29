package com.spendly.app.domain.model

data class Category(
    val id: Long = 0L,
    val name: String,
    val type: String = "EXPENSE",
    val colorHex: String,
    val iconName: String,
    val isSystem: Boolean = false,
    val sortOrder: Int = 0
)

data class CategorySpending(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val iconName: String,
    val totalAmountPaise: Long,
    val percentage: Float
) {
    val formattedRupees: String
        get() = formatPaiseToRupees(totalAmountPaise)
}

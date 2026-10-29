package com.spendly.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["period_year", "period_month", "category_id"], unique = true),
        Index(value = ["category_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null, // null = overall monthly budget

    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long,

    @ColumnInfo(name = "period_month")
    val periodMonth: Int, // 1 to 12

    @ColumnInfo(name = "period_year")
    val periodYear: Int, // e.g. 2025, 2026

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "threshold_percent")
    val thresholdPercent: Int = 80,

    @ColumnInfo(name = "alert_triggered")
    val alertTriggered: Boolean = false,

    @ColumnInfo(name = "is_demo")
    val isDemo: Boolean = false
)

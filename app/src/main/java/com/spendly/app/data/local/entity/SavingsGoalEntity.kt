package com.spendly.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "target_amount_paise")
    val targetAmountPaise: Long,

    @ColumnInfo(name = "saved_amount_paise")
    val savedAmountPaise: Long = 0L,

    @ColumnInfo(name = "deadline_ms")
    val deadlineMs: Long? = null,

    @ColumnInfo(name = "icon_name")
    val iconName: String = "savings",

    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#C8FF5A",

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "is_demo")
    val isDemo: Boolean = false,

    @ColumnInfo(name = "created_at_ms")
    val createdAtMs: Long = System.currentTimeMillis()
)

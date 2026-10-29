package com.spendly.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal_contributions",
    indices = [
        Index(value = ["goal_id"]),
        Index(value = ["timestamp_ms"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "goal_id")
    val goalId: Long,

    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long, // Positive for addition, negative for withdrawal

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long = System.currentTimeMillis()
)

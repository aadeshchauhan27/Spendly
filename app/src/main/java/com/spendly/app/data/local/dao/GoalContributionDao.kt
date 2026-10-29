package com.spendly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendly.app.data.local.entity.GoalContributionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalContributionDao {

    @Query("SELECT * FROM goal_contributions WHERE goal_id = :goalId ORDER BY timestamp_ms DESC")
    fun getContributionsForGoalFlow(goalId: Long): Flow<List<GoalContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contribution: GoalContributionEntity): Long

    @Delete
    suspend fun delete(contribution: GoalContributionEntity)

    @Query("DELETE FROM goal_contributions WHERE goal_id = :goalId")
    suspend fun deleteForGoal(goalId: Long)
}

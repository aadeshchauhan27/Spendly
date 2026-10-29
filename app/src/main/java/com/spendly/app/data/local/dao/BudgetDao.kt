package com.spendly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendly.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE period_year = :year AND period_month = :month")
    fun getBudgetsForPeriodFlow(year: Int, month: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE period_year = :year AND period_month = :month AND category_id IS NULL LIMIT 1")
    fun getOverallMonthlyBudgetFlow(year: Int, month: Int): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE period_year = :year AND period_month = :month AND category_id = :categoryId LIMIT 1")
    suspend fun getCategoryBudget(year: Int, month: Int, categoryId: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>): List<Long>

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM budgets WHERE is_demo = 1")
    suspend fun clearDemoBudgets()
}

package com.spendly.app.data.repository

import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.DashboardSummary
import com.spendly.app.domain.model.GoalContribution
import com.spendly.app.domain.model.ImportResultSummary
import com.spendly.app.domain.model.ImportSession
import com.spendly.app.domain.model.SavingsGoal
import com.spendly.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface SpendlyRepository {
    fun getAllTransactionsFlow(): Flow<List<Transaction>>
    fun getTransactionsBetweenFlow(startMs: Long, endMs: Long): Flow<List<Transaction>>
    fun searchTransactionsFlow(query: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Result<Long>
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>
    suspend fun deleteTransaction(id: Long): Result<Unit>
    suspend fun countSimilarTransactions(excludeId: Long, counterpartyName: String, title: String): Int
    suspend fun updateCategoryForSimilarTransactions(counterpartyName: String, title: String, newCategoryId: Long): Int

    fun getAllCategoriesFlow(): Flow<List<Category>>
    suspend fun insertCategory(category: Category): Result<Long>

    fun getBudgetsForPeriodFlow(year: Int, month: Int): Flow<List<Budget>>
    fun getOverallMonthlyBudgetFlow(year: Int, month: Int): Flow<Budget?>
    suspend fun upsertBudget(budget: Budget): Result<Long>
    suspend fun deleteBudget(id: Long): Result<Unit>

    fun getAllGoalsFlow(): Flow<List<SavingsGoal>>
    suspend fun insertGoal(goal: SavingsGoal): Result<Long>
    suspend fun updateGoal(goal: SavingsGoal): Result<Unit>
    suspend fun deleteGoal(id: Long): Result<Unit>
    suspend fun addGoalContribution(goalId: Long, amountPaise: Long, note: String?): Result<Long>
    fun getContributionsForGoalFlow(goalId: Long): Flow<List<GoalContribution>>

    fun getDashboardSummaryFlow(startMs: Long, endMs: Long): Flow<DashboardSummary>

    suspend fun batchInsertTransactions(
        transactions: List<Transaction>,
        fileName: String,
        source: String,
        duplicateCount: Int,
        errorCount: Int
    ): ImportResultSummary

    fun getAllImportSessionsFlow(): Flow<List<ImportSession>>
    suspend fun clearAllImportSessions()

    suspend fun insertTransactionFromSMS(sender: String, content: String): Result<Long>
    suspend fun insertTransactionsFromSMSBatch(messages: List<Pair<String, String>>): Result<List<Long>>

    suspend fun seedDemoData()
    suspend fun clearDemoData()
}

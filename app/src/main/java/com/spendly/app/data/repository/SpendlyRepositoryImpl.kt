package com.spendly.app.data.repository

import com.spendly.app.data.local.SpendlyDatabase
import com.spendly.app.data.local.entity.BudgetEntity
import com.spendly.app.data.local.entity.CategoryEntity
import com.spendly.app.data.local.entity.GoalContributionEntity
import com.spendly.app.data.local.entity.ImportSessionEntity
import com.spendly.app.data.local.entity.SavingsGoalEntity
import com.spendly.app.data.local.entity.TransactionEntity
import com.spendly.app.data.mapper.toDomain
import com.spendly.app.data.mapper.toEntity
import com.spendly.app.domain.engine.FinancialCalculationEngine
import com.spendly.app.domain.engine.MerchantNormalizer
import com.spendly.app.domain.engine.SMSTransactionParser
import com.spendly.app.domain.engine.TransactionDeduplicator
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.DashboardSummary
import com.spendly.app.domain.model.GoalContribution
import com.spendly.app.domain.model.ImportResultSummary
import com.spendly.app.domain.model.ImportSession
import com.spendly.app.domain.model.SavingsGoal
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SpendlyRepositoryImpl(
    private val database: SpendlyDatabase
) : SpendlyRepository {

    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val budgetDao = database.budgetDao()
    private val savingsGoalDao = database.savingsGoalDao()
    private val goalContributionDao = database.goalContributionDao()
    private val importSessionDao = database.importSessionDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                database.seedDefaultCategories()
                transactionDao.sanitizeSpamTransactions()
                transactionDao.cleanupMiscalibratedIdfcLimit()
            } catch (e: Exception) {
                // Silently handle
            }
        }
    }

    override fun getAllTransactionsFlow(): Flow<List<Transaction>> {
        return combine(
            transactionDao.getAllTransactionsFlow(),
            categoryDao.getAllCategoriesFlow()
        ) { txEntities, catEntities ->
            val catMap = catEntities.associateBy { it.id }
            txEntities.map { entity ->
                val cat = catMap[entity.categoryId]
                entity.toDomain(
                    categoryName = cat?.name ?: "Others",
                    categoryColorHex = cat?.colorHex ?: "#546E7A"
                )
            }
        }
    }

    override fun getTransactionsBetweenFlow(startMs: Long, endMs: Long): Flow<List<Transaction>> {
        return combine(
            transactionDao.getTransactionsBetweenFlow(startMs, endMs),
            categoryDao.getAllCategoriesFlow()
        ) { txEntities, catEntities ->
            val catMap = catEntities.associateBy { it.id }
            txEntities.map { entity ->
                val cat = catMap[entity.categoryId]
                entity.toDomain(
                    categoryName = cat?.name ?: "Others",
                    categoryColorHex = cat?.colorHex ?: "#546E7A"
                )
            }
        }
    }

    override fun searchTransactionsFlow(query: String): Flow<List<Transaction>> {
        return combine(
            transactionDao.searchTransactionsFlow(query),
            categoryDao.getAllCategoriesFlow()
        ) { txEntities, catEntities ->
            val catMap = catEntities.associateBy { it.id }
            txEntities.map { entity ->
                val cat = catMap[entity.categoryId]
                entity.toDomain(
                    categoryName = cat?.name ?: "Others",
                    categoryColorHex = cat?.colorHex ?: "#546E7A"
                )
            }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        val entity = transactionDao.getTransactionById(id) ?: return null
        val cat = categoryDao.getCategoryById(entity.categoryId)
        return entity.toDomain(
            categoryName = cat?.name ?: "Others",
            categoryColorHex = cat?.colorHex ?: "#546E7A"
        )
    }

    override suspend fun insertTransaction(transaction: Transaction): Result<Long> {
        // Validation: Positive amount
        if (transaction.amountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Transaction amount must be strictly greater than 0"))
        }
        if (transaction.counterpartyName.isBlank() && transaction.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Merchant or transaction title cannot be empty"))
        }

        // Deduplication check against existing transactions
        val existing = getAllTransactionsFlow().first()
        val duplicate = TransactionDeduplicator.findDuplicate(transaction, existing)
        if (duplicate != null) {
            return Result.failure(IllegalStateException("Duplicate transaction detected matching: ${duplicate.title} (₹${duplicate.amountPaise / 100})"))
        }

        val normalizedMerchant = MerchantNormalizer.normalize(transaction.counterpartyName.ifBlank { transaction.title })
        val entityToSave = transaction.copy(
            counterpartyName = normalizedMerchant,
            title = if (transaction.title.isBlank()) normalizedMerchant else transaction.title
        ).toEntity()

        val newId = transactionDao.insert(entityToSave)
        return Result.success(newId)
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
        if (transaction.amountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Transaction amount must be strictly greater than 0"))
        }
        val entity = transaction.copy(updatedAtMs = System.currentTimeMillis()).toEntity()
        transactionDao.update(entity)
        return Result.success(Unit)
    }

    override suspend fun deleteTransaction(id: Long): Result<Unit> {
        transactionDao.deleteById(id)
        return Result.success(Unit)
    }

    override suspend fun countSimilarTransactions(excludeId: Long, counterpartyName: String, title: String): Int {
        return transactionDao.countSimilarTransactions(excludeId, counterpartyName.trim(), title.trim())
    }

    override suspend fun updateCategoryForSimilarTransactions(counterpartyName: String, title: String, newCategoryId: Long): Int {
        return transactionDao.updateCategoryForSimilarTransactions(counterpartyName.trim(), title.trim(), newCategoryId)
    }

    override fun getAllCategoriesFlow(): Flow<List<Category>> {
        return categoryDao.getAllCategoriesFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertCategory(category: Category): Result<Long> {
        if (category.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Category name cannot be empty"))
        }
        val id = categoryDao.insert(category.toEntity())
        return Result.success(id)
    }

    override fun getBudgetsForPeriodFlow(year: Int, month: Int): Flow<List<Budget>> {
        return combine(
            budgetDao.getBudgetsForPeriodFlow(year, month),
            categoryDao.getAllCategoriesFlow(),
            getAllTransactionsFlow()
        ) { budgetEntities, catEntities, allTx ->
            val catMap = catEntities.associateBy { it.id }

            // Calculate spent amount for each budget category in this month/year
            budgetEntities.map { entity ->
                val cat = entity.categoryId?.let { catMap[it] }
                val spentPaise = if (entity.categoryId == null) {
                    // Overall budget: Sum of all expenses in period
                    allTx.filter { tx ->
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = tx.timestampMs }
                        val txYear = cal.get(java.util.Calendar.YEAR)
                        val txMonth = cal.get(java.util.Calendar.MONTH) + 1
                        txYear == year && txMonth == month && tx.type == TransactionType.EXPENSE && tx.status == TransactionStatus.COMPLETED
                    }.sumOf { it.amountPaise }
                } else {
                    // Category budget
                    allTx.filter { tx ->
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = tx.timestampMs }
                        val txYear = cal.get(java.util.Calendar.YEAR)
                        val txMonth = cal.get(java.util.Calendar.MONTH) + 1
                        txYear == year && txMonth == month && tx.categoryId == entity.categoryId && tx.type == TransactionType.EXPENSE && tx.status == TransactionStatus.COMPLETED
                    }.sumOf { it.amountPaise }
                }

                entity.toDomain(
                    categoryName = cat?.name ?: "Overall Budget",
                    categoryColorHex = cat?.colorHex ?: "#C8FF5A",
                    spentPaise = spentPaise
                )
            }
        }
    }

    override fun getOverallMonthlyBudgetFlow(year: Int, month: Int): Flow<Budget?> {
        return getBudgetsForPeriodFlow(year, month).map { list ->
            list.find { it.categoryId == null }
        }
    }

    override suspend fun upsertBudget(budget: Budget): Result<Long> {
        if (budget.amountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Budget amount must be strictly greater than 0"))
        }
        val id = budgetDao.insertOrUpdate(budget.toEntity())
        return Result.success(id)
    }

    override suspend fun deleteBudget(id: Long): Result<Unit> {
        budgetDao.deleteById(id)
        return Result.success(Unit)
    }

    override fun getAllGoalsFlow(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getAllGoalsFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertGoal(goal: SavingsGoal): Result<Long> {
        if (goal.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Goal name cannot be empty"))
        }
        if (goal.targetAmountPaise <= 0L) {
            return Result.failure(IllegalArgumentException("Target amount must be greater than 0"))
        }
        val id = savingsGoalDao.insert(goal.toEntity())
        return Result.success(id)
    }

    override suspend fun updateGoal(goal: SavingsGoal): Result<Unit> {
        savingsGoalDao.update(goal.toEntity())
        return Result.success(Unit)
    }

    override suspend fun deleteGoal(id: Long): Result<Unit> {
        savingsGoalDao.deleteById(id)
        return Result.success(Unit)
    }

    override suspend fun addGoalContribution(goalId: Long, amountPaise: Long, note: String?): Result<Long> {
        if (amountPaise == 0L) {
            return Result.failure(IllegalArgumentException("Contribution amount cannot be zero"))
        }
        val contribution = GoalContributionEntity(
            goalId = goalId,
            amountPaise = amountPaise,
            note = note,
            timestampMs = System.currentTimeMillis()
        )
        val id = goalContributionDao.insert(contribution)
        // Atomically update saved amount on goal
        savingsGoalDao.updateSavedAmount(goalId, amountPaise)
        return Result.success(id)
    }

    override fun getContributionsForGoalFlow(goalId: Long): Flow<List<GoalContribution>> {
        return goalContributionDao.getContributionsForGoalFlow(goalId).map { list -> list.map { it.toDomain() } }
    }

    override fun getDashboardSummaryFlow(startMs: Long, endMs: Long): Flow<DashboardSummary> {
        val periodDuration = endMs - startMs
        val prevStartMs = startMs - periodDuration
        val prevEndMs = startMs

        return combine(
            getTransactionsBetweenFlow(startMs, endMs),
            getTransactionsBetweenFlow(prevStartMs, prevEndMs),
            getAllCategoriesFlow()
        ) { currentTxs, prevTxs, categories ->
            FinancialCalculationEngine.calculateSummary(
                currentPeriodTransactions = currentTxs,
                previousPeriodTransactions = prevTxs,
                categories = categories
            )
        }
    }

    override suspend fun seedDemoData() {
        // Ensure default system categories exist. Dummy transactions, budgets and goals removed.
        database.seedDefaultCategories()
        transactionDao.sanitizeSpamTransactions()
        transactionDao.cleanupMiscalibratedIdfcLimit()
    }

    override suspend fun batchInsertTransactions(
        transactions: List<Transaction>,
        fileName: String,
        source: String,
        duplicateCount: Int,
        errorCount: Int
    ): ImportResultSummary {
        val sessionEntity = ImportSessionEntity(
            source = source,
            fileName = fileName,
            totalRows = transactions.size + duplicateCount + errorCount,
            importedCount = transactions.size,
            duplicateCount = duplicateCount,
            errorCount = errorCount,
            timestampMs = System.currentTimeMillis()
        )
        val sessionId = importSessionDao.insert(sessionEntity)

        val entities = transactions.map { txn ->
            txn.copy(importSessionId = sessionId).toEntity()
        }

        transactionDao.insertAll(entities)

        return ImportResultSummary(
            sessionId = sessionId,
            fileName = fileName,
            totalRows = sessionEntity.totalRows,
            importedCount = transactions.size,
            duplicateCount = duplicateCount,
            errorCount = errorCount,
            timestampMs = sessionEntity.timestampMs
        )
    }

    override fun getAllImportSessionsFlow(): Flow<List<ImportSession>> {
        return importSessionDao.getAllSessionsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun clearAllImportSessions() {
        importSessionDao.clearAll()
    }

    override suspend fun clearDemoData() {
        transactionDao.clearDemoTransactions()
        budgetDao.clearDemoBudgets()
        savingsGoalDao.clearDemoGoals()
    }

    override suspend fun insertTransactionFromSMS(sender: String, content: String): Result<Long> {
        val availableCategories = categoryDao.getAllCategoriesFlow().map { list -> list.map { it.toDomain() } }.firstOrNull() ?: emptyList()
        
        val transaction = SMSTransactionParser.parseSms(
            sender = sender,
            content = content,
            availableCategories = availableCategories
        ) ?: return Result.failure(IllegalArgumentException("SMS content does not appear to be a transaction"))
        
        val existingTransactions = getAllTransactionsFlow().firstOrNull() ?: emptyList()
        val duplicate = TransactionDeduplicator.findDuplicate(transaction, existingTransactions)
        
        if (duplicate != null) {
            return Result.failure(IllegalStateException("Duplicate transaction detected"))
        }
        
        val entity = transaction.copy(
            source = TransactionSource.SMS_IMPORT,
            createdAtMs = System.currentTimeMillis(),
            updatedAtMs = System.currentTimeMillis()
        ).toEntity()
        
        val newId = transactionDao.insert(entity)
        return Result.success(newId)
    }

    override suspend fun insertTransactionsFromSMSBatch(messages: List<Pair<String, String>>): Result<List<Long>> {
        val availableCategories = categoryDao.getAllCategoriesFlow().map { list -> list.map { it.toDomain() } }.firstOrNull() ?: emptyList()
        val existingTransactions = getAllTransactionsFlow().firstOrNull() ?: emptyList()
        
        val insertedIds = mutableListOf<Long>()
        
        for ((sender, content) in messages) {
            val transaction = SMSTransactionParser.parseSms(
                sender = sender,
                content = content,
                availableCategories = availableCategories
            )
            
            if (transaction != null) {
                val duplicate = TransactionDeduplicator.findDuplicate(transaction, existingTransactions)
                
                if (duplicate == null) {
                    val entity = transaction.copy(
                        source = TransactionSource.SMS_IMPORT,
                        createdAtMs = System.currentTimeMillis(),
                        updatedAtMs = System.currentTimeMillis()
                    ).toEntity()
                    
                    val newId = transactionDao.insert(entity)
                    insertedIds.add(newId)
                }
            }
        }
        
        return Result.success(insertedIds)
    }
}

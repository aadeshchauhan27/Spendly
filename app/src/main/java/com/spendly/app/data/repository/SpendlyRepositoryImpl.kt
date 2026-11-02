package com.spendly.app.data.repository

import com.spendly.app.data.local.SpendlyDatabase
import com.spendly.app.data.local.entity.BudgetEntity
import com.spendly.app.data.local.entity.CategoryEntity
import com.spendly.app.data.local.entity.DeletedTransactionEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpendlyRepositoryImpl(
    private val database: SpendlyDatabase
) : SpendlyRepository {

    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val budgetDao = database.budgetDao()
    private val savingsGoalDao = database.savingsGoalDao()
    private val goalContributionDao = database.goalContributionDao()
    private val importSessionDao = database.importSessionDao()
    private val deletedTransactionDao = database.deletedTransactionDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                database.seedDefaultCategories()
                transactionDao.sanitizeSpamTransactions()
                transactionDao.cleanupMiscalibratedIdfcLimit()
                transactionDao.cleanupUpcomingMandates()
                transactionDao.cleanupMisclassifiedCred()
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
                    categoryColorHex = cat?.colorHex ?: "#546E7A",
                    categoryIconName = cat?.iconName ?: ""
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
                    categoryColorHex = cat?.colorHex ?: "#546E7A",
                    categoryIconName = cat?.iconName ?: ""
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
                    categoryColorHex = cat?.colorHex ?: "#546E7A",
                    categoryIconName = cat?.iconName ?: ""
                )
            }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        val entity = transactionDao.getTransactionById(id) ?: return null
        val cat = categoryDao.getCategoryById(entity.categoryId)
        return entity.toDomain(
            categoryName = cat?.name ?: "Others",
            categoryColorHex = cat?.colorHex ?: "#546E7A",
            categoryIconName = cat?.iconName ?: ""
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
        val existing = transactionDao.getTransactionById(id)
        if (existing != null) {
            deletedTransactionDao.insert(
                DeletedTransactionEntity(
                    bankReferenceNum = existing.bankReferenceNum,
                    rawIdentifier = existing.rawIdentifier,
                    amountPaise = existing.amountPaise,
                    timestampMs = existing.timestampMs,
                    counterpartyName = existing.counterpartyName,
                    smsBody = existing.tag
                )
            )
        }
        transactionDao.deleteById(id)
        return Result.success(Unit)
    }

    override suspend fun getAllDeletedTransactions(): List<DeletedTransactionEntity> {
        return deletedTransactionDao.getAllDeleted()
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
                categories = categories,
                startMs = startMs,
                endMs = endMs
            )
        }
    }

    override suspend fun seedDemoData() {
        // Ensure default system categories exist. Dummy transactions, budgets and goals removed.
        database.seedDefaultCategories()
        transactionDao.sanitizeSpamTransactions()
        transactionDao.cleanupMiscalibratedIdfcLimit()
        transactionDao.cleanupUpcomingMandates()
        transactionDao.cleanupMisclassifiedCred()
        transactionDao.markFailedTransactions()
        cleanupDuplicateTransactions()
    }

    private suspend fun cleanupDuplicateTransactions() {
        val allEntities = transactionDao.getAllTransactionsList()
        if (allEntities.size < 2) return

        val domainList = allEntities.map { it.toDomain() }
        val toDelete = mutableSetOf<Long>()
        val toUpdate = mutableListOf<TransactionEntity>()

        val grouped = domainList.groupBy { it.amountPaise to it.direction }

        for ((_, group) in grouped) {
            if (group.size < 2) continue

            val dayGroups = mutableMapOf<String, MutableList<Transaction>>()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            for (tx in group) {
                val key = sdf.format(Date(tx.timestampMs))
                dayGroups.getOrPut(key) { mutableListOf() }.add(tx)
            }

            for ((_, dayTxs) in dayGroups) {
                if (dayTxs.size < 2) continue

                for (i in 0 until dayTxs.size) {
                    val tx1 = dayTxs[i]
                    if (tx1.id in toDelete) continue

                    for (j in i + 1 until dayTxs.size) {
                        val tx2 = dayTxs[j]
                        if (tx2.id in toDelete) continue

                        val merchantMatch = MerchantNormalizer.normalize(tx1.counterpartyName).equals(
                            MerchantNormalizer.normalize(tx2.counterpartyName), ignoreCase = true
                        ) || (tx1.title.isNotBlank() && tx1.title.equals(tx2.title, ignoreCase = true)) ||
                        (tx1.notes != null && tx2.notes != null && tx1.notes.contains("ending") && tx1.notes == tx2.notes)

                        val timeDiff = Math.abs(tx1.timestampMs - tx2.timestampMs)
                        val isMid1 = TransactionDeduplicator.isMidnight(tx1.timestampMs)
                        val isMid2 = TransactionDeduplicator.isMidnight(tx2.timestampMs)

                        val isDuplicate = merchantMatch && (timeDiff <= 15 * 60 * 1000L || isMid1 || isMid2)

                        if (isDuplicate) {
                            val candidateKeep = if (TransactionDeduplicator.shouldPreferCandidate(tx2, tx1)) tx2 else tx1
                            val dropTx = if (candidateKeep.id == tx1.id) tx2 else tx1

                            var finalKeep = candidateKeep
                            if (TransactionDeduplicator.isMidnight(finalKeep.timestampMs) && !TransactionDeduplicator.isMidnight(dropTx.timestampMs)) {
                                finalKeep = finalKeep.copy(timestampMs = dropTx.timestampMs)
                            }
                            if (finalKeep.bankReferenceNum.isNullOrBlank() && !dropTx.bankReferenceNum.isNullOrBlank()) {
                                finalKeep = finalKeep.copy(bankReferenceNum = dropTx.bankReferenceNum)
                            }

                            if (finalKeep != candidateKeep) {
                                toUpdate.add(finalKeep.toEntity())
                            }

                            toDelete.add(dropTx.id)
                        }
                    }
                }
            }
        }

        for (id in toDelete) {
            transactionDao.deleteById(id)
        }
        for (entity in toUpdate) {
            transactionDao.update(entity)
        }
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
        transactionDao.markFailedTransactions()

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

    override suspend fun clearAllTransactions() {
        transactionDao.clearAll()
    }

    override suspend fun insertTransactionFromSMS(sender: String, content: String): Result<Long> {
        val availableCategories = categoryDao.getAllCategoriesFlow().map { list -> list.map { it.toDomain() } }.firstOrNull() ?: emptyList()
        
        val existingTransactions = getAllTransactionsFlow().firstOrNull() ?: emptyList()
        val transaction = SMSTransactionParser.parseSms(
            sender = sender,
            content = content,
            availableCategories = availableCategories,
            existingTransactions = existingTransactions
        ) ?: return Result.failure(IllegalArgumentException("SMS content does not appear to be a transaction"))
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
                availableCategories = availableCategories,
                existingTransactions = existingTransactions
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

    override suspend fun deduplicateDatabaseTransactions(): Int {
        transactionDao.markFailedTransactions()
        transactionDao.deleteFailedTransactions()
        transactionDao.cleanupCcSettlementDuplicates()
        transactionDao.cleanupUpcomingMandates()

        val allEntities = transactionDao.getAllTransactionsList()
        val allTx = allEntities.map { it.toDomain() }
        val toDelete = mutableListOf<Long>()
        val kept = mutableListOf<Transaction>()

        for (tx in allTx) {
            val duplicate = TransactionDeduplicator.findDuplicate(tx, kept)
            if (duplicate != null) {
                if (TransactionDeduplicator.shouldPreferCandidate(tx, duplicate)) {
                    toDelete.add(duplicate.id)
                    kept.remove(duplicate)
                    kept.add(tx)
                    transactionDao.update(tx.copy(id = duplicate.id).toEntity())
                } else {
                    toDelete.add(tx.id)
                }
            } else {
                kept.add(tx)
            }
        }

        toDelete.distinct().forEach { id ->
            transactionDao.deleteById(id)
        }

        // Also fix any transactions that have generic title but specific ACH/NACH merchant in tag/body, or false phone UTR
        for (tx in kept) {
            val hasPhoneUtr = tx.bankReferenceNum?.startsWith("91") == true && tx.bankReferenceNum.length == 12
            if ((TransactionDeduplicator.isGenericMerchant(tx.counterpartyName) || hasPhoneUtr) && !tx.tag.isNullOrBlank()) {
                val parsed = SMSTransactionParser.parseSms(
                    sender = "BANK",
                    content = tx.tag,
                    messageTimestampMs = tx.timestampMs
                )
                if (parsed != null) {
                    val updated = tx.copy(
                        title = if (!TransactionDeduplicator.isGenericMerchant(parsed.title)) parsed.title else tx.title,
                        counterpartyName = if (!TransactionDeduplicator.isGenericMerchant(parsed.counterpartyName)) parsed.counterpartyName else tx.counterpartyName,
                        counterpartyUpiId = if (tx.counterpartyUpiId.isNullOrBlank()) parsed.counterpartyUpiId else tx.counterpartyUpiId,
                        bankReferenceNum = if (hasPhoneUtr || tx.bankReferenceNum.isNullOrBlank()) parsed.bankReferenceNum else tx.bankReferenceNum
                    )
                    transactionDao.update(updated.toEntity())
                }
            }
        }

        return toDelete.size
    }
}

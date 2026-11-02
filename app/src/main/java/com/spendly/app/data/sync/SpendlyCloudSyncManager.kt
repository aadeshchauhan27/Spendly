package com.spendly.app.data.sync

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.spendly.app.core.preferences.UserPreferences
import com.spendly.app.data.local.SpendlyDatabase
import com.spendly.app.data.local.entity.BudgetEntity
import com.spendly.app.data.local.entity.CategoryEntity
import com.spendly.app.data.local.entity.SavingsGoalEntity
import com.spendly.app.data.local.entity.TransactionEntity
import com.spendly.app.data.mapper.toDomain
import com.spendly.app.data.mapper.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class CloudSyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    DISABLED
}

class SpendlyCloudSyncManager(
    private val context: Context,
    private val database: SpendlyDatabase,
    private val userPreferences: UserPreferences
) {
    companion object {
        private const val TAG = "SpendlyCloudSync"
        private const val USERS_COLLECTION = "spendly_users"
        private const val TRANSACTIONS_SUBCOLLECTION = "transactions"
        private const val CATEGORIES_SUBCOLLECTION = "categories"
        private const val BUDGETS_SUBCOLLECTION = "budgets"
        private const val GOALS_SUBCOLLECTION = "goals"
    }

    private val _syncStatus = MutableStateFlow(CloudSyncStatus.IDLE)
    val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

    private val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Exception) {
            false
        }

    suspend fun syncNow(): Result<Unit> = withContext(Dispatchers.IO) {
        val email = userPreferences.googleAccountEmailFlow.first()
        if (email.isNullOrBlank()) {
            _syncStatus.value = CloudSyncStatus.DISABLED
            return@withContext Result.failure(IllegalStateException("Please sign in with Google to enable cloud backup."))
        }

        val isOptedIn = userPreferences.isCloudSyncEnabledFlow.first()
        if (!isOptedIn) {
            _syncStatus.value = CloudSyncStatus.DISABLED
            return@withContext Result.failure(IllegalStateException("Cloud backup is turned off."))
        }

        if (!isFirebaseAvailable) {
            _syncStatus.value = CloudSyncStatus.ERROR
            return@withContext Result.failure(IllegalStateException("Google Firebase Services unavailable."))
        }

        _syncStatus.value = CloudSyncStatus.SYNCING
        try {
            val firestore = FirebaseFirestore.getInstance()
            val userDoc = firestore.collection(USERS_COLLECTION).document(email.trim().lowercase())

            // 1. Upload Categories
            val categories = database.categoryDao().getAllCategoriesFlow().first()
            for (cat in categories) {
                val data = mapOf(
                    "id" to cat.id,
                    "name" to cat.name,
                    "type" to cat.type,
                    "colorHex" to cat.colorHex,
                    "iconName" to cat.iconName,
                    "isSystem" to cat.isSystem,
                    "sortOrder" to cat.sortOrder
                )
                userDoc.collection(CATEGORIES_SUBCOLLECTION).document(cat.id.toString())
                    .set(data, SetOptions.merge()).await()
            }

            // 2. Upload Transactions
            val transactions = database.transactionDao().getAllTransactionsList()
            for (tx in transactions) {
                val data = mapOf(
                    "id" to tx.id,
                    "amountPaise" to tx.amountPaise,
                    "title" to tx.title,
                    "counterpartyName" to tx.counterpartyName,
                    "categoryId" to tx.categoryId,
                    "type" to tx.type,
                    "direction" to tx.direction,
                    "status" to tx.status,
                    "source" to tx.source,
                    "paymentApp" to tx.paymentApp,
                    "bankReferenceNum" to tx.bankReferenceNum,
                    "rawIdentifier" to tx.rawIdentifier,
                    "notes" to tx.notes,
                    "tag" to tx.tag,
                    "timestampMs" to tx.timestampMs,
                    "createdAtMs" to tx.createdAtMs,
                    "updatedAtMs" to tx.updatedAtMs
                )
                userDoc.collection(TRANSACTIONS_SUBCOLLECTION).document(tx.id.toString())
                    .set(data, SetOptions.merge()).await()
            }

            // Update user document metadata
            val metadata = mapOf(
                "email" to email,
                "lastBackupTimestamp" to System.currentTimeMillis(),
                "transactionCount" to transactions.size,
                "backupVersion" to 1
            )
            userDoc.set(metadata, SetOptions.merge()).await()

            userPreferences.setLastCloudSyncTime(System.currentTimeMillis())
            _syncStatus.value = CloudSyncStatus.SUCCESS
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Cloud sync failed", e)
            _syncStatus.value = CloudSyncStatus.ERROR
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloud(): Result<Int> = withContext(Dispatchers.IO) {
        val email = userPreferences.googleAccountEmailFlow.first()
        if (email.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Sign in with Google to restore backup."))
        }

        if (!isFirebaseAvailable) {
            return@withContext Result.failure(IllegalStateException("Firebase Services unavailable."))
        }

        _syncStatus.value = CloudSyncStatus.SYNCING
        try {
            val firestore = FirebaseFirestore.getInstance()
            val userDoc = firestore.collection(USERS_COLLECTION).document(email.trim().lowercase())

            // 1. Restore Categories
            val catSnap = userDoc.collection(CATEGORIES_SUBCOLLECTION).get().await()
            val restoredCats = catSnap.documents.mapNotNull { doc ->
                val id = doc.getLong("id") ?: return@mapNotNull null
                val name = doc.getString("name") ?: return@mapNotNull null
                val type = doc.getString("type") ?: "EXPENSE"
                val colorHex = doc.getString("colorHex") ?: "#546E7A"
                val iconName = doc.getString("iconName") ?: "category"
                val isSystem = doc.getBoolean("isSystem") ?: false
                val sortOrder = doc.getLong("sortOrder")?.toInt() ?: 0

                CategoryEntity(
                    id = id,
                    name = name,
                    type = type,
                    colorHex = colorHex,
                    iconName = iconName,
                    isSystem = isSystem,
                    sortOrder = sortOrder
                )
            }
            if (restoredCats.isNotEmpty()) {
                database.categoryDao().insertAll(restoredCats)
            }

            // 2. Restore Transactions
            val txSnap = userDoc.collection(TRANSACTIONS_SUBCOLLECTION).get().await()
            val restoredTxs = txSnap.documents.mapNotNull { doc ->
                val id = doc.getLong("id") ?: return@mapNotNull null
                val amountPaise = doc.getLong("amountPaise") ?: return@mapNotNull null
                val title = doc.getString("title") ?: "UPI Payment"
                val counterpartyName = doc.getString("counterpartyName") ?: ""
                val categoryId = doc.getLong("categoryId") ?: 8L
                val type = doc.getString("type") ?: "EXPENSE"
                val direction = doc.getString("direction") ?: "OUTGOING"
                val status = doc.getString("status") ?: "COMPLETED"
                val source = doc.getString("source") ?: "SMS_IMPORT"
                val paymentApp = doc.getString("paymentApp")
                val bankRef = doc.getString("bankReferenceNum")
                val rawId = doc.getString("rawIdentifier")
                val notes = doc.getString("notes")
                val tag = doc.getString("tag")
                val timestampMs = doc.getLong("timestampMs") ?: System.currentTimeMillis()
                val createdAtMs = doc.getLong("createdAtMs") ?: System.currentTimeMillis()
                val updatedAtMs = doc.getLong("updatedAtMs") ?: System.currentTimeMillis()

                TransactionEntity(
                    id = id,
                    amountPaise = amountPaise,
                    title = title,
                    counterpartyName = counterpartyName,
                    categoryId = categoryId,
                    type = type,
                    direction = direction,
                    status = status,
                    source = source,
                    paymentApp = paymentApp,
                    bankReferenceNum = bankRef,
                    rawIdentifier = rawId,
                    notes = notes,
                    tag = tag,
                    timestampMs = timestampMs,
                    createdAtMs = createdAtMs,
                    updatedAtMs = updatedAtMs
                )
            }

            if (restoredTxs.isNotEmpty()) {
                database.transactionDao().insertAll(restoredTxs)
            }

            userPreferences.setLastCloudSyncTime(System.currentTimeMillis())
            _syncStatus.value = CloudSyncStatus.SUCCESS
            Result.success(restoredTxs.size)
        } catch (e: Exception) {
            Log.e(TAG, "Restore from cloud failed", e)
            _syncStatus.value = CloudSyncStatus.ERROR
            Result.failure(e)
        }
    }
}

package com.spendly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendly.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp_ms DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp_ms DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE timestamp_ms BETWEEN :startMs AND :endMs ORDER BY timestamp_ms DESC")
    fun getTransactionsBetweenFlow(startMs: Long, endMs: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category_id = :categoryId ORDER BY timestamp_ms DESC")
    fun getTransactionsByCategoryFlow(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE title LIKE '%' || :query || '%' OR counterparty_name LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY timestamp_ms DESC")
    fun searchTransactionsFlow(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE bank_reference_num = :refNum LIMIT 1")
    suspend fun getTransactionByRefNum(refNum: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE is_demo = 1")
    suspend fun clearDemoTransactions()

    @Query("""
        UPDATE transactions 
        SET type = 'SPAM', category_id = :spamCategoryId
        WHERE title LIKE '%Disbursement%'
           OR title LIKE '%Require Consent%'
           OR title LIKE '%Consent To Continue%'
           OR title LIKE '%Can Be Converted Into Emis%'
           OR title LIKE '%Emis With%'
           OR title LIKE '%Loan Of%'
    """)
    suspend fun sanitizeSpamTransactions(spamCategoryId: Long = 9L): Int

    @Query("DELETE FROM transactions WHERE amount_paise = 40158075 AND raw_identifier LIKE '%IDFCFB%'")
    suspend fun cleanupMiscalibratedIdfcLimit(): Int

    @Query("""
        DELETE FROM transactions 
        WHERE id = 3481 
           OR amount_paise = 536664 
           OR tag LIKE '%upcoming mandate%' 
           OR tag LIKE '%pause mandate%' 
           OR tag LIKE '%merchantmandate%'
           OR notes LIKE '%upcoming mandate%'
           OR notes LIKE '%pause mandate%'
           OR notes LIKE '%merchantmandate%'
    """)
    suspend fun cleanupUpcomingMandates(): Int

    @Query("""
        UPDATE transactions
        SET title = 'Money Received', counterparty_name = 'Money Received'
        WHERE (title = 'CRED' OR counterparty_name = 'CRED')
          AND direction = 'INCOMING'
          AND (tag IS NULL OR tag NOT LIKE '%cred cash%')
          AND (notes IS NULL OR notes NOT LIKE '%cred cash%')
    """)
    suspend fun cleanupMisclassifiedCred(): Int

    @Query("""
        UPDATE transactions 
        SET status = 'FAILED' 
        WHERE status != 'FAILED'
          AND (
            title LIKE '%failed%' 
            OR title LIKE '%declined%' 
            OR title LIKE '%unsuccessful%' 
            OR title LIKE '%autopay%' 
            OR counterparty_name LIKE '%failed%' 
            OR notes LIKE '%failed%' 
            OR raw_identifier LIKE '%failed%' 
            OR raw_identifier LIKE '%declined%' 
            OR raw_identifier LIKE '%autopay%'
          )
    """)
    suspend fun markFailedTransactions(): Int

    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE id != :excludeId 
          AND (
              (counterparty_name = :counterpartyName AND counterparty_name != '') 
              OR (title = :title AND title != '')
          )
    """)
    suspend fun countSimilarTransactions(excludeId: Long, counterpartyName: String, title: String): Int

    @Query("""
        UPDATE transactions 
        SET category_id = :newCategoryId 
        WHERE (counterparty_name = :counterpartyName AND counterparty_name != '') 
           OR (title = :title AND title != '')
    """)
    suspend fun updateCategoryForSimilarTransactions(counterpartyName: String, title: String, newCategoryId: Long): Int

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

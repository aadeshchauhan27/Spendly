package com.spendly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendly.app.data.local.entity.DeletedTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeletedTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deleted: DeletedTransactionEntity): Long

    @Query("SELECT * FROM deleted_transactions ORDER BY deleted_at_ms DESC")
    suspend fun getAllDeleted(): List<DeletedTransactionEntity>

    @Query("SELECT * FROM deleted_transactions ORDER BY deleted_at_ms DESC")
    fun getAllDeletedFlow(): Flow<List<DeletedTransactionEntity>>

    @Query("DELETE FROM deleted_transactions")
    suspend fun clearAll()
}

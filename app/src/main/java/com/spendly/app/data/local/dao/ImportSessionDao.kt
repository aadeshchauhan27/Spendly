package com.spendly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendly.app.data.local.entity.ImportSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportSessionDao {

    @Query("SELECT * FROM import_sessions ORDER BY timestamp_ms DESC")
    fun getAllSessionsFlow(): Flow<List<ImportSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ImportSessionEntity): Long

    @Query("DELETE FROM import_sessions")
    suspend fun clearAll()
}

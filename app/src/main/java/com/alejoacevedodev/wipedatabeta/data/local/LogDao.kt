package com.alejoacevedodev.wipedatabeta.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alejoacevedodev.wipedatabeta.domain.model.WipeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: WipeLog)

    @Query("SELECT * FROM wipe_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WipeLog>>
}
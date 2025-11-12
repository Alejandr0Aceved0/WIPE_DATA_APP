package com.alejoacevedodev.wipe_data_beta.domain.repository

import com.alejoacevedodev.wipe_data_beta.domain.model.WipeLog
import kotlinx.coroutines.flow.Flow

interface ILogRepository {
    suspend fun saveLog(log: WipeLog)

    fun getLogs(): Flow<List<WipeLog>>
}
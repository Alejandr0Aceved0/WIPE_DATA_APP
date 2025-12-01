package com.alejoacevedodev.wipedatabeta.domain.repository

import com.alejoacevedodev.wipedatabeta.domain.model.WipeLog
import kotlinx.coroutines.flow.Flow

interface ILogRepository {
    suspend fun saveLog(log: WipeLog)

    fun getLogs(): Flow<List<WipeLog>>
}
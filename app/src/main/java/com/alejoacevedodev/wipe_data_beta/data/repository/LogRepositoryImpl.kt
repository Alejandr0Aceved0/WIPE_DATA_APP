package com.alejoacevedodev.wipe_data_beta.data.repository

import com.alejoacevedodev.wipe_data_beta.data.local.LogDao
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeLog
import com.alejoacevedodev.wipe_data_beta.domain.repository.ILogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Esta es la implementación "real" de ILogRepository.
 * Su único trabajo es hablar con el DAO (Room).
 */
class LogRepositoryImpl @Inject constructor(
    private val logDao: LogDao // Pide el DAO
) : ILogRepository {

    override suspend fun saveLog(log: WipeLog) {
        logDao.insert(log)
    }

    override fun getLogs(): Flow<List<WipeLog>> {
        return logDao.getAllLogs()
    }
}
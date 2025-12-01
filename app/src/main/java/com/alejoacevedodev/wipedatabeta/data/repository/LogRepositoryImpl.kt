package com.alejoacevedodev.wipedatabeta.data.repository

import com.alejoacevedodev.wipedatabeta.data.local.LogDao
import com.alejoacevedodev.wipedatabeta.domain.model.WipeLog
import com.alejoacevedodev.wipedatabeta.domain.repository.ILogRepository
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
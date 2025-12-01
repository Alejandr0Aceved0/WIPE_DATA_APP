package com.alejoacevedodev.wipedatabeta.domain.usecase

import com.alejoacevedodev.wipedatabeta.domain.model.WipeLog
import com.alejoacevedodev.wipedatabeta.domain.repository.ILogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para obtener todos los registros (logs) de borrado.
 */
class GetLogsUseCase @Inject constructor(
    private val logRepository: ILogRepository // Inyecta la interfaz del repositorio
) {

    /**
     * Al invocar esta clase, simplemente llama al método getLogs()
     * del repositorio, que devuelve un Flow.
     */
    operator fun invoke(): Flow<List<WipeLog>> {
        return logRepository.getLogs()
    }
}
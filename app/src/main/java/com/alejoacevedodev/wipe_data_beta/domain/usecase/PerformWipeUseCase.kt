package com.alejoacevedodev.wipe_data_beta.domain.usecase

import android.net.Uri
import com.alejoacevedodev.wipe_data_beta.data.repository.IWipeRepository
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeLog
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeMethod
import com.alejoacevedodev.wipe_data_beta.domain.repository.ILogRepository
import javax.inject.Inject

class PerformWipeUseCase @Inject constructor(
    private val wipeRepository: IWipeRepository,
    private val logRepository: ILogRepository
) {
    /**
     * Ejecuta el borrado llamando al repositorio.
     * * @param uri El URI de la carpeta o archivo a borrar.
     * @param method El método de borrado (DoD, NIST, BSI).
     * @param fileName El nombre del recurso principal (para el log).
     * @return Una lista de Strings con los nombres de todos los archivos y carpetas eliminados.
     */
    suspend operator fun invoke(uri: Uri, method: WipeMethod, fileName: String): List<String> {
        // 1. Ejecuta el borrado en el repositorio
        // Ahora 'wipe' devuelve un Result<List<String>>
        val result = wipeRepository.wipe(uri, method)

        // 2. Obtiene la lista de archivos borrados (o lista vacía si falló)
        val deletedFiles = result.getOrDefault(emptyList())
        val count = deletedFiles.size

        // 3. Determina el estado para el log interno
        val statusStr = if (result.isSuccess) "SUCCESS" else "FAILURE"

        // 4. Guarda el registro en la base de datos local (Room)
        val log = WipeLog(
            fileName = fileName,
            method = method,
            timestamp = System.currentTimeMillis(),
            status = "$statusStr ($count items)"
        )
        logRepository.saveLog(log)

        // 5. Devuelve la lista detallada para que el ViewModel la use en el PDF
        return deletedFiles
    }
}
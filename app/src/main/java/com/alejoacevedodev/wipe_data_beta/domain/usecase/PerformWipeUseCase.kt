package com.alejoacevedodev.wipe_data_beta.domain.usecase

import android.net.Uri
import com.alejoacevedodev.wipe_data_beta.data.model.WipeResult
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
     *
     * @param uri El URI de la carpeta o archivo a borrar.
     * @param method El método de borrado (DoD, NIST, BSI).
     * @param fileName El nombre del recurso principal (para el log interno).
     * @return Un objeto WipeResult con la lista de archivos y el total de bytes liberados.
     */
    suspend operator fun invoke(uri: Uri, method: WipeMethod, fileName: String): WipeResult {
        // 1. Ejecuta el borrado en el repositorio
        // Ahora 'wipe' devuelve un Result<WipeResult>
        val result = wipeRepository.wipe(uri, method)

        // 2. Obtiene el resultado o un valor vacío por defecto si falló
        val wipeData = result.getOrDefault(WipeResult(emptyList(), 0L))

        val count = wipeData.deletedFiles.size
        val statusStr = if (result.isSuccess) "SUCCESS" else "FAILURE"

        // 3. Guarda el registro en la base de datos local (Room) para historial
        val log = WipeLog(
            fileName = fileName,
            method = method,
            timestamp = System.currentTimeMillis(),
            // Guardamos un resumen en el status: "SUCCESS (25 items)"
            status = "$statusStr ($count items)"
        )
        logRepository.saveLog(log)

        // 4. Devuelve los datos completos al ViewModel para el reporte PDF
        return wipeData
    }
}
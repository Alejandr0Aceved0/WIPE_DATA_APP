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
    suspend operator fun invoke(uri: Uri, method: WipeMethod, fileName: String): Int {
        val result = wipeRepository.wipe(uri, method)

        val itemsDeleted = result.getOrDefault(0)
        val statusStr = if (result.isSuccess) "SUCCESS" else "FAILURE"

        // Registra en el log
        val log = WipeLog(
            fileName = fileName,
            method = method,
            timestamp = System.currentTimeMillis(),
            status = "$statusStr ($itemsDeleted items)"
        )
        logRepository.saveLog(log)

        // Devuelve la cantidad para la UI
        return itemsDeleted
    }
}
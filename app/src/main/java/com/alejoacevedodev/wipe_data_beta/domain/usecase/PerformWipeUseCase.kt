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
    suspend operator fun invoke(uri: Uri, method: WipeMethod, fileName: String) {

        val result = wipeRepository.wipe(uri, method)

        // Registra el resultado en el log
        val log = WipeLog(
            fileName = fileName, // Necesitarás una forma de obtener el nombre
            method = method,
            timestamp = System.currentTimeMillis(),
            status = if (result.isSuccess) "SUCCESS" else "FAILURE"
        )
        logRepository.saveLog(log)
    }
}
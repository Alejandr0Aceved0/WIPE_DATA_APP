package com.alejoacevedodev.wipe_data_beta.presentation.viewmodel

import android.net.Uri
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeMethod

data class WipeUiState(
    // Selección de Archivos
    val selectedFolders: List<Uri> = emptyList(),
    val selectedMethod: WipeMethod? = null,

    // Estado de Ejecución
    val isWiping: Boolean = false,
    val currentWipingFile: String = "",

    // Resultados del Proceso (Para el Reporte)
    val wipeFinished: Boolean = false,
    val deletedCount: Int = 0,
    val wipeStartTime: Long = 0,
    val wipeEndTime: Long = 0,
    // Lista detallada de archivos borrados para el PDF
    val deletedFilesList: List<String> = emptyList(),

    // Configuración FTP
    val ftpHost: String = "",
    val ftpPort: String = "21",
    val ftpUser: String = "",
    val ftpPass: String = ""
)
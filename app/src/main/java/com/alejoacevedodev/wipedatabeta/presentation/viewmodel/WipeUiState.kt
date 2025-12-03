package com.alejoacevedodev.wipedatabeta.presentation.viewmodel

import android.net.Uri
import com.alejoacevedodev.wipedatabeta.domain.model.WipeMethod

data class WipeUiState(
    // Selección de Archivos
    val userName: String = "",
    val selectedFolders: List<Uri> = emptyList(),
    val selectedMethod: WipeMethod? = null,

    // Estado de Ejecución
    val isWiping: Boolean = false,
    val currentWipingFile: String = "",

    // Resultados del Proceso (Para el Reporte)
    val wipeFinished: Boolean = false,
    val deletedCount: Int = 0,
    val deletedFilesList: List<String> = emptyList(),
    val freedBytes: Long = 0L,
    val wipeStartTime: Long = 0,
    val wipeEndTime: Long = 0,

    // Configuración FTP
    val ftpHost: String = "",
    val ftpPort: String = "21",
    val ftpUser: String = "",
    val ftpPass: String = "",


    val packagesToWipe: List<String> = emptyList(),
    val packageWeights: MutableMap<String, Long> = mutableMapOf(),
    val isPackageSelected: Boolean = false,
    val isFolderSelected: Boolean = false
)
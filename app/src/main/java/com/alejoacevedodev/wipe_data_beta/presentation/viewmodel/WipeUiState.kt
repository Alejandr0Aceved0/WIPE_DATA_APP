package com.alejoacevedodev.wipe_data_beta.presentation.viewmodel

import android.net.Uri
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeMethod

data class WipeUiState(
    val selectedFolders: List<Uri> = emptyList(),
    val selectedMethod: WipeMethod? = null,
    val isWiping: Boolean = false,
    val currentWipingFile: String = "",
    val wipeFinished: Boolean = false,
    val deletedCount: Int = 0
)
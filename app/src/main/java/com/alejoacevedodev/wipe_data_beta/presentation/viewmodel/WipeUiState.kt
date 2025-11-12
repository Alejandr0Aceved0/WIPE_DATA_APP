package com.alejoacevedodev.wipe_data_beta.presentation.viewmodel

import android.net.Uri
import com.alejoacevedodev.wipe_data_beta.domain.model.WipeMethod

data class WipeUiState(
    val selectedFolders: List<Uri> = emptyList(),
    val selectedMethod: WipeMethod = WipeMethod.NIST_SP_800_88,
    val isWiping: Boolean = false
)
package com.alejoacevedodev.wipe_data_beta.data.model

data class WipeResult(
    val deletedFiles: List<String>,
    val freedBytes: Long
)
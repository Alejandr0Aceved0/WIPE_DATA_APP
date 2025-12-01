package com.alejoacevedodev.wipedatabeta.data.model

data class WipeResult(
    val deletedFiles: List<String>,
    val freedBytes: Long
)
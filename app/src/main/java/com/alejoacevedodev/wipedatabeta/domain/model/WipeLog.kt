package com.alejoacevedodev.wipedatabeta.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wipe_logs")
data class WipeLog(
    // 2. Añade la anotación @PrimaryKey
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val method: WipeMethod,
    val timestamp: Long,
    val status: String // "SUCCESS" o "FAILURE"
)
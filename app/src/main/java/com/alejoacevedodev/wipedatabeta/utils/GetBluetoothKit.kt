package com.alejoacevedodev.wipedatabeta.utils

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

fun getBluetoothKitName(): String {
    return try {
        val process = Shizuku.newProcess(arrayOf("settings", "get", "global", "bluetooth_name"), null, null)
        val name = BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim()
        process.waitFor()

        if (name == "null" || name.isNullOrEmpty()) {
            // Intento en tabla 'secure' si 'global' falla
            val processAlt = Shizuku.newProcess(arrayOf("settings", "get", "secure", "bluetooth_name"), null, null)
            val nameAlt = BufferedReader(InputStreamReader(processAlt.inputStream)).readLine()?.trim()
            processAlt.waitFor()
            nameAlt ?: "Sin_Kit"
        } else {
            name
        }
    } catch (e: Exception) {
        "Error_Kit"
    }
}
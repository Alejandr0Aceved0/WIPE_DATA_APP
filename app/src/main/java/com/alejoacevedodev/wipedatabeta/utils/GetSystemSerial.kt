package com.alejoacevedodev.wipedatabeta.utils

import android.os.Build
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

fun getSystemSerial(): String {
    return try {
        val process = Shizuku.newProcess(arrayOf("getprop", "ro.serialno"), null, null)
        val serial = BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim()
        process.waitFor()
        if (serial.isNullOrEmpty() || serial == "unknown") Build.SERIAL else serial
    } catch (e: Exception) {
        Build.SERIAL // Fallback al serial estándar (que suele ser "unknown" en Android 10+)
    }
}
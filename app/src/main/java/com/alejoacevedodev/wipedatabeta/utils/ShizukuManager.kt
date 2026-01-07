package com.alejoacevedodev.wipedatabeta.utils

import android.content.pm.PackageManager
import com.alejoacevedodev.wipedatabeta.data.model.WipeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import javax.inject.Inject

/**
 * Copyright 2026 Wireless&Mobile, Inc. All rights reserved
 * File ShizukuManager 1.2.5 6/01/26
 *
 * @author Carlos Alejandro Acevedo Espitia
 * @version 1.2.5 6/01/26
 * @since 1.2.5
 */

class ShizukuManager @Inject constructor() {

    suspend fun executeMultiWipe(packages: List<String>): WipeResult {
        return withContext(Dispatchers.IO) {
            var total = 0L
            val logs = mutableListOf<String>()
            packages.forEach { pkg ->
                val size = getPackageSize(pkg)
                if (performPmClear(pkg)) {
                    total += size
                    logs.add("Paquete $pkg eliminado ($size bytes)")
                }
            }
            WipeResult(logs, total)
        }
    }

    private fun performPmClear(pkg: String): Boolean {
        val process = Shizuku.newProcess(arrayOf("sh", "-c", "pm clear $pkg"), null, null)
        return process.waitFor() == 0
    }

    private fun getPackageSize(pkg: String): Long {
        return 0L
    }

    /**
     * Verifica si el servicio de Shizuku está activo y el binder disponible.
     */
    fun isServiceAvailable(): Boolean {
        return Shizuku.pingBinder()
    }

    /**
     * Comprueba si el permiso de Shizuku ha sido otorgado por el usuario.
     */
    fun isPermissionGranted(): Boolean {
        return if (isServiceAvailable()) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    /**
     * Lanza la solicitud de permiso nativa de Shizuku.
     */
    fun requestPermission(requestCode: Int) {
        if (isServiceAvailable()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    /**
     * Ejecuta un comando shell vía Shizuku para validar si un paquete existe.
     */
    fun checkIfPackageExists(packageName: String): Boolean {
        if (!isServiceAvailable()) return false

        return try {
            // Comando: pm path <nombre_paquete>
            val command = arrayOf("sh", "-c", "pm path $packageName")
            val process = Shizuku.newProcess(command, null, null)
            val reader = process.inputStream.bufferedReader()
            val output = reader.readLine()
            process.waitFor()

            // Si la salida contiene "package:", el paquete existe
            output != null && output.startsWith("package:")
        } catch (e: Exception) {
            false
        }
    }
}
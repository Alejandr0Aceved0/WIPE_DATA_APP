package com.alejoacevedodev.wipedatabeta.shizuku

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.alejoacevedodev.wipedatabeta.IClearService
import rikka.shizuku.Shizuku
import java.io.InputStreamReader

class ShizukuClearService : Service() {

    private val binder = object : IClearService.Stub() {

        override fun clearAppData(packageName: String): String {
            val currentUid = Shizuku.getUid()
            val command = "pm clear $packageName"
            Log.d("ShizukuService", "UID de ejecución: $currentUid. Comando: $command")

            return try {
                Log.d("ShizukuService", "Ejecutando comando: $command")
                val process = Runtime.getRuntime().exec(command)
                process.waitFor()

                val output = InputStreamReader(process.inputStream).readText().trim()

                val error = InputStreamReader(process.errorStream).readText().trim()

                if (process.exitValue() == 0 && output.equals("Success", ignoreCase = true)) {
                    Log.d("ShizukuService", "Comando ejecutado con éxito: $output")
                    "SUCCESS: $packageName (UID $currentUid)"
                } else {
                    Log.e("ShizukuService", "Error al ejecutar comando: $error")
                    "FAILED: $packageName - Error: $error. Output: $output"
                }
            } catch (e: Exception) {
                Log.e("ShizukuService", "Excepción al ejecutar comando", e)
                "EXCEPTION: ${e.message}"
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d("ShizukuService", "Servicio iniciado con éxito.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ShizukuService", "Servicio destruido.")
    }
}
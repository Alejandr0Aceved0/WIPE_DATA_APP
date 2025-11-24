package com.alejoacevedodev.wipe_data_beta.utils

import android.util.Log
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FtpUploader {

    private const val TAG = "FtpLog"

    suspend fun uploadFile(
        file: File,
        host: String,
        user: String,
        pass: String,
        port: Int = 21
    ): Boolean = withContext(Dispatchers.IO) {

        Log.d(TAG, "🚀 INICIANDO INTENTO DE SUBIDA (AUTO-DETECT)")

        // INTENTO 1: MODO PASIVO (El estándar moderno)
        if (uploadWithMode(file, host, user, pass, port, usePassive = true)) {
            return@withContext true
        }

        Log.w(TAG, "⚠ Modo Pasivo falló. Intentando Modo Activo...")

        // INTENTO 2: MODO ACTIVO (Fallback para redes corporativas/viejas)
        if (uploadWithMode(file, host, user, pass, port, usePassive = false)) {
            return@withContext true
        }

        Log.e(TAG, "❌ Todos los intentos fallaron.")
        return@withContext false
    }

    private fun uploadWithMode(
        file: File,
        host: String,
        user: String,
        pass: String,
        port: Int,
        usePassive: Boolean
    ): Boolean {
        val ftpClient = FTPClient()
        // Timeouts agresivos pero razonables
        ftpClient.connectTimeout = 15000 // 15s para conectar
        ftpClient.setDataTimeout(15000)
        ftpClient.bufferSize = 1024 * 1024 // 1MB buffer

        try {
            Log.d(TAG, "🔌 Conectando a $host:$port (${if (usePassive) "Pasivo" else "Activo"})...")
            ftpClient.connect(host, port)

            if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
                Log.e(TAG, "❌ Rechazo inicial: ${ftpClient.replyString}")
                ftpClient.disconnect()
                return false
            }

            if (!ftpClient.login(user, pass)) {
                Log.e(TAG, "❌ Login inválido.")
                ftpClient.logout()
                return false
            }

            // --- CONFIGURACIÓN CLAVE ---
            if (usePassive) {
                ftpClient.enterLocalPassiveMode()
                // CRÍTICO: Desactivar verificación de IP remota (Fix para NAT/4G)
                ftpClient.isRemoteVerificationEnabled = false
            } else {
                ftpClient.enterLocalActiveMode()
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            // Forzar codificación UTF-8 para nombres de archivo (evita errores con tildes)
            ftpClient.controlEncoding = "UTF-8"

            Log.d(TAG, "📤 Subiendo archivo...")

            val success = FileInputStream(file).use { inputStream ->
                ftpClient.storeFile(file.name, inputStream)
            }

            if (success) {
                Log.i(TAG, "✅ ¡SUBIDA EXITOSA EN MODO ${if (usePassive) "PASIVO" else "ACTIVO"}!")
                ftpClient.logout()
                return true
            } else {
                Log.e(TAG, "❌ Falló storeFile. Respuesta: ${ftpClient.replyString}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción en modo ${if (usePassive) "Pasivo" else "Activo"}: ${e.message}")
        } finally {
            try { if (ftpClient.isConnected) ftpClient.disconnect() } catch (e: Exception) {}
        }
        return false
    }
}
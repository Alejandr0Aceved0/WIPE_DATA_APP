package com.alejoacevedodev.wipe_data_beta.utils

import android.content.Context

/**
 * Clase utilitaria para guardar y recuperar la configuración del FTP
 * (Host, Puerto, Usuario, Contraseña) usando SharedPreferences.
 */
object FtpPrefs {
    private const val PREF_NAME = "ftp_config"
    private const val KEY_HOST = "ftp_host"
    private const val KEY_PORT = "ftp_port" // Clave para el puerto
    private const val KEY_USER = "ftp_user"
    private const val KEY_PASS = "ftp_pass"

    /**
     * Guarda la configuración completa, incluyendo el puerto.
     */
    fun saveConfig(context: Context, host: String, port: Int, user: String, pass: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_HOST, host)
            putInt(KEY_PORT, port)
            putString(KEY_USER, user)
            putString(KEY_PASS, pass)
            apply() // Guarda asíncronamente
        }
    }

    fun getHost(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HOST, "") ?: ""

    /**
     * Recupera el PUERTO. Si no existe, devuelve 21 por defecto.
     */
    fun getPort(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_PORT, 21)

    fun getUser(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER, "") ?: ""

    fun getPass(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PASS, "") ?: ""
}
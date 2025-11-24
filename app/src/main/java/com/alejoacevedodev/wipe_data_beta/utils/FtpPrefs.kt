package com.alejoacevedodev.wipe_data_beta.utils

import android.content.Context

/**
 * Clase utilitaria para guardar y recuperar la configuración del FTP
 * usando SharedPreferences.
 */
object FtpPrefs {
    // Nombre del archivo de preferencias
    private const val PREF_NAME = "ftp_config"

    // Claves para los valores
    private const val KEY_HOST = "ftp_host"
    private const val KEY_PORT = "ftp_host"
    private const val KEY_USER = "ftp_user"
    private const val KEY_PASS = "ftp_pass"

    /**
     * Guarda los datos de conexión.
     */
    fun saveConfig(context: Context, host: String, user: String, pass: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_HOST, host)
            putString(KEY_USER, user)
            putString(KEY_PASS, pass)
            apply() // Guarda los cambios asíncronamente
        }
    }

    /**
     * Recupera el HOST (o devuelve cadena vacía si no existe).
     */
    fun getHost(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PORT, "") ?: ""

    fun getPort(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HOST, "") ?: ""

    /**
     * Recupera el USUARIO.
     */
    fun getUser(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER, "") ?: ""

    /**
     * Recupera la CONTRASEÑA.
     */
    fun getPass(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PASS, "") ?: ""
}
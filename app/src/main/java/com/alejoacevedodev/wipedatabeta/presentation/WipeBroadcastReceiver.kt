package com.alejoacevedodev.wipedatabeta.presentation

/**
 * Copyright 2026 Wireless&Mobile, Inc. All rights reserved
 * File WipeBroadcastReceiver 1.2.5 14/01/26
 *
 * @author Carlos Alejandro Acevedo Espitia
 * @version 1.2.5 14/01/26
 * @since 1.2.5
 */

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WipeBroadcastReceiver : BroadcastReceiver() {

    // Nota: Si usas Hilt, puedes inyectar tu repositorio o servicio de borrado directamente
    // para evitar depender de la UI/ViewModel si la app está en segundo plano.

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "com.alejoacevedodev.START_REMOTE_WIPE") {
            Log.d("WIPE_REMOTE", "Orden de borrado recibida vía ADB")

            // Aquí lanzas tu servicio de borrado Shizuku
            val serviceIntent = Intent(context, com.alejoacevedodev.wipedatabeta.shizuku.ShizukuClearService::class.java)
            serviceIntent.putExtra("ACTION", "START_FULL_WIPE")
            context.startService(serviceIntent)
        }
    }
}
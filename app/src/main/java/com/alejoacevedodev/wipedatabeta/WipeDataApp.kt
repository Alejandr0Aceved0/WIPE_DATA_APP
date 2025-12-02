package com.alejoacevedodev.wipedatabeta

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import rikka.shizuku.Shizuku

@HiltAndroidApp
class WipeDataApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Shizuku.addBinderReceivedListener {
            Log.i("Shizuku", "Binder recibido: Shizuku está listo")
        }

        Shizuku.addBinderDeadListener {
            Log.w("Shizuku", "Shizuku murió, reiniciar conexión")
        }
    }
}
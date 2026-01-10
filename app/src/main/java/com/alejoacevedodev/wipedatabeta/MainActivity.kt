package com.alejoacevedodev.wipedatabeta

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.alejoacevedodev.wipedatabeta.presentation.ui.screen.MainScreen
import com.alejoacevedodev.wipedatabeta.ui.theme.WIPE_DATA_BETATheme
import dagger.hilt.android.AndroidEntryPoint
import rikka.shizuku.Shizuku


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            onShizukuPermissionResult(grantResult)
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        setContent {
            WIPE_DATA_BETATheme {
                MainScreen(
                    onRequestStoragePermission = { launcher ->
                        requestManageAllFilesPermission(launcher)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }

    private fun onShizukuPermissionResult(grantResult: Int) {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            // Granted
        } else {
            // Denied
        }
    }

    private fun requestManageAllFilesPermission(
        launcher: ActivityResultLauncher<Intent>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                launcher.launch(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        .setData(Uri.parse("package:$packageName"))
                )
            } catch (e: Exception) {
                launcher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }
}
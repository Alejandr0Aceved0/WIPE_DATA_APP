package com.alejoacevedodev.wipe_data_beta

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alejoacevedodev.wipe_data_beta.presentation.ui.WipeScreen
import com.alejoacevedodev.wipe_data_beta.presentation.ui.theme.WipeDataTestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WipeDataTestTheme {

                // 1. Estado para rastrear el permiso
                var hasPermission by remember {
                    mutableStateOf(checkPermission())
                }

                // 2. [CÓDIGO FALTANTE] Launcher para abrir los Ajustes
                val settingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    // 3. Cuando el usuario vuelve, revisamos de nuevo
                    hasPermission = checkPermission()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (hasPermission) {
                        // 4. Si hay permiso, muestra la app principal
                        WipeScreen(modifier = Modifier.padding(innerPadding))
                    } else {
                        // 5. Si NO hay permiso, muestra la pantalla de solicitud
                        PermissionRequestScreen(
                            modifier = Modifier.padding(innerPadding),
                            onRequestPermission = {
                                // 6. [CÓDIGO FALTANTE] Llamamos a la función
                                requestPermission(settingsLauncher)
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Verifica si el permiso MANAGE_EXTERNAL_STORAGE está concedido.
     */
    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            Environment.isExternalStorageManager()
        } else {
            // En versiones antiguas (<11), asumimos 'true'
            true
        }
    }

    /**
     * [CÓDIGO FALTANTE]
     * Lanza la pantalla de Ajustes del sistema para que el usuario
     * conceda el permiso manualmente.
     */
    private fun requestPermission(launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // Intenta abrir la pantalla específica de la app
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                launcher.launch(intent)
            } catch (e: Exception) {
                // Fallback: abrir la pantalla genérica de gestión de archivos
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                launcher.launch(intent)
            }
        }
    }
}

/**
 * [CÓDIGO FALTANTE]
 * Un Composable simple que se muestra cuando el usuario
 * aún no ha concedido el permiso de almacenamiento.
 */
@Composable
private fun PermissionRequestScreen(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit // La acción que se ejecuta al presionar el botón
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Permiso Requerido",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Para borrar archivos de forma segura en carpetas externas (como DCIM o Descargas), esta aplicación necesita permiso para 'Gestionar todos los archivos'.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("Ir a Ajustes para Conceder Permiso")
            }
        }
    }
}
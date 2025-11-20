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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alejoacevedodev.wipe_data_beta.presentation.ui.ConfirmationScreen
import com.alejoacevedodev.wipe_data_beta.presentation.ui.LoginScreen
import com.alejoacevedodev.wipe_data_beta.presentation.ui.OriginSelectionScreen
import com.alejoacevedodev.wipe_data_beta.presentation.ui.WipeMethodScreen
import com.alejoacevedodev.wipe_data_beta.presentation.ui.theme.WipeDataTestTheme
import com.alejoacevedodev.wipe_data_beta.presentation.viewmodel.WipeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WipeDataTestTheme {

                var hasPermission by remember {
                    mutableStateOf(checkPermission())
                }

                val settingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    hasPermission = checkPermission()
                }

                if (hasPermission) {
                    AppNavigation()
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        PermissionRequestScreen(
                            modifier = Modifier.padding(innerPadding),
                            onRequestPermission = {
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
            true
        }
    }

    /**
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
 * Configuración de navegación entre pantallas.
 * Flujo: Login -> Orígenes -> Métodos -> Confirmación
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val sharedViewModel: WipeViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "login") {

        //Pantalla de Login
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("origins")
                }
            )
        }

        //Pantalla de Selección de Origen (Carpetas)
        composable("origins") {
            OriginSelectionScreen(
                viewModel = sharedViewModel,
                onNavigateToMethods = {
                    navController.navigate("methods")
                }
            )
        }

        //Pantalla de Selección de Método
        composable("methods") {
            WipeMethodScreen(
                viewModel = sharedViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onMethodSelected = {
                    navController.navigate("confirmation")
                }
            )
        }

        // Pantalla de Confirmación y Borrado
        composable("confirmation") {
            ConfirmationScreen(
                viewModel = sharedViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProcessFinished = {
                    // Volver a la pantalla de orígenes limpiando la pila
                    navController.navigate("origins") {
                        popUpTo("origins") { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
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
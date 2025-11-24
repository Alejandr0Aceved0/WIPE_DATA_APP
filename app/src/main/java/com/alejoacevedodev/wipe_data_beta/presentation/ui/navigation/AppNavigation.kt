package com.alejoacevedodev.wipe_data_beta.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alejoacevedodev.wipe_data_beta.presentation.ui.screen.ConfirmationScreen
import com.alejoacevedodev.wipe_data_beta.presentation.ui.screen.FtpSettingsScreen
import com.alejoacevedodev.wipe_data_beta.presentation.ui.screen.LoginScreen
import com.alejoacevedodev.wipe_data_beta.presentation.ui.screen.OriginSelectionScreen
import com.alejoacevedodev.wipe_data_beta.presentation.ui.screen.ReportScreen
import com.alejoacevedodev.wipe_data_beta.presentation.ui.screen.WipeMethodScreen
import com.alejoacevedodev.wipe_data_beta.presentation.viewmodel.WipeViewModel

/**
 * Configuración de navegación entre pantallas.
 * Flujo: Login -> Orígenes -> Métodos -> Confirmación -> Reporte
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val sharedViewModel: WipeViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "login") {

        // 1. LOGIN
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("origins")
                },
                onConfigureFtp = {
                    navController.navigate("ftp_settings")
                }
            )
        }

        // 2. CONFIGURACIÓN FTP
        composable("ftp_settings") {
            FtpSettingsScreen(
                viewModel = sharedViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 3. SELECCIÓN DE ORIGEN (CARPETAS)
        composable("origins") {
            OriginSelectionScreen(
                viewModel = sharedViewModel,
                onNavigateToMethods = {
                    navController.navigate("methods")
                }
            )
        }

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

        // 5. CONFIRMACIÓN Y PROCESO
        composable("confirmation") {
            ConfirmationScreen(
                viewModel = sharedViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProcessFinished = {
                    // Al terminar el borrado, vamos al REPORTE
                    navController.navigate("report")
                }
            )
        }

        // 6. REPORTE FINAL
        composable("report") {
            ReportScreen(
                viewModel = sharedViewModel,
                onNavigateHome = {
                    // Reseteamos el estado para una nueva operación
                    sharedViewModel.resetWipeStatus()

                    // Volvemos a la pantalla de selección de carpetas (origins)
                    // y limpiamos el historial de navegación
                    navController.navigate("origins") {
                        popUpTo("origins") { inclusive = true }
                    }
                }
            )
        }
    }
}
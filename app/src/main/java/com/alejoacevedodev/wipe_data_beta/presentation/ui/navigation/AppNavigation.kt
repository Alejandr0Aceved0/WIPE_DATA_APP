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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // ViewModel compartido para mantener el estado (carpetas, método) durante el flujo
    val sharedViewModel: WipeViewModel = hiltViewModel()

    // Función helper para cerrar sesión y volver al login
    val navigateToLogin = {
        sharedViewModel.resetWipeStatus() // Limpiamos estado
        navController.navigate("login") {
            // Limpiamos toda la pila de navegación hasta el inicio
            popUpTo(0) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = "login") {

        // 1. LOGIN
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("origins")
                }
            )
        }

        // 2. CONFIGURACIÓN FTP
        composable("ftp_settings") {
            FtpSettingsScreen(
                viewModel = sharedViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateHome = { navigateToLogin() },
            )
        }

        // 3. SELECCIÓN DE ORIGEN (CARPETAS)
        composable("origins") {
            OriginSelectionScreen(
                viewModel = sharedViewModel,
                onNavigateToMethods = {
                    navController.navigate("methods")
                },
                onNavigateHome = { navigateToLogin() },
                onConfigureFtp = {
                    navController.navigate("ftp_settings")
                }
            )
        }

        // 4. SELECCIÓN DE MÉTODO
        composable("methods") {
            WipeMethodScreen(
                viewModel = sharedViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onMethodSelected = {
                    navController.navigate("confirmation")
                },
                onNavigateHome = { navigateToLogin() } // Salir al Login
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
                    navController.navigate("report")
                },
                onNavigateHome = { navigateToLogin() } // Salir al Login
            )
        }

        // 6. REPORTE FINAL
        composable("report") {
            ReportScreen(
                viewModel = sharedViewModel,
                onNavigateHome = {
                    // En el reporte, "Volver al inicio" significa volver a seleccionar carpetas
                    // (No necesariamente logout, pero si quieres logout usa navigateToLogin())
                    sharedViewModel.resetWipeStatus()
                    navController.navigate("origins") {
                        popUpTo("origins") { inclusive = true }
                    }
                }
            )
        }
    }
}
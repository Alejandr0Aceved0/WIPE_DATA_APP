package com.alejoacevedodev.wipedatabeta.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alejoacevedodev.wipedatabeta.presentation.ui.screen.ConfirmationScreen
import com.alejoacevedodev.wipedatabeta.presentation.ui.screen.FtpSettingsScreen
import com.alejoacevedodev.wipedatabeta.presentation.ui.screen.LoginScreen
import com.alejoacevedodev.wipedatabeta.presentation.ui.screen.OriginSelectionScreen
import com.alejoacevedodev.wipedatabeta.presentation.ui.screen.ReportScreen
import com.alejoacevedodev.wipedatabeta.presentation.ui.screen.WipeMethodScreen
import com.alejoacevedodev.wipedatabeta.presentation.viewmodel.WipeViewModel

/**
 * Configuración de navegación entre pantallas.
 * Flujo: Login -> Orígenes -> Métodos -> Confirmación -> Reporte
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // ViewModel compartido para mantener el estado durante el flujo
    val sharedViewModel: WipeViewModel = hiltViewModel()

    // Función para volver al login y limpiar la pila
    val navigateToLogin = {
        sharedViewModel.resetWipeStatus()
        navController.navigate("login") {
            popUpTo("login") { inclusive = true }
        }
    }

    val isLoggedIn by sharedViewModel.isLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "origins" else "login"
    ) {

        // 1. LOGIN
        composable("login") {
            LoginScreen(
                onLoginSuccess = { userName ->
                    sharedViewModel.loginSuccess(userName)
                    navController.navigate("origins") {
                        popUpTo("login") { inclusive = true }
                    }
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
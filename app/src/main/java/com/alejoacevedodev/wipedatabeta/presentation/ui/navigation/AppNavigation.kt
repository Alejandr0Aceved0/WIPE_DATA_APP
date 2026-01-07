package com.alejoacevedodev.wipedatabeta.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alejoacevedodev.wipedatabeta.presentation.auth.AuthViewModel
import com.alejoacevedodev.wipedatabeta.presentation.settings.SettingsViewModel
import com.alejoacevedodev.wipedatabeta.presentation.ui.screen.*
import com.alejoacevedodev.wipedatabeta.presentation.wipe.WipeViewModel

/**
 * Gestor central de navegación de la aplicación (Jetpack Navigation Compose).
 * * Define el flujo de trabajo secuencial del borrado de datos y la jerarquía de pantallas.
 * Utiliza Dependency Injection via Hilt para proveer los ViewModels necesarios en cada nodo.
 * *
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    /** * sharedViewModel: Se declara a este nivel para que persista durante todo el flujo
     * de borrado (Origins -> Methods -> Confirmation -> Report).
     * Mantiene los datos de selección del usuario a través de las pantallas.
     */
    val sharedViewModel: WipeViewModel = hiltViewModel()

    /** * authViewModel: Gestiona el estado global de la sesión.
     */
    val authViewModel: AuthViewModel = hiltViewModel()

    /**
     * Helper para realizar el Logout.
     * Limpia el estado de borrado y reinicia la pila de navegación hacia el Login.
     */
    val navigateToLogin = {
        sharedViewModel.resetWipeStatus()
        navController.navigate("login") {
            popUpTo("login") { inclusive = true }
        }
    }

    // Observa el estado de sesión para decidir el punto de entrada (Conditional Navigation)
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "origins" else "login"
    ) {

        /**
         * 1. Pantalla de Autenticación.
         * Punto de entrada inicial si no hay sesión activa.
         */
        composable("login") {
            LoginScreen(
                onLoginSuccess = { userName ->
                    authViewModel.loginSuccess(userName)
                    navController.navigate("origins") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        /**
         * 2. Configuración FTP.
         * Pantalla independiente para gestionar credenciales de red.
         * Utiliza [SettingsViewModel] con ciclo de vida limitado a este destino.
         */
        composable("ftp_settings") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            FtpSettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = { navigateToLogin() }
            )
        }

        /**
         * 3. Selección de Origen.
         * Permite elegir entre borrado de Paquetes (Shizuku) o Carpetas (SAF).
         */
        composable("origins") {
            // Se inyectan los ViewModels necesarios para esta sección
            OriginSelectionScreen(
                wipeViewModel = sharedViewModel,
                authViewModel = authViewModel,
                onNavigateToMethods = { navController.navigate("methods") },
                onNavigateHome = { navigateToLogin() },
                onConfigureFtp = { navController.navigate("ftp_settings") }
            )
        }

        /**
         * 4. Selección de Método.
         * Define el estándar de borrado (NIST, DoD, BSI).
         */
        composable("methods") {
            WipeMethodScreen(
                viewModel = sharedViewModel,
                onNavigateBack = { navController.popBackStack() },
                onMethodSelected = { navController.navigate("confirmation") },
                onNavigateHome = { navigateToLogin() }
            )
        }

        /**
         * 5. Confirmación y Proceso Activo.
         * Muestra el resumen antes de ejecutar y el progreso en tiempo real del borrado.
         */
        composable("confirmation") {
            ConfirmationScreen(
                viewModel = sharedViewModel,
                onNavigateBack = { navController.popBackStack() },
                onProcessFinished = { navController.navigate("report") },
                onNavigateHome = { navigateToLogin() }
            )
        }

        /**
         * 6. Reporte Final y Certificado.
         * Pantalla de cierre que muestra resultados y permite generar el PDF.
         */
        composable("report") {
            ReportScreen(
                viewModel = sharedViewModel,
                onNavigateHome = {
                    sharedViewModel.resetWipeStatus()
                    navController.navigate("origins") {
                        popUpTo("origins") { inclusive = true }
                    }
                }
            )
        }
    }
}
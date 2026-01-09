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
    val wipeViewModel: WipeViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    val navigateToLogin = {
        wipeViewModel.resetWipeStatus()
        authViewModel.logout() // Aseguramos que el estado de sesión se limpie
        navController.navigate("login") {
            popUpTo(0) { inclusive = true } // Limpia toda la pila de navegación
        }
    }

    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "origins" else "login"
    ) {

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

        composable("ftp_settings") {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            FtpSettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = { navigateToLogin() }
            )
        }

        composable("origins") {
            OriginSelectionScreen(
                wipeViewModel = wipeViewModel,
                authViewModel = authViewModel,
                onNavigateToMethods = { navController.navigate("methods") },
                onNavigateHome = { navigateToLogin() },
                onConfigureFtp = { navController.navigate("ftp_settings") }
            )
        }

        composable("methods") {
            WipeMethodScreen(
                viewModel = wipeViewModel,
                onNavigateBack = { navController.popBackStack() },
                onMethodSelected = { navController.navigate("confirmation") },
                onNavigateHome = { navigateToLogin() }
            )
        }

        composable("confirmation") {
            ConfirmationScreen(
                viewModel = wipeViewModel,
                onNavigateBack = { navController.popBackStack() },
                onStartWipe = {
                    // Al iniciar el borrado, navegamos a la pantalla de animación
                    navController.navigate("wipe_animation")
                },
                onProcessFinished = {
                    // Si el proceso terminara aquí (fallback), ir al reporte
                    navController.navigate("report") {
                        popUpTo("origins") { inclusive = false }
                    }
                },
                onNavigateHome = { navigateToLogin() }
            )
        }

        /**
         * Pantalla de Animación de Borrado Activo.
         * Se muestra mientras state.isWiping es verdadero.
         */
        composable("wipe_animation") {
            WipeAnimationScreen(
                viewModel = wipeViewModel,
                onProcessFinished = {
                    // Al finalizar la animación, saltamos al reporte
                    navController.navigate("report") {
                        // Evitamos que el usuario regrese a la animación con el botón atrás
                        popUpTo("origins") { inclusive = false }
                    }
                }
            )
        }

        composable("report") {
            ReportScreen(
                viewModel = wipeViewModel,
                onNavigateHome = {
                    wipeViewModel.resetWipeStatus()
                    navController.navigate("origins") {
                        popUpTo("origins") { inclusive = true }
                    }
                }
            )
        }
    }
}
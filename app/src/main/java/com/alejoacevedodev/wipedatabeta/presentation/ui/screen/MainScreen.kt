package com.alejoacevedodev.wipedatabeta.presentation.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.alejoacevedodev.wipedatabeta.presentation.PermissionRequestScreen
import com.alejoacevedodev.wipedatabeta.presentation.ui.navigation.AppNavigation
import com.alejoacevedodev.wipedatabeta.presentation.wipe.WipeViewModel

@Composable
fun MainScreen(
    onRequestStoragePermission: (ActivityResultLauncher<Intent>) -> Unit,
    viewModel: WipeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var hasPermission by remember {
        mutableStateOf(viewModel.hasStoragePermission())
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = viewModel.hasStoragePermission()
    }

    if (hasPermission) {
        AppNavigation()
    } else {
        PermissionRequestScreen(
            onRequestPermission = {
                onRequestStoragePermission(settingsLauncher)
            }
        )
    }
}

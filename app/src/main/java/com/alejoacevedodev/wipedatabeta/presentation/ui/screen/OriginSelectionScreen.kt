package com.alejoacevedodev.wipedatabeta.presentation.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.alejoacevedodev.wipedatabeta.R
import com.alejoacevedodev.wipedatabeta.presentation.auth.AuthViewModel
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.CardOption
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.CurvedHeader
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.ShizukuInstallBanner
import com.alejoacevedodev.wipedatabeta.presentation.ui.composables.ShizukuWipeSection
import com.alejoacevedodev.wipedatabeta.presentation.wipe.WipeViewModel
import com.alejoacevedodev.wipedatabeta.utils.getAppVersion
import java.io.File

@Composable
fun OriginSelectionScreen(
    wipeViewModel: WipeViewModel,
    authViewModel: AuthViewModel,
    onNavigateToMethods: () -> Unit,
    onNavigateHome: () -> Unit,
    onConfigureFtp: () -> Unit
) {
    val state by wipeViewModel.uiState.collectAsState()
    val primaryDarkBlue = Color(0xFF1E3A8A)
    val primaryBlue = Color(0xFF2E61F1)
    val context = LocalContext.current
    val hasItemsToProcess = state.selectedFolders.isNotEmpty() || state.packagesToWipe.isNotEmpty()
    val appVersion = remember { getAppVersion(context) }

    // Estado para saber si Shizuku está instalado
    var isShizukuInstalled by remember {
        mutableStateOf(AppInstaller.isShizukuInstalled(context))
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri -> if (uri != null) wipeViewModel.onFolderSelected(uri) }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        if (!isShizukuInstalled) {
            ShizukuInstallBanner(
                onInstallClick = {
                    AppInstaller.installShizukuFromAssets(context)
                }
            )
        }

        CurvedHeader(
            settingsAvailable = true,
            onSettingsClick = onConfigureFtp,
            onLogoutClick = {
                onNavigateHome()
                authViewModel.logout()
            }
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 55.dp)
                .offset(y = (-48).dp), // Efecto de solapamiento sobre el azul
            color = Color.White,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                item {
                    ShizukuWipeSection(
                        viewModel = wipeViewModel,
                        headerColor = primaryBlue,
                        onNavigateToMethods = onNavigateToMethods
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Text(
                        text = "Opción 2 – Borrado seguro desde archivos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryDarkBlue,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    CardOption(
                        title = "Memoria interna",
                        curveColor = Color(0xFFE6EEFF),
                        image = painterResource(id = R.drawable.img_folder)
                    ) { folderPickerLauncher.launch(null) }
                    Spacer(modifier = Modifier.height(12.dp))
                    CardOption(
                        title = "Memoria externa",
                        curveColor = Color(0xFFE6EEFF),
                        image = painterResource(id = R.drawable.img_folder)
                    ) { folderPickerLauncher.launch(null) }
                    Spacer(modifier = Modifier.height(12.dp))
                    CardOption(
                        title = "Tarjeta SD",
                        curveColor = Color(0xFFE6EEFF),
                        image = painterResource(id = R.drawable.img_folder)
                    ) { folderPickerLauncher.launch(null) }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Text(
                        text = "Nota: Si no visualizas Android/data, usa Mover Archivos.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(
                        onClick = { wipeViewModel.moveAllDataToMedia(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("MOVER ARCHIVOS", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }

                if (hasItemsToProcess) {
                    items(state.selectedFolders.toList()) { uri ->
                        FolderItem(uri, onRemove = { wipeViewModel.onRemoveFolder(uri) })
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToMethods,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Text(
                                "Continuar a Métodos",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(text = "Versión", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(text = "$appVersion", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}


// Archivo: com.alejoacevedodev.wipedatabeta.util.AppInstaller.kt
object AppInstaller {
    fun installShizukuFromAssets(context: Context) {
        try {
            // 1. Extraer el APK de assets a la caché interna
            val inputStream = context.assets.open("shizuku.apk")
            val file = File(context.cacheDir, "shizuku.apk")
            file.outputStream().use { inputStream.copyTo(it) }

            // 2. Obtener URI segura mediante FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // 3. Lanzar el instalador del sistema
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: Exception) {
            Log.e("AppInstaller", "Shizuku no está instalado: ${e.message}")
            false
        }
    }
}



@Composable
fun FolderItem(uri: Uri, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Carpeta: ${uri.path?.substringAfterLast('/')}",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                fontSize = 14.sp,
                color = Color.Black
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Quitar",
                    tint = Color.Red,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
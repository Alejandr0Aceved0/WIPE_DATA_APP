package com.alejoacevedodev.wipedatabeta.presentation.ui.composables

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.alejoacevedodev.wipedatabeta.domain.model.WipeMethod
import com.alejoacevedodev.wipedatabeta.presentation.wipe.WipeViewModel
import rikka.shizuku.Shizuku

@Composable
fun ShizukuWipeSection(
    viewModel: WipeViewModel,
    headerColor: Color,
    onNavigateToMethods: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val isShizukuPermitted by viewModel.isShizukuPermitted.collectAsState()

    val REGISTRADURIA_APP_PACKAGE = "co.net.tps.idc"

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkShizukuStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Opción 1 – Borrado Seguro de Paquetes",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = headerColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Nota: Si vas a borrar archivos protegidos del sistema utiliza este método.",
                fontSize = 12.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de paquete predefinido
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PackagePlaceholderCard(REGISTRADURIA_APP_PACKAGE, Modifier.weight(1f))

                Button(
                    onClick = {
                        if (uiState.packagesToWipe.contains(REGISTRADURIA_APP_PACKAGE)) {
                            Toast.makeText(context, "El paquete ya está en la lista.", Toast.LENGTH_SHORT).show()
                        } else if (viewModel.validatePackageExists(REGISTRADURIA_APP_PACKAGE)) {
                            viewModel.onPackageSelected(REGISTRADURIA_APP_PACKAGE)
                        }
                    },
                    enabled = isShizukuPermitted && Shizuku.pingBinder(),
                    colors = ButtonDefaults.buttonColors(containerColor = headerColor),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir")
                }
            }

            // Status de Shizuku
            ShizukuStatusText(
                isPermitted = isShizukuPermitted,
                onRequestPermission = { viewModel.requestShizukuPermission() }
            )

            // Lista de paquetes seleccionados
            if (uiState.packagesToWipe.isNotEmpty()) {
                Text(
                    text = "Paquetes seleccionados (${uiState.packagesToWipe.size}):",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(uiState.packagesToWipe) { pkg ->
                        PackageItem(
                            packageName = pkg,
                            onRemove = { viewModel.onRemovePackage(pkg) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de acción
            Button(
                onClick = {
                    viewModel.selectMethod(WipeMethod.PM_CLEAR)
                    onNavigateToMethods()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = headerColor),
                shape = RoundedCornerShape(8.dp),
                enabled = uiState.packagesToWipe.isNotEmpty()
            ) {
                Text("Continuar a Métodos", color = Color.White)
            }
        }
    }
}

@Composable
private fun PackagePlaceholderCard(packageName: String, modifier: Modifier) {
    Card(
        modifier = modifier.padding(end = 8.dp).height(56.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            Text(text = packageName, color = Color.DarkGray, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ShizukuStatusText(isPermitted: Boolean, onRequestPermission: () -> Unit) {
    val statusInfo = when {
        !Shizuku.pingBinder() -> "⚠️ Shizuku no activo. Inicie el servicio ADB." to Color.Red
        isPermitted -> "✅ Privilegio Shell (UID 2000) ACTIVO." to Color(0xFF2E7D32)
        else -> "❌ Permiso no otorgado. Toque para solicitar." to Color.Red
    }

    Text(
        text = statusInfo.first,
        color = statusInfo.second,
        fontSize = 12.sp,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clickable(enabled = !isPermitted && Shizuku.pingBinder()) { onRequestPermission() }
    )
}


@Composable
fun PackageItem(packageName: String, onRemove: () -> Unit) {
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
                text = packageName,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                fontSize = 14.sp,
                color = Color.Black
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = Color.Red, modifier = Modifier.size(20.dp))
            }
        }
    }
}
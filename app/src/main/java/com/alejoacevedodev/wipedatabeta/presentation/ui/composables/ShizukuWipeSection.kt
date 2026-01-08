package com.alejoacevedodev.wipedatabeta.presentation.ui.composables

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val isServiceReady = isShizukuPermitted && Shizuku.pingBinder()
    val contentAlpha = if (isServiceReady) 1f else 0.5f
    var packageNameInput by remember { mutableStateOf("") }
    val PrimaryDarkBlue = Color(0xFF1A3365)

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkShizukuStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isServiceReady) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceReady) Color.White else Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isServiceReady) headerColor else Color.LightGray)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .alpha(contentAlpha)
            ) {
                Text(
                    text = "Opción 1 - borrado seguro desde paquetes del sistema",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkBlue,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = packageNameInput,
                    onValueChange = { packageNameInput = it },
                    enabled = isServiceReady,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nombre del paquete") },
                    shape = RoundedCornerShape(25.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = headerColor,
                        unfocusedBorderColor = headerColor,
                        disabledBorderColor = Color.LightGray,
                        disabledTextColor = Color.Gray
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (packageNameInput.isNotBlank()) {
                                    val isAlreadyAdded = uiState.packagesToWipe.contains(packageNameInput)
                                    if (isAlreadyAdded) {
                                        Toast.makeText(context, "El paquete ya está en la lista", Toast.LENGTH_SHORT).show()
                                    } else {
                                        if (viewModel.validatePackageExists(packageNameInput)) {
                                            viewModel.onPackageSelected(packageNameInput)
                                            packageNameInput = ""
                                            Toast.makeText(context, "Agregado", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Paquete no encontrado", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = isServiceReady
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                contentDescription = null,
                                tint = if (isServiceReady) headerColor else Color.Gray
                            )
                        }
                    }
                )

                ShizukuStatusText(
                    isPermitted = isShizukuPermitted,
                    onRequestPermission = { viewModel.requestShizukuPermission() }
                )

                Text(
                    text = "Notas: Si vas a borrar archivos protegidos del sistema, utilizar este método",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )

                if (uiState.packagesToWipe.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Paquetes seleccionados (${uiState.packagesToWipe.size}):",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkBlue,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.heightIn(max = 150.dp), // Altura máxima para no romper el scroll
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.packagesToWipe) { pkg ->
                            PackageItem(
                                packageName = pkg,
                                onRemove = { viewModel.onRemovePackage(pkg) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PackageItem(packageName: String, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E7F5))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = packageName,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "Borrar",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ShizukuStatusText(isPermitted: Boolean, onRequestPermission: () -> Unit) {
    val statusInfo = when {
        !Shizuku.pingBinder() -> "⚠️ Shizuku no activo. Inicie el servicio ADB." to Color(0xFFFF9800)
        isPermitted -> "✅ Privilegio Shell (UID 2000) ACTIVO." to Color(0xFF689F38)
        else -> "❌ Permiso no otorgado. Toque para solicitar." to Color(0xFFFF5252)
    }

    Text(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        text = statusInfo.first,
        color = statusInfo.second,
        modifier = Modifier
            .padding(vertical = 12.dp)
            .clickable(enabled = !isPermitted && Shizuku.pingBinder()) { onRequestPermission() }
    )
}

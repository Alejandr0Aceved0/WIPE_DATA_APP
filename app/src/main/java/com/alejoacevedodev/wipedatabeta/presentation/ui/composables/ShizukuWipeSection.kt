package com.alejoacevedodev.wipedatabeta.presentation.ui.composables

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.LifecycleOwner
import com.alejoacevedodev.wipedatabeta.domain.model.WipeMethod
import com.alejoacevedodev.wipedatabeta.presentation.viewmodel.WipeViewModel
import rikka.shizuku.Shizuku

@Composable
fun ShizukuWipeSection(
    viewModel: WipeViewModel,
    headerColor: Color,
    onNavigateToMethods: () -> Unit
) {
    val REGISTRADURIA_APP_PACKAGE = "co.net.tps.idc"
    val state by viewModel.uiState.collectAsState()
    val isPermitted by viewModel.isShizukuPermitted.collectAsState()
    val context = LocalContext.current
    val packagesToWipe = state.packagesToWipe.toList()
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkShizukuStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkShizukuStatus()
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
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(end = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = REGISTRADURIA_APP_PACKAGE,
                            color = Color.DarkGray,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (packagesToWipe.contains(REGISTRADURIA_APP_PACKAGE)) {
                            Toast.makeText(context, "El paquete ya está en la lista.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (viewModel.validatePackageExists(REGISTRADURIA_APP_PACKAGE)) {
                            viewModel.onPackageSelected(REGISTRADURIA_APP_PACKAGE)
                            Toast.makeText(context, "Paquete añadido.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "El paquete no existe en el dispositivo.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = isPermitted && Shizuku.pingBinder(),
                    colors = ButtonDefaults.buttonColors(containerColor = headerColor),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir paquete")
                }
            }

            Text(
                text = "Se añadirá el paquete predefinido a la lista de borrado.",
                fontSize = 12.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (packagesToWipe.isNotEmpty()) {
                Text(
                    text = "Paquetes seleccionados (${packagesToWipe.size}):",
                    fontWeight = FontWeight.Bold,
                    color = headerColor,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                    items(packagesToWipe) { packageName ->
                        PackageItem(
                            packageName = packageName,
                            onRemove = { viewModel.onRemovePackage(packageName) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = when {
                    !Shizuku.pingBinder() -> "⚠️ Shizuku no activo. Inicie el servicio ADB."
                    isPermitted -> "✅ Privilegio Shell (UID 2000) ACTIVO."
                    else -> "❌ Permiso no otorgado. Toque para solicitar."
                },
                color = if (isPermitted) Color.Green.copy(alpha = 0.8f) else Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.clickable { viewModel.requestShizukuPermission() }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (packagesToWipe.isNotEmpty()) {
                        viewModel.selectMethod(WipeMethod.PM_CLEAR)
                        onNavigateToMethods()
                    } else {
                        Toast.makeText(context, "Seleccione al menos un paquete para continuar.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = headerColor),
                shape = RoundedCornerShape(8.dp),
                enabled = packagesToWipe.isNotEmpty()
            ) {
                Text("Continuar a Métodos", color = Color.White, fontSize = 16.sp)
            }
        }
    }
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
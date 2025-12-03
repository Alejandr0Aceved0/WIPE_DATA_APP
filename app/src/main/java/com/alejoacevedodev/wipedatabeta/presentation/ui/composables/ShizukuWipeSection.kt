package com.alejoacevedodev.wipedatabeta.presentation.ui.composables

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejoacevedodev.wipedatabeta.presentation.viewmodel.WipeViewModel
import rikka.shizuku.Shizuku

@Composable
fun ShizukuWipeSection(
    viewModel: WipeViewModel,
    headerColor: Color,
    onNavigateToMethods: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isPermitted by viewModel.isShizukuPermitted.collectAsState()
    val hardcodedPackageName = "co.net.tps.idc"
    val context = LocalContext.current
    val packagesToWipe = state.packagesToWipe.toList()

    LaunchedEffect(Unit) {
        viewModel.requestShizukuPermission()
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
                            text = hardcodedPackageName,
                            color = Color.DarkGray,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Button(
                    onClick = {

                        if (packagesToWipe.contains(hardcodedPackageName)) {
                            Toast.makeText(context, "El paquete ya está en la lista.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (viewModel.validatePackageExists(hardcodedPackageName)) {
                            viewModel.onPackageSelected(hardcodedPackageName)
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
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
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
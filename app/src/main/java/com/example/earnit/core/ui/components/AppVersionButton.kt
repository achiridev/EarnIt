package com.example.earnit.core.ui.components

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/**
 * Botón de versión de la app. Se lee del `PackageManager` porque el proyecto no
 * genera `BuildConfig`; así no hace falta tocar la configuración de build.
 */
@Composable
fun AppVersionButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfoCompat(context.packageName)
    }
    val versionName = packageInfo?.versionName ?: "?"
    var showDialog by remember { mutableStateOf(false) }

    TextButton(onClick = { showDialog = true }, modifier = modifier) {
        Text(text = "v$versionName")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Acerca de EarnIt") },
            text = { Text("Versión $versionName") },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Suppress("DEPRECATION")
private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo? =
    runCatching { getPackageInfo(packageName, 0) }.getOrNull()

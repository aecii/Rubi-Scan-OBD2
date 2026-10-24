package com.example.obdscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.obdscanner.model.AppTheme
import com.example.obdscanner.model.ObdAdapterInfo
import com.example.obdscanner.obd.AppUpdateInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdapterInfoScreen(
    adapterInfo: ObdAdapterInfo,
    appTheme: AppTheme,
    appUpdateInfo: AppUpdateInfo?,
    onSetAppTheme: (AppTheme) -> Unit,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: (String) -> Unit
) {
    val buildDateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())
    val testRedColor = Color(0xFFFF1744) // Bright vivid red for testing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // About Rubi-Scan OBDII Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = testRedColor)
                        Spacer(Modifier.width(8.dp))
                        Text("About Rubi-Scan OBDII", style = MaterialTheme.typography.titleMedium, color = testRedColor)
                    }

                    Button(onClick = onCheckForUpdates) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Check for Updates")
                    }
                }

                HorizontalDivider()

                InfoRow("Application Name", "Rubi-Scan OBDII", "Vehicle Diagnostic Scanner", valueColor = testRedColor)
                InfoRow("Installed Version", "v0.2.0 (Version Code: 2)", "Current Release", valueColor = testRedColor)
                InfoRow("Build Date", buildDateStr, "Compilation Date", valueColor = testRedColor)
                InfoRow("Update Server Location", "https://aecii.kindfox.org/OBDII/version.json", "Apache Server", valueColor = testRedColor)

                if (appUpdateInfo != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    if (appUpdateInfo.isUpdateAvailable) {
                        Text(
                            "🎉 New Update Available: v${appUpdateInfo.latestVersionName}",
                            style = MaterialTheme.typography.titleSmall,
                            color = testRedColor
                        )
                        Text(
                            "Release Notes: ${appUpdateInfo.releaseNotes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = testRedColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { onDownloadUpdate(appUpdateInfo.downloadUrl) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Download & Install v${appUpdateInfo.latestVersionName}")
                        }
                    } else {
                        Text(
                            "✓ Rubi-Scan OBDII status: ${if (appUpdateInfo.statusText.isNotBlank()) appUpdateInfo.statusText else "Up to date"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = testRedColor
                        )
                    }
                }
            }
        }

        // App Theme Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("App Theme & Configuration", style = MaterialTheme.typography.titleMedium)
                }

                HorizontalDivider()

                Text("Display Theme:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = appTheme == theme,
                            onClick = { onSetAppTheme(theme) },
                            label = { Text(theme.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // OBDLink Adapter Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("OBDLink Adapter Hardware", style = MaterialTheme.typography.titleMedium)
                }

                HorizontalDivider()

                InfoRow("Device Name", if (adapterInfo.name.isNotBlank()) adapterInfo.name else "Not Connected")
                InfoRow("Bluetooth Address", if (adapterInfo.address.isNotBlank()) adapterInfo.address else "—")
                InfoRow("STN / Device ID", adapterInfo.deviceId, "STDI query")
                InfoRow("ELM327 Version", adapterInfo.elmVersion, "ATI query")
                InfoRow("Battery / Adapter Voltage", adapterInfo.voltage, "ATRV query")
                InfoRow("Communication Protocol", adapterInfo.protocol, "ATDP query")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Hardware diagnostic connection parameters provided by OBDLink MX.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    caption: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor)
        }
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

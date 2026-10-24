package com.example.obdscanner.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.obdscanner.model.DrivingCriticality
import com.example.obdscanner.model.DtcCategory
import com.example.obdscanner.model.DtcCode
import com.example.obdscanner.obd.VehicleProfile
import java.io.File

@Composable
fun TroubleCodesScreen(
    codes: List<DtcCode>,
    vehicleProfile: VehicleProfile?,
    isConnected: Boolean,
    isReading: Boolean,
    onReadCodes: () -> Unit,
    onClearCodes: (onResult: (Boolean, String) -> Unit) -> Unit,
    onExportPdfReport: (Context) -> File?
) {
    val context = LocalContext.current
    var showClearConfirmation by remember { mutableStateOf(false) }
    var clearMessage by remember { mutableStateOf<String?>(null) }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear Trouble Codes?") },
            text = {
                Text(
                    "This will send Mode 04 to reset PCM trouble codes and turn off the Check Engine Light. " +
                            "Make sure the ignition key is ON with the engine OFF."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmation = false
                        onClearCodes { success, msg ->
                            clearMessage = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Codes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (clearMessage != null) {
        AlertDialog(
            onDismissRequest = { clearMessage = null },
            title = { Text("Clear Codes Result") },
            text = { Text(clearMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { clearMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Vehicle Identification Banner
        if (vehicleProfile != null && vehicleProfile.displayName != "Generic OBD2 Vehicle") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Scanned Vehicle:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = vehicleProfile.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Trouble Codes (DTCs)", style = MaterialTheme.typography.titleLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onReadCodes,
                    enabled = isConnected && !isReading
                ) {
                    if (isReading) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(16.dp).height(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Read")
                    }
                }

                OutlinedButton(
                    onClick = {
                        val pdfFile = onExportPdfReport(context)
                        if (pdfFile != null && pdfFile.exists()) {
                            sharePdfReport(context, pdfFile)
                        }
                    }
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Report")
                }

                OutlinedButton(
                    onClick = { showClearConfirmation = true },
                    enabled = isConnected && !isReading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }

        if (codes.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("No Diagnostic Trouble Codes Loaded", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (!isConnected) "Connect to an OBDLink adapter to read vehicle trouble codes."
                        else "Tap 'Read' to scan Mode 03 (Stored), Mode 07 (Pending), and Mode 0A (Permanent) codes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(codes) { code ->
                    DtcCodeCard(dtc = code)
                }
            }
        }
    }
}

@Composable
private fun DtcCodeCard(dtc: DtcCode) {
    var expanded by remember { mutableStateOf(false) }

    val categoryColor = when (dtc.category) {
        DtcCategory.STORED -> MaterialTheme.colorScheme.error
        DtcCategory.PENDING -> MaterialTheme.colorScheme.tertiary
        DtcCategory.PERMANENT -> MaterialTheme.colorScheme.secondary
    }

    val criticalityColor = when (dtc.criticality) {
        DrivingCriticality.CRITICAL_STOP_IMMEDIATELY -> Color(0xFFD32F2F) // Red
        DrivingCriticality.CAUTION_DRIVE_HOME -> Color(0xFFE65100) // Orange
        DrivingCriticality.SERVICE_SOON -> Color(0xFF388E3C) // Green
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            dtc.code,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            dtc.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(dtc.title, style = MaterialTheme.typography.titleSmall)

            // Criticality Badge
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(criticalityColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = dtc.criticality.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = criticalityColor
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    Text("Plain-Language Explanation:", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = if (dtc.plainLanguageExplanation.isNotBlank()) dtc.plainLanguageExplanation else dtc.description,
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (dtc.verifiedFixes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Verified Repairs & Common Fixes:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        dtc.verifiedFixes.forEach { fix ->
                            Text("✓ $fix", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (dtc.possibleCauses.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Summary Of What To Check:", style = MaterialTheme.typography.labelMedium)
                        dtc.possibleCauses.forEach { cause ->
                            val isVehicleSpecific = cause.contains("[WHAT TO CHECK")
                            Text(
                                text = "• $cause",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isVehicleSpecific) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sharePdfReport(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, "Rubi-Scan OBDII Vehicle Inspection Report")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Diagnostic PDF Report"))
    } catch (_: Exception) {}
}

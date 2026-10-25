package com.example.obdscanner.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.obdscanner.model.SavedVehicle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.obdscanner.model.AppTheme
import com.example.obdscanner.model.ObdAdapterInfo
import com.example.obdscanner.obd.VersionCheck
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdapterInfoScreen(
    adapterInfo: ObdAdapterInfo,
    appTheme: AppTheme,
    onSetAppTheme: (AppTheme) -> Unit,
    vehicles: List<SavedVehicle>,
    activeVehicleId: String?,
    onSelectVehicle: (String) -> Unit,
    onDeleteVehicle: (String) -> Unit,
    onSaveVehicle: (id: String?, name: String, vin: String, plate: String, phone: String) -> Unit
) {
    val context = LocalContext.current
    val buildDateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())
    val apkDownloadUrl = "https://aecii.kindfox.org/OBDII/RubiScan-OBDII.apk"
    val installedVersion = "v${VersionCheck.installedVersionName(context)} (Version Code: ${VersionCheck.installedVersionCode(context)})"
    var editor by remember { mutableStateOf<VehicleEditorState?>(null) }
    var pendingDelete by remember { mutableStateOf<SavedVehicle?>(null) }

    editor?.let { state ->
        VehicleEditorDialog(
            state = state,
            onDismiss = { editor = null },
            onSave = { name, vin, plate, phone ->
                onSaveVehicle(state.id, name, vin, plate, phone)
                editor = null
            }
        )
    }

    pendingDelete?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete vehicle profile?") },
            text = { Text("${vehicle.displayName} will be removed from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteVehicle(vehicle.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Vehicles", style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider()
                Text(
                    "Connect and the app reads the VIN. Save each vehicle here with a plate and phone number. Profiles stay on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (vehicles.isEmpty()) {
                    Text("No saved vehicles yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    vehicles.forEach { vehicle ->
                        val active = vehicle.id == activeVehicleId
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onSelectVehicle(vehicle.id) }
                                ) {
                                    Text(vehicle.displayName, style = MaterialTheme.typography.titleSmall)
                                    if (vehicle.vin.isNotBlank()) {
                                        Text(
                                            vehicle.vin,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (vehicle.plateNumber.isNotBlank()) {
                                        Text("Plate ${vehicle.plateNumber}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (vehicle.phoneNumber.isNotBlank()) {
                                        Text(vehicle.phoneNumber, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (active) {
                                        Text(
                                            "In use",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                TextButton(onClick = {
                                    editor = VehicleEditorState(
                                        id = vehicle.id,
                                        name = vehicle.displayName,
                                        vin = vehicle.vin,
                                        plate = vehicle.plateNumber,
                                        phone = vehicle.phoneNumber
                                    )
                                }) { Text("Edit") }
                                IconButton(onClick = { pendingDelete = vehicle }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete ${vehicle.displayName}")
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        editor = VehicleEditorState(id = null, name = "", vin = "", plate = "", phone = "")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add vehicle") }
            }
        }

        // About Rubi-Scan OBDII Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("About Rubi-Scan OBDII", style = MaterialTheme.typography.titleMedium)
                }

                HorizontalDivider()

                InfoRow("Application Name", "Rubi-Scan OBDII", "Vehicle Diagnostic Scanner")
                InfoRow("Installed Version", installedVersion, "Current Release")
                InfoRow("Build Date", buildDateStr, "Compilation Date")
                Text(
                    text = apkDownloadUrl,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkDownloadUrl)))
                    }
                )
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

private data class VehicleEditorState(
    val id: String?,
    val name: String,
    val vin: String,
    val plate: String,
    val phone: String
)

@Composable
private fun VehicleEditorDialog(
    state: VehicleEditorState,
    onDismiss: () -> Unit,
    onSave: (name: String, vin: String, plate: String, phone: String) -> Unit
) {
    var name by remember(state.id) { mutableStateOf(state.name) }
    var vin by remember(state.id) { mutableStateOf(state.vin) }
    var plate by remember(state.id) { mutableStateOf(state.plate) }
    var phone by remember(state.id) { mutableStateOf(state.phone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.id == null) "Add vehicle" else "Edit vehicle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vin,
                    onValueChange = { vin = it.uppercase() },
                    label = { Text("VIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text("Plate number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, vin, plate, phone) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    caption: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.titleSmall, color = textColor ?: valueColor)
        }
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = textColor ?: MaterialTheme.colorScheme.outline
            )
        }
    }
}

package com.example.obdscanner.ui

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.obdscanner.model.AppTheme
import com.example.obdscanner.model.ConnectionState
import com.example.obdscanner.viewmodel.ObdScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObdScannerApp(
    viewModel: ObdScannerViewModel,
    pairedDevices: List<BluetoothDevice>,
    showDevicePicker: Boolean,
    onRequestConnect: () -> Unit,
    onDismissDevicePicker: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val liveMetrics by viewModel.liveMetrics.collectAsState()
    val troubleCodes by viewModel.troubleCodes.collectAsState()
    val freezeFrame by viewModel.freezeFrameData.collectAsState()
    val sensorReadings by viewModel.sensorReadings.collectAsState()
    val savedVehicles by viewModel.savedVehicles.collectAsState()
    val activeVehicleId by viewModel.activeVehicleId.collectAsState()
    val vehicleSaveOffer by viewModel.vehicleSaveOffer.collectAsState()
    val vehicleInfo by viewModel.vehicleInfo.collectAsState()
    val adapterInfo by viewModel.adapterInfo.collectAsState()
    val unitSystem by viewModel.unitSystem.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val o2DisplayMode by viewModel.o2DisplayMode.collectAsState()
    val o2History by viewModel.o2History.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isReadingCodes by viewModel.isReadingCodes.collectAsState()
    val isLoggingCsv by viewModel.isLoggingCsv.collectAsState()
    val csvSampleCount by viewModel.csvSampleCount.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkForUpdateOnLaunch(context)
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    DisposableEffect(selectedTab) {
        viewModel.setSensorsPageVisible(selectedTab == 1)
        onDispose { viewModel.setSensorsPageVisible(false) }
    }

    val isConnected = connectionState is ConnectionState.Connected

    val useDarkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        availableUpdate?.let { update ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateNotice() },
                title = { Text("Update available.") },
                text = {
                    Column {
                        Text("Version ${update.versionName} is ready to download.")
                        if (update.releaseNotes.isNotBlank()) {
                            Text(update.releaseNotes, modifier = Modifier.padding(top = 8.dp))
                        }
                        Text("Direct download", modifier = Modifier.padding(top = 12.dp))
                        Text(
                            text = update.downloadUrl,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)))
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)))
                    }) { Text("Download") }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateNotice() }) { Text("Not now") }
                }
            )
        }

        vehicleSaveOffer?.let { offer ->
            var plate by remember(offer.profile.vin) { mutableStateOf("") }
            var phone by remember(offer.profile.vin) { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.dismissVehicleOffer() },
                title = { Text("Save vehicle profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(offer.profile.displayName)
                        Text(
                            "VIN ${offer.profile.vin}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Save this vehicle on this phone. You can keep more than one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = plate,
                            onValueChange = { plate = it },
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
                    TextButton(onClick = { viewModel.saveOfferedVehicle(plate, phone) }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissVehicleOffer() }) {
                        Text("Not now")
                    }
                }
            )
        }

        if (showDevicePicker) {
            DeviceSelectionDialog(
                devices = pairedDevices,
                onDeviceSelected = { device ->
                    onDismissDevicePicker()
                    viewModel.connect(device)
                },
                onDismiss = onDismissDevicePicker
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Rubi-Scan OBDII", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusDot(connectionState)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = when (val state = connectionState) {
                                        is ConnectionState.Connected -> "Connected: ${state.adapterName}"
                                        is ConnectionState.Connecting -> "Connecting to ${state.deviceName}…"
                                        is ConnectionState.Initializing -> state.step
                                        is ConnectionState.Error -> "Connection Error"
                                        is ConnectionState.Disconnected -> "Disconnected"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        FilterChip(
                            selected = isDemoMode,
                            onClick = { viewModel.toggleDemoMode() },
                            label = { Text(if (isDemoMode) "Demo: ON" else "Demo Mode", style = MaterialTheme.typography.labelSmall) }
                        )
                        Spacer(Modifier.width(6.dp))
                        if (isConnected) {
                            OutlinedButton(onClick = { viewModel.disconnect() }) {
                                Text("Disconnect")
                            }
                        } else {
                            Button(onClick = onRequestConnect) {
                                Text("Connect")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Sensors, contentDescription = "Sensors") },
                        label = { Text("Sensors") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Build, contentDescription = "Trouble Codes") },
                        label = { Text("Codes") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Speed, contentDescription = "Performance & Video") },
                        label = { Text("Track") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Status banner bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    when (selectedTab) {
                        0 -> DashboardScreen(
                            metrics = liveMetrics,
                            unitSystem = unitSystem,
                            o2DisplayMode = o2DisplayMode,
                            o2History = o2History,
                            onToggleUnits = { viewModel.toggleUnitSystem() },
                            onSetO2DisplayMode = { viewModel.setO2DisplayMode(it) }
                        )
                        1 -> SensorListScreen(
                            readings = sensorReadings,
                            unitSystem = unitSystem,
                            isConnected = isConnected
                        )
                        2 -> TroubleCodesScreen(
                            codes = troubleCodes,
                            vehicleProfile = vehicleInfo.profile,
                            plateNumber = vehicleInfo.plateNumber,
                            phoneNumber = vehicleInfo.phoneNumber,
                            freezeFrame = freezeFrame,
                            unitSystem = unitSystem,
                            isConnected = isConnected,
                            isReading = isReadingCodes,
                            onReadCodes = { viewModel.readTroubleCodes() },
                            onClearCodes = { onResult -> viewModel.clearTroubleCodes(onResult) },
                            onExportPdfReport = { ctx -> viewModel.generatePdfReport(ctx) }
                        )
                        3 -> PerformanceAndVideoScreen(
                            metrics = liveMetrics,
                            unitSystem = unitSystem,
                            isLoggingCsv = isLoggingCsv,
                            csvSampleCount = csvSampleCount,
                            onToggleCsvLogging = { viewModel.toggleCsvLogging() },
                            onExportCsv = { ctx -> viewModel.exportCsvFile(ctx) }
                        )
                        4 -> AdapterInfoScreen(
                            adapterInfo = adapterInfo,
                            appTheme = appTheme,
                            onSetAppTheme = { viewModel.setAppTheme(it) },
                            vehicles = savedVehicles,
                            activeVehicleId = activeVehicleId,
                            onSelectVehicle = { viewModel.selectVehicle(it) },
                            onDeleteVehicle = { viewModel.deleteVehicle(it) },
                            onSaveVehicle = { id, name, vin, plate, phone ->
                                viewModel.saveVehicleForm(id, name, vin, plate, phone)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(connectionState: ConnectionState) {
    val color = when (connectionState) {
        is ConnectionState.Connected -> Color(0xFF4CAF50) // Green
        is ConnectionState.Connecting, is ConnectionState.Initializing -> Color(0xFFFF9800) // Orange
        is ConnectionState.Error -> MaterialTheme.colorScheme.error
        is ConnectionState.Disconnected -> Color.Gray
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

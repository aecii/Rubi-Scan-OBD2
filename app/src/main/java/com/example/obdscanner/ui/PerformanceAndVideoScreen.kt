package com.example.obdscanner.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.obdscanner.model.LiveMetrics
import com.example.obdscanner.model.UnitSystem
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerformanceAndVideoScreen(
    metrics: LiveMetrics,
    unitSystem: UnitSystem,
    isLoggingCsv: Boolean,
    csvSampleCount: Int,
    onToggleCsvLogging: () -> Unit,
    onExportCsv: (Context) -> File?
) {
    val context = LocalContext.current

    // HUD Telemetry Options
    var showSpeed by remember { mutableStateOf(true) }
    var showCoolant by remember { mutableStateOf(true) }
    var showMil by remember { mutableStateOf(true) }
    var showLoop by remember { mutableStateOf(true) }
    var showO2Graph by remember { mutableStateOf(true) }
    var showIat by remember { mutableStateOf(true) }
    var showAmbientTemp by remember { mutableStateOf(true) }

    // Performance Test State (0-60 mph / 1/4 mile)
    var testStatus by remember { mutableStateOf("Stopped. Ready for 0-60 mph launch.") }
    var isTimerRunning by remember { mutableStateOf(false) }
    var startTimeMs by remember { mutableStateOf(0L) }
    var zeroToSixtySec by remember { mutableStateOf<Double?>(null) }
    var currentSpeedMph by remember { mutableDoubleStateOf(0.0) }
    var peakGForce by remember { mutableDoubleStateOf(0.0) }

    val speedMph = (metrics.speedKmh ?: 0.0) * 0.621371
    currentSpeedMph = speedMph

    // Auto Performance Launch Logic
    LaunchedEffect(speedMph) {
        if (!isTimerRunning && zeroToSixtySec == null) {
            if (speedMph < 1.0) {
                testStatus = "Vehicle stopped (0 mph). Floor gas pedal to launch test!"
            } else if (speedMph >= 1.0) {
                isTimerRunning = true
                startTimeMs = System.currentTimeMillis()
                testStatus = "LAUNCH DETECTED! Accelerating to 60 mph…"
            }
        } else if (isTimerRunning) {
            val elapsed = (System.currentTimeMillis() - startTimeMs) / 1000.0
            val estimatedG = (speedMph / 22.0) / (elapsed.coerceAtLeast(0.1)) // rough G estimation
            if (estimatedG > peakGForce && estimatedG < 3.0) {
                peakGForce = estimatedG
            }

            if (speedMph >= 60.0 && zeroToSixtySec == null) {
                zeroToSixtySec = elapsed
                isTimerRunning = false
                testStatus = "0-60 mph Complete: %.2f seconds! Peak G: %.2f G".format(elapsed, peakGForce)
            } else {
                testStatus = "Accelerating... Current Speed: %.0f mph (%.1fs)".format(speedMph, elapsed)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0-60 mph Performance Timer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Performance Acceleration Test", style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            isTimerRunning = false
                            zeroToSixtySec = null
                            peakGForce = 0.0
                            testStatus = "Reset complete. Stop vehicle (0 mph) for next launch."
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("0 - 60 mph Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = zeroToSixtySec?.let { "%.2f s".format(it) } ?: if (isTimerRunning) "Timing..." else "—",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Peak Acceleration G", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (peakGForce > 0) "%.2f G".format(peakGForce) else "—",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = testStatus,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // CSV Logging & Export Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Live Sensor CSV Recorder", style = MaterialTheme.typography.titleMedium)
                }

                HorizontalDivider()

                Text(
                    "Record real-time telemetry (RPM, Speed, Coolant, Load, Fuel Trims, O2 Sensors, Fuel Loop, Temps) to a spreadsheet file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onToggleCsvLogging,
                        colors = if (isLoggingCsv) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (isLoggingCsv) Icons.Default.Refresh else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isLoggingCsv) "Stop CSV ($csvSampleCount samples)" else "Start Recording")
                    }

                    OutlinedButton(
                        onClick = {
                            val file = onExportCsv(context)
                            if (file != null && file.exists()) {
                                shareCsvFile(context, file)
                            }
                        },
                        enabled = csvSampleCount > 0
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Export CSV")
                    }
                }
            }
        }

        // Video Track Recorder Camera HUD Overlay
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Video Track Recorder HUD Overlay", style = MaterialTheme.typography.titleMedium)
                }

                HorizontalDivider()

                Text("Configure HUD Overlay Parameters:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = showSpeed, onClick = { showSpeed = !showSpeed }, label = { Text("MPH / Speed") })
                    FilterChip(selected = showCoolant, onClick = { showCoolant = !showCoolant }, label = { Text("Coolant Temp") })
                    FilterChip(selected = showMil, onClick = { showMil = !showMil }, label = { Text("MIL Status") })
                    FilterChip(selected = showLoop, onClick = { showLoop = !showLoop }, label = { Text("Open/Closed Loop") })
                    FilterChip(selected = showO2Graph, onClick = { showO2Graph = !showO2Graph }, label = { Text("O2 Graph Lines") })
                    FilterChip(selected = showIat, onClick = { showIat = !showIat }, label = { Text("Intake Air Temp") })
                    FilterChip(selected = showAmbientTemp, onClick = { showAmbientTemp = !showAmbientTemp }, label = { Text("Ambient Temp") })
                }

                CameraHudViewfinder(
                    metrics = metrics,
                    unitSystem = unitSystem,
                    showSpeed = showSpeed,
                    showCoolant = showCoolant,
                    showMil = showMil,
                    showLoop = showLoop,
                    showO2Graph = showO2Graph,
                    showIat = showIat,
                    showAmbientTemp = showAmbientTemp
                )
            }
        }
    }
}

@Composable
private fun CameraHudViewfinder(
    metrics: LiveMetrics,
    unitSystem: UnitSystem,
    showSpeed: Boolean,
    showCoolant: Boolean,
    showMil: Boolean,
    showLoop: Boolean,
    showO2Graph: Boolean,
    showIat: Boolean,
    showAmbientTemp: Boolean
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    if (!hasCameraPermission) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.DarkGray, RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Camera Permission Required for Video HUD Overlay", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Grant Camera Permission")
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            CameraPreviewView(modifier = Modifier.fillMaxSize())

            // Transparent Telemetry HUD Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(12.dp)
            ) {
                // Top HUD Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        val rpmVal = metrics.rpm?.let { "%,d RPM".format(it.toInt()) } ?: "0 RPM"
                        Text(rpmVal, style = MaterialTheme.typography.titleLarge, color = Color(0xFF00E676))

                        if (showLoop) {
                            Text(
                                text = metrics.fuelLoopStatus?.title ?: "OL - Open Loop",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (showSpeed) {
                            val speedVal = metrics.speedKmh?.let {
                                if (unitSystem == UnitSystem.IMPERIAL) "%.0f mph".format(it * 0.621371)
                                else "%.0f km/h".format(it)
                            } ?: "0 mph"

                            Text(speedVal, style = MaterialTheme.typography.headlineLarge, color = Color(0xFF29B6F6))
                        }
                        Text("BATTERY: ${metrics.batteryVoltage?.let { "%.1f V".format(it) } ?: "12.0 V"}", style = MaterialTheme.typography.labelSmall, color = Color.Yellow)
                    }
                }

                // G-Force Center Reticle & Optional Mini O2 Waveform overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.Center)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 35.dp.toPx(), style = Stroke(2f))
                        drawCircle(color = Color.Cyan.copy(alpha = 0.8f), radius = 6.dp.toPx(), center = center)

                        if (showO2Graph) {
                            // Mini O2 waveform line across HUD
                            val v1 = metrics.o2b1s1 ?: 0.5
                            val v2 = metrics.o2b2s1 ?: 0.5
                            val path1 = Path().apply {
                                moveTo(0f, center.y + (0.5f - v1.toFloat()) * 40f)
                                lineTo(size.width, center.y + (0.5f - v2.toFloat()) * 40f)
                            }
                            drawPath(path1, color = Color(0xFF00E676).copy(alpha = 0.6f), style = Stroke(2f))
                        }
                    }
                }

                // Bottom HUD Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        if (showCoolant) {
                            Text(
                                "COOLANT: ${metrics.coolantCelsius?.let { "%.0f °C".format(it) } ?: "—"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                        if (showIat) {
                            Text(
                                "IAT: ${metrics.intakeAirTempCelsius?.let { "%.0f °C".format(it) } ?: "—"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Cyan
                            )
                        }
                    }

                    if (showMil) {
                        Text(
                            if (metrics.milOn) "⚠️ MIL ON ($metrics.dtcCount DTCs)" else "✓ MIL OFF",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (metrics.milOn) Color(0xFFFF9800) else Color(0xFF00E676)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}

private fun shareCsvFile(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Rubi-Scan OBDII Telemetry CSV Export")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Telemetry CSV"))
    } catch (_: Exception) {}
}

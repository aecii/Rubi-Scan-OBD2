package com.example.obdscanner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.obdscanner.model.DisplayUnits
import com.example.obdscanner.model.LiveMetrics
import com.example.obdscanner.model.O2DisplayMode
import com.example.obdscanner.model.O2Sample
import com.example.obdscanner.model.SpeedUnit
import com.example.obdscanner.model.TemperatureUnit

@Composable
fun DashboardScreen(
    metrics: LiveMetrics,
    units: DisplayUnits,
    o2DisplayMode: O2DisplayMode,
    o2History: List<O2Sample>,
    onSetSpeedUnit: (SpeedUnit) -> Unit,
    onSetTemperatureUnit: (TemperatureUnit) -> Unit,
    onSetO2DisplayMode: (O2DisplayMode) -> Unit,
    onOpenTroubleCodes: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // MIL / Check Engine Banner (ORANGE when fault code present)
        MilStatusBanner(
            milOn = metrics.milOn,
            dtcCount = metrics.dtcCount,
            onOpenTroubleCodes = onOpenTroubleCodes
        )

        // Fuel Loop Status Card
        metrics.fuelLoopStatus?.let { loop ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Fuel System Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(loop.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(loop.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Text("Real-Time Engine Telemetry", style = MaterialTheme.typography.titleLarge)
        UnitSelectorRow(
            units = units,
            onSetSpeedUnit = onSetSpeedUnit,
            onSetTemperatureUnit = onSetTemperatureUnit
        )

        // Gauges Row 1 (RPM & Speed)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val rpmStr = metrics.rpm?.let { "%,d RPM".format(it.toInt()) } ?: "—"
            MetricGaugeCard(
                title = "Engine Speed",
                value = rpmStr,
                subtitle = "Mode 01 PID 0C",
                modifier = Modifier.weight(1f)
            )

            val speedVal = units.formatSpeed(metrics.speedKmh)

            MetricGaugeCard(
                title = "Vehicle Speed",
                value = speedVal,
                subtitle = "Mode 01 PID 0D",
                modifier = Modifier.weight(1f)
            )
        }

        // Gauges Row 2 (Engine Load, Throttle & Voltage)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val loadStr = metrics.loadPercent?.let { "%.1f %%".format(it) } ?: "—"
            MetricGaugeCard(
                title = "Engine Load",
                value = loadStr,
                subtitle = "Mode 01 PID 04",
                modifier = Modifier.weight(1f)
            )

            val throttleStr = metrics.throttlePercent?.let { "%.1f %%".format(it) } ?: "—"
            MetricGaugeCard(
                title = "Throttle Position",
                value = throttleStr,
                subtitle = "Mode 01 PID 11",
                modifier = Modifier.weight(1f)
            )
        }

        // Gauges Row 3 (Pressure & Mass Air Flow)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val mapVal = units.formatPressure(metrics.mapKpa)

            MetricGaugeCard(
                title = "Manifold Pressure (MAP)",
                value = mapVal,
                subtitle = "Mode 01 PID 0B",
                modifier = Modifier.weight(1f)
            )

            val mafVal = metrics.mafGps?.let { "%.1f g/s".format(it) } ?: "—"
            MetricGaugeCard(
                title = "Mass Air Flow (MAF)",
                value = mafVal,
                subtitle = "Mode 01 PID 10",
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Temperature Section (All Temperatures Grouped Together)
        Text("Engine Temperatures", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val coolantVal = units.formatTemperature(metrics.coolantCelsius)

            MetricGaugeCard(
                title = "Engine Coolant Temp",
                value = coolantVal,
                subtitle = "Mode 01 PID 05",
                modifier = Modifier.weight(1f)
            )

            val iatVal = units.formatTemperature(metrics.intakeAirTempCelsius)

            MetricGaugeCard(
                title = "Intake Air Temp (IAT)",
                value = iatVal,
                subtitle = "Mode 01 PID 0F",
                modifier = Modifier.weight(1f)
            )
        }

        // Ambient Temp Card
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val voltageStr = metrics.batteryVoltage?.let { "%.1f V".format(it) } ?: "—"
            MetricGaugeCard(
                title = "Battery Voltage",
                value = voltageStr,
                subtitle = "ATRV / Module Volts",
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Fuel Trim Data Section (Placed right next to Temperatures)
        Text("Fuel Trim Data", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val stft1Str = metrics.stftBank1?.let { "%+.1f %%".format(it) } ?: "—"
            MetricGaugeCard(
                title = "Short Term FT B1",
                value = stft1Str,
                subtitle = "Mode 01 PID 06",
                modifier = Modifier.weight(1f)
            )

            val ltft1Str = metrics.ltftBank1?.let { "%+.1f %%".format(it) } ?: "—"
            MetricGaugeCard(
                title = "Long Term FT B1",
                value = ltft1Str,
                subtitle = "Mode 01 PID 07",
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val stft2Str = metrics.stftBank2?.let { "%+.1f %%".format(it) } ?: "—"
            MetricGaugeCard(
                title = "Short Term FT B2",
                value = stft2Str,
                subtitle = "Mode 01 PID 08",
                modifier = Modifier.weight(1f)
            )

            val ltft2Str = metrics.ltftBank2?.let { "%+.1f %%".format(it) } ?: "—"
            MetricGaugeCard(
                title = "Long Term FT B2",
                value = ltft2Str,
                subtitle = "Mode 01 PID 09",
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // O2 Sensor Monitoring Section (Default Graph)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("O2 Sensor Monitoring", style = MaterialTheme.typography.titleLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                O2DisplayMode.entries.forEach { mode ->
                    FilterChip(
                        selected = o2DisplayMode == mode,
                        onClick = { onSetO2DisplayMode(mode) },
                        label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        if (o2DisplayMode == O2DisplayMode.NUMBERS) {
            O2NumbersSection(metrics = metrics)
        } else {
            O2GraphSection(o2History = o2History)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Note: Upstream O2 sensors (S1) oscillate between 0.1V (Lean) and 0.9V (Rich). Downstream O2 sensors (S2) stay steady at ~0.6V to 0.8V when catalytic converter is active.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UnitSelectorRow(
    units: DisplayUnits,
    onSetSpeedUnit: (SpeedUnit) -> Unit,
    onSetTemperatureUnit: (TemperatureUnit) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpeedUnit.entries.forEach { unit ->
            FilterChip(
                selected = units.speed == unit,
                onClick = { onSetSpeedUnit(unit) },
                label = { Text(unit.label) }
            )
        }
        Spacer(Modifier.width(8.dp))
        TemperatureUnit.entries.forEach { unit ->
            FilterChip(
                selected = units.temperature == unit,
                onClick = { onSetTemperatureUnit(unit) },
                label = { Text(unit.label) }
            )
        }
    }
}

@Composable
private fun MilStatusBanner(
    milOn: Boolean,
    dtcCount: Int,
    onOpenTroubleCodes: () -> Unit
) {
    val isWarning = milOn || dtcCount > 0
    val cardColor = if (isWarning) Color(0xFFFF9800).copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer
    val iconColor = if (isWarning) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
    val textColor = if (isWarning) Color(0xFFE65100) else MaterialTheme.colorScheme.onPrimaryContainer
    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isWarning) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = iconColor
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isWarning) "FAULT CODE DETECTED / CHECK ENGINE LIGHT" else "CHECK ENGINE LIGHT IS OFF",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Text(
                    text = if (isWarning) {
                        "$dtcCount fault code(s) reported by the engine computer. Tap to open them."
                    } else {
                        "No active Malfunction Indicator Light (MIL) requested."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }

    if (isWarning) {
        Card(
            onClick = onOpenTroubleCodes,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) { body() }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) { body() }
    }
}

@Composable
private fun O2NumbersSection(metrics: LiveMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val b1s1Str = metrics.o2b1s1?.let { "%.3f V (%s)".format(it, if (it >= 0.45) "Rich" else "Lean") } ?: "—"
            MetricGaugeCard(
                title = "O2 B1S1 (Upstream)",
                value = b1s1Str,
                subtitle = "Mode 01 PID 14",
                modifier = Modifier.weight(1f)
            )

            val b1s2Str = metrics.o2b1s2?.let { "%.3f V (%s)".format(it, if (it >= 0.45) "Rich" else "Lean") } ?: "—"
            MetricGaugeCard(
                title = "O2 B1S2 (Downstream)",
                value = b1s2Str,
                subtitle = "Mode 01 PID 15",
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val b2s1Str = metrics.o2b2s1?.let { "%.3f V (%s)".format(it, if (it >= 0.45) "Rich" else "Lean") } ?: "—"
            MetricGaugeCard(
                title = "O2 B2S1 (Upstream)",
                value = b2s1Str,
                subtitle = "Mode 01 PID 18",
                modifier = Modifier.weight(1f)
            )

            val b2s2Str = metrics.o2b2s2?.let { "%.3f V (%s)".format(it, if (it >= 0.45) "Rich" else "Lean") } ?: "—"
            MetricGaugeCard(
                title = "O2 B2S2 (Downstream)",
                value = b2s2Str,
                subtitle = "Mode 01 PID 19",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun O2GraphSection(o2History: List<O2Sample>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Real-Time O2 Sensor Waveform (0.0V - 1.0V)", style = MaterialTheme.typography.titleSmall)

            val colorB1S1 = Color(0xFF4CAF50) // Green
            val colorB1S2 = Color(0xFF00BCD4) // Cyan
            val colorB2S1 = Color(0xFFFF9800) // Amber
            val colorB2S2 = Color(0xFF9C27B0) // Purple

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF1E1E1E), shape = MaterialTheme.shapes.medium)
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Grid lines
                    val gridColor = Color.Gray.copy(alpha = 0.3f)
                    drawLine(gridColor, Offset(0f, height * 0.2f), Offset(width, height * 0.2f), strokeWidth = 1f) // 0.8V
                    drawLine(gridColor, Offset(0f, height * 0.55f), Offset(width, height * 0.55f), strokeWidth = 1f) // 0.45V (Stoich)
                    drawLine(gridColor, Offset(0f, height * 0.9f), Offset(width, height * 0.9f), strokeWidth = 1f) // 0.1V

                    if (o2History.size >= 2) {
                        val maxPoints = o2History.size.toFloat()

                        fun drawSensorLine(sensorValues: List<Double?>, color: Color) {
                            val path = Path()
                            var started = false

                            sensorValues.forEachIndexed { index, v ->
                                if (v != null) {
                                    val x = (index / (maxPoints - 1)) * width
                                    val clampedV = v.coerceIn(0.0, 1.0)
                                    val y = height - (clampedV.toFloat() * height)

                                    if (!started) {
                                        path.moveTo(x, y)
                                        started = true
                                    } else {
                                        path.lineTo(x, y)
                                    }
                                }
                            }
                            if (started) {
                                drawPath(path, color = color, style = Stroke(width = 3f))
                            }
                        }

                        drawSensorLine(o2History.map { it.b1s1 }, colorB1S1)
                        drawSensorLine(o2History.map { it.b1s2 }, colorB1S2)
                        drawSensorLine(o2History.map { it.b2s1 }, colorB2S1)
                        drawSensorLine(o2History.map { it.b2s2 }, colorB2S2)
                    }
                }
            }

            // Legend with 2 balanced, neatly-spaced rows
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem("Bank 1 Upstream (S1)", colorB1S1, modifier = Modifier.weight(1f))
                    LegendItem("Bank 1 Downstream (S2)", colorB1S2, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem("Bank 2 Upstream (S1)", colorB2S1, modifier = Modifier.weight(1f))
                    LegendItem("Bank 2 Downstream (S2)", colorB2S2, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun MetricGaugeCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

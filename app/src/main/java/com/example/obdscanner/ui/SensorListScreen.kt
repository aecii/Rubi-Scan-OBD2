package com.example.obdscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.obdscanner.model.DisplayUnits
import com.example.obdscanner.model.SensorKind
import com.example.obdscanner.model.SensorReading

@Composable
fun SensorListScreen(
    readings: List<SensorReading>,
    units: DisplayUnits,
    isConnected: Boolean
) {
    val reporting = readings.count { it.number != null || it.text != null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sensors", style = MaterialTheme.typography.titleLarge)
        Text(
            text = when {
                !isConnected -> "Connect to an adapter to list the sensors this vehicle supports."
                readings.isEmpty() -> "Asking the engine computer which sensors it supports."
                else -> "$reporting of ${readings.size} sensors reporting. Values are live from Mode 01."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (readings.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (!isConnected) {
                        "No vehicle is connected."
                    } else {
                        "The sensor list will appear after the engine computer answers."
                    },
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(readings, key = { it.pid }) { reading ->
                    SensorRow(reading = reading, units = units)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SensorRow(reading: SensorReading, units: DisplayUnits) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(reading.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Mode 01 PID %02X".format(reading.pid),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatSensor(reading, units),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatSensor(reading: SensorReading, units: DisplayUnits): String {
    reading.text?.let { return it }
    val value = reading.number ?: return "—"
    return when (reading.kind) {
        SensorKind.RPM -> "%,d rpm".format(value.toInt())
        SensorKind.SPEED_KMH -> units.formatSpeed(value)
        SensorKind.CELSIUS -> units.formatTemperature(value)
        SensorKind.PERCENT -> "%.1f%%".format(value)
        SensorKind.SIGNED_PERCENT -> "%+.1f%%".format(value)
        SensorKind.KPA -> units.formatPressure(value)
        SensorKind.VOLTS -> "%.2f V".format(value)
        SensorKind.O2_VOLTS -> "%.3f V".format(value)
        SensorKind.GRAMS_PER_SEC -> "%.1f g/s".format(value)
        SensorKind.DEGREES -> "%.1f°".format(value)
        SensorKind.SECONDS -> formatDuration(value)
        SensorKind.MINUTES -> "%.0f min".format(value)
        SensorKind.KILOMETERS -> units.formatDistanceKm(value)
        SensorKind.COUNT -> "%.0f".format(value)
        SensorKind.RATIO -> "%.3f".format(value)
        SensorKind.LITERS_PER_HOUR -> units.formatFuelRate(value)
        SensorKind.NEWTON_METERS -> units.formatTorque(value)
        SensorKind.PASCAL -> "%.0f Pa".format(value)
        null -> "—"
    }
}

private fun formatDuration(seconds: Double): String {
    val total = seconds.toInt().coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val remain = total % 60
    return when {
        hours > 0 -> "%dh %dm".format(hours, minutes)
        minutes > 0 -> "%dm %ds".format(minutes, remain)
        else -> "%ds".format(remain)
    }
}

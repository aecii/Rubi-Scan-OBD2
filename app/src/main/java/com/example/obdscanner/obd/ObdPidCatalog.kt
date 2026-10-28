/*
 * Copyright 2026 aecii_3d
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.obdscanner.obd

import com.example.obdscanner.model.SensorKind
import com.example.obdscanner.model.SensorReading

/**
 * SAE J1979 Mode 01 PIDs. The vehicle reports which of these it supports.
 * A supported PID with no formula is still listed, using the raw data bytes.
 */
object ObdPidCatalog {

    val fallbackPids: List<Int> = listOf(
        0x01, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
        0x0B, 0x0C, 0x0D, 0x0F, 0x10, 0x11, 0x14, 0x15, 0x18, 0x19
    )

    fun nameOf(pid: Int): String = specs[pid]?.name ?: "PID %02X".format(pid)

    fun placeholder(pid: Int): SensorReading = SensorReading(pid = pid, name = nameOf(pid))

    fun decode(pid: Int, payload: String): SensorReading {
        val spec = specs[pid]
        if (spec == null) {
            return SensorReading(pid, nameOf(pid), text = hexBytes(payload))
        }
        return try {
            spec.decode(payload) ?: SensorReading(pid, spec.name, text = hexBytes(payload))
        } catch (_: Exception) {
            SensorReading(pid, spec.name, text = hexBytes(payload))
        }
    }

    fun demoReadings(): List<SensorReading> = listOf(
        SensorReading(0x01, nameOf(0x01), text = "MIL on · 1 code"),
        SensorReading(0x02, nameOf(0x02), text = "P0303"),
        SensorReading(0x03, nameOf(0x03), text = "Closed Loop - Normal Operating Status"),
        SensorReading(0x04, nameOf(0x04), SensorKind.PERCENT, number = 38.5),
        SensorReading(0x05, nameOf(0x05), SensorKind.CELSIUS, number = 90.0),
        SensorReading(0x06, nameOf(0x06), SensorKind.SIGNED_PERCENT, number = 3.2),
        SensorReading(0x07, nameOf(0x07), SensorKind.SIGNED_PERCENT, number = 1.5),
        SensorReading(0x08, nameOf(0x08), SensorKind.SIGNED_PERCENT, number = 2.8),
        SensorReading(0x09, nameOf(0x09), SensorKind.SIGNED_PERCENT, number = 0.9),
        SensorReading(0x0B, nameOf(0x0B), SensorKind.KPA, number = 42.0),
        SensorReading(0x0C, nameOf(0x0C), SensorKind.RPM, number = 2450.0),
        SensorReading(0x0D, nameOf(0x0D), SensorKind.SPEED_KMH, number = 72.4),
        SensorReading(0x0E, nameOf(0x0E), SensorKind.DEGREES, number = 12.5),
        SensorReading(0x0F, nameOf(0x0F), SensorKind.CELSIUS, number = 28.0),
        SensorReading(0x10, nameOf(0x10), SensorKind.GRAMS_PER_SEC, number = 18.5),
        SensorReading(0x11, nameOf(0x11), SensorKind.PERCENT, number = 22.0),
        SensorReading(0x13, nameOf(0x13), text = "B1S1, B1S2, B2S1, B2S2"),
        SensorReading(0x14, nameOf(0x14), text = "0.825 V · trim +1.2%"),
        SensorReading(0x15, nameOf(0x15), text = "0.650 V · trim +0.4%"),
        SensorReading(0x18, nameOf(0x18), text = "0.180 V · trim -0.8%"),
        SensorReading(0x19, nameOf(0x19), text = "0.620 V · trim +0.2%"),
        SensorReading(0x1C, nameOf(0x1C), text = "OBD-II as defined by CARB"),
        SensorReading(0x1F, nameOf(0x1F), SensorKind.SECONDS, number = 754.0),
        SensorReading(0x21, nameOf(0x21), SensorKind.KILOMETERS, number = 18.0),
        SensorReading(0x2E, nameOf(0x2E), SensorKind.PERCENT, number = 12.5),
        SensorReading(0x2F, nameOf(0x2F), SensorKind.PERCENT, number = 64.0),
        SensorReading(0x30, nameOf(0x30), SensorKind.COUNT, number = 3.0),
        SensorReading(0x31, nameOf(0x31), SensorKind.KILOMETERS, number = 126.0),
        SensorReading(0x33, nameOf(0x33), SensorKind.KPA, number = 101.0),
        SensorReading(0x3C, nameOf(0x3C), SensorKind.CELSIUS, number = 486.0),
        SensorReading(0x42, nameOf(0x42), SensorKind.VOLTS, number = 13.8),
        SensorReading(0x43, nameOf(0x43), SensorKind.PERCENT, number = 28.4),
        SensorReading(0x44, nameOf(0x44), SensorKind.RATIO, number = 0.998),
        SensorReading(0x45, nameOf(0x45), SensorKind.PERCENT, number = 18.0),
        SensorReading(0x46, nameOf(0x46), SensorKind.CELSIUS, number = 22.0),
        SensorReading(0x47, nameOf(0x47), SensorKind.PERCENT, number = 21.0),
        SensorReading(0x49, nameOf(0x49), SensorKind.PERCENT, number = 16.4),
        SensorReading(0x4C, nameOf(0x4C), SensorKind.PERCENT, number = 19.5),
        SensorReading(0x51, nameOf(0x51), text = "Gasoline"),
        SensorReading(0x5C, nameOf(0x5C), SensorKind.CELSIUS, number = 96.0),
        SensorReading(0x5E, nameOf(0x5E), SensorKind.LITERS_PER_HOUR, number = 4.8),
        SensorReading(0x62, nameOf(0x62), SensorKind.SIGNED_PERCENT, number = 22.0),
        SensorReading(0x63, nameOf(0x63), SensorKind.NEWTON_METERS, number = 353.0)
    )

    private class PidSpec(
        val pid: Int,
        val name: String,
        val decode: (String) -> SensorReading?
    )

    private val specs: Map<Int, PidSpec> = buildList {
        add(spec(0x01, "Monitor status") { payload ->
            val mil = ObdProtocolHandler.decodeMilStatus(payload) ?: return@spec null
            val state = if (mil.first) "MIL on" else "MIL off"
            val count = if (mil.second == 1) "1 code" else "${mil.second} codes"
            SensorReading(0x01, "Monitor status", text = "$state · $count")
        })
        add(spec(0x02, "Freeze frame DTC") { payload ->
            val code = ObdProtocolHandler.decodeFreezeFrameDtc(payload) ?: "None"
            SensorReading(0x02, "Freeze frame DTC", text = code)
        })
        add(spec(0x03, "Fuel system status") { payload ->
            val bank1 = ObdProtocolHandler.decodeFuelLoopStatus(payload) ?: return@spec null
            val bank2 = if (payload.length >= 4) {
                ObdProtocolHandler.decodeFuelLoopStatus(payload.substring(2, 4))
            } else {
                null
            }
            val text = if (bank2 != null && bank2 != bank1) {
                "${bank1.title} · Bank 2: ${bank2.title}"
            } else {
                bank1.title
            }
            SensorReading(0x03, "Fuel system status", text = text)
        })
        add(percent(0x04, "Calculated engine load"))
        add(temp(0x05, "Coolant temperature"))
        add(trim(0x06, "Short term fuel trim (Bank 1)"))
        add(trim(0x07, "Long term fuel trim (Bank 1)"))
        add(trim(0x08, "Short term fuel trim (Bank 2)"))
        add(trim(0x09, "Long term fuel trim (Bank 2)"))
        add(spec(0x0A, "Fuel pressure") { payload ->
            val a = byteValue(payload) ?: return@spec null
            SensorReading(0x0A, "Fuel pressure", SensorKind.KPA, number = a * 3.0)
        })
        add(kpa(0x0B, "Intake manifold pressure"))
        add(spec(0x0C, "Engine speed") { payload ->
            val rpm = ObdProtocolHandler.decodeRpm(payload) ?: return@spec null
            SensorReading(0x0C, "Engine speed", SensorKind.RPM, number = rpm)
        })
        add(spec(0x0D, "Vehicle speed") { payload ->
            val speed = ObdProtocolHandler.decodeSpeed(payload) ?: return@spec null
            SensorReading(0x0D, "Vehicle speed", SensorKind.SPEED_KMH, number = speed)
        })
        add(spec(0x0E, "Timing advance") { payload ->
            val a = byteValue(payload) ?: return@spec null
            SensorReading(0x0E, "Timing advance", SensorKind.DEGREES, number = (a / 2.0) - 64.0)
        })
        add(temp(0x0F, "Intake air temperature"))
        add(spec(0x10, "Mass air flow") { payload ->
            val maf = ObdProtocolHandler.decodeMaf(payload) ?: return@spec null
            SensorReading(0x10, "Mass air flow", SensorKind.GRAMS_PER_SEC, number = maf)
        })
        add(percent(0x11, "Throttle position"))
        add(spec(0x12, "Commanded secondary air") { payload ->
            val a = byteValue(payload) ?: return@spec null
            val text = when (a) {
                1 -> "Upstream"
                2 -> "Downstream of catalyst"
                4 -> "Outside air or off"
                8 -> "Pump on for diagnostics"
                else -> "Code $a"
            }
            SensorReading(0x12, "Commanded secondary air", text = text)
        })
        add(o2Present(0x13, "Oxygen sensors present", listOf(
            "B1S1", "B1S2", "B1S3", "B1S4", "B2S1", "B2S2", "B2S3", "B2S4"
        )))
        narrowband(0x14, "O2 Bank 1 Sensor 1")
        narrowband(0x15, "O2 Bank 1 Sensor 2")
        narrowband(0x16, "O2 Bank 1 Sensor 3")
        narrowband(0x17, "O2 Bank 1 Sensor 4")
        narrowband(0x18, "O2 Bank 2 Sensor 1")
        narrowband(0x19, "O2 Bank 2 Sensor 2")
        narrowband(0x1A, "O2 Bank 2 Sensor 3")
        narrowband(0x1B, "O2 Bank 2 Sensor 4")
        add(spec(0x1C, "OBD standard") { payload ->
            val a = byteValue(payload) ?: return@spec null
            SensorReading(0x1C, "OBD standard", text = obdStandard(a))
        })
        add(o2Present(0x1D, "Oxygen sensors present (alt)", listOf(
            "B1S1", "B1S2", "B2S1", "B2S2", "B3S1", "B3S2", "B4S1", "B4S2"
        )))
        add(wordKind(0x1F, "Run time since start", SensorKind.SECONDS) { it.toDouble() })
        add(wordKind(0x21, "Distance with MIL on", SensorKind.KILOMETERS) { it.toDouble() })
        add(wordKind(0x22, "Fuel rail pressure (relative)", SensorKind.KPA) { it * 0.079 })
        add(wordKind(0x23, "Fuel rail gauge pressure", SensorKind.KPA) { it * 10.0 })
        add(percent(0x2C, "Commanded EGR"))
        add(trim(0x2D, "EGR error"))
        add(percent(0x2E, "Commanded evap purge"))
        add(percent(0x2F, "Fuel tank level"))
        add(spec(0x30, "Warm-ups since codes cleared") { payload ->
            val a = byteValue(payload) ?: return@spec null
            SensorReading(0x30, "Warm-ups since codes cleared", SensorKind.COUNT, number = a.toDouble())
        })
        add(wordKind(0x31, "Distance since codes cleared", SensorKind.KILOMETERS) { it.toDouble() })
        add(spec(0x32, "Evap system vapor pressure") { payload ->
            val raw = wordValue(payload) ?: return@spec null
            val signed = if (raw > 32767) raw - 65536 else raw
            SensorReading(0x32, "Evap system vapor pressure", SensorKind.PASCAL, number = signed / 4.0)
        })
        add(kpa(0x33, "Barometric pressure"))
        wideband(0x34, "Wideband O2 Bank 1 Sensor 1")
        wideband(0x35, "Wideband O2 Bank 1 Sensor 2")
        wideband(0x36, "Wideband O2 Bank 1 Sensor 3")
        wideband(0x37, "Wideband O2 Bank 1 Sensor 4")
        wideband(0x38, "Wideband O2 Bank 2 Sensor 1")
        wideband(0x39, "Wideband O2 Bank 2 Sensor 2")
        wideband(0x3A, "Wideband O2 Bank 2 Sensor 3")
        wideband(0x3B, "Wideband O2 Bank 2 Sensor 4")
        add(catalyst(0x3C, "Catalyst temperature B1S1"))
        add(catalyst(0x3D, "Catalyst temperature B2S1"))
        add(catalyst(0x3E, "Catalyst temperature B1S2"))
        add(catalyst(0x3F, "Catalyst temperature B2S2"))
        add(wordKind(0x42, "Control module voltage", SensorKind.VOLTS) { it / 1000.0 })
        add(wordKind(0x43, "Absolute load value", SensorKind.PERCENT) { it * 100.0 / 255.0 })
        add(wordKind(0x44, "Commanded equivalence ratio", SensorKind.RATIO) { it / 32768.0 })
        add(percent(0x45, "Relative throttle position"))
        add(temp(0x46, "Ambient air temperature"))
        add(percent(0x47, "Absolute throttle position B"))
        add(percent(0x48, "Absolute throttle position C"))
        add(percent(0x49, "Accelerator pedal position D"))
        add(percent(0x4A, "Accelerator pedal position E"))
        add(percent(0x4B, "Accelerator pedal position F"))
        add(percent(0x4C, "Commanded throttle actuator"))
        add(wordKind(0x4D, "Time run with MIL on", SensorKind.MINUTES) { it.toDouble() })
        add(wordKind(0x4E, "Time since codes cleared", SensorKind.MINUTES) { it.toDouble() })
        add(spec(0x51, "Fuel type") { payload ->
            val a = byteValue(payload) ?: return@spec null
            SensorReading(0x51, "Fuel type", text = fuelType(a))
        })
        add(percent(0x52, "Ethanol fuel percent"))
        add(wordKind(0x53, "Absolute evap vapor pressure", SensorKind.KPA) { it / 200.0 })
        add(spec(0x54, "Evap vapor pressure") { payload ->
            val raw = wordValue(payload) ?: return@spec null
            SensorReading(0x54, "Evap vapor pressure", SensorKind.PASCAL, number = (raw - 32767).toDouble())
        })
        add(percent(0x5A, "Relative accelerator pedal"))
        add(percent(0x5B, "Hybrid battery remaining"))
        add(temp(0x5C, "Engine oil temperature"))
        add(wordKind(0x5D, "Fuel injection timing", SensorKind.DEGREES) { (it - 26880) / 128.0 })
        add(wordKind(0x5E, "Engine fuel rate", SensorKind.LITERS_PER_HOUR) { it * 0.05 })
        add(torque(0x61, "Driver demand torque"))
        add(torque(0x62, "Actual engine torque"))
        add(wordKind(0x63, "Engine reference torque", SensorKind.NEWTON_METERS) { it.toDouble() })
    }.associateBy { it.pid }

    private fun spec(pid: Int, name: String, decode: (String) -> SensorReading?) = PidSpec(pid, name, decode)

    private fun percent(pid: Int, name: String) = spec(pid, name) { payload ->
        val value = ObdProtocolHandler.decodeLoad(payload) ?: return@spec null
        SensorReading(pid, name, SensorKind.PERCENT, number = value)
    }

    private fun temp(pid: Int, name: String) = spec(pid, name) { payload ->
        val value = ObdProtocolHandler.decodeCoolant(payload) ?: return@spec null
        SensorReading(pid, name, SensorKind.CELSIUS, number = value)
    }

    private fun trim(pid: Int, name: String) = spec(pid, name) { payload ->
        val value = ObdProtocolHandler.decodeFuelTrim(payload) ?: return@spec null
        SensorReading(pid, name, SensorKind.SIGNED_PERCENT, number = value)
    }

    private fun kpa(pid: Int, name: String) = spec(pid, name) { payload ->
        val value = ObdProtocolHandler.decodeMap(payload) ?: return@spec null
        SensorReading(pid, name, SensorKind.KPA, number = value)
    }

    private fun torque(pid: Int, name: String) = spec(pid, name) { payload ->
        val a = byteValue(payload) ?: return@spec null
        SensorReading(pid, name, SensorKind.SIGNED_PERCENT, number = (a - 125).toDouble())
    }

    private fun wordKind(pid: Int, name: String, kind: SensorKind, convert: (Int) -> Double) =
        spec(pid, name) { payload ->
            val raw = wordValue(payload) ?: return@spec null
            SensorReading(pid, name, kind, number = convert(raw))
        }

    private fun catalyst(pid: Int, name: String) = spec(pid, name) { payload ->
        val raw = wordValue(payload) ?: return@spec null
        SensorReading(pid, name, SensorKind.CELSIUS, number = (raw / 10.0) - 40.0)
    }

    private fun MutableList<PidSpec>.narrowband(pid: Int, name: String) {
        add(spec(pid, name) { payload ->
            val volts = ObdProtocolHandler.decodeO2SensorVoltage(payload) ?: return@spec null
            val trim = if (payload.length >= 4) {
                ObdProtocolHandler.decodeFuelTrim(payload.substring(2, 4))
            } else {
                null
            }
            val text = if (trim != null) {
                "%.3f V · trim %+.1f%%".format(volts, trim)
            } else {
                "%.3f V".format(volts)
            }
            SensorReading(pid, name, text = text)
        })
    }

    private fun MutableList<PidSpec>.wideband(pid: Int, name: String) {
        add(spec(pid, name) { payload ->
            val ratioWord = wordValue(payload) ?: return@spec null
            val ratio = ratioWord / 32768.0
            val text = if (payload.length >= 8) {
                val currentWord = wordValue(payload.substring(4, 8)) ?: return@spec null
                val current = (currentWord / 256.0) - 128.0
                "λ %.3f · %.2f mA".format(ratio, current)
            } else {
                "λ %.3f".format(ratio)
            }
            SensorReading(pid, name, text = text)
        })
    }

    private fun o2Present(pid: Int, name: String, labels: List<String>) = spec(pid, name) { payload ->
        val mask = byteValue(payload) ?: return@spec null
        val present = labels.filterIndexed { index, _ -> ((mask shr index) and 1) == 1 }
        SensorReading(pid, name, text = if (present.isEmpty()) "None reported" else present.joinToString(", "))
    }

    private fun byteValue(payload: String): Int? {
        if (payload.length < 2) return null
        return payload.take(2).toIntOrNull(16)
    }

    private fun wordValue(payload: String): Int? {
        if (payload.length < 4) return null
        val high = payload.substring(0, 2).toIntOrNull(16) ?: return null
        val low = payload.substring(2, 4).toIntOrNull(16) ?: return null
        return (high shl 8) or low
    }

    private fun hexBytes(payload: String): String {
        val clean = payload.filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
        if (clean.isEmpty()) return "—"
        return clean.chunked(2).joinToString(" ")
    }

    private fun obdStandard(code: Int): String = when (code) {
        1 -> "OBD-II as defined by CARB"
        2 -> "OBD as defined by EPA"
        3 -> "OBD and OBD-II"
        4 -> "OBD-I"
        5 -> "Not OBD compliant"
        6 -> "EOBD"
        7 -> "EOBD and OBD-II"
        8 -> "EOBD and OBD"
        9 -> "EOBD, OBD, and OBD-II"
        10 -> "JOBD"
        11 -> "JOBD and OBD-II"
        12 -> "JOBD and EOBD"
        13 -> "JOBD, EOBD, and OBD-II"
        else -> "Standard code $code"
    }

    private fun fuelType(code: Int): String = when (code) {
        0 -> "Not available"
        1 -> "Gasoline"
        2 -> "Methanol"
        3 -> "Ethanol"
        4 -> "Diesel"
        5 -> "LPG"
        6 -> "CNG"
        7 -> "Propane"
        8 -> "Electric"
        9 -> "Bifuel running gasoline"
        10 -> "Bifuel running methanol"
        11 -> "Bifuel running ethanol"
        12 -> "Bifuel running LPG"
        13 -> "Bifuel running CNG"
        14 -> "Bifuel running propane"
        15 -> "Bifuel running electricity"
        16 -> "Bifuel running electric and combustion"
        17 -> "Hybrid gasoline"
        18 -> "Hybrid ethanol"
        19 -> "Hybrid diesel"
        20 -> "Hybrid electric"
        21 -> "Hybrid running electric and combustion"
        22 -> "Hybrid regenerative"
        23 -> "Bifuel running diesel"
        else -> "Fuel code $code"
    }
}

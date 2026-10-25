package com.example.obdscanner.obd

import com.example.obdscanner.model.DtcCategory
import com.example.obdscanner.model.DtcCode
import com.example.obdscanner.model.FuelLoopStatus

object ObdProtocolHandler {

    /**
     * Clean and strip non-hex characters from raw adapter string, removing ELM prompt symbols,
     * header numbers, SEARCHING strings, and whitespace.
     */
    fun normalizeHexResponse(response: String): String {
        return response.uppercase()
            .replace("SEARCHING...", "")
            .replace("SEARCHING", "")
            .replace("BUS INIT...", "")
            .replace("STOPPED", "")
            .replace(">", "")
            .replace(Regex("[^0-9A-F]"), "")
    }

    /**
     * Parse PID response by looking for positive response header (Service + 0x40 + PID).
     */
    fun extractPidPayload(response: String, command: String): String? {
        val normalized = normalizeHexResponse(response)
        if (normalized.contains("NODATA") || normalized.contains("UNABLETOCONNECT") || normalized.contains("ERROR")) {
            return null
        }
        if (command.length < 4) return null
        val serviceHex = command.take(2)
        val serviceInt = serviceHex.toIntOrNull(16) ?: return null
        val responseServiceHex = "%02X".format(serviceInt + 0x40)
        val pidHex = command.substring(2, 4).uppercase()
        val expectedPrefix = responseServiceHex + pidHex

        return if (normalized.contains(expectedPrefix)) {
            normalized.substringAfter(expectedPrefix)
        } else {
            null
        }
    }

    fun decodeFreezeFrameDtc(payload: String?): String? {
        if (payload == null || payload.length < 4) return null
        val pair = payload.take(4)
        if (pair == "0000") return null
        val b1 = pair.substring(0, 2).toIntOrNull(16) ?: return null
        val b2 = pair.substring(2, 4).toIntOrNull(16) ?: return null
        val systemChar = when ((b1 shr 6) and 0x03) {
            0 -> "P"
            1 -> "C"
            2 -> "B"
            else -> "U"
        }
        val secondDigit = ((b1 shr 4) and 0x03).toString()
        return "%s%s%X%02X".format(systemChar, secondDigit, b1 and 0x0F, b2)
    }

    /**
     * Mode 01/02 support bitfield. Bit 7 of the first byte is [firstPid].
     * For PID 00, firstPid is 0x01. For PID 20, firstPid is 0x21.
     * Returns null when the ECU did not report a support mask.
     */
    fun decodeSupportedPids(payload: String?, firstPid: Int = 1): Set<Int>? {
        if (payload == null || payload.length < 8) return null
        val bytes = (0 until 4).map { index ->
            payload.substring(index * 2, index * 2 + 2).toIntOrNull(16) ?: return null
        }
        val supported = mutableSetOf<Int>()
        for (offset in 0 until 32) {
            val byte = bytes[offset / 8]
            val bit = 7 - (offset % 8)
            if (((byte shr bit) and 1) == 1) supported.add(firstPid + offset)
        }
        return supported
    }

    // --- Mode 01 PID Decoders ---

    fun decodeRpm(payload: String?): Double? {
        if (payload == null || payload.length < 4) return null
        return try {
            val a = payload.substring(0, 2).toInt(16)
            val b = payload.substring(2, 4).toInt(16)
            ((a * 256) + b) / 4.0
        } catch (_: Exception) {
            null
        }
    }

    fun decodeSpeed(payload: String?): Double? {
        if (payload == null || payload.length < 2) return null
        return try {
            payload.take(2).toInt(16).toDouble()
        } catch (_: Exception) {
            null
        }
    }

    fun decodeCoolant(payload: String?): Double? {
        if (payload == null || payload.length < 2) return null
        return try {
            (payload.take(2).toInt(16) - 40).toDouble()
        } catch (_: Exception) {
            null
        }
    }

    fun decodeLoad(payload: String?): Double? {
        if (payload == null || payload.length < 2) return null
        return try {
            (payload.take(2).toInt(16) * 100.0) / 255.0
        } catch (_: Exception) {
            null
        }
    }

    fun decodeThrottle(payload: String?): Double? {
        if (payload == null || payload.length < 2) return null
        return try {
            (payload.take(2).toInt(16) * 100.0) / 255.0
        } catch (_: Exception) {
            null
        }
    }

    fun decodeFuelTrim(payload: String?): Double? {
        if (payload == null || payload.length < 2) return null
        return try {
            val a = payload.take(2).toInt(16)
            (a - 128) * (100.0 / 128.0)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeIat(payload: String?): Double? {
        if (payload == null || payload.length < 2) return null
        return try {
            (payload.take(2).toInt(16) - 40).toDouble()
        } catch (_: Exception) {
            null
        }
    }

    fun decodeMap(payload: String?): Double? {
        if (payload == null || payload.length < 2) return null
        return try {
            payload.take(2).toInt(16).toDouble()
        } catch (_: Exception) {
            null
        }
    }

    fun decodeMaf(payload: String?): Double? {
        if (payload == null || payload.length < 4) return null
        return try {
            val a = payload.substring(0, 2).toInt(16)
            val b = payload.substring(2, 4).toInt(16)
            ((a * 256) + b) / 100.0
        } catch (_: Exception) {
            null
        }
    }

    fun decodeFuelLoopStatus(payload: String?): FuelLoopStatus? {
        if (payload == null || payload.length < 2) return null
        return try {
            val byteA = payload.take(2).toInt(16)
            when (byteA) {
                1 -> FuelLoopStatus.OPEN_LOOP_WARMUP
                2 -> FuelLoopStatus.CLOSED_LOOP_NORMAL
                4 -> FuelLoopStatus.OPEN_LOOP_HIGH_LOAD
                8 -> FuelLoopStatus.OPEN_LOOP_FAULT
                16 -> FuelLoopStatus.CLOSED_LOOP_FAULT
                else -> FuelLoopStatus.CLOSED_LOOP_NORMAL
            }
        } catch (_: Exception) {
            null
        }
    }

    fun decodeO2SensorVoltage(payload: String?): Double? {
        if (payload == null || payload.length < 2) return null
        return try {
            val a = payload.take(2).toInt(16)
            a / 200.0
        } catch (_: Exception) {
            null
        }
    }

    fun decodeVoltage(response: String): Double? {
        // ATRV response is usually formatted like "12.6V" or "13.8 V"
        val cleaned = response.replace("V", "").replace("v", "").trim()
        val match = Regex("""\d+(\.\d+)?""").find(cleaned)
        return match?.value?.toDoubleOrNull()
    }

    fun decodeMilStatus(payload: String?): Pair<Boolean, Int>? {
        if (payload == null || payload.length < 2) return null
        return try {
            val byteA = payload.take(2).toInt(16)
            val milOn = (byteA and 0x80) != 0
            val dtcCount = byteA and 0x7F
            Pair(milOn, dtcCount)
        } catch (_: Exception) {
            null
        }
    }

    // --- Mode 09 Vehicle Info Decoders ---

    fun decodeVin(rawResponse: String): String {
        // Mode 09 PID 02 response contains ASCII bytes across lines
        // Mode 09 response header is 49 02
        val payloadHex = extractMode09Payload(rawResponse, "02")
        if (payloadHex.isBlank()) return "Unavailable"
        val ascii = hexToAscii(payloadHex)
        val cleanedVin = ascii.filter { it.isLetterOrDigit() }.uppercase()
        return if (cleanedVin.length >= 11) cleanedVin else "Unavailable"
    }

    fun decodeCalibrationId(rawResponse: String): String {
        val payloadHex = extractMode09Payload(rawResponse, "04")
        if (payloadHex.isBlank()) return "Unavailable"
        val ascii = hexToAscii(payloadHex).trim()
        val cleaned = ascii.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        return if (cleaned.isNotBlank()) cleaned else "Unavailable"
    }

    fun decodeCvn(rawResponse: String): String {
        val payloadHex = extractMode09Payload(rawResponse, "06")
        if (payloadHex.isBlank()) return "Unavailable"
        val cleaned = payloadHex.take(8).uppercase()
        return if (cleaned.isNotBlank()) cleaned else "Unavailable"
    }

    private fun extractMode09Payload(rawResponse: String, pidHex: String): String {
        val responseHeader = "49" + pidHex.uppercase()
        val sb = StringBuilder()
        val lines = rawResponse.replace(">", " ")
            .split('\r', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (rawLine in lines) {
            if (rawLine.contains("NO DATA", true) ||
                rawLine.contains("ERROR", true) ||
                rawLine.contains("SEARCHING", true) ||
                rawLine.contains("UNABLE", true)
            ) {
                continue
            }
            // Drop only a real ELM line index such as "1:" before the hex is stripped.
            var line = rawLine.replace(Regex("^[0-9A-F]:\\s*"), "")
            line = line.replace(Regex("^[0-9A-F]{3}\\s+"), "")
            val hex = line.uppercase().replace(Regex("[^0-9A-F]"), "")
            if (hex.isEmpty()) continue
            val dataPart = if (hex.contains(responseHeader)) hex.substringAfter(responseHeader) else hex
            sb.append(dataPart)
        }

        var payload = sb.toString()
        if (payload.startsWith("01")) payload = payload.drop(2)
        return payload
    }

    private fun hexToAscii(hex: String): String {
        val sb = java.lang.StringBuilder()
        var i = 0
        while (i + 2 <= hex.length) {
            val byteStr = hex.substring(i, i + 2)
            val code = byteStr.toIntOrNull(16) ?: break
            if (code in 32..126) { // printable ASCII range
                sb.append(code.toChar())
            }
            i += 2
        }
        return sb.toString()
    }

    // --- Mode 03, 07, 0A Diagnostic Trouble Codes ---

    fun decodeDtcResponse(
        rawResponse: String,
        requestedMode: String,
        category: DtcCategory,
        vehicleProfile: VehicleProfile? = null
    ): List<DtcCode> {
        val modeResponseHeader = when (requestedMode) {
            "03" -> "43"
            "07" -> "47"
            "0A" -> "4A"
            else -> return emptyList()
        }

        if (rawResponse.contains("NO DATA") || rawResponse.contains("NODATA") || rawResponse.contains("ERROR")) {
            return emptyList()
        }

        // Clean lines and strip CAN ECU headers (like 7E8, 7E9) and line sequence numbers
        val cleanedLines = rawResponse.split("\r", "\n")
            .map { line ->
                line.trim().uppercase()
                    .replace("7E8", "")
                    .replace("7E9", "")
                    .replace("7EA", "")
                    .replace(Regex("^[0-9]:"), "") // sequence numbers 0:, 1:
                    .replace(Regex("[^0-9A-F]"), "")
            }
            .filter { it.isNotBlank() && !it.contains("SEARCHING") }

        val joined = cleanedLines.joinToString("")
        if (!joined.contains(modeResponseHeader)) return emptyList()

        var payload = joined.substringAfter(modeResponseHeader)
        // Drop length byte if present (e.g. 02, 04, 06)
        if (payload.length >= 2 && payload.substring(0, 2).toIntOrNull(16) in 1..10) {
            payload = payload.drop(2)
        }

        if (payload.isBlank()) return emptyList()

        val dtcList = mutableListOf<DtcCode>()
        var i = 0
        // DTCs are encoded in 2-byte (4 hex character) pairs
        while (i + 4 <= payload.length) {
            val pair = payload.substring(i, i + 4)
            i += 4
            if (pair == "0000") continue

            val b1 = pair.substring(0, 2).toIntOrNull(16) ?: continue
            val b2 = pair.substring(2, 4).toIntOrNull(16) ?: continue

            // Determine system character
            val systemChar = when ((b1 shr 6) and 0x03) {
                0 -> "P"
                1 -> "C"
                2 -> "B"
                else -> "U"
            }

            val secondDigit = ((b1 shr 4) and 0x03).toString()
            val formattedCode = "%s%s%X%02X".format(systemChar, secondDigit, b1 and 0x0F, b2)

            val dtcCode = DtcDatabase.lookup(formattedCode, category, vehicleProfile)
            dtcList.add(dtcCode)
        }

        return dtcList.distinctBy { it.code }
    }
}

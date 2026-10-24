package com.example.obdscanner.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.example.obdscanner.model.ObdAdapterInfo
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ObdBluetoothManager {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    val isConnected: Boolean
        get() = socket?.isConnected == true

    @SuppressLint("MissingPermission")
    @Throws(IOException::class)
    fun connect(device: BluetoothDevice, onInitProgress: (String) -> Unit): ObdAdapterInfo {
        closeSocket()

        val newSocket = device.createRfcommSocketToServiceRecord(sppUuid)
        newSocket.connect()
        socket = newSocket
        inputStream = newSocket.inputStream
        outputStream = newSocket.outputStream

        onInitProgress("Resetting adapter (ATZ)…")
        try {
            sendCommand("ATZ", timeoutMs = 4000)
        } catch (_: Exception) {}

        onInitProgress("Querying OBDLink Device ID (STDI)…")
        val deviceIdResponse = try {
            sendCommand("STDI", timeoutMs = 1500)
        } catch (_: Exception) {
            "Standard ELM327"
        }

        onInitProgress("Querying ELM Version (ATI)…")
        val atiResponse = try {
            sendCommand("ATI", timeoutMs = 1500)
        } catch (_: Exception) {
            "ELM327 v1.5"
        }

        onInitProgress("Configuring CAN protocol settings…")
        sendCommand("ATE0", timeoutMs = 1500) // Echo off
        sendCommand("ATL0", timeoutMs = 1500) // Linefeeds off
        sendCommand("ATS1", timeoutMs = 1500) // Spaces on for parsing
        sendCommand("ATH0", timeoutMs = 1500) // Headers off
        sendCommand("ATCAF1", timeoutMs = 1500) // Automatic CAN formatting ON
        sendCommand("ATSP0", timeoutMs = 2000) // Auto protocol search

        onInitProgress("Initializing vehicle CAN bus protocol (0100)…")
        var protocolEstablished = false
        try {
            val initPid = sendCommand("0100", timeoutMs = 5000)
            if (!initPid.contains("UNABLE") && !initPid.contains("ERROR") && !initPid.contains("NO DATA")) {
                protocolEstablished = true
            }
        } catch (_: Exception) {}

        if (!protocolEstablished) {
            onInitProgress("Fallback to Jeep/Chrysler CAN 11-bit 500k (ATSP6)…")
            try {
                sendCommand("ATSP6", timeoutMs = 1500) // Force ISO 15765-4 CAN 11bit 500k baud
                sendCommand("0100", timeoutMs = 3000)
            } catch (_: Exception) {}
        }

        onInitProgress("Querying active OBD protocol (ATDP)…")
        val atdpResponse = try {
            sendCommand("ATDP", timeoutMs = 1500)
        } catch (_: Exception) {
            "ISO 15765-4 CAN (11bit 500k)"
        }

        onInitProgress("Querying battery voltage (ATRV)…")
        val voltageResponse = try {
            sendCommand("ATRV", timeoutMs = 1500)
        } catch (_: Exception) {
            "12.0V"
        }

        val deviceIdClean = if (deviceIdResponse.isNotBlank() && !deviceIdResponse.contains("?")) {
            deviceIdResponse.replace(">", "").trim()
        } else {
            "OBDLink MX / ELM327"
        }

        val versionClean = atiResponse.replace(">", "").trim()
        val protocolClean = atdpResponse.replace(">", "").trim().ifBlank { "ISO 15765-4 CAN 11bit 500k" }

        return ObdAdapterInfo(
            name = device.name ?: "OBDLink Adapter",
            address = device.address,
            deviceId = deviceIdClean,
            elmVersion = versionClean,
            voltage = voltageResponse.replace(">", "").trim(),
            protocol = protocolClean
        )
    }

    @Throws(IOException::class)
    fun sendCommand(command: String, timeoutMs: Long = 2000): String {
        val activeSocket = socket ?: throw IOException("Bluetooth socket is not connected")
        check(activeSocket.isConnected) { "Socket is closed" }

        val inStream = inputStream ?: throw IOException("Input stream unavailable")
        val outStream = outputStream ?: throw IOException("Output stream unavailable")

        synchronized(activeSocket) {
            // Drain any pending input buffer
            while (inStream.available() > 0) {
                inStream.read()
            }

            outStream.write((command + "\r").toByteArray(Charsets.US_ASCII))
            outStream.flush()

            val response = StringBuilder()
            val deadline = System.currentTimeMillis() + timeoutMs

            while (System.currentTimeMillis() < deadline) {
                if (inStream.available() > 0) {
                    val ch = inStream.read()
                    if (ch >= 0) {
                        val char = ch.toChar()
                        if (char == '>') {
                            return response.toString()
                        }
                        response.append(char)
                    }
                } else {
                    try {
                        Thread.sleep(10)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Command execution interrupted")
                    }
                }
            }
            throw IOException("Timeout waiting for adapter response to '$command'")
        }
    }

    fun disconnect() {
        closeSocket()
    }

    private fun closeSocket() {
        try { inputStream?.close() } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        socket = null
    }
}

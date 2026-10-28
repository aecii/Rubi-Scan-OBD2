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

package com.example.obdscanner.viewmodel

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.obdscanner.model.AppTheme
import com.example.obdscanner.model.ConnectionState
import com.example.obdscanner.model.DtcCategory
import com.example.obdscanner.model.DtcCode
import com.example.obdscanner.model.FreezeFrameData
import com.example.obdscanner.model.FuelLoopStatus
import com.example.obdscanner.model.LiveMetrics
import com.example.obdscanner.model.O2DisplayMode
import com.example.obdscanner.model.O2Sample
import com.example.obdscanner.model.ObdAdapterInfo
import com.example.obdscanner.model.SavedVehicle
import com.example.obdscanner.model.SensorKind
import com.example.obdscanner.model.SensorReading
import com.example.obdscanner.model.DisplayUnits
import com.example.obdscanner.model.SpeedUnit
import com.example.obdscanner.model.TemperatureUnit
import com.example.obdscanner.model.VehicleInfo
import com.example.obdscanner.model.VehicleSaveOffer
import com.example.obdscanner.obd.AvailableUpdate
import com.example.obdscanner.obd.DtcDatabase
import com.example.obdscanner.obd.ObdPidCatalog
import com.example.obdscanner.obd.VersionCheck
import com.example.obdscanner.obd.ObdBluetoothManager
import com.example.obdscanner.obd.ObdProtocolHandler
import com.example.obdscanner.obd.PdfReportGenerator
import com.example.obdscanner.obd.VehicleProfile
import com.example.obdscanner.obd.VehicleProfileStore
import com.example.obdscanner.obd.VinDecoder
import com.example.obdscanner.obd.toVehicleInfo
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

class ObdScannerViewModel : ViewModel() {

    private val bluetoothManager = ObdBluetoothManager()
    private var pollJob: Job? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _liveMetrics = MutableStateFlow(LiveMetrics())
    val liveMetrics: StateFlow<LiveMetrics> = _liveMetrics.asStateFlow()

    private val _o2History = MutableStateFlow<List<O2Sample>>(emptyList())
    val o2History: StateFlow<List<O2Sample>> = _o2History.asStateFlow()

    private val _o2DisplayMode = MutableStateFlow(O2DisplayMode.GRAPH)
    val o2DisplayMode: StateFlow<O2DisplayMode> = _o2DisplayMode.asStateFlow()

    private val _troubleCodes = MutableStateFlow<List<DtcCode>>(emptyList())
    val troubleCodes: StateFlow<List<DtcCode>> = _troubleCodes.asStateFlow()

    private val _freezeFrameData = MutableStateFlow<FreezeFrameData?>(null)
    val freezeFrameData: StateFlow<FreezeFrameData?> = _freezeFrameData.asStateFlow()

    private val _sensorReadings = MutableStateFlow<List<SensorReading>>(emptyList())
    val sensorReadings: StateFlow<List<SensorReading>> = _sensorReadings.asStateFlow()
    private val sensorLock = Any()
    private val supportedDataPids = mutableListOf<Int>()
    private val sensorByPid = linkedMapOf<Int, SensorReading>()
    private var sensorCursor = 0

    @Volatile
    private var sensorsPageVisible = false

    private val metricsPids = setOf(
        0x01, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
        0x0B, 0x0C, 0x0D, 0x0F, 0x10, 0x11
    )

    private val _vehicleInfo = MutableStateFlow(VehicleInfo())
    val vehicleInfo: StateFlow<VehicleInfo> = _vehicleInfo.asStateFlow()

    private val _savedVehicles = MutableStateFlow<List<SavedVehicle>>(emptyList())
    val savedVehicles: StateFlow<List<SavedVehicle>> = _savedVehicles.asStateFlow()

    private val _activeVehicleId = MutableStateFlow<String?>(null)
    val activeVehicleId: StateFlow<String?> = _activeVehicleId.asStateFlow()

    private val _vehicleSaveOffer = MutableStateFlow<VehicleSaveOffer?>(null)
    val vehicleSaveOffer: StateFlow<VehicleSaveOffer?> = _vehicleSaveOffer.asStateFlow()
    private var pendingVehicleSave: VehicleSaveOffer? = null
    private var neverAskToSave = false

    private var appContext: Context? = null
    private var demoScenario: DemoScenario? = null
    private var lastDemoSignature: String? = null
    private var demoMilOn: Boolean = true
    private var demoStoredCount: Int = 1
    private var demoPrimaryCode: String = "P0300"

    private val demoVinAlphabet = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789"
    private val demoYearChars = "ABCDEFGHJKLMNPRST123456789"
    private val demoWmis = listOf(
        "1J4", "1J8", "1HG", "1FA", "1G1", "1GC", "1N4", "4T1", "JM1", "WBA",
        "WDD", "KMH", "JF1", "1VW", "5NP", "3FA", "JHM", "1C4", "2T1", "5FN",
        "WVW", "4S3", "1YV", "JN1"
    )
    private val demoCodePool = listOf(
        "P0100", "P0101", "P0115", "P0128", "P0171", "P0174",
        "P0300", "P0301", "P0302", "P0303", "P0304", "P0305", "P0306",
        "P0420", "P0442", "P0456", "P0520"
    )

    private val _adapterInfo = MutableStateFlow(ObdAdapterInfo())
    val adapterInfo: StateFlow<ObdAdapterInfo> = _adapterInfo.asStateFlow()

    private val _displayUnits = MutableStateFlow(DisplayUnits())
    val displayUnits: StateFlow<DisplayUnits> = _displayUnits.asStateFlow()

    private val _appTheme = MutableStateFlow(AppTheme.SYSTEM)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _statusMessage = MutableStateFlow("Pair your OBDLink adapter in Bluetooth settings, then tap Connect.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isReadingCodes = MutableStateFlow(false)
    val isReadingCodes: StateFlow<Boolean> = _isReadingCodes.asStateFlow()

    private val _isReadingVehicleInfo = MutableStateFlow(false)
    val isReadingVehicleInfo: StateFlow<Boolean> = _isReadingVehicleInfo.asStateFlow()

    // Demo Mode Simulation
    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AvailableUpdate?>(null)
    val availableUpdate: StateFlow<AvailableUpdate?> = _availableUpdate.asStateFlow()

    // CSV Telemetry Recorder
    private val _isLoggingCsv = MutableStateFlow(false)
    val isLoggingCsv: StateFlow<Boolean> = _isLoggingCsv.asStateFlow()

    private val csvLogBuffer = mutableListOf<String>()
    private val _csvSampleCount = MutableStateFlow(0)
    val csvSampleCount: StateFlow<Int> = _csvSampleCount.asStateFlow()

    fun checkForUpdateOnLaunch(context: Context) {
        val application = context.applicationContext
        appContext = application
        neverAskToSave = VehicleProfileStore.neverAskToSave(application)
        val stored = VehicleProfileStore.load(application)
        _savedVehicles.value = stored.vehicles
        _activeVehicleId.value = stored.activeId
        restoreActiveSavedVehicle()
        viewModelScope.launch {
            _availableUpdate.value = VersionCheck.findUpdate(application)
        }
    }

    fun dismissUpdateNotice() {
        _availableUpdate.value = null
    }

    fun setSensorsPageVisible(visible: Boolean) {
        sensorsPageVisible = visible
    }

    fun connect(device: BluetoothDevice) {
        pollJob?.cancel()
        pollJob = null
        if (_isDemoMode.value) {
            _isDemoMode.value = false
        }
        clearSensorCache()
        if (_connectionState.value is ConnectionState.Connecting || _connectionState.value is ConnectionState.Initializing) return

        _connectionState.value = ConnectionState.Connecting(device.name ?: "OBDLink Adapter")
        _statusMessage.value = "Connecting to ${device.name ?: "adapter"}…"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val adapterInfoResult = bluetoothManager.connect(device) { step ->
                    _connectionState.value = ConnectionState.Initializing(step)
                    _statusMessage.value = step
                }

                _adapterInfo.value = adapterInfoResult
                _connectionState.value = ConnectionState.Connected(adapterInfoResult.name)
                _vehicleInfo.value = VehicleInfo()
                _vehicleSaveOffer.value = null
                pendingVehicleSave = null
                _statusMessage.value = "Connected to ${adapterInfoResult.name}. Protocol: ${adapterInfoResult.protocol}."

                readVehicleIdentityInternal()

                // Read initial trouble codes on connect
                readTroubleCodesInternal()

                discoverSupportedPids()

                // Start live data polling loop
                startPolling()

            } catch (e: Exception) {
                bluetoothManager.disconnect()
                _connectionState.value = ConnectionState.Error(e.message ?: "Failed to connect")
                restoreActiveSavedVehicle()
                _statusMessage.value = "Connection failed: ${e.message ?: "Unknown error"}. Ensure adapter is paired and vehicle key is ON."
            }
        }
    }

    fun toggleDemoMode() {
        if (_isDemoMode.value) {
            _isDemoMode.value = false
            disconnect()
        } else {
            _isDemoMode.value = true
            enableDemoModeInternal(offerSave = true)
        }
    }

    private fun enableDemoModeInternal(offerSave: Boolean = false) {
        pollJob?.cancel()
        pollJob = null

        val scenario = if (offerSave || demoScenario == null) newDemoScenario() else demoScenario!!
        demoScenario = scenario
        demoStoredCount = scenario.storedCount
        demoMilOn = scenario.storedCount > 0
        demoPrimaryCode = scenario.primaryCode

        _vehicleInfo.value = VehicleInfo(
            vin = scenario.profile.vin,
            calibrationId = scenario.calibrationId,
            cvn = scenario.cvn,
            ecuName = "Engine control module",
            profile = scenario.profile
        )

        _adapterInfo.value = ObdAdapterInfo(
            name = "Demo OBDLink MX Simulator",
            address = "00:11:22:33:44:55",
            deviceId = "STN1170 v4.2 (Demo)",
            elmVersion = "ELM327 v2.1 (Demo)",
            voltage = "13.8 V",
            protocol = "ISO 15765-4 CAN (11bit 500k)"
        )

        _connectionState.value = ConnectionState.Connected("Demo Vehicle Simulator")
        val codeWord = if (scenario.codes.size == 1) "1 trouble code" else "${scenario.codes.size} trouble codes"
        _statusMessage.value = "Demo Mode Active: Simulating ${scenario.profile.displayName} with $codeWord."

        _liveMetrics.value = LiveMetrics(
            rpm = 2450.0,
            speedKmh = 72.4,
            coolantCelsius = 90.0,
            loadPercent = 38.5,
            throttlePercent = 22.0,
            batteryVoltage = 13.8,
            milOn = demoMilOn,
            dtcCount = demoStoredCount,
            stftBank1 = 3.2,
            ltftBank1 = 1.5,
            stftBank2 = 2.8,
            ltftBank2 = 0.9,
            o2b1s1 = 0.825,
            o2b1s2 = 0.650,
            o2b2s1 = 0.180,
            o2b2s2 = 0.620,
            intakeAirTempCelsius = 28.0,
            mapKpa = 42.0,
            mafGps = 18.5,
            fuelLoopStatus = FuelLoopStatus.CLOSED_LOOP_NORMAL
        )

        _troubleCodes.value = scenario.codes.map { entry ->
            // Demo names a make from the VIN only. Shop notes stay generic until the user names the engine.
            DtcDatabase.lookup(entry.code, entry.category, null)
        }
        _freezeFrameData.value = scenario.freezeFrame
        val milLabel = if (demoStoredCount == 1) "1 code" else "$demoStoredCount codes"
        _sensorReadings.value = ObdPidCatalog.demoReadings().map { reading ->
            when (reading.pid) {
                0x01 -> reading.copy(text = if (demoMilOn) "MIL on · $milLabel" else "MIL off · 0 codes")
                0x02 -> reading.copy(text = demoPrimaryCode)
                else -> reading
            }
        }
        offerSaveIfNew(scenario.profile, scenario.calibrationId, scenario.cvn)
        startDemoLiveRead()
    }

    private fun newDemoScenario(): DemoScenario {
        var scenario: DemoScenario
        do {
            scenario = rollDemoScenario()
        } while (scenario.signature == lastDemoSignature)
        lastDemoSignature = scenario.signature
        return scenario
    }

    private fun rollDemoScenario(): DemoScenario {
        val profile = VinDecoder.decode(rollDemoVin())
        val count = Random.nextInt(1, 5)
        val picked = demoCodePool.shuffled().take(count)
        val codes = picked.map { code -> DemoCode(code, DtcCategory.STORED) }
        val primary = codes.first().code
        val freezeFrame = FreezeFrameData(
            dtcCode = primary,
            rpm = Random.nextDouble(650.0, 3600.0),
            speedKmh = Random.nextDouble(0.0, 120.0),
            coolantCelsius = Random.nextDouble(60.0, 108.0),
            loadPercent = Random.nextDouble(12.0, 80.0),
            fuelTrimB1 = Random.nextDouble(-10.0, 15.0),
            throttlePercent = Random.nextDouble(5.0, 55.0),
            intakeAirCelsius = Random.nextDouble(5.0, 45.0),
            mapKpa = Random.nextDouble(25.0, 95.0),
            fuelLoopStatus = FuelLoopStatus.CLOSED_LOOP_NORMAL
        )
        return DemoScenario(
            profile = profile,
            calibrationId = "CAL" + List(8) { demoVinAlphabet.random() }.joinToString(""),
            cvn = List(8) { "0123456789ABCDEF".random() }.joinToString(""),
            codes = codes,
            freezeFrame = freezeFrame
        )
    }

    private fun rollDemoVin(): String {
        val chars = CharArray(17)
        demoWmis.random().forEachIndexed { index, char -> chars[index] = char }
        for (index in 3..7) chars[index] = demoVinAlphabet.random()
        chars[9] = demoYearChars.random()
        chars[10] = demoVinAlphabet.random()
        for (index in 11..16) chars[index] = demoVinAlphabet.random()
        chars[8] = vinCheckDigit(chars)
        return String(chars)
    }

    private fun vinCheckDigit(chars: CharArray): Char {
        val weights = intArrayOf(8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2)
        var sum = 0
        for (index in chars.indices) {
            sum += vinTransliterate(chars[index]) * weights[index]
        }
        val remainder = sum % 11
        return if (remainder == 10) 'X' else ('0' + remainder)
    }

    private fun vinTransliterate(char: Char): Int {
        if (char.isDigit()) return char - '0'
        return when (char) {
            'A', 'J' -> 1
            'B', 'K', 'S' -> 2
            'C', 'L', 'T' -> 3
            'D', 'M', 'U' -> 4
            'E', 'N', 'V' -> 5
            'F', 'W' -> 6
            'G', 'P', 'X' -> 7
            'H', 'Y' -> 8
            'R', 'Z' -> 9
            else -> 0
        }
    }

    fun disconnect() {
        pollJob?.cancel()
        pollJob = null
        bluetoothManager.disconnect()
        _isDemoMode.value = false
        _connectionState.value = ConnectionState.Disconnected
        _liveMetrics.value = LiveMetrics()
        _o2History.value = emptyList()
        _troubleCodes.value = emptyList()
        _freezeFrameData.value = null
        _adapterInfo.value = ObdAdapterInfo()
        clearSensorCache()
        demoScenario = null
        restoreActiveSavedVehicle()
        _statusMessage.value = "Disconnected."
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        _displayUnits.value = _displayUnits.value.copy(speed = unit)
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        _displayUnits.value = _displayUnits.value.copy(temperature = unit)
    }

    fun setAppTheme(theme: AppTheme) {
        _appTheme.value = theme
    }

    fun setO2DisplayMode(mode: O2DisplayMode) {
        _o2DisplayMode.value = mode
    }

    fun toggleCsvLogging() {
        if (_isLoggingCsv.value) {
            _isLoggingCsv.value = false
            _statusMessage.value = "CSV logging stopped. Ready for export (${csvLogBuffer.size} samples)."
        } else {
            csvLogBuffer.clear()
            csvLogBuffer.add("Timestamp_ms,RPM,Speed_kmh,Coolant_C,Load_pct,Throttle_pct,Battery_V,STFT1_pct,LTFT1_pct,STFT2_pct,LTFT2_pct,O2_B1S1_V,O2_B1S2_V,O2_B2S1_V,O2_B2S2_V,IAT_C,MAP_kpa,MAF_gps,Fuel_Loop")
            _isLoggingCsv.value = true
            _csvSampleCount.value = 0
            _statusMessage.value = "CSV telemetry recording started."
        }
    }

    fun exportCsvFile(context: Context): File? {
        if (csvLogBuffer.isEmpty()) return null
        return try {
            val file = File(context.cacheDir, "RubiScan_Telemetry_${System.currentTimeMillis()}.csv")
            FileWriter(file).use { writer ->
                csvLogBuffer.forEach { line ->
                    writer.appendLine(line)
                }
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    fun generatePdfReport(context: Context): File? {
        return PdfReportGenerator.generateReport(
            context = context,
            vehicleInfo = _vehicleInfo.value,
            adapterInfo = _adapterInfo.value,
            liveMetrics = _liveMetrics.value,
            dtcCodes = _troubleCodes.value,
            freezeFrame = _freezeFrameData.value,
            units = _displayUnits.value
        )
    }

    fun readTroubleCodes() {
        if (_isDemoMode.value) {
            enableDemoModeInternal()
            return
        }
        if (_connectionState.value !is ConnectionState.Connected) return
        viewModelScope.launch(Dispatchers.IO) {
            readTroubleCodesInternal()
        }
    }

    fun clearTroubleCodes(onComplete: (Boolean, String) -> Unit) {
        if (_isDemoMode.value) {
            _troubleCodes.value = emptyList()
            _freezeFrameData.value = null
            _statusMessage.value = "Demo Mode: Trouble codes cleared."
            onComplete(true, "Demo Mode: Fault codes reset successfully.")
            return
        }
        if (_connectionState.value !is ConnectionState.Connected) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _statusMessage.value = "Sending Mode 04 clear trouble codes command…"
                val response = bluetoothManager.sendCommand("04", timeoutMs = 3500)
                _statusMessage.value = "Clear codes command sent ($response). Refreshing trouble codes…"
                delay(1000)
                readTroubleCodesInternal()
                withContext(Dispatchers.Main) {
                    onComplete(true, "Trouble codes clear command executed.")
                }
            } catch (e: Exception) {
                _statusMessage.value = "Failed to clear codes: ${e.message}"
                withContext(Dispatchers.Main) {
                    onComplete(false, e.message ?: "Error clearing codes")
                }
            }
        }
    }

    private suspend fun readTroubleCodesInternal() {
        if (_isReadingCodes.value) return
        _isReadingCodes.value = true
        _statusMessage.value = "Reading stored, pending, and permanent trouble codes…"

        try {
            val currentProfile = _vehicleInfo.value.profile
            val allCodes = mutableListOf<DtcCode>()

            // Mode 03 - Stored
            val respMode03 = try { bluetoothManager.sendCommand("03", timeoutMs = 3500) } catch (_: Exception) { "" }
            val stored = ObdProtocolHandler.decodeDtcResponse(respMode03, "03", DtcCategory.STORED, currentProfile)
            allCodes.addAll(stored)

            // Mode 07 - Pending
            val respMode07 = try { bluetoothManager.sendCommand("07", timeoutMs = 3500) } catch (_: Exception) { "" }
            val pending = ObdProtocolHandler.decodeDtcResponse(respMode07, "07", DtcCategory.PENDING, currentProfile)
            allCodes.addAll(pending)

            // Mode 0A - Permanent
            val respMode0A = try { bluetoothManager.sendCommand("0A", timeoutMs = 3500) } catch (_: Exception) { "" }
            val permanent = ObdProtocolHandler.decodeDtcResponse(respMode0A, "0A", DtcCategory.PERMANENT, currentProfile)
            allCodes.addAll(permanent)

            val distinctCodes = allCodes.distinctBy { "${it.code}_${it.category}" }
            _troubleCodes.value = distinctCodes

            if (distinctCodes.isNotEmpty()) {
                _statusMessage.value = "Reading freeze frame saved with ${distinctCodes.first().code}…"
                readFreezeFrameInternal(distinctCodes.first().code)
            } else {
                _freezeFrameData.value = null
            }

            _statusMessage.value = if (distinctCodes.isEmpty()) {
                "No active diagnostic trouble codes returned by ECU."
            } else {
                "Read ${distinctCodes.size} trouble code(s)."
            }
        } catch (e: Exception) {
            _statusMessage.value = "Error reading trouble codes: ${e.message}"
        } finally {
            _isReadingCodes.value = false
        }
    }

    private suspend fun readFreezeFrameInternal(primaryDtc: String) {
        try {
            suspend fun readPid(command: String) = try {
                bluetoothManager.sendCommand(command, timeoutMs = 2500)
            } catch (_: Exception) {
                ""
            }

            val supportPayload = ObdProtocolHandler.extractPidPayload(readPid("020000"), "0200")
            val supported = ObdProtocolHandler.decodeSupportedPids(supportPayload)

            suspend fun payloadFor(pid: Int, command: String): String? {
                if (supported != null && pid !in supported) return null
                return ObdProtocolHandler.extractPidPayload(readPid(command), command.take(4))
            }

            val dtcPayload = payloadFor(0x02, "020200")
            val rpm = ObdProtocolHandler.decodeRpm(payloadFor(0x0C, "020C00"))
            val speed = ObdProtocolHandler.decodeSpeed(payloadFor(0x0D, "020D00"))
            val coolant = ObdProtocolHandler.decodeCoolant(payloadFor(0x05, "020500"))
            val load = ObdProtocolHandler.decodeLoad(payloadFor(0x04, "020400"))
            val fuelTrim = ObdProtocolHandler.decodeFuelTrim(payloadFor(0x06, "020600"))
            val throttle = ObdProtocolHandler.decodeThrottle(payloadFor(0x11, "021100"))
            val intake = ObdProtocolHandler.decodeIat(payloadFor(0x0F, "020F00"))
            val map = ObdProtocolHandler.decodeMap(payloadFor(0x0B, "020B00"))
            val fuelLoop = ObdProtocolHandler.decodeFuelLoopStatus(payloadFor(0x03, "020300"))
            val dtc = ObdProtocolHandler.decodeFreezeFrameDtc(dtcPayload) ?: primaryDtc
            val hasSnapshot = listOf(rpm, speed, coolant, load, fuelTrim, throttle, intake, map).any { it != null } ||
                fuelLoop != null

            _freezeFrameData.value = if (hasSnapshot) {
                FreezeFrameData(
                    dtcCode = dtc,
                    rpm = rpm,
                    speedKmh = speed,
                    coolantCelsius = coolant,
                    loadPercent = load,
                    fuelTrimB1 = fuelTrim,
                    throttlePercent = throttle,
                    intakeAirCelsius = intake,
                    mapKpa = map,
                    fuelLoopStatus = fuelLoop
                )
            } else {
                null
            }
        } catch (_: Exception) {
            _freezeFrameData.value = null
        }
    }

    fun requestSavePromptOnExit(): Boolean {
        if (neverAskToSave) return false
        val pending = pendingVehicleSave ?: return false
        if (_vehicleSaveOffer.value != null) return true
        _vehicleSaveOffer.value = pending
        return true
    }

    fun hideExitSavePrompt() {
        _vehicleSaveOffer.value = null
    }

    fun declineVehicleSave() {
        pendingVehicleSave = null
        _vehicleSaveOffer.value = null
    }

    fun neverAskToSaveVehicles() {
        neverAskToSave = true
        pendingVehicleSave = null
        _vehicleSaveOffer.value = null
        appContext?.let { VehicleProfileStore.setNeverAskToSave(it) }
    }

    fun saveOfferedVehicle() {
        val offer = _vehicleSaveOffer.value ?: pendingVehicleSave ?: return
        val vehicle = SavedVehicle(
            id = UUID.randomUUID().toString(),
            vin = offer.profile.vin,
            year = offer.profile.year,
            make = offer.profile.make,
            model = offer.profile.model,
            engine = offer.profile.engine,
            displayName = offer.profile.displayName,
            calibrationId = offer.calibrationId
        )
        upsertVehicle(vehicle, matchVin = true)
        pendingVehicleSave = null
        _vehicleSaveOffer.value = null
        _statusMessage.value = "Saved ${vehicle.displayName}."
    }

    fun selectVehicle(id: String) {
        val selected = _savedVehicles.value.firstOrNull { it.id == id } ?: return
        _activeVehicleId.value = id
        val connectedVin = _vehicleInfo.value.vin
        val liveConnection = _connectionState.value is ConnectionState.Connected && !_isDemoMode.value
        val sameVehicle = connectedVin.equals(selected.vin, ignoreCase = true) || connectedVin == "Not Read"
        if (!liveConnection || sameVehicle) {
            _vehicleInfo.value = selected.toVehicleInfo(cvn = _vehicleInfo.value.cvn)
        }
        persistVehicles()
        _statusMessage.value = "Vehicle profile: ${selected.displayName}."
    }

    fun deleteVehicle(id: String) {
        _savedVehicles.value = _savedVehicles.value.filterNot { it.id == id }
        if (_activeVehicleId.value == id) {
            _activeVehicleId.value = _savedVehicles.value.firstOrNull()?.id
        }
        if (_vehicleInfo.value.savedId == id) {
            restoreActiveSavedVehicle()
        }
        persistVehicles()
    }

    fun saveVehicleForm(
        id: String?,
        displayName: String,
        vin: String,
        make: String = "",
        model: String = "",
        engine: String = ""
    ) {
        val cleanVin = vin.trim().uppercase().filter { it.isLetterOrDigit() }
        val decoded = if (cleanVin.length >= 11) VinDecoder.decode(cleanVin) else null
        val name = displayName.trim().ifBlank {
            decoded?.displayName?.takeUnless { it == "Generic OBD2 Vehicle" } ?: "Vehicle"
        }
        val existing = id?.let { savedId -> _savedVehicles.value.firstOrNull { it.id == savedId } }
        val vehicle = SavedVehicle(
            id = existing?.id ?: UUID.randomUUID().toString(),
            vin = decoded?.vin?.takeIf { it.length == 17 } ?: cleanVin,
            year = knownText(decoded?.year) ?: existing?.year.orEmpty(),
            make = make.trim().ifBlank { knownText(decoded?.make).orEmpty() },
            model = model.trim().ifBlank { knownText(decoded?.model).orEmpty() },
            engine = engine.trim().ifBlank { knownText(decoded?.engine).orEmpty() },
            displayName = name,
            calibrationId = existing?.calibrationId.orEmpty()
        )
        upsertVehicle(vehicle, matchVin = existing == null)
        _statusMessage.value = "Saved ${vehicle.displayName}."
    }

    private suspend fun readVehicleIdentityInternal() {
        _isReadingVehicleInfo.value = true
        _statusMessage.value = "Reading vehicle identification…"
        try {
            val vinRaw = try {
                bluetoothManager.sendCommand("0902", timeoutMs = 5000)
            } catch (_: Exception) {
                ""
            }
            val vin = ObdProtocolHandler.decodeVin(vinRaw)
            val calRaw = try {
                bluetoothManager.sendCommand("0904", timeoutMs = 4000)
            } catch (_: Exception) {
                ""
            }
            val calibrationId = ObdProtocolHandler.decodeCalibrationId(calRaw)
            val cvnRaw = try {
                bluetoothManager.sendCommand("0906", timeoutMs = 3000)
            } catch (_: Exception) {
                ""
            }
            val cvn = ObdProtocolHandler.decodeCvn(cvnRaw)
            if (vin.length != 17 || vin == "Unavailable") {
                _vehicleInfo.value = VehicleInfo()
                _statusMessage.value = "The engine computer did not return a VIN. Select a saved vehicle in Settings if you want this car identified."
                return
            }
            val decoded = VinDecoder.decode(vin)
            val storedCal = calibrationId.takeUnless { it == "Unavailable" }.orEmpty()
            val storedCvn = cvn.takeUnless { it == "Unavailable" } ?: "Not Read"
            val alreadySaved = offerSaveIfNew(decoded, storedCal, storedCvn)
            val shownName = _vehicleInfo.value.profile?.displayName ?: decoded.displayName
            _statusMessage.value = if (alreadySaved) {
                "Saved vehicle: $shownName."
            } else {
                "Detected $shownName."
            }
        } finally {
            _isReadingVehicleInfo.value = false
        }
    }

    private fun knownText(value: String?): String? {
        val text = value?.trim().orEmpty()
        if (text.isBlank() || text.equals("Unknown", ignoreCase = true)) return null
        if (text.equals("Generic OBD2 Vehicle", ignoreCase = true)) return null
        return text
    }

    private fun offerSaveIfNew(profile: VehicleProfile, calibrationId: String, cvn: String = "Not Read"): Boolean {
        val existing = _savedVehicles.value.firstOrNull { it.vin.equals(profile.vin, ignoreCase = true) && it.vin.isNotBlank() }
        if (existing != null) {
            val refreshed = if (calibrationId.isNotBlank() && calibrationId != existing.calibrationId) {
                existing.copy(calibrationId = calibrationId)
            } else {
                existing
            }
            if (refreshed != existing) {
                _savedVehicles.value = _savedVehicles.value.map { if (it.id == existing.id) refreshed else it }
            }
            _activeVehicleId.value = refreshed.id
            _vehicleInfo.value = refreshed.toVehicleInfo(
                calibrationId = calibrationId.ifBlank { refreshed.calibrationId },
                cvn = cvn
            )
            pendingVehicleSave = null
            _vehicleSaveOffer.value = null
            persistVehicles()
            return true
        }
        _vehicleInfo.value = VehicleInfo(
            vin = profile.vin,
            calibrationId = calibrationId.ifBlank { "Not Read" },
            cvn = cvn,
            ecuName = "Engine control module",
            profile = profile
        )
        pendingVehicleSave = if (
            !neverAskToSave &&
            profile.vin.length == 17 &&
            profile.displayName != "Generic OBD2 Vehicle"
        ) {
            VehicleSaveOffer(profile, calibrationId)
        } else {
            null
        }
        _vehicleSaveOffer.value = null
        return false
    }

    private fun upsertVehicle(vehicle: SavedVehicle, matchVin: Boolean) {
        val vinMatch = if (matchVin && vehicle.vin.length == 17) {
            _savedVehicles.value.firstOrNull { it.vin.equals(vehicle.vin, ignoreCase = true) }
        } else {
            null
        }
        val stored = if (vinMatch != null) vehicle.copy(id = vinMatch.id) else vehicle
        _savedVehicles.value = if (_savedVehicles.value.any { it.id == stored.id }) {
            _savedVehicles.value.map { if (it.id == stored.id) stored else it }
        } else {
            _savedVehicles.value + stored
        }
        _activeVehicleId.value = stored.id
        _vehicleInfo.value = stored.toVehicleInfo(cvn = _vehicleInfo.value.cvn)
        if (pendingVehicleSave?.profile?.vin.equals(stored.vin, ignoreCase = true)) {
            pendingVehicleSave = null
            _vehicleSaveOffer.value = null
        }
        persistVehicles()
    }

    private fun restoreActiveSavedVehicle() {
        val active = _savedVehicles.value.firstOrNull { it.id == _activeVehicleId.value }
            ?: _savedVehicles.value.firstOrNull()
        _vehicleInfo.value = active?.toVehicleInfo() ?: VehicleInfo()
        if (active != null) _activeVehicleId.value = active.id
    }

    private fun persistVehicles() {
        val context = appContext ?: return
        VehicleProfileStore.save(context, _savedVehicles.value, _activeVehicleId.value)
    }

    private fun clearSensorCache() {
        synchronized(sensorLock) {
            supportedDataPids.clear()
            sensorByPid.clear()
            sensorCursor = 0
        }
        _sensorReadings.value = emptyList()
    }

    private fun discoverSupportedPids() {
        _statusMessage.value = "Reading which sensors this vehicle supports…"
        val found = linkedSetOf<Int>()
        var range = 0
        for (step in 0 until 8) {
            val command = "01%02X".format(range)
            val raw = try {
                bluetoothManager.sendCommand(command, timeoutMs = 1500)
            } catch (_: Exception) {
                ""
            }
            val payload = ObdProtocolHandler.extractPidPayload(raw, command)
            val mask = ObdProtocolHandler.decodeSupportedPids(payload, firstPid = range + 1)
            if (mask == null) break
            val nextRange = range + 0x20
            found.addAll(mask.filter { it != nextRange })
            if (nextRange !in mask) break
            range = nextRange
        }

        val dataPids = (if (found.isEmpty()) ObdPidCatalog.fallbackPids else found.toList()).sorted()
        synchronized(sensorLock) {
            supportedDataPids.clear()
            supportedDataPids.addAll(dataPids)
            sensorByPid.keys.retainAll(dataPids.toSet())
        }
        publishSensors()
        val adapterName = _adapterInfo.value.name.ifBlank { "adapter" }
        val protocol = _adapterInfo.value.protocol
        _statusMessage.value = "Connected to $adapterName. $protocol. ${dataPids.size} sensors available."
    }

    private fun updateSensorListFromMetrics(metrics: LiveMetrics) {
        synchronized(sensorLock) {
            fun allowed(pid: Int) = supportedDataPids.isEmpty() || pid in supportedDataPids
            fun putNumber(pid: Int, kind: SensorKind, value: Double?) {
                if (value == null || !allowed(pid)) return
                sensorByPid[pid] = SensorReading(pid, ObdPidCatalog.nameOf(pid), kind, number = value)
            }

            putNumber(0x04, SensorKind.PERCENT, metrics.loadPercent)
            putNumber(0x05, SensorKind.CELSIUS, metrics.coolantCelsius)
            putNumber(0x06, SensorKind.SIGNED_PERCENT, metrics.stftBank1)
            putNumber(0x07, SensorKind.SIGNED_PERCENT, metrics.ltftBank1)
            putNumber(0x08, SensorKind.SIGNED_PERCENT, metrics.stftBank2)
            putNumber(0x09, SensorKind.SIGNED_PERCENT, metrics.ltftBank2)
            putNumber(0x0B, SensorKind.KPA, metrics.mapKpa)
            putNumber(0x0C, SensorKind.RPM, metrics.rpm)
            putNumber(0x0D, SensorKind.SPEED_KMH, metrics.speedKmh)
            putNumber(0x0F, SensorKind.CELSIUS, metrics.intakeAirTempCelsius)
            putNumber(0x10, SensorKind.GRAMS_PER_SEC, metrics.mafGps)
            putNumber(0x11, SensorKind.PERCENT, metrics.throttlePercent)

            if (allowed(0x03) && metrics.fuelLoopStatus != null) {
                sensorByPid[0x03] = SensorReading(
                    pid = 0x03,
                    name = ObdPidCatalog.nameOf(0x03),
                    text = metrics.fuelLoopStatus.title
                )
            }
            if (allowed(0x01)) {
                val state = if (metrics.milOn) "MIL on" else "MIL off"
                val count = if (metrics.dtcCount == 1) "1 code" else "${metrics.dtcCount} codes"
                sensorByPid[0x01] = SensorReading(
                    pid = 0x01,
                    name = ObdPidCatalog.nameOf(0x01),
                    text = "$state · $count"
                )
            }

            fun putO2(pid: Int, value: Double?) {
                if (value == null || !allowed(pid)) return
                if (sensorByPid[pid]?.text != null) return
                sensorByPid[pid] = SensorReading(
                    pid = pid,
                    name = ObdPidCatalog.nameOf(pid),
                    kind = SensorKind.O2_VOLTS,
                    number = value
                )
            }
            putO2(0x14, metrics.o2b1s1)
            putO2(0x15, metrics.o2b1s2)
            putO2(0x18, metrics.o2b2s1)
            putO2(0x19, metrics.o2b2s2)
        }
        publishSensors()
    }

    private fun readAdditionalSensors() {
        val targets = synchronized(sensorLock) {
            val extras = supportedDataPids.filter { it !in metricsPids }
            if (extras.isEmpty()) {
                emptyList()
            } else if (sensorsPageVisible) {
                extras
            } else {
                List(4) {
                    val pid = extras[sensorCursor % extras.size]
                    sensorCursor++
                    pid
                }
            }
        }

        var changed = false
        for (pid in targets) {
            if (_isReadingCodes.value || _isReadingVehicleInfo.value) break
            val reading = readSensorPid(pid) ?: continue
            synchronized(sensorLock) {
                sensorByPid[pid] = reading
            }
            changed = true
        }
        if (changed) publishSensors()
    }

    private fun readSensorPid(pid: Int): SensorReading? {
        val command = "01%02X".format(pid)
        val raw = try {
            bluetoothManager.sendCommand(command, timeoutMs = 900)
        } catch (_: Exception) {
            return null
        }
        val payload = ObdProtocolHandler.extractPidPayload(raw, command) ?: return null
        return ObdPidCatalog.decode(pid, payload)
    }

    private fun publishSensors() {
        val list = synchronized(sensorLock) {
            if (supportedDataPids.isEmpty()) {
                sensorByPid.values.sortedBy { it.pid }
            } else {
                supportedDataPids.map { pid -> sensorByPid[pid] ?: ObdPidCatalog.placeholder(pid) }
            }
        }
        _sensorReadings.value = list
    }

    private fun startDemoLiveRead() {
        pollJob?.cancel()
        val now = System.currentTimeMillis()
        val seeded = (0 until 34).map { tick -> demoFrame(tick, now - (33 - tick) * 300L) }
        _o2History.value = seeded.map { it.first }
        publishDemoFrame(seeded.last().second)
        var tick = 34
        pollJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive && _isDemoMode.value) {
                delay(300)
                val (sample, metrics) = demoFrame(tick, System.currentTimeMillis())
                tick++
                val history = _o2History.value.toMutableList()
                history.add(sample)
                if (history.size > 35) history.removeAt(0)
                _o2History.value = history
                publishDemoFrame(metrics)
                if (_isLoggingCsv.value) {
                    csvLogBuffer.add(
                        "${sample.timestamp},${metrics.rpm ?: ""},${metrics.speedKmh ?: ""},${metrics.coolantCelsius ?: ""},${metrics.loadPercent ?: ""},${metrics.throttlePercent ?: ""},${metrics.batteryVoltage ?: ""},${metrics.stftBank1 ?: ""},${metrics.ltftBank1 ?: ""},${metrics.stftBank2 ?: ""},${metrics.ltftBank2 ?: ""},${sample.b1s1 ?: ""},${sample.b1s2 ?: ""},${sample.b2s1 ?: ""},${sample.b2s2 ?: ""},${metrics.intakeAirTempCelsius ?: ""},${metrics.mapKpa ?: ""},${metrics.mafGps ?: ""},${metrics.fuelLoopStatus?.codeStr ?: ""}"
                    )
                    _csvSampleCount.value = csvLogBuffer.size - 1
                }
            }
        }
    }

    private fun publishDemoFrame(metrics: LiveMetrics) {
        _liveMetrics.value = metrics
        _adapterInfo.value = _adapterInfo.value.copy(
            voltage = "%.1f V".format(metrics.batteryVoltage ?: 13.8)
        )
        fun o2Text(volts: Double?, trim: Double?): String? {
            if (volts == null || trim == null) return null
            return "%.3f V · trim %+.1f%%".format(volts, trim)
        }
        _sensorReadings.value = _sensorReadings.value.map { reading ->
            when (reading.pid) {
                0x04 -> reading.copy(number = metrics.loadPercent)
                0x05 -> reading.copy(number = metrics.coolantCelsius)
                0x06 -> reading.copy(number = metrics.stftBank1)
                0x07 -> reading.copy(number = metrics.ltftBank1)
                0x08 -> reading.copy(number = metrics.stftBank2)
                0x09 -> reading.copy(number = metrics.ltftBank2)
                0x0B -> reading.copy(number = metrics.mapKpa)
                0x0C -> reading.copy(number = metrics.rpm)
                0x0D -> reading.copy(number = metrics.speedKmh)
                0x0F -> reading.copy(number = metrics.intakeAirTempCelsius)
                0x10 -> reading.copy(number = metrics.mafGps)
                0x11 -> reading.copy(number = metrics.throttlePercent)
                0x01 -> {
                    val label = if (demoStoredCount == 1) "1 code" else "$demoStoredCount codes"
                    reading.copy(text = if (demoMilOn) "MIL on · $label" else "MIL off · 0 codes")
                }
                0x02 -> reading.copy(text = demoPrimaryCode)
                0x14 -> reading.copy(text = o2Text(metrics.o2b1s1, metrics.stftBank1))
                0x15 -> reading.copy(text = o2Text(metrics.o2b1s2, metrics.stftBank1))
                0x18 -> reading.copy(text = o2Text(metrics.o2b2s1, metrics.stftBank2))
                0x19 -> reading.copy(text = o2Text(metrics.o2b2s2, metrics.stftBank2))
                0x42 -> reading.copy(number = metrics.batteryVoltage)
                else -> reading
            }
        }
    }

    private fun demoFrame(tick: Int, timestamp: Long): Pair<O2Sample, LiveMetrics> {
        val time = tick * 1.15
        val b1s1 = demoNarrowband(time, phase = 0.0)
        val b2s1 = demoNarrowband(time, phase = 2.1, shift = -0.03)
        val b1s2 = (0.67 + sin(time * 0.08) * 0.02).coerceIn(0.0, 1.0)
        val b2s2 = (0.63 + sin(time * 0.07 + 0.6) * 0.018).coerceIn(0.0, 1.0)
        val stft1 = 2.2 + (0.45 - b1s1) * 14.0
        val stft2 = 1.6 + (0.45 - b2s1) * 14.0
        val load = 38.5 + sin(time * 0.22) * 2.4
        val metrics = LiveMetrics(
            rpm = 2450.0 + sin(time * 0.31) * 45.0,
            speedKmh = 72.4 + sin(time * 0.12) * 1.8,
            coolantCelsius = 90.0 + sin(time * 0.02) * 0.6,
            loadPercent = load,
            throttlePercent = (22.0 + sin(time * 0.18) * 1.4).coerceAtLeast(0.0),
            batteryVoltage = 13.8 + sin(time * 0.15) * 0.08,
            milOn = demoMilOn,
            dtcCount = demoStoredCount,
            stftBank1 = stft1,
            ltftBank1 = 1.5 + sin(time * 0.04) * 0.3,
            stftBank2 = stft2,
            ltftBank2 = 0.9 + sin(time * 0.035 + 0.4) * 0.25,
            o2b1s1 = b1s1,
            o2b1s2 = b1s2,
            o2b2s1 = b2s1,
            o2b2s2 = b2s2,
            intakeAirTempCelsius = 28.0 + sin(time * 0.03) * 0.4,
            mapKpa = 42.0 + (load - 38.5) * 0.35,
            mafGps = 18.5 + (load - 38.5) * 0.25,
            fuelLoopStatus = FuelLoopStatus.CLOSED_LOOP_NORMAL
        )
        return O2Sample(timestamp, b1s1, b1s2, b2s1, b2s2) to metrics
    }

    private fun demoNarrowband(time: Double, phase: Double, shift: Double = 0.0): Double {
        val shaped = tanh(sin(time + phase) * 2.6)
        return (0.47 + shift + shaped * 0.38).coerceIn(0.05, 0.95)
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive && bluetoothManager.isConnected && !_isDemoMode.value) {
                // If reading trouble codes, temporarily suspend PID polling
                if (_isReadingCodes.value || _isReadingVehicleInfo.value) {
                    delay(500)
                    continue
                }

                try {
                    val rpmRaw = try { bluetoothManager.sendCommand("010C") } catch (_: Exception) { null }
                    val rpm = rpmRaw?.let { ObdProtocolHandler.decodeRpm(ObdProtocolHandler.extractPidPayload(it, "010C")) }

                    val speedRaw = try { bluetoothManager.sendCommand("010D") } catch (_: Exception) { null }
                    val speed = speedRaw?.let { ObdProtocolHandler.decodeSpeed(ObdProtocolHandler.extractPidPayload(it, "010D")) }

                    val coolantRaw = try { bluetoothManager.sendCommand("0105") } catch (_: Exception) { null }
                    val coolant = coolantRaw?.let { ObdProtocolHandler.decodeCoolant(ObdProtocolHandler.extractPidPayload(it, "0105")) }

                    val loadRaw = try { bluetoothManager.sendCommand("0104") } catch (_: Exception) { null }
                    val load = loadRaw?.let { ObdProtocolHandler.decodeLoad(ObdProtocolHandler.extractPidPayload(it, "0104")) }

                    val throttleRaw = try { bluetoothManager.sendCommand("0111") } catch (_: Exception) { null }
                    val throttle = throttleRaw?.let { ObdProtocolHandler.decodeThrottle(ObdProtocolHandler.extractPidPayload(it, "0111")) }

                    // Fuel System Status (Loop Status)
                    val fuelLoopRaw = try { bluetoothManager.sendCommand("0103") } catch (_: Exception) { null }
                    val fuelLoop = fuelLoopRaw?.let { ObdProtocolHandler.decodeFuelLoopStatus(ObdProtocolHandler.extractPidPayload(it, "0103")) }

                    // IAT, MAP, MAF
                    val iatRaw = try { bluetoothManager.sendCommand("010F") } catch (_: Exception) { null }
                    val iat = iatRaw?.let { ObdProtocolHandler.decodeIat(ObdProtocolHandler.extractPidPayload(it, "010F")) }

                    val mapRaw = try { bluetoothManager.sendCommand("010B") } catch (_: Exception) { null }
                    val mapVal = mapRaw?.let { ObdProtocolHandler.decodeMap(ObdProtocolHandler.extractPidPayload(it, "010B")) }

                    val mafRaw = try { bluetoothManager.sendCommand("0110") } catch (_: Exception) { null }
                    val mafVal = mafRaw?.let { ObdProtocolHandler.decodeMaf(ObdProtocolHandler.extractPidPayload(it, "0110")) }

                    // Fuel Trims
                    val stft1Raw = try { bluetoothManager.sendCommand("0106") } catch (_: Exception) { null }
                    val stft1 = stft1Raw?.let { ObdProtocolHandler.decodeFuelTrim(ObdProtocolHandler.extractPidPayload(it, "0106")) }

                    val ltft1Raw = try { bluetoothManager.sendCommand("0107") } catch (_: Exception) { null }
                    val ltft1 = ltft1Raw?.let { ObdProtocolHandler.decodeFuelTrim(ObdProtocolHandler.extractPidPayload(it, "0107")) }

                    val stft2Raw = try { bluetoothManager.sendCommand("0108") } catch (_: Exception) { null }
                    val stft2 = stft2Raw?.let { ObdProtocolHandler.decodeFuelTrim(ObdProtocolHandler.extractPidPayload(it, "0108")) }

                    val ltft2Raw = try { bluetoothManager.sendCommand("0109") } catch (_: Exception) { null }
                    val ltft2 = ltft2Raw?.let { ObdProtocolHandler.decodeFuelTrim(ObdProtocolHandler.extractPidPayload(it, "0109")) }

                    // O2 Sensors
                    val o2b1s1Raw = try { bluetoothManager.sendCommand("0114") } catch (_: Exception) { null }
                    val o2b1s1 = o2b1s1Raw?.let { ObdProtocolHandler.decodeO2SensorVoltage(ObdProtocolHandler.extractPidPayload(it, "0114")) }

                    val o2b1s2Raw = try { bluetoothManager.sendCommand("0115") } catch (_: Exception) { null }
                    val o2b1s2 = o2b1s2Raw?.let { ObdProtocolHandler.decodeO2SensorVoltage(ObdProtocolHandler.extractPidPayload(it, "0115")) }

                    val o2b2s1Raw = try { bluetoothManager.sendCommand("0118") } catch (_: Exception) { null }
                    val o2b2s1 = o2b2s1Raw?.let { ObdProtocolHandler.decodeO2SensorVoltage(ObdProtocolHandler.extractPidPayload(it, "0118")) }

                    val o2b2s2Raw = try { bluetoothManager.sendCommand("0119") } catch (_: Exception) { null }
                    val o2b2s2 = o2b2s2Raw?.let { ObdProtocolHandler.decodeO2SensorVoltage(ObdProtocolHandler.extractPidPayload(it, "0119")) }

                    val milRaw = try { bluetoothManager.sendCommand("0101") } catch (_: Exception) { null }
                    val milData = milRaw?.let { ObdProtocolHandler.decodeMilStatus(ObdProtocolHandler.extractPidPayload(it, "0101")) }

                    val voltageRaw = try { bluetoothManager.sendCommand("ATRV") } catch (_: Exception) { null }
                    val voltage = voltageRaw?.let { ObdProtocolHandler.decodeVoltage(it) }

                    val now = System.currentTimeMillis()
                    val newSample = O2Sample(
                        timestamp = now,
                        b1s1 = o2b1s1 ?: _liveMetrics.value.o2b1s1,
                        b1s2 = o2b1s2 ?: _liveMetrics.value.o2b1s2,
                        b2s1 = o2b2s1 ?: _liveMetrics.value.o2b2s1,
                        b2s2 = o2b2s2 ?: _liveMetrics.value.o2b2s2
                    )

                    val currentHistory = _o2History.value.toMutableList()
                    currentHistory.add(newSample)
                    if (currentHistory.size > 35) {
                        currentHistory.removeAt(0)
                    }
                    _o2History.value = currentHistory

                    val updatedMetrics = LiveMetrics(
                        rpm = rpm ?: _liveMetrics.value.rpm,
                        speedKmh = speed ?: _liveMetrics.value.speedKmh,
                        coolantCelsius = coolant ?: _liveMetrics.value.coolantCelsius,
                        loadPercent = load ?: _liveMetrics.value.loadPercent,
                        throttlePercent = throttle ?: _liveMetrics.value.throttlePercent,
                        batteryVoltage = voltage ?: _liveMetrics.value.batteryVoltage,
                        milOn = milData?.first ?: _liveMetrics.value.milOn,
                        dtcCount = milData?.second ?: _liveMetrics.value.dtcCount,
                        stftBank1 = stft1 ?: _liveMetrics.value.stftBank1,
                        ltftBank1 = ltft1 ?: _liveMetrics.value.ltftBank1,
                        stftBank2 = stft2 ?: _liveMetrics.value.stftBank2,
                        ltftBank2 = ltft2 ?: _liveMetrics.value.ltftBank2,
                        o2b1s1 = newSample.b1s1,
                        o2b1s2 = newSample.b1s2,
                        o2b2s1 = newSample.b2s1,
                        o2b2s2 = newSample.b2s2,
                        intakeAirTempCelsius = iat ?: _liveMetrics.value.intakeAirTempCelsius,
                        mapKpa = mapVal ?: _liveMetrics.value.mapKpa,
                        mafGps = mafVal ?: _liveMetrics.value.mafGps,
                        fuelLoopStatus = fuelLoop ?: _liveMetrics.value.fuelLoopStatus
                    )

                    _liveMetrics.value = updatedMetrics
                    updateSensorListFromMetrics(updatedMetrics)
                    readAdditionalSensors()

                    // Record CSV line if enabled
                    if (_isLoggingCsv.value) {
                        val csvLine = "${now},${rpm ?: ""},${speed ?: ""},${coolant ?: ""},${load ?: ""},${throttle ?: ""},${voltage ?: ""},${stft1 ?: ""},${ltft1 ?: ""},${stft2 ?: ""},${ltft2 ?: ""},${newSample.b1s1 ?: ""},${newSample.b1s2 ?: ""},${newSample.b2s1 ?: ""},${newSample.b2s2 ?: ""},${iat ?: ""},${mapVal ?: ""},${mafVal ?: ""},${fuelLoop?.codeStr ?: ""}"
                        csvLogBuffer.add(csvLine)
                        _csvSampleCount.value = csvLogBuffer.size - 1
                    }

                    // Also update adapter battery voltage string
                    if (voltage != null) {
                        _adapterInfo.value = _adapterInfo.value.copy(
                            voltage = "%.1f V".format(voltage)
                        )
                    }

                } catch (e: Exception) {
                    _statusMessage.value = "Read error: ${e.message}"
                }

                delay(300)
            }
        }
    }

    private data class DemoCode(val code: String, val category: DtcCategory)

    private data class DemoScenario(
        val profile: VehicleProfile,
        val calibrationId: String,
        val cvn: String,
        val codes: List<DemoCode>,
        val freezeFrame: FreezeFrameData
    ) {
        val signature: String
            get() = profile.vin + "|" + codes.joinToString(",") { "${it.code}:${it.category}" }
        val storedCount: Int
            get() = codes.count { it.category == DtcCategory.STORED }
        val primaryCode: String
            get() = codes.first { it.category == DtcCategory.STORED }.code
    }

    override fun onCleared() {
        pollJob?.cancel()
        bluetoothManager.disconnect()
        super.onCleared()
    }
}

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
import com.example.obdscanner.model.UnitSystem
import com.example.obdscanner.model.VehicleInfo
import com.example.obdscanner.obd.AppUpdateInfo
import com.example.obdscanner.obd.AutoUpdateManager
import com.example.obdscanner.obd.DtcDatabase
import com.example.obdscanner.obd.ObdBluetoothManager
import com.example.obdscanner.obd.ObdProtocolHandler
import com.example.obdscanner.obd.PdfReportGenerator
import com.example.obdscanner.obd.VehicleProfile
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

    private val _vehicleInfo = MutableStateFlow(VehicleInfo())
    val vehicleInfo: StateFlow<VehicleInfo> = _vehicleInfo.asStateFlow()

    private val _adapterInfo = MutableStateFlow(ObdAdapterInfo())
    val adapterInfo: StateFlow<ObdAdapterInfo> = _adapterInfo.asStateFlow()

    private val _unitSystem = MutableStateFlow(UnitSystem.IMPERIAL)
    val unitSystem: StateFlow<UnitSystem> = _unitSystem.asStateFlow()

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

    // Auto-Update Manifest State
    private val _appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    // CSV Telemetry Recorder
    private val _isLoggingCsv = MutableStateFlow(false)
    val isLoggingCsv: StateFlow<Boolean> = _isLoggingCsv.asStateFlow()

    private val csvLogBuffer = mutableListOf<String>()
    private val _csvSampleCount = MutableStateFlow(0)
    val csvSampleCount: StateFlow<Int> = _csvSampleCount.asStateFlow()

    fun connect(device: BluetoothDevice) {
        if (_isDemoMode.value) {
            _isDemoMode.value = false
        }
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
                _statusMessage.value = "Connected to ${adapterInfoResult.name}. Protocol: ${adapterInfoResult.protocol}."

                // Read initial trouble codes on connect
                readTroubleCodesInternal()

                // Check for app update in background
                checkForAppUpdates()

                // Start live data polling loop
                startPolling()

            } catch (e: Exception) {
                bluetoothManager.disconnect()
                _connectionState.value = ConnectionState.Error(e.message ?: "Failed to connect")
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
            enableDemoModeInternal()
        }
    }

    private fun enableDemoModeInternal() {
        pollJob?.cancel()
        pollJob = null

        val jeepProfile = VehicleProfile(
            vin = "1C4HJWDG2FL123456",
            make = "Jeep",
            model = "Wrangler Rubicon / JKU",
            year = "2015",
            engine = "3.6L V6 Pentastar Engine",
            displayName = "2015 Jeep Wrangler Rubicon / JKU (3.6L V6 Pentastar Engine)"
        )

        _vehicleInfo.value = VehicleInfo(
            vin = "1C4HJWDG2FL123456",
            calibrationId = "CALID_JEEP_36L_2015",
            cvn = "CVN98765",
            ecuName = "Jeep PCM / ECM",
            profile = jeepProfile
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
        _statusMessage.value = "Demo Mode Active: Simulating 2015 Jeep Wrangler Rubicon JKU code scan."

        _liveMetrics.value = LiveMetrics(
            rpm = 2450.0,
            speedKmh = 72.4, // ~45 mph
            coolantCelsius = 90.0, // ~194 °F
            loadPercent = 38.5,
            throttlePercent = 22.0,
            batteryVoltage = 13.8,
            milOn = true,
            dtcCount = 1,
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

        // Simulated DTC P0303 - Cylinder 3 Misfire Detected
        val p0303Dtc = DtcDatabase.lookup("P0303", DtcCategory.STORED, jeepProfile)
        _troubleCodes.value = listOf(p0303Dtc)

        _freezeFrameData.value = FreezeFrameData(
            dtcCode = "P0303",
            rpm = 2450.0,
            speedKmh = 72.4,
            coolantCelsius = 90.0,
            loadPercent = 38.5,
            fuelTrimB1 = 3.2
        )
    }

    fun checkForAppUpdates(currentVersionCode: Int = 2) {
        viewModelScope.launch(Dispatchers.IO) {
            val updateInfo = AutoUpdateManager.checkForUpdates(currentVersionCode)
            _appUpdateInfo.value = updateInfo
        }
    }

    fun downloadAndInstallUpdate(context: Context, downloadUrl: String) {
        AutoUpdateManager.downloadAndInstallUpdate(context, downloadUrl)
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
        _statusMessage.value = "Disconnected."
    }

    fun toggleUnitSystem() {
        _unitSystem.value = if (_unitSystem.value == UnitSystem.IMPERIAL) UnitSystem.METRIC else UnitSystem.IMPERIAL
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
            freezeFrame = _freezeFrameData.value
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
                // Read Mode 02 Freeze Frame
                readFreezeFrameInternal(distinctCodes.first().code)
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
            val ffDtcRaw = try { bluetoothManager.sendCommand("020200", timeoutMs = 2500) } catch (_: Exception) { "" }
            val ffRpmRaw = try { bluetoothManager.sendCommand("020C00", timeoutMs = 2500) } catch (_: Exception) { "" }
            val ffSpeedRaw = try { bluetoothManager.sendCommand("020D00", timeoutMs = 2500) } catch (_: Exception) { "" }
            val ffCoolantRaw = try { bluetoothManager.sendCommand("020500", timeoutMs = 2500) } catch (_: Exception) { "" }
            val ffLoadRaw = try { bluetoothManager.sendCommand("020400", timeoutMs = 2500) } catch (_: Exception) { "" }

            val rpm = ffRpmRaw.let { ObdProtocolHandler.decodeRpm(ObdProtocolHandler.extractPidPayload(it, "020C")) }
            val speed = ffSpeedRaw.let { ObdProtocolHandler.decodeSpeed(ObdProtocolHandler.extractPidPayload(it, "020D")) }
            val coolant = ffCoolantRaw.let { ObdProtocolHandler.decodeCoolant(ObdProtocolHandler.extractPidPayload(it, "0205")) }
            val load = ffLoadRaw.let { ObdProtocolHandler.decodeLoad(ObdProtocolHandler.extractPidPayload(it, "0204")) }

            _freezeFrameData.value = FreezeFrameData(
                dtcCode = primaryDtc,
                rpm = rpm,
                speedKmh = speed,
                coolantCelsius = coolant,
                loadPercent = load
            )
        } catch (_: Exception) {}
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

    override fun onCleared() {
        pollJob?.cancel()
        bluetoothManager.disconnect()
        super.onCleared()
    }
}

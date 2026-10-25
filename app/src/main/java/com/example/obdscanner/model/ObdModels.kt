package com.example.obdscanner.model

import com.example.obdscanner.obd.VehicleProfile

enum class CodeSeverity {
    INFO,
    WARNING,
    CRITICAL
}

enum class DrivingCriticality(val label: String, val plainAdvice: String) {
    CRITICAL_STOP_IMMEDIATELY(
        "STOP IMMEDIATELY / DO NOT DRIVE",
        "Risk of severe engine, catalyst, or mechanical damage. Pull over safely."
    ),
    CAUTION_DRIVE_HOME(
        "CAUTION - SAFE TO DRIVE SHORT DISTANCE / HOME",
        "Safe to drive home or to a nearby repair shop at moderate speed."
    ),
    SERVICE_SOON(
        "SAFE TO DRIVE - SCHEDULE SERVICE SOON",
        "Emissions or minor sensor fault detected. Safe for normal driving; schedule service when convenient."
    )
}

enum class FuelLoopStatus(
    val codeStr: String,
    val title: String,
    val description: String
) {
    OPEN_LOOP_WARMUP(
        "01 / OL",
        "Open Loop - Warming Up",
        "Engine is cold; waiting for O₂ sensors and coolant to reach operating temperature."
    ),
    CLOSED_LOOP_NORMAL(
        "02 / CL",
        "Closed Loop - Normal Operating Status",
        "Using O₂ sensors to optimize fuel mixture and maintain ideal 14.7:1 air/fuel ratio."
    ),
    OPEN_LOOP_HIGH_LOAD(
        "04 / OL-Drive",
        "Open Loop - High Load Acceleration",
        "Heavy acceleration or deceleration; running rich fuel enrichment for maximum power/cooling."
    ),
    OPEN_LOOP_FAULT(
        "08 / OL-Fault",
        "Open Loop - System Fault!",
        "Running on backup ECU tables due to an active sensor failure or severe misfire."
    ),
    CLOSED_LOOP_FAULT(
        "16 / CL-Fault",
        "Closed Loop - Fault!",
        "Using O₂ sensors for feedback, but at least one primary sensor is failing or degraded."
    )
}

enum class DtcCategory(val label: String) {
    STORED("Stored"),
    PENDING("Pending"),
    PERMANENT("Permanent")
}

enum class AppTheme(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Theme"),
    DARK("Dark Theme")
}

enum class O2DisplayMode(val label: String) {
    GRAPH("Graph"),
    NUMBERS("Numbers")
}

data class DtcCode(
    val code: String,
    val category: DtcCategory,
    val title: String,
    val description: String,
    val plainLanguageExplanation: String = "",
    val criticality: DrivingCriticality = DrivingCriticality.CAUTION_DRIVE_HOME,
    val verifiedFixes: List<String> = emptyList(),
    val possibleCauses: List<String> = emptyList(),
    val severity: CodeSeverity = CodeSeverity.WARNING
)

data class FreezeFrameData(
    val dtcCode: String = "None",
    val rpm: Double? = null,
    val speedKmh: Double? = null,
    val coolantCelsius: Double? = null,
    val loadPercent: Double? = null,
    val fuelTrimB1: Double? = null,
    val throttlePercent: Double? = null,
    val intakeAirCelsius: Double? = null,
    val mapKpa: Double? = null,
    val fuelLoopStatus: FuelLoopStatus? = null
)

data class PerformanceTestState(
    val currentSpeedMph: Double = 0.0,
    val zeroToSixtySec: Double? = null,
    val quarterMileSec: Double? = null,
    val quarterMileSpeedMph: Double? = null,
    val peakGForce: Double = 0.0,
    val isTesting: Boolean = false,
    val statusText: String = "Stopped. Ready for 0-60 mph launch."
)

data class VehicleInfo(
    val vin: String = "Not Read",
    val calibrationId: String = "Not Read",
    val cvn: String = "Not Read",
    val ecuName: String = "Not Read",
    val profile: VehicleProfile? = null,
    val plateNumber: String = "",
    val phoneNumber: String = "",
    val savedId: String? = null
)

data class SavedVehicle(
    val id: String,
    val vin: String = "",
    val year: String = "",
    val make: String = "",
    val model: String = "",
    val engine: String = "",
    val displayName: String,
    val plateNumber: String = "",
    val phoneNumber: String = "",
    val calibrationId: String = ""
)

data class VehicleSaveOffer(
    val profile: VehicleProfile,
    val calibrationId: String = ""
)

data class O2Sample(
    val timestamp: Long,
    val b1s1: Double?,
    val b1s2: Double?,
    val b2s1: Double?,
    val b2s2: Double?
)

data class LiveMetrics(
    val rpm: Double? = null,
    val speedKmh: Double? = null,
    val coolantCelsius: Double? = null,
    val loadPercent: Double? = null,
    val throttlePercent: Double? = null,
    val batteryVoltage: Double? = null,
    val milOn: Boolean = false,
    val dtcCount: Int = 0,
    val stftBank1: Double? = null,
    val ltftBank1: Double? = null,
    val stftBank2: Double? = null,
    val ltftBank2: Double? = null,
    val o2b1s1: Double? = null,
    val o2b1s2: Double? = null,
    val o2b2s1: Double? = null,
    val o2b2s2: Double? = null,
    val intakeAirTempCelsius: Double? = null,
    val mapKpa: Double? = null,
    val mafGps: Double? = null,
    val fuelLoopStatus: FuelLoopStatus? = null
)

data class ObdAdapterInfo(
    val name: String = "",
    val address: String = "",
    val deviceId: String = "Unknown",
    val elmVersion: String = "Unknown",
    val voltage: String = "—",
    val protocol: String = "Auto (SP0)"
)

sealed interface ConnectionState {
    object Disconnected : ConnectionState
    data class Connecting(val deviceName: String) : ConnectionState
    data class Initializing(val step: String) : ConnectionState
    data class Connected(val adapterName: String) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

enum class UnitSystem {
    METRIC,
    IMPERIAL
}

enum class SensorKind {
    RPM,
    SPEED_KMH,
    CELSIUS,
    PERCENT,
    SIGNED_PERCENT,
    KPA,
    VOLTS,
    O2_VOLTS,
    GRAMS_PER_SEC,
    DEGREES,
    SECONDS,
    MINUTES,
    KILOMETERS,
    COUNT,
    RATIO,
    LITERS_PER_HOUR,
    NEWTON_METERS,
    PASCAL
}

data class SensorReading(
    val pid: Int,
    val name: String,
    val kind: SensorKind? = null,
    val number: Double? = null,
    val text: String? = null
)

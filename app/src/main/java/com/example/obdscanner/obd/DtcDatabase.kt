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

import com.example.obdscanner.model.CodeSeverity
import com.example.obdscanner.model.DrivingCriticality
import com.example.obdscanner.model.DtcCategory
import com.example.obdscanner.model.DtcCode

data class DtcDetails(
    val title: String,
    val description: String,
    val plainLanguageExplanation: String,
    val criticality: DrivingCriticality,
    val verifiedFixes: List<String>,
    val possibleCauses: List<String>,
    val severity: CodeSeverity
)

object DtcDatabase {

    private val database = mapOf(
        "P0100" to DtcDetails(
            "Mass or Volume Air Flow Circuit Malfunction",
            "The Engine Control Module (ECM) detects an incorrect signal from the MAF sensor.",
            "The engine computer cannot accurately measure incoming air to calculate fuel mixture.",
            DrivingCriticality.CAUTION_DRIVE_HOME,
            listOf("Clean or replace MAF sensor", "Replace air filter", "Fix intake air leak"),
            listOf("Faulty MAF sensor", "Dirty air filter", "Air intake leak", "Damaged MAF sensor wiring"),
            CodeSeverity.WARNING
        ),
        "P0101" to DtcDetails(
            "Mass Air Flow Circuit Range/Performance Problem",
            "The MAF sensor signal is out of expected operating bounds.",
            "MAF sensor is sending fluctuating or out-of-spec airflow values.",
            DrivingCriticality.CAUTION_DRIVE_HOME,
            listOf("Clean MAF sensor with aerosol cleaner", "Inspect PCV valve", "Check air filter box seal"),
            listOf("Dirty/contaminated MAF sensor", "Air intake leak", "Clogged air filter", "Faulty PCV valve"),
            CodeSeverity.WARNING
        ),
        "P0115" to DtcDetails(
            "Engine Coolant Temperature Circuit Malfunction",
            "Abnormal voltage detected from Engine Coolant Temperature (ECT) sensor.",
            "Engine computer does not know current coolant temperature. Engine cooling fans may run continuously.",
            DrivingCriticality.CRITICAL_STOP_IMMEDIATELY,
            listOf("Replace Engine Coolant Temperature sensor", "Inspect coolant level", "Fix corroded sensor connector"),
            listOf("Faulty ECT sensor", "Corroded sensor wiring", "Engine overheating"),
            CodeSeverity.CRITICAL
        ),
        "P0128" to DtcDetails(
            "Coolant Thermostat (Temp Below Thermostat Regulating Temp)",
            "Engine coolant temperature fails to reach operating temperature in specified time.",
            "The thermostat is stuck open, preventing the engine from reaching full operating temperature.",
            DrivingCriticality.SERVICE_SOON,
            listOf("Replace coolant thermostat assembly & housing", "Top up engine coolant level", "Replace coolant temp sensor"),
            listOf("Stuck open thermostat", "Faulty coolant temperature sensor", "Low coolant level"),
            CodeSeverity.INFO
        ),
        "P0171" to DtcDetails(
            "System Too Lean (Bank 1)",
            "Air/Fuel mixture is too lean; too much air or not enough fuel.",
            "The engine is drawing unmetered air or not getting enough fuel. Driving extended distances may overheat exhaust valves.",
            DrivingCriticality.CAUTION_DRIVE_HOME,
            listOf("Locate & repair vacuum hose / intake gasket leak", "Clean MAF sensor", "Replace fuel filter / test fuel pump pressure"),
            listOf("Vacuum leak", "Dirty/faulty MAF sensor", "Low fuel pressure / clogged injector", "Exhaust leak"),
            CodeSeverity.CRITICAL
        ),
        "P0174" to DtcDetails(
            "System Too Lean (Bank 2)",
            "Bank 2 air/fuel mixture is running lean.",
            "Bank 2 cylinders are receiving too much air or insufficient fuel supply.",
            DrivingCriticality.CAUTION_DRIVE_HOME,
            listOf("Locate vacuum leak on Bank 2 intake runner", "Clean MAF sensor", "Test fuel pressure"),
            listOf("Vacuum intake leak on Bank 2", "Dirty MAF sensor", "Weak fuel pump"),
            CodeSeverity.CRITICAL
        ),
        "P0300" to DtcDetails(
            "Random/Multiple Cylinder Misfire Detected",
            "Multiple cylinders are misfiring randomly across the engine cycle.",
            "Engine cylinders are failing to ignite fuel properly. Flashing Check Engine light indicates active misfire that can ruin catalytic converters.",
            DrivingCriticality.CRITICAL_STOP_IMMEDIATELY,
            listOf("Replace all spark plugs & ignition coil packs", "Repair vacuum leak / intake gasket", "Replace fuel injectors"),
            listOf("Worn spark plugs / ignition coils", "Low fuel pressure", "Vacuum leaks", "Clogged fuel injectors"),
            CodeSeverity.CRITICAL
        ),
        "P0301" to DtcDetails("Cylinder 1 Misfire Detected", "Cylinder 1 is failing to ignite fuel correctly.", "Cylinder 1 is misfiring. Avoid heavy acceleration.", DrivingCriticality.CRITICAL_STOP_IMMEDIATELY, listOf("Replace Cylinder 1 spark plug & coil pack", "Swap coil pack with Cylinder 2 to test"), listOf("Faulty Cylinder 1 spark plug/coil", "Faulty Cylinder 1 fuel injector", "Low compression Cylinder 1"), CodeSeverity.CRITICAL),
        "P0302" to DtcDetails("Cylinder 2 Misfire Detected", "Cylinder 2 is failing to ignite fuel correctly.", "Cylinder 2 is misfiring.", DrivingCriticality.CRITICAL_STOP_IMMEDIATELY, listOf("Replace Cylinder 2 spark plug & coil pack"), listOf("Faulty Cylinder 2 spark plug/coil", "Injector"), CodeSeverity.CRITICAL),
        "P0303" to DtcDetails("Cylinder 3 Misfire Detected", "Cylinder 3 is failing to ignite fuel correctly.", "Cylinder 3 is misfiring.", DrivingCriticality.CRITICAL_STOP_IMMEDIATELY, listOf("Replace Cylinder 3 spark plug & coil pack"), listOf("Faulty Cylinder 3 spark plug/coil", "Injector"), CodeSeverity.CRITICAL),
        "P0304" to DtcDetails("Cylinder 4 Misfire Detected", "Cylinder 4 is failing to ignite fuel correctly.", "Cylinder 4 is misfiring.", DrivingCriticality.CRITICAL_STOP_IMMEDIATELY, listOf("Replace Cylinder 4 spark plug & coil pack"), listOf("Faulty Cylinder 4 spark plug/coil", "Injector"), CodeSeverity.CRITICAL),
        "P0305" to DtcDetails("Cylinder 5 Misfire Detected", "Cylinder 5 misfire failure.", "Cylinder 5 is misfiring.", DrivingCriticality.CRITICAL_STOP_IMMEDIATELY, listOf("Replace Cylinder 5 spark plug & coil pack"), listOf("Spark plug/coil", "Injector"), CodeSeverity.CRITICAL),
        "P0306" to DtcDetails("Cylinder 6 Misfire Detected", "Cylinder 6 misfire failure.", "Cylinder 6 is misfiring.", DrivingCriticality.CRITICAL_STOP_IMMEDIATELY, listOf("Replace Cylinder 6 spark plug & coil pack"), listOf("Spark plug/coil", "Injector"), CodeSeverity.CRITICAL),
        "P0420" to DtcDetails(
            "Catalyst System Efficiency Below Threshold (Bank 1)",
            "The catalytic converter on Bank 1 is not operating efficiently.",
            "Bank 1 catalytic converter is degraded or failing to reduce exhaust emissions.",
            DrivingCriticality.SERVICE_SOON,
            listOf("Replace Bank 1 Catalytic Converter", "Replace downstream O2 sensor", "Repair upstream exhaust leaks"),
            listOf("Degraded Catalytic Converter", "Faulty rear O2 sensor", "Exhaust system leaks"),
            CodeSeverity.WARNING
        ),
        "P0442" to DtcDetails(
            "Evaporative Emission System Leak Detected (Small Leak)",
            "Small fuel vapor leak detected in EVAP system.",
            "Fuel tank vapor system has a minor leak (often gas cap rubber seal).",
            DrivingCriticality.SERVICE_SOON,
            listOf("Replace fuel gas cap", "Replace EVAP purge solenoid", "Inspect EVAP vent valve and canister hoses"),
            listOf("Loose/defective fuel cap", "Small crack in EVAP hose", "Purge/vent valve leak"),
            CodeSeverity.INFO
        ),
        "P0456" to DtcDetails(
            "Evaporative Emission System Leak Detected (Very Small Leak)",
            "Very small EVAP vapor pressure loss detected during engine off test.",
            "Micro pressure leak in fuel vapor recovery system.",
            DrivingCriticality.SERVICE_SOON,
            listOf("Inspect / replace gas cap O-ring seal", "Inspect EVAP vent valve", "Replace EVAP purge valve"),
            listOf("Loose gas cap seal", "EVAP vent valve faulty", "Purge solenoid leak"),
            CodeSeverity.INFO
        ),
        "P0520" to DtcDetails(
            "Engine Oil Pressure Sensor/Switch Circuit Malfunction",
            "Oil pressure sensor output voltage is out of normal operating range.",
            "Engine computer detects abnormal oil pressure sensor reading. Check engine oil level on dipstick immediately!",
            DrivingCriticality.CRITICAL_STOP_IMMEDIATELY,
            listOf("Check and top off engine oil level immediately", "Inspect the oil pressure sensor and connector", "Check the sensor circuit for a short or open"),
            listOf("Faulty oil pressure sensor", "Oil pressure sensor harness short or open", "Oil pressure below specification"),
            CodeSeverity.CRITICAL
        )
    )

    fun lookup(code: String, category: DtcCategory, vehicleProfile: VehicleProfile? = null): DtcCode {
        val uppercaseCode = code.uppercase().trim()
        val details = database[uppercaseCode]

        val title = details?.title ?: "Diagnostic Fault Code ($uppercaseCode)"
        val description = details?.description ?: "Diagnostic trouble code $uppercaseCode returned by vehicle ECU."
        val plainExplanation = details?.plainLanguageExplanation ?: "Diagnostic fault code $uppercaseCode stored in vehicle ECU memory."
        val criticality = details?.criticality ?: DrivingCriticality.CAUTION_DRIVE_HOME
        val severity = details?.severity ?: CodeSeverity.WARNING

        val fixes = mutableListOf<String>()
        if (details != null && details.verifiedFixes.isNotEmpty()) {
            fixes.addAll(details.verifiedFixes)
        } else {
            fixes.add("Inspect wiring harness and connector for $uppercaseCode")
            fixes.add("Consult manufacturer service bulletin / repair manual for $uppercaseCode")
        }

        val causes = mutableListOf<String>()
        if (details != null) {
            causes.addAll(details.possibleCauses)
        }

        // Add Vehicle-Specific "What To Check" Diagnostic Summary
        if (vehicleProfile != null) {
            val specificSummary = getVehicleSpecificCauses(uppercaseCode, vehicleProfile)
            if (specificSummary.isNotEmpty()) {
                causes.addAll(0, specificSummary)
            }
        }

        return DtcCode(
            code = uppercaseCode,
            category = category,
            title = title,
            description = description,
            plainLanguageExplanation = plainExplanation,
            criticality = criticality,
            verifiedFixes = fixes.distinct(),
            possibleCauses = causes.distinct(),
            severity = severity
        )
    }

    private fun getVehicleSpecificCauses(code: String, vehicle: VehicleProfile): List<String> {
        val make = knownMake(vehicle) ?: return emptyList()
        val causes = mutableListOf<String>()
        val label = vehicle.displayName

        when (make) {
            "JEEP" -> {
                if (isPentastarFamily(vehicle)) {
                    when (code) {
                        "P0520" -> causes.add("[WHAT TO CHECK - $label]: Oil pressure sensor failure is common on 3.6L Pentastar engines. The sensor is in the lower engine valley under the oil filter adapter. Check the sensor plug for oil contamination.")
                        "P0128" -> causes.add("[WHAT TO CHECK - $label]: The thermostat often sticks open on the Pentastar V6. Check the coolant level and the thermostat housing.")
                        "P0300", "P0301", "P0302", "P0303", "P0304", "P0305", "P0306" -> causes.add("[WHAT TO CHECK - $label]: 1) Inspect the ignition coil and spark plug gap. 2) Check for lower intake manifold vacuum leaks. 3) Listen for rocker arm or lifter tick.")
                        "P0171", "P0174" -> causes.add("[WHAT TO CHECK - $label]: Check the lower intake manifold runner gaskets, PCV valve hose, and brake booster check valve for vacuum leaks.")
                    }
                }
                if (code == "P0456" || code == "P0442" || code == "P0455") {
                    causes.add("[WHAT TO CHECK - $label]: 1) Inspect the gas cap seal. 2) Check the ESIM switch on the charcoal canister. 3) Test the EVAP purge valve.")
                }
            }
            "CHRYSLER", "DODGE", "RAM" -> {
                if (code == "P0456" || code == "P0442" || code == "P0455") {
                    causes.add("[WHAT TO CHECK - $label]: 1) Inspect the gas cap seal. 2) Check the ESIM switch on the charcoal canister. 3) Test the EVAP purge valve.")
                }
            }
            "FORD" -> {
                when (code) {
                    "P0300", "P0301", "P0302", "P0303", "P0304" -> {
                        val extra = if (vehicle.engine.uppercase().contains("ECOBOOST")) {
                            " 3) On an EcoBoost engine, check the intercooler for condensation."
                        } else {
                            ""
                        }
                        causes.add("[WHAT TO CHECK - $label]: 1) Inspect the spark plug boots for carbon tracking. 2) Test the coil packs.$extra")
                    }
                    "P0171", "P0174" -> causes.add("[WHAT TO CHECK - $label]: Check the PCV elbow hose, MAF sensor, and fuel pressure.")
                }
            }
            "CHEVROLET", "GMC", "CADILLAC" -> {
                if (code == "P0171" || code == "P0174") {
                    causes.add("[WHAT TO CHECK - $label]: Check the intake manifold gaskets, clean the MAF sensor, and inspect the purge valve.")
                }
            }
        }
        return causes
    }

    private fun knownMake(vehicle: VehicleProfile): String? {
        val make = vehicle.make.trim().uppercase()
        if (make.isBlank() || make in genericMakes) return null
        return make
    }

    private fun isPentastarFamily(vehicle: VehicleProfile): Boolean {
        val engine = vehicle.engine.uppercase()
        return engine.contains("PENTASTAR") || engine.contains("3.6")
    }

    private val genericMakes = setOf(
        "VEHICLE",
        "GENERIC VEHICLE",
        "GENERIC OBD2 VEHICLE",
        "UNKNOWN",
        "GENERAL MOTORS"
    )
}

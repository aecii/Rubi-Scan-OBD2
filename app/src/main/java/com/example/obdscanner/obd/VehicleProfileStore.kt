package com.example.obdscanner.obd

import android.content.Context
import com.example.obdscanner.model.SavedVehicle
import com.example.obdscanner.model.VehicleInfo
import org.json.JSONArray
import org.json.JSONObject

object VehicleProfileStore {

    private const val PREFS = "rubiscan_vehicles"
    private const val KEY = "saved_json"

    data class Snapshot(
        val vehicles: List<SavedVehicle>,
        val activeId: String?
    )

    fun load(context: Context): Snapshot {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return Snapshot(emptyList(), null)
        return try {
            val root = JSONObject(raw)
            val array = root.optJSONArray("vehicles") ?: JSONArray()
            val vehicles = buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val id = item.optString("id")
                    val name = item.optString("displayName")
                    if (id.isBlank() || name.isBlank()) continue
                    add(
                        SavedVehicle(
                            id = id,
                            vin = item.optString("vin"),
                            year = item.optString("year"),
                            make = item.optString("make"),
                            model = item.optString("model"),
                            engine = item.optString("engine"),
                            displayName = name,
                            plateNumber = item.optString("plateNumber"),
                            phoneNumber = item.optString("phoneNumber"),
                            calibrationId = item.optString("calibrationId")
                        )
                    )
                }
            }
            Snapshot(vehicles, root.optString("activeId").ifBlank { null })
        } catch (_: Exception) {
            Snapshot(emptyList(), null)
        }
    }

    fun save(context: Context, vehicles: List<SavedVehicle>, activeId: String?) {
        val array = JSONArray()
        vehicles.forEach { vehicle ->
            array.put(
                JSONObject()
                    .put("id", vehicle.id)
                    .put("vin", vehicle.vin)
                    .put("year", vehicle.year)
                    .put("make", vehicle.make)
                    .put("model", vehicle.model)
                    .put("engine", vehicle.engine)
                    .put("displayName", vehicle.displayName)
                    .put("plateNumber", vehicle.plateNumber)
                    .put("phoneNumber", vehicle.phoneNumber)
                    .put("calibrationId", vehicle.calibrationId)
            )
        }
        val root = JSONObject()
            .put("vehicles", array)
            .put("activeId", activeId ?: "")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, root.toString())
            .apply()
    }
}

fun SavedVehicle.toVehicleInfo(
    calibrationId: String = this.calibrationId,
    cvn: String = "Not Read"
): VehicleInfo {
    return VehicleInfo(
        vin = vin.ifBlank { "Not Read" },
        calibrationId = calibrationId.ifBlank { "Not Read" },
        cvn = cvn.ifBlank { "Not Read" },
        ecuName = "Saved profile",
        profile = VehicleProfile(
            vin = vin.ifBlank { "Unknown" },
            make = make.ifBlank { "Vehicle" },
            model = model.ifBlank { "Vehicle" },
            year = year.ifBlank { "Unknown" },
            engine = engine,
            displayName = displayName
        ),
        plateNumber = plateNumber,
        phoneNumber = phoneNumber,
        savedId = id
    )
}

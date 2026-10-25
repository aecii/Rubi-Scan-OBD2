package com.example.obdscanner.obd

data class VehicleProfile(
    val vin: String,
    val make: String,
    val model: String,
    val year: String,
    val engine: String,
    val displayName: String
)

object VinDecoder {

    fun decode(vin: String): VehicleProfile {
        val cleanVin = vin.uppercase().trim().filter { it.isLetterOrDigit() }
        if (cleanVin.length < 11) {
            return VehicleProfile(
                vin = if (cleanVin.isBlank()) "Unknown" else cleanVin,
                make = "",
                model = "",
                year = "Unknown",
                engine = "",
                displayName = "Generic OBD2 Vehicle"
            )
        }

        val wmi = cleanVin.take(3)
        val yearStr = decodeYear(cleanVin[9])
        val makeStr = makeFromWmi(wmi)
        val modelStr = ""
        val engineStr = ""

        val displayName = when {
            makeStr.isNotBlank() && yearStr != "Unknown" -> "$yearStr $makeStr"
            makeStr.isNotBlank() -> makeStr
            yearStr != "Unknown" -> "$yearStr vehicle"
            else -> "Generic OBD2 Vehicle"
        }

        return VehicleProfile(
            vin = cleanVin,
            make = makeStr,
            model = modelStr,
            year = yearStr,
            engine = engineStr,
            displayName = displayName
        )
    }

    /**
     * Manufacturer from the world manufacturer identifier only.
     * Model and engine are left blank unless the user names them.
     * Shared Chrysler-group identifiers such as 1C4 are not treated as a Jeep.
     */
    private fun makeFromWmi(wmi: String): String {
        exactWmi[wmi]?.let { return it }
        return when {
            wmi.startsWith("1F") || wmi.startsWith("2F") || wmi.startsWith("3F") -> "Ford"
            wmi.startsWith("1G") || wmi.startsWith("2G") || wmi.startsWith("3G") -> "General Motors"
            wmi.startsWith("4T") || wmi.startsWith("5T") || wmi.startsWith("2T") || wmi.startsWith("JT") -> "Toyota"
            wmi.startsWith("1H") || wmi.startsWith("5F") || wmi.startsWith("JH") -> "Honda"
            wmi.startsWith("1N") || wmi.startsWith("3N") || wmi.startsWith("5N") || wmi.startsWith("JN") -> "Nissan"
            else -> ""
        }
    }

    private val exactWmi = mapOf(
        "1J4" to "Jeep",
        "1J7" to "Jeep",
        "1J8" to "Jeep",
        "1C3" to "Chrysler",
        "2C3" to "Chrysler",
        "3C3" to "Chrysler",
        "1C6" to "RAM",
        "3C6" to "RAM",
        "3C7" to "RAM",
        "1B3" to "Dodge",
        "1B4" to "Dodge",
        "1B7" to "Dodge",
        "1D3" to "Dodge",
        "1D7" to "Dodge",
        "1D8" to "Dodge",
        "2D3" to "Dodge",
        "2D4" to "Dodge",
        "2D8" to "Dodge",
        "3D3" to "Dodge",
        "3D4" to "Dodge",
        "3D7" to "Dodge",
        "1G1" to "Chevrolet",
        "1GB" to "Chevrolet",
        "1GC" to "Chevrolet",
        "1GN" to "Chevrolet",
        "2G1" to "Chevrolet",
        "3G1" to "Chevrolet",
        "3GC" to "Chevrolet",
        "1GK" to "GMC",
        "1GT" to "GMC",
        "2GT" to "GMC",
        "1G4" to "Buick",
        "1G6" to "Cadillac",
        "1GY" to "Cadillac",
        "JTH" to "Lexus",
        "JTJ" to "Lexus",
        "JT6" to "Lexus",
        "JT8" to "Lexus",
        "19U" to "Acura",
        "JH4" to "Acura",
        "5J6" to "Honda",
        "5FN" to "Honda",
        "5KB" to "Honda",
        "1HG" to "Honda",
        "2HG" to "Honda",
        "JHM" to "Honda",
        "JNK" to "Infiniti",
        "1N4" to "Nissan",
        "1N6" to "Nissan",
        "3N1" to "Nissan",
        "5N1" to "Nissan",
        "JN1" to "Nissan",
        "JN8" to "Nissan",
        "KMH" to "Hyundai",
        "5NP" to "Hyundai",
        "5NM" to "Hyundai",
        "KNA" to "Kia",
        "KND" to "Kia",
        "5XX" to "Kia",
        "WVW" to "Volkswagen",
        "WVG" to "Volkswagen",
        "1VW" to "Volkswagen",
        "3VW" to "Volkswagen",
        "WAU" to "Audi",
        "WA1" to "Audi",
        "WBA" to "BMW",
        "WBS" to "BMW",
        "4US" to "BMW",
        "5UX" to "BMW",
        "WDD" to "Mercedes-Benz",
        "WDC" to "Mercedes-Benz",
        "4JG" to "Mercedes-Benz",
        "JF1" to "Subaru",
        "JF2" to "Subaru",
        "4S3" to "Subaru",
        "4S4" to "Subaru",
        "JM1" to "Mazda",
        "JM3" to "Mazda",
        "1YV" to "Mazda",
        "YV1" to "Volvo",
        "YV4" to "Volvo"
    )

    private fun decodeYear(char: Char): String {
        return when (char) {
            'A' -> "2010"
            'B' -> "2011"
            'C' -> "2012"
            'D' -> "2013"
            'E' -> "2014"
            'F' -> "2015"
            'G' -> "2016"
            'H' -> "2017"
            'J' -> "2018"
            'K' -> "2019"
            'L' -> "2020"
            'M' -> "2021"
            'N' -> "2022"
            'P' -> "2023"
            'R' -> "2024"
            'S' -> "2025"
            'T' -> "2026"
            '1' -> "2001"
            '2' -> "2002"
            '3' -> "2003"
            '4' -> "2004"
            '5' -> "2005"
            '6' -> "2006"
            '7' -> "2007"
            '8' -> "2008"
            '9' -> "2009"
            else -> "Unknown"
        }
    }
}

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
                make = "Generic OBD2 Vehicle",
                model = "Vehicle",
                year = "Unknown",
                engine = "Standard Internal Combustion Engine",
                displayName = "Generic OBD2 Vehicle"
            )
        }

        val wmi = cleanVin.take(3)
        val yearChar = cleanVin[9]
        val vdsChar4 = cleanVin[3]
        val vdsChar5 = cleanVin[4]
        val vdsChar8 = cleanVin[7]

        val yearStr = decodeYear(yearChar)
        var makeStr = "Vehicle"
        var modelStr = "Vehicle"
        var engineStr = "Engine"

        // Manufacturer specific decoding
        when {
            // Jeep / Chrysler / Dodge / RAM (WMI 1J4, 1C4, 2C4, 3C4, 1D3, 2D3, etc.)
            wmi.startsWith("1J") || wmi.startsWith("1C") || wmi.startsWith("2C") || wmi.startsWith("3C") || wmi.startsWith("1D") -> {
                makeStr = "Jeep"
                if (wmi.startsWith("1C") || wmi.startsWith("2C") || wmi.startsWith("3C")) {
                    makeStr = when {
                        vdsChar5 == 'A' || vdsChar5 == 'B' -> "Chrysler"
                        vdsChar5 == 'D' || vdsChar5 == '3' -> "Dodge"
                        vdsChar5 == 'R' || vdsChar5 == '6' -> "RAM"
                        else -> "Jeep"
                    }
                }

                if (makeStr == "Jeep" || vdsChar4 == 'H') {
                    makeStr = "Jeep"
                    modelStr = when (vdsChar5) {
                        'J' -> "Wrangler JKU / JK"
                        'W' -> "Wrangler Rubicon / Sahara"
                        'E' -> "Grand Cherokee"
                        'A' -> "Cherokee"
                        'N' -> "Compass / Patriot"
                        else -> "Wrangler JKU / JK"
                    }
                    engineStr = when (vdsChar8) {
                        'G', 'D', 'E' -> "3.6L V6 Pentastar Engine"
                        '1', 'S' -> "3.8L V6 Engine"
                        'B', 'K' -> "2.0L I4 Turbo Engine"
                        'M', 'V' -> "5.7L V8 HEMI Engine"
                        else -> "3.6L V6 Pentastar Engine"
                    }
                }
            }

            // Ford (WMI 1FA, 1FB, 1FC, 1FD, 1FM, 1FT, 2FM, 3FA)
            wmi.startsWith("1F") || wmi.startsWith("2F") || wmi.startsWith("3F") -> {
                makeStr = "Ford"
                modelStr = when (vdsChar5) {
                    'T', 'W' -> "F-150 / Super Duty"
                    'K', 'U' -> "Explorer"
                    'E' -> "Escape"
                    'P' -> "Mustang"
                    else -> "Ford Truck / SUV"
                }
                engineStr = when (vdsChar8) {
                    'G', '6' -> "3.5L V6 EcoBoost"
                    'F', '5' -> "5.0L V8 Coyote"
                    '9', '2' -> "2.7L V6 EcoBoost"
                    'U', 'X' -> "2.0L EcoBoost"
                    else -> "Ford Gas Engine"
                }
            }

            // General Motors / Chevrolet / GMC (WMI 1G1, 1GC, 1GT, 1G2, 1G6, 2G1, 3G1)
            wmi.startsWith("1G") || wmi.startsWith("2G") || wmi.startsWith("3G") -> {
                makeStr = when (wmi) {
                    "1GT", "2GT" -> "GMC"
                    "1G6" -> "Cadillac"
                    "1G3" -> "Oldsmobile"
                    "1G4" -> "Buick"
                    else -> "Chevrolet"
                }
                modelStr = when (vdsChar5) {
                    'C' -> "Silverado / Sierra"
                    'K', 'Z' -> "Tahoe / Suburban / Yukon"
                    '1', '2' -> "Corvette / Camaro"
                    else -> "GM Truck / SUV"
                }
                engineStr = when (vdsChar8) {
                    'C', 'L', '3' -> "5.3L V8 EcoTec3"
                    'J', '8' -> "6.2L V8 EcoTec3"
                    'M', '7' -> "4.3L V6 EcoTec3"
                    else -> "GM V8 / V6 Engine"
                }
            }

            // Toyota / Lexus (WMI 4T1, 5TD, 2T1, JTE, JT2, JTD)
            wmi.startsWith("4T") || wmi.startsWith("5T") || wmi.startsWith("JT") -> {
                makeStr = if (wmi.startsWith("JT2") || wmi.startsWith("JTH")) "Lexus" else "Toyota"
                modelStr = when (vdsChar4) {
                    'B', 'K' -> "Camry"
                    'D', 'R' -> "RAV4"
                    'A', 'G' -> "Tacoma / Tundra"
                    'J' -> "4Runner"
                    else -> "Toyota SUV / Sedan"
                }
                engineStr = "Toyota V6 / I4 Engine"
            }

            // Honda / Acura (WMI 1HG, 5FN, 1NY, JHM)
            wmi.startsWith("1H") || wmi.startsWith("5F") || wmi.startsWith("JH") -> {
                makeStr = if (wmi.startsWith("1NY") || wmi.startsWith("JH4")) "Acura" else "Honda"
                modelStr = "Honda Civic / Accord / CR-V / Pilot"
                engineStr = "Honda i-VTEC Engine"
            }

            // Nissan / Infiniti (WMI 1N4, 5N1, JN1)
            wmi.startsWith("1N") || wmi.startsWith("5N") || wmi.startsWith("JN") -> {
                makeStr = "Nissan"
                modelStr = "Nissan Altima / Rogue / Frontier / Titan"
                engineStr = "Nissan VQ V6 / QR I4 Engine"
            }

            else -> {
                makeStr = "Generic Vehicle"
                modelStr = "Vehicle"
                engineStr = "Gasoline / Diesel Engine"
            }
        }

        val displayName = "$yearStr $makeStr $modelStr ($engineStr)".trim()

        return VehicleProfile(
            vin = cleanVin,
            make = makeStr,
            model = modelStr,
            year = yearStr,
            engine = engineStr,
            displayName = displayName
        )
    }

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
            else -> "2015"
        }
    }
}

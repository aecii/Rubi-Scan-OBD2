package com.example.obdscanner.obd

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.obdscanner.model.DisplayUnits
import com.example.obdscanner.model.DrivingCriticality
import com.example.obdscanner.model.DtcCode
import com.example.obdscanner.model.FreezeFrameData
import com.example.obdscanner.model.LiveMetrics
import com.example.obdscanner.model.ObdAdapterInfo
import com.example.obdscanner.model.VehicleInfo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateReport(
        context: Context,
        vehicleInfo: VehicleInfo,
        adapterInfo: ObdAdapterInfo,
        liveMetrics: LiveMetrics,
        dtcCodes: List<DtcCode>,
        freezeFrame: FreezeFrameData?,
        units: DisplayUnits = DisplayUnits()
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595x842 pt)
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val textPaint = Paint()

        var y = 40f
        val margin = 35f
        val contentWidth = 525f

        // --- Header Banner ---
        paint.color = Color.rgb(30, 136, 229) // Blue Accent
        canvas.drawRect(margin, y, margin + contentWidth, y + 50f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 20f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Rubi-Scan OBDII - Vehicle Inspection Report", margin + 15f, y + 32f, textPaint)

        y += 65f

        // --- Timestamp ---
        val dateStr = SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.US).format(Date())
        textPaint.color = Color.DKGRAY
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Generated on: $dateStr", margin, y, textPaint)

        y += 20f

        // --- Adapter & Communication Summary ---
        paint.color = Color.rgb(240, 240, 240)
        canvas.drawRect(margin, y, margin + contentWidth, y + 60f, paint)

        textPaint.color = Color.BLACK
        textPaint.textSize = 12f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("OBDLink Hardware & Protocol Summary", margin + 10f, y + 20f, textPaint)

        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Adapter: ${adapterInfo.name.ifBlank { "OBDLink MX" }} (${adapterInfo.address.ifBlank { "Bluetooth Classic" }})", margin + 10f, y + 36f, textPaint)
        canvas.drawText("Protocol: ${adapterInfo.protocol} | Voltage: ${adapterInfo.voltage}", margin + 10f, y + 50f, textPaint)

        y += 75f

        // --- Vehicle System Status Box ---
        val milWarning = liveMetrics.milOn || dtcCodes.isNotEmpty()
        paint.color = if (milWarning) Color.rgb(255, 243, 224) else Color.rgb(232, 245, 233)
        canvas.drawRect(margin, y, margin + contentWidth, y + 50f, paint)

        textPaint.color = if (milWarning) Color.rgb(230, 81, 0) else Color.rgb(46, 125, 50)
        textPaint.textSize = 13f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(
            if (milWarning) "MALFUNCTION INDICATOR LIGHT (MIL): ON (${dtcCodes.size} DTCs Stored)"
            else "MALFUNCTION INDICATOR LIGHT (MIL): OFF (No Active Codes)",
            margin + 10f, y + 22f, textPaint
        )

        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(
            "Fuel Loop: ${liveMetrics.fuelLoopStatus?.title ?: "Open Loop"} | Coolant: ${units.formatTemperature(liveMetrics.coolantCelsius)}",
            margin + 10f, y + 40f, textPaint
        )

        y += 65f

        // --- Diagnostic Trouble Codes Section ---
        textPaint.color = Color.BLACK
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Diagnostic Trouble Codes (DTCs)", margin, y, textPaint)

        y += 15f
        paint.color = Color.GRAY
        canvas.drawLine(margin, y, margin + contentWidth, y, paint)
        y += 15f

        if (dtcCodes.isEmpty()) {
            textPaint.textSize = 11f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textPaint.color = Color.DKGRAY
            canvas.drawText("✓ No diagnostic trouble codes returned by vehicle engine computer.", margin, y, textPaint)
            y += 20f
        } else {
            for (dtc in dtcCodes) {
                if (y > 750f) break // Protect page bottom overflow

                // Code Header line
                textPaint.textSize = 12f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.color = Color.rgb(211, 47, 47)
                canvas.drawText("${dtc.code} [${dtc.category.label}] - ${dtc.title}", margin, y, textPaint)

                y += 15f

                // Criticality rating
                val critColor = when (dtc.criticality) {
                    DrivingCriticality.CRITICAL_STOP_IMMEDIATELY -> Color.rgb(211, 47, 47)
                    DrivingCriticality.CAUTION_DRIVE_HOME -> Color.rgb(230, 81, 0)
                    DrivingCriticality.SERVICE_SOON -> Color.rgb(56, 142, 60)
                }
                textPaint.color = critColor
                textPaint.textSize = 10f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("CRITICALITY: ${dtc.criticality.label}", margin, y, textPaint)

                y += 15f

                // Explanation
                textPaint.color = Color.BLACK
                textPaint.textSize = 10f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Explanation: ${dtc.plainLanguageExplanation.ifBlank { dtc.description }}", margin, y, textPaint)

                y += 15f

                // Verified Fixes
                if (dtc.verifiedFixes.isNotEmpty()) {
                    textPaint.color = Color.rgb(21, 101, 192)
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("Verified Fixes: ${dtc.verifiedFixes.take(2).joinToString("; ")}", margin, y, textPaint)
                    y += 20f
                }
                y += 10f
            }
        }

        // --- Freeze Frame Snapshot Section ---
        if (freezeFrame != null && y < 680f) {
            y += 10f
            textPaint.color = Color.BLACK
            textPaint.textSize = 12f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Freeze Frame Snapshot (conditions when ${freezeFrame.dtcCode} was stored)", margin, y, textPaint)

            y += 15f
            val lines = listOf(
                "RPM: ${freezeFrame.rpm?.toInt() ?: "—"}    Speed: ${units.formatSpeed(freezeFrame.speedKmh)}    Coolant: ${units.formatTemperature(freezeFrame.coolantCelsius)}",
                "Load: ${freezeFrame.loadPercent?.let { "%.0f%%".format(it) } ?: "—"}    Trim: ${freezeFrame.fuelTrimB1?.let { "%+.1f%%".format(it) } ?: "—"}    Throttle: ${freezeFrame.throttlePercent?.let { "%.0f%%".format(it) } ?: "—"}",
                "Intake: ${units.formatTemperature(freezeFrame.intakeAirCelsius)}    MAP: ${units.formatPressure(freezeFrame.mapKpa)}    Fuel: ${freezeFrame.fuelLoopStatus?.title ?: "—"}"
            )
            val blockHeight = 12f + (lines.size * 14f)
            paint.color = Color.rgb(245, 245, 245)
            canvas.drawRect(margin, y, margin + contentWidth, y + blockHeight, paint)

            textPaint.textSize = 9f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            lines.forEachIndexed { index, line ->
                canvas.drawText(line, margin + 10f, y + 14f + (index * 14f), textPaint)
            }
            y += blockHeight + 12f
        }

        // Footer
        textPaint.color = Color.GRAY
        textPaint.textSize = 8f
        canvas.drawText("Rubi-Scan OBDII Diagnostic Scanner · Professional Inspection Report", margin, 810f, textPaint)

        pdfDocument.finishPage(page)

        return try {
            val file = File(context.cacheDir, "RubiScan_Inspection_Report.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}

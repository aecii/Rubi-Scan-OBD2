package com.example.obdscanner.obd

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AvailableUpdate(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String
)

object VersionCheck {
    private const val MANIFEST_URL = "https://aecii.kindfox.org/OBDII/version.json"
    private const val DEFAULT_DOWNLOAD_URL = "https://aecii.kindfox.org/OBDII/RubiScan-OBDII.apk"
    private const val LOG_TAG = "RubiScanUpdate"

    fun installedVersionName(context: Context): String {
        return packageInfo(context).versionName ?: ""
    }

    fun installedVersionCode(context: Context): Int {
        val packageInfo = packageInfo(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
    }

    private fun packageInfo(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }

    suspend fun findUpdate(context: Context): AvailableUpdate? = withContext(Dispatchers.IO) {
        val installed = installedVersionCode(context)
        try {
            val connection = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "RubiScanOBDII")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.i(LOG_TAG, "Version file returned HTTP ${connection.responseCode}")
                return@withContext null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val latestCode = json.optInt("versionCode", 0)
            if (latestCode <= installed) return@withContext null
            AvailableUpdate(
                versionName = json.optString("versionName", latestCode.toString()),
                versionCode = latestCode,
                downloadUrl = json.optString("downloadUrl", DEFAULT_DOWNLOAD_URL),
                releaseNotes = json.optString("releaseNotes", "")
            )
        } catch (error: Exception) {
            Log.i(LOG_TAG, "Version check failed: ${error.message}")
            null
        }
    }
}

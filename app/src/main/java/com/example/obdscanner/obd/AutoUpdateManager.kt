package com.example.obdscanner.obd

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersionCode: Int,
    val latestVersionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val statusText: String = ""
)

object AutoUpdateManager {

    private const val MANIFEST_URL = "https://aecii.kindfox.org/OBDII/version.json"

    suspend fun checkForUpdates(currentVersionCode: Int): AppUpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL(MANIFEST_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "RubiScanOBDII-AndroidApp/0.2.0")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                val json = JSONObject(response)

                val latestCode = json.optInt("versionCode", currentVersionCode)
                val latestName = json.optString("versionName", "0.2.0")
                val downloadUrl = json.optString("downloadUrl", "https://aecii.kindfox.org/OBDII/RubiScan-OBDII.apk")
                val notes = json.optString("releaseNotes", "New performance and stability improvements.")

                AppUpdateInfo(
                    isUpdateAvailable = latestCode > currentVersionCode,
                    latestVersionCode = latestCode,
                    latestVersionName = latestName,
                    downloadUrl = downloadUrl,
                    releaseNotes = notes,
                    statusText = "Server checked (Code $latestCode vs Installed $currentVersionCode)"
                )
            } else {
                AppUpdateInfo(
                    isUpdateAvailable = false,
                    latestVersionCode = currentVersionCode,
                    latestVersionName = "0.2.0",
                    downloadUrl = "",
                    releaseNotes = "",
                    statusText = "Server returned HTTP $responseCode"
                )
            }
        } catch (e: Exception) {
            AppUpdateInfo(
                isUpdateAvailable = false,
                latestVersionCode = currentVersionCode,
                latestVersionName = "0.2.0",
                downloadUrl = "",
                releaseNotes = "",
                statusText = "Network check error: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    fun downloadAndInstallUpdate(context: Context, downloadUrl: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Rubi-Scan OBDII Update")
                setDescription("Downloading latest app update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "RubiScan-OBDII-update.apk")
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        } catch (_: Exception) {}
    }
}

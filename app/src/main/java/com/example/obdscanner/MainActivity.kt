package com.example.obdscanner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.obdscanner.ui.ObdScannerApp
import com.example.obdscanner.viewmodel.ObdScannerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ObdScannerViewModel by viewModels()

    private var showDevicePicker by mutableStateOf(false)
    private var pairedDevices by mutableStateOf<List<BluetoothDevice>>(emptyList())

    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadBondedDevicesAndShowPicker()
            } else {
                Toast.makeText(
                    this,
                    "Bluetooth permission denied. Allow Nearby devices permission in Settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ObdScannerApp(
                        viewModel = viewModel,
                        pairedDevices = pairedDevices,
                        showDevicePicker = showDevicePicker,
                        onRequestConnect = { checkPermissionAndConnect() },
                        onDismissDevicePicker = { showDevicePicker = false }
                    )
                }
            }
        }
    }

    private fun checkPermissionAndConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            loadBondedDevicesAndShowPicker()
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadBondedDevicesAndShowPicker() {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!btAdapter.isEnabled) {
            Toast.makeText(this, "Turn Bluetooth on in Android settings first.", Toast.LENGTH_SHORT).show()
            return
        }

        pairedDevices = btAdapter.bondedDevices
            .sortedBy { it.name ?: it.address }
            .toList()

        if (pairedDevices.isEmpty()) {
            Toast.makeText(
                this,
                "No paired Bluetooth devices found. Pair your OBDLink MX in Android Settings first.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            showDevicePicker = true
        }
    }
}

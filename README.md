# Rubi-Scan OBDII

Android OBD-II scanner for a paired Bluetooth Classic adapter (OBDLink or another ELM327-style adapter). It reads standard SAE J1979 data from the engine computer and keeps vehicle details on the phone.

The app stays generic until a VIN identifies the manufacturer, or you name the vehicle yourself. It does not assume a Jeep, a Mustang, or any other model.

## Features

- Connects to an adapter that is already paired in Android Bluetooth settings. The adapter searches for a protocol, then falls back to common CAN settings if the first check does not answer.
- Dashboard gauges for engine speed, vehicle speed, load, throttle, manifold pressure, mass airflow, coolant, intake air temperature, battery voltage, fuel trims, fuel-loop status, and the malfunction indicator lamp.
- Live O2 sensor graph for both banks, upstream and downstream, with a numbers view of the same sensors. Short-term fuel trim moves with the upstream readings.
- Sensor list of the Mode 01 PIDs the engine computer says it supports. A PID without a known formula is still listed, using the raw data.
- Stored, pending, and permanent trouble codes, with plain-language descriptions. Make-specific notes appear only when the saved make, and where needed the engine, match. Codes can be cleared from the app.
- Freeze frame: the snapshot the engine computer stored when a fault was set, including speed, coolant, load, fuel trim, and related readings. The same snapshot is included in the PDF report.
- Vehicle profiles stored on the phone. A 17-character VIN is read after connect. A specific manufacturer code names the make and year. Model and engine stay blank until you enter them. Each profile can hold a plate number and a phone number. More than one vehicle can be saved.
- Demo Mode simulates a connected car with moving gauges, an O2 waveform, fuel trim, a stored misfire code, and freeze frame, without an adapter.
- Selectable speed and temperature units, chosen separately: km/h or mph, and °C or °F. Miles, psi, gal/h, and lb-ft follow the speed choice. The acceleration timer is 0–60 mph or 0–100 km/h to match.
- CSV log of the live poll, and a camera preview with a telemetry overlay for RPM, speed, coolant, intake temperature, loop status, and the O2 trace.
- On startup, the app reads the version file from the download server. If that version code is higher than the installed app, it offers a direct download link.

## Build

1. Install Android Studio and a JDK 17 or 21.
2. Open this folder as a project.
3. Let Gradle sync and install Android SDK Platform 35 if prompted.
4. Run on a phone with Bluetooth Classic.
5. Pair the adapter in Android Settings before tapping Connect.

`assembleDebug` also publishes the debug APK and `version.json` to the download server when that machine can reach it.

## Limits

- The app talks only to an already paired Bluetooth Classic adapter. The adapter usually accepts one app at a time.
- If the engine computer does not return a VIN, the session stays generic until you select or create a profile.
- Shared manufacturer codes are not guessed into a model. A code such as `1C4` is not labeled as a Jeep.
- Some PIDs and code modes are unsupported on a given car. Missing live values stay blank.
- Use the readings as a diagnostic aid. Confirm them on the vehicle before a repair.

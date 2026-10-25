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
- Vehicle profiles stored on the phone. See [Saved vehicle profiles](#saved-vehicle-profiles).
- Demo Mode, for trying the screens with no adapter and no car. See [Demo Mode](#demo-mode).
- Selectable speed and temperature units, chosen separately: km/h or mph, and °C or °F. Miles, psi, gal/h, and lb-ft follow the speed choice. The acceleration timer is 0–60 mph or 0–100 km/h to match.
- CSV log of the live poll, and a camera preview with a telemetry overlay for RPM, speed, coolant, intake temperature, loop status, and the O2 trace.
- On startup, the app reads the version file from the download server. If that version code is higher than the installed app, it offers a direct download link.
- When the dashboard says a fault was detected, tap that banner to open the trouble codes.

## Demo Mode

Demo Mode is the way to use Rubi-Scan with no Bluetooth adapter and no vehicle nearby. Turn it on from the Demo chip in the top bar.

Each time you turn it on, the app rolls a new 17-character VIN and one to four stored trouble codes, then shows a freeze frame for the first code. The gauges, the O2 waveform, and the fuel trim keep moving, the way a live read looks. Turning Demo Mode off and on again rolls a different VIN and a different set of codes. Reading the codes again, while Demo Mode stays on, keeps the same set.

The manufacturer name comes only from that VIN. Model and engine stay blank, and the trouble-code notes stay generic. Demo Mode does not assume a Jeep, a Mustang, or any other model.

## Saved vehicle profiles

A profile is how the phone remembers a car. After a connection, the app reads the VIN once. A specific manufacturer code fills in the year and make. Model and engine stay blank until you enter them. The session stays generic when the VIN is missing or the manufacturer code is not specific.

Saving that profile means the next connection to the same VIN opens the car you already named, including the model and engine you typed. Trouble-code notes that depend on the make, and on the engine where that matters, are used only for that saved vehicle. You can keep more than one vehicle and pick the one you are working on in Settings.

The app does not ask you to save while you are still looking at the data. When you leave, and the detected vehicle is not saved yet, it asks. Save keeps the profile. Not now leaves without saving and will ask the next time. Never ask again turns the prompt off. You can still add or edit a vehicle yourself in Settings.

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

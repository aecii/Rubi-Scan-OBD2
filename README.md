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

Demo Mode does not mean limited version, this app is free. 100%. 
What i mean by demo mode: Demo mode is the way to use Rubi-Scan with no Bluetooth adapter and no vehicle nearby. Turn it on from the Demo chip in the top bar.

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


Screen shots: 

Sample PDF report: 
[RubiScan_Inspection_Report (2).pdf](https://github.com/user-attachments/files/33938822/RubiScan_Inspection_Report.2.pdf)
<img width="819" height="1152" alt="image" src="https://github.com/user-attachments/assets/81e0c0ae-5c69-4078-bcb5-4be6504c60b0" />

<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/b94d0369-f2b1-4640-b362-2195bdce97a3" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/900112b4-b9b6-4b7e-91ef-b2aa0a9bc338" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/d41a023b-9453-4877-8f9e-0abd7270cb5a" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/257980c1-3db1-48e9-b3e6-85937837882a" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/9bd01305-f5e0-429d-9a5d-f77658091878" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/52b09a77-f818-4cdb-9373-9aec352cb6eb" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/1d97006a-0ed1-4217-b258-78ab046080ad" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/84b0dd8f-7539-43cc-88de-58d7ce4a2f21" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/604d4c04-c1d6-443f-90f0-0b6b2ef3c41c" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/6d03c64d-3392-4024-8674-91ae264d87e9" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/84d8e0e2-1e5a-465a-9157-98e5a2ed386d" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/00b0b185-1e97-4581-b116-065a82d92ed5" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/2e332517-39be-4ab3-aa0f-4f02fb4ad1f1" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/5d9079c9-fca5-4257-bbb1-0ff211e191a2" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/52bc453f-87df-4018-aac9-caf3d2a33271" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/ed670f42-f8a7-4d82-91f2-bd669da52baa" />
<img width="430" height="959" alt="image" src="https://github.com/user-attachments/assets/d34ea3c6-ae1d-4e20-9d3c-8d7919f18c1f" />


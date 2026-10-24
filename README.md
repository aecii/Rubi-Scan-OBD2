# Rubi-Scan OBDII

A native Kotlin + Jetpack Compose Android starter for a read-only OBD-II scanner over Bluetooth Classic SPP.

## Features in this first pass
- Lists already-paired Bluetooth devices (no Bluetooth scan).
- Connects using the standard Bluetooth SPP UUID.
- Initializes the adapter using ELM-compatible commands.
- Polls standard Mode 01 PIDs for RPM, speed, coolant temperature, and calculated engine load.
- Requests stored (Mode 03), pending (Mode 07), and permanent (Mode 0A) diagnostic trouble codes.

## Build
1. Install Android Studio and a JDK 17.
2. Open this folder as a project.
3. Let Gradle sync and install Android SDK Platform 35 if prompted.
4. Run on an Android phone with Bluetooth Classic.
5. Pair the OBDLink adapter in Android Settings > Bluetooth before tapping Connect.

## Important limitations
- This is an initial prototype and has not been built/tested against your specific phone/adapter in this environment.
- It connects only to already-paired devices. This avoids requiring Bluetooth discovery permissions.
- Keep the adapter disconnected from other apps; OBDLink adapters generally support one active app connection at a time.
- Some vehicle PIDs or DTC modes may be unsupported; the UI currently reports unavailable live values as N/A.
- Speed is displayed in km/h in this first pass.
- The prototype uses synchronous serial reads on an IO dispatcher, but the command timeout / disconnect handling should be hardened before regular use.
- It does not clear codes, access Ford-enhanced modules, perform active tests, or control vehicle systems.
- Validate code decoding and adapter responses with a real adapter before relying on results for repair decisions.

## Next engineering steps
1. Improve adapter response parsing for `NO DATA`, `UNABLE TO CONNECT`, multi-ECU replies, and adapter startup banners.
2. Add selectable units (km/h / mph, °C / °F).
3. Move connection/protocol state into a ViewModel + StateFlow repository.
4. Add retry/backoff, explicit cancellation, and automated unit tests for PID/DTC decoding.
5. Add Ford-specific support only after confirming the adapter model and required commands.

# health-monitor

Native Android/Wear OS app (package `com.monitor.health`, Gradle root project `monitor-your-health`).
Wearable/monitoring-side companion in the **FamilyWatchToday** ecosystem — see root
[`../CLAUDE.md`](../CLAUDE.md). No README in this repo; details below are inferred from source.

## Purpose

Continuously reads on-device/paired-sensor vitals (heart rate, SpO2, BP, temperature, weight/body
composition, blood glucose, steps, sleep, ECG) via foreground services (`HeartRateSensorService`,
`BloodOxygenSensorService`, `StepCounterService`, `SleepMonitor`, `FallDetectionService`, etc.),
stores them locally in Room, and syncs to the backend via Retrofit. Also supports fall-detection
alerts, Twilio voice/video calls, FCM push, medication schedules, and a "DR Watch"/telemedicine
flow. Entry points: `LoginActivity` → `MainActivity`/`DashActivity`; also `FallAlertActivity`,
`BleDeviceSelectionActivity`, `MessagesActivity`, `CallActivity`/`VideoActivity`/`VoiceCallActivity`,
`IncomingCallActivity`/`IncomingVoiceActivity`. Wear OS support (`libs.wear`, round/non-round
resource variants) indicates this targets a smartwatch form factor.

## Stack

100% Java, AGP 8.11.0, Gradle wrapper 8.13, `compileSdk 36`, `minSdk 26`, `targetSdk 36`, Java 11
source/target compatibility. Retrofit 2.8.1 + Gson + OkHttp 3.12.12 + logging-interceptor,
Conscrypt (custom TLS provider), Room 2.6.1, WorkManager 2.9.1, Firebase Cloud Messaging, Twilio
Video & Voice Android SDKs, MPAndroidChart, Glide, Play Services Location.

**Build gotcha:** `app/build.gradle` has a `fileTree` dependency on a hardcoded Windows path
(`C:\Users\DeeDee\...`) — will break the build on macOS/Linux until fixed/removed.

## Layout

`app/src/main/java/com/monitor/health/`: `adapter/`, `chart/`, `dao/`, `database/`, `dto/`,
`entity/`, `graph/`, `model/` (incl. `healthscore/`), `receiver/`, `repository/`, `request/`/
`response/` (per-vitals-type DTOs), `services/` (sensor/monitoring foreground services), `sleep/`,
`ui/` (activities/fragments), `utility/`, `viewmodel/`, `voice/` (Twilio + FCM), `worker/`.
`app/src/test`/`androidTest` contain only unmodified boilerplate — no real test coverage.

## Build / run

```
./gradlew assembleDebug     # or assembleRelease
./gradlew installDebug
./gradlew test                # boilerplate only
./gradlew connectedAndroidTest # needs a connected device/emulator
```
From the workspace root: `make build-health-monitor`.

## Backend

`Constant.java`: primary `BASE_URL`/`BASE_URL_BGM` = **`https://familywatchtoday.com/`** (same host
as `familyhealth-app` and `patient-monitoring-web`), plus a commented-out alternate
`https://api.smarthealth1on1.com/` (unused — that's the *other* ecosystem's host, don't confuse the
two). Separate fall-detection backend: `BASE_URL_FALL_DETECTION =
"https://lbaws.drsecurity.es/appservices/webservices/"` (shared with `pers`). `ApiClient.java`
builds a Retrofit client over OkHttp with a Conscrypt TLS setup and an `AuthInterceptor`. No local
`socketio`/`patient-monitoring-web` code paths referenced — only the deployed host.

## Config / security notes

`google-services.json` present at repo root (Firebase, not read for secrets). `local.properties`
present (machine-specific). **`Constant.java` hardcodes plaintext secrets committed to the repo**:
a WiFi SSID/password, a default login email/password, and several API tokens — technical debt/
security risk, should be externalized to BuildConfig/`local.properties` or a secrets manager.

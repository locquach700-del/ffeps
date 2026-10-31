# Pixel Assist — Android visual QA

Native Kotlin overlay for validating FOV geometry and screen-capture permission flows.

## Scope
This project draws a transparent FOV circle, crosshair, diagonals, tracer, box, and an app-generated TEST marker. MediaProjection creates an ImageReader capture surface but frames are not analyzed or stored. Shizuku is checked for permission status only. No player/enemy recognition, aim-lock, injection, hooks, or automated game input is implemented.

## Build
- Android Gradle Plugin 8.5.2, Gradle 8.7, Kotlin 1.9.24
- compileSdk 34, minSdk 26, JDK 17
- Run: `gradle assembleDebug`
- GitHub Actions builds on every push to main and manual dispatch. Download `pixel-assist-debug-apk` from the workflow run Artifacts.

## Device setup
1. Install/start Shizuku if testing its permission-status button.
2. Install APK and grant overlay permission.
3. Select a test app, tap Start visual overlay, then approve screen capture.
4. Stop using the persistent notification.

# Pixel Assist — Android Visual QA

Native Kotlin Android overlay for visual alignment tests and basic screenshot inspection.

## Features
- Adjustable FOV circle and centered crosshair with size control.
- Diagonal guides, tracer, test box, and app-generated TEST marker.
- Visual Detection Demo draws a synthetic object created by the app.
- Synthetic Tracking Demo animates that generated object across the overlay.
- Image Analyzer reads a user-selected image's dimensions and sampled average brightness.
- MediaProjection permission flow and floating overlay menu.
- Shizuku permission-status check.

## Demo boundaries
The detection/tracking demos use only a synthetic shape drawn by Pixel Assist. The Image Analyzer reports basic image properties only. Captured screen frames are not analyzed or stored. No player/opponent recognition, aim-lock, injection, hooks, or automated game input is included.

## Build
- Android Gradle Plugin 8.5.2, Gradle 8.7, Kotlin 1.9.24
- compileSdk 34, minSdk 26, JDK 17
- Run: `gradle assembleDebug`
- GitHub Actions builds on every push to main and manual dispatch. Download `pixel-assist-debug-apk` from the workflow run Artifacts.

## Device setup
1. Install/start Shizuku if testing its permission-status button.
2. Install APK and grant overlay permission.
3. Select a test app, tap Start visual overlay, then approve screen capture.
4. Use the overlay menu to toggle the synthetic demos.
5. Use Analyze an image to inspect a screenshot selected from the device.
6. Stop using the persistent notification.

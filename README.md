# Performance Overlay Replica

A Kotlin Android Studio starter replica based on static inspection of the supplied APK.

## Included behaviour

- Overlay permission check
- Battery-optimization exemption request
- Android 13+ notification permission request
- Foreground `PerformanceService`
- Transparent, hardware-accelerated 1x1 overlay view
- 3-second heartbeat invalidation loop
- Notification with a Stop action
- Service startup is delayed by **10,000 ms** after permissions are granted (replacing the original 500 ms callback)
- Launcher activity closes after **5,000 ms** once the service starts

## Build

1. Open this folder in Android Studio.
2. Allow Gradle sync to complete.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Install the generated debug APK.

## Important fidelity notes

The supplied APK is compiled Kotlin bytecode, not an original source project. Static inspection confirms the package `com.antutu.ABenchMark`, `MainActivity`, `PerformanceService`, permission flow, overlay attachment, and 3-second heartbeat. The source here reproduces that architecture but is not a byte-for-byte decompilation.

The original APK's inspected activity callbacks include a 500 ms startup callback and a 200 ms lifecycle callback. This replica changes the startup callback to 10,000 ms. Separately, the launcher activity closes 5,000 ms after service startup; that is a different timer and remains unchanged.

The original overlay is created as a plain transparent Android `View`; this project uses a 1x1 transparent view as a safe starting approximation. If you want a visually exact overlay, provide a screenshot of the overlay while active.

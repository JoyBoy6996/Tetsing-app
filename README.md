# forcehz

A minimal Vivo / iQOO refresh-rate controller with four fixed requests:

- 60 Hz
- 90 Hz
- 120 Hz
- 144 Hz
- Reset to system default

## Build with GitHub Actions

1. Create a GitHub repository.
2. Upload the contents of this project to the repository root.
3. Push to `main` (or `master`), or open **Actions → Build forcehz APK → Run workflow**.
4. Open the completed workflow run.
5. Download the `forcehz-debug-apk` artifact.

The workflow builds `app/build/outputs/apk/debug/app-debug.apk`. This project ZIP intentionally does not contain a prebuilt APK.

## Implementation

The global refresh-rate request uses Vivo's `rms` Binder service:

- `ServiceManager.getService("rms")`
- transaction 31: `acquireRefreshRate V2`
- transaction 24: release vote
- transaction 26: read active refresh rate

The request payload follows the published implementation in
[AndroPlus-org/vivo-HighRefresh-NoRoot](https://github.com/AndroPlus-org/vivo-HighRefresh-NoRoot).
The app does not use root, Shizuku, Accessibility, or `WRITE_SETTINGS` for the global vote.

The selected rate is stored locally and reapplied on `BOOT_COMPLETED`. Reset releases both handles used by forcehz and removes the saved selection.

## Important limitations

- This targets Vivo firmware that exposes the compatible `rms` Binder interface. It is not a generic Android refresh-rate API.
- A successful one-way Binder transaction means the request was accepted for delivery; it does not prove the display is currently running at that rate.
- Vivo firmware, thermal policy, app-specific policy, and supported display modes may override the request.
- The current rate shown by the app is read from the vendor RMS service.
- After installing, open the app once. Enable the app's Auto-start permission in Vivo/iQOO settings if the firmware restricts boot receivers.

## Project details

- App label: `forcehz`
- Application ID: `com.joyboy.forcehz`
- Minimum Android: 10 (API 29)
- Compile SDK: 36
- Target SDK: 34
- Java: 17
- Gradle: 8.13
- Android Gradle Plugin: 8.13.2

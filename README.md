# HyperPulse: Performance Booster & System Monitor

HyperPulse is an all-in-one Android performance booster, hardware telemetry monitor, intelligent clean engine, and dedicated Game Booster module built with Kotlin, Jetpack Compose, Material 3, Clean Architecture, and Shizuku integration.

---

## Key Modules & Features

### 1. Dashboard & Telemetry
- **Dynamic Circular Health Gauge:** Animated SVG-like canvas gauge calculating real-time system health score (0–100) from live CPU, RAM, thermal headroom, and storage pressure.
- **One-Tap Boost:** Clears background tasks with memory measurements before and after, records session logs, and refreshes JVM memory pages.
- **Hardware Telemetry:** Direct reads of CPU usage via `/proc/stat`, per-core clock frequencies via `/sys/devices/system/cpu/`, battery voltage (mV), current (mA), temperature (°C), health, storage statistics via `StatFs`, and live network throughput via `TrafficStats`.
- **Sparkline History:** Live canvas timeline tracking CPU and RAM telemetry over time.

### 2. Junk Cleaner
- Scans App Cache (`cacheDir`, `externalCacheDir`), Temporary files (`.tmp`, `.log`, `.bak`), APK leftovers, Media Thumbnails, and Empty Directories.
- Selective deletion by category with precise size tallies and instant storage reclamation feedback.

### 3. Dedicated Game Booster (`feature/gamebooster`)
- **Game Library:** Auto-detects installed games and allows adding custom game profiles.
- **6 Performance Modes:**
  1. **Balanced:** Standard governor, telemetry monitoring only.
  2. **Performance:** RAM trim, Sustained Performance Mode, ADPF hints, DND activation.
  3. **Turbo:** Performance + max refresh rate request, keep-screen-on, Wi-Fi high-performance lock, thermal safety monitors.
  4. **Turbo++:** Turbo + Shizuku shell/root tweaks (`cmd game mode performance`, `pm trim-caches`, background process freeze) with **automatic downgrade** on thermal limit (>45°C).
  5. **Battery Gaming:** 60Hz display cap and power-saving profiles.
  6. **Custom:** Individual hardware toggles.
- **In-Game Floating HUD:** Draggable overlay (`SYSTEM_ALERT_WINDOW`) displaying live FPS, CPU%, RAM%, Temperature, and an instant 1-tap Boost button.
- **Session Reports:** Logs duration, peak/average FPS, temperature curve, and RAM delta.

### 4. Shizuku & Root Hub (`feature/shizuku`)
- Runtime detection of Shizuku state: Not Installed, Not Running, Permission Required, Shell UID 2000, Root UID 0.
- Safe execution: All privileged actions run with safety timeouts and are logged to Room database with one-tap rollbacks.
- Capabilities: Real `dumpsys SurfaceFlinger` FPS, system cache trimming (`pm trim-caches`), appops background restriction, and game mode switching.

### 5. Hardware Tools & Diagnostics
- **RAM Booster:** Inspect running background processes with memory footprint and selective termination.
- **Device Cooler:** Thermal telemetry, heat warnings, and cooling action.
- **Battery Optimizer:** Normal, Power Saver, and Ultra Endurance profiles.
- **Hardware Benchmark:** Real mathematical prime calculation, RAM buffer bandwidth copy, and storage I/O test with composite scoring.
- **Sensor Diagnostics:** Enumerates all physical hardware sensors, vendors, power draw in mA, and resolutions.
- **Privacy Advisor:** Audits installed apps accessing Camera, Microphone, and Fine Location permissions.

### 6. Background Services & Quick Settings
- **`SystemMonitorService`:** Persistent notification in the notification shade displaying live CPU, RAM, and temperature with an embedded "1-Tap Boost" button.
- **`QuickBoostTileService`:** Quick Settings tile in the system notification shade providing 1-tap boost with haptic feedback.
- **`InGameOverlayService`:** Floating interactive telemetry HUD.

---

## Google Play Store Policy Compliance & Honesty Constraints
- **Zero Fabricated Metrics:** No fake "+40% FPS" claims or synthetic boost numbers.
- **Standard OS Restrictions Respected:** Android restricts force-stopping third-party apps without user confirmation or privileged APIs. HyperPulse uses standard `ActivityManager.killBackgroundProcesses` for safe memory reclamation and reserves advanced process control for opt-in Shizuku ADB/root sessions.
- **Privacy by Design:** No user data collection, external analytics trackers, or intrusive advertising.

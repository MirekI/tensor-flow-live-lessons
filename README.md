# Sensor Monitor

A small Jetpack Compose app that lists every hardware sensor on an Android phone and shows
its current value, refreshed once per second.

## Build and run

1. Install Android Studio.
2. **File > Open** and select this folder. Let Gradle sync. Android Studio creates the Gradle wrapper
   on first sync. If you build from the command line instead, run `gradle wrapper` once first.
3. Plug in a phone with USB debugging enabled, or start an emulator, then press **Run**.

Command line:

```bash
./gradlew installDebug
```

## How it works

- `SensorMonitor.kt` calls `SensorManager.getSensorList(Sensor.TYPE_ALL)` and registers a listener on
  each sensor. It keeps the latest reading per sensor.
- `MainActivity.kt` copies those readings into Compose state every second. Listeners are registered
  while the screen is visible and removed when it closes.

## Notes on the values

- Units are not shown. They depend on the sensor type (for example, m/s² for accelerometers and
  µT for magnetometers).
- Some sensors only send events when something changes, such as the proximity sensor. They show
  "Waiting for data…" until the first change.
- One-shot sensors (such as significant-motion) are listed but not streamed.
- Body sensors (heart rate) and the step counter need the permissions in the manifest. The app asks
  for them on first launch. If you deny them, those sensors stay silent.
- Some devices hide certain sensors from third-party apps, so the list can differ between phones.

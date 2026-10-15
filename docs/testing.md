# Test checklist

Use the built-in simulation first, then bridge fixtures. Live OpenSky access is a separate test after permission and credentials are in place. All phone and head-unit boxes below are still open.

## Automated checks

```bash
python -m unittest discover -s bridge -p 'test_*.py' -v
python tools/check-project.py
bash tools/check-core.sh
```

With the Android toolchain installed:

```bash
./gradlew :core:test :app:assembleDebug :app:lintDebug
```

On Windows, use `.\gradlew.bat` and `tools/check-core.ps1`. The command-line core checks do not compile Android classes. See [validation.md](validation.md) for the last results.

## Phone

- [ ] Build and install `debug` on an Android 9+ device.
- [ ] Launch with no bridge or credentials; confirm simulation is the default.
- [ ] Confirm the OpenFreeMap street map and required attribution render.
- [ ] Select Test map, disconnect the network, and confirm the schematic and fictional aircraft remain usable.
- [ ] Reconnect and select Street map; confirm the hosted style reloads.
- [ ] Test zoom, recenter, nearest, show/hide, pause/resume, and reset.
- [ ] Confirm no more than five aircraft appear and all details say SIMULATED.
- [ ] Trigger Stale test; confirm expired positions disappear, then restore them.
- [ ] Switch sources while a bridge request is pending; confirm the old response cannot replace the selected source.
- [ ] Verify portrait/landscape, day/night, large text, system bars, background/resume, and process recreation.
- [ ] Open About and confirm licenses and source links are present.

The origin and distance are fixed to downtown Austin. Do not evaluate this build as GPS tracking or driving guidance.

## Desktop Head Unit

Install the Android Auto Desktop Head Unit through Android Studio's SDK Tools. Enable Android Auto developer mode and start the head-unit server on the phone. Connect USB, authorize debugging, and keep the phone unlocked for the initial connection. Follow the [official DHU setup](https://developer.android.com/training/cars/testing/dhu) for current host settings and device-specific requirements.

```bash
adb devices
adb forward tcp:5277 tcp:5277
```

Run `desktop-head-unit.exe` on Windows or `./desktop-head-unit` on Linux/macOS from the SDK's `extras/google/auto/` directory. Accept initial host prompts on the phone. The bridge, when used, needs its own reverse tunnel:

```bash
adb reverse tcp:8765 tcp:8765
```

Those are different tunnels: port 5277 is the host connection, and port 8765 is the phone-to-bridge connection.

- [ ] Launch AeroDrive in the DHU and confirm the map surface appears.
- [ ] Check pan mode, zoom buttons, recenter, and host-supported pinch gestures.
- [ ] Test touch and rotary input using the DHU's available input modes.
- [ ] Confirm Nearest and Show/Hide are available while parked and blocked by the host while driving is simulated.
- [ ] Change simulation/source/map settings on the phone while parked; confirm shared traffic state reaches the car.
- [ ] Verify day/night styles and readable, unobstructed attribution.
- [ ] Resize/reconnect the host and test layouts with different visible/stable map areas.
- [ ] Confirm repeated disconnect/reconnect does not leave blank surfaces or crash the app.
- [ ] Confirm a stopped screen unsubscribes and a destroyed surface releases app-owned display resources only.
- [ ] Check that map updates do not cause continuous template invalidation or unstable focus.
- [ ] Confirm selected distance is explicitly a snapshot from the fixed origin, not a live navigation distance.

Use host simulation for driving restrictions. Do not troubleshoot phone controls while driving a real vehicle. A DHU pass is not Google Play approval for the aircraft layer.

## Bridge integration

- [ ] Start default fixture mode, create the reverse tunnel, and select it on the phone.
- [ ] Confirm the source is visibly a fixture, not live OpenSky.
- [ ] Stop the bridge; verify an error appears without crashing the map.
- [ ] Wait beyond the position freshness window; verify old aircraft disappear.
- [ ] Test `--scenario stale` and `--scenario empty` after restarting the bridge.
- [ ] Repeatedly switch simulation/bridge; verify cached/backoff behavior is not bypassed.
- [ ] Remove and recreate the reverse tunnel; verify recovery without an app reinstall.

## Authorized live test

- [ ] Record the actual OpenSky agreement and permitted display/caching scope outside the public repo.
- [ ] Start live mode with server-side credentials; confirm OAuth and a successful bounded query.
- [ ] Compare real returned positions and timestamps with the rendered layer.
- [ ] Verify missing altitude, callsign, heading, and position times do not produce invented details.
- [ ] Measure Austin coverage and latency at several times, with aircraft on the ground excluded.
- [ ] Check actual account credit usage and failures; do not equate an empty sky with an app error.
- [ ] Recheck provider terms and attribution before sharing any captures containing live aircraft data.

## Record a run

Copy this table into an issue or a new Markdown test log. Include logs with credentials and personal information removed.

| Field | Value |
| --- | --- |
| Commit | |
| Date | |
| Phone / Android version | |
| Android Auto / DHU version | |
| Display and input mode | |
| Map / aircraft source | |
| Build, lint, and core results | |
| Checks passed / failed | |
| Reproduction steps and logs | |

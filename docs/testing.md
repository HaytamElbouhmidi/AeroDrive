# Phone and DHU checklist

I record the APK hash, selected source-input fingerprint, device versions, profiles, and evidence for every run. All execution checks in this handoff are still pending. The automated runner does not tick these boxes.

I use fictional traffic first, then the fixture bridge. Live OpenSky testing stays separate. The Austin origin is fixed, so this build is not GPS tracking or driving guidance.

## Build and install

I run the full-project bootstrap when needed, then:

```powershell
py -3 tools/validate_aerodrive.py --install --dhu
```

I do not mark installation as a visual pass. I open the app and inspect it. The runner records its selected source-input hashes and the installed APK SHA-256 in `validation/runs/<RUN>/run.json`. I fill in `manual-checklist.json` in that same run directory.

Valid manual statuses are `pending`, `pass`, `fail`, `blocked`, and `not_applicable`. A pass needs an observation and an evidence file. A blocked or not-applicable item needs a reason and does not satisfy a required test. A combined row needs all listed subcases recorded before it can pass.

## Phone

| ID | Check | Procedure and expected observation | Status |
| --- | --- | --- | --- |
| P01 | Install and launch | Install the new debug APK, open AeroDrive, and confirm there is no launch crash. Record the APK hash and phone Android version. | Pending |
| P02 | Default simulation | Start without the bridge or credentials. Confirm fictional traffic is the default and is visibly labeled SIMULATED. | Pending |
| P03 | Street map and attribution | Confirm the Austin street map renders and its required attribution remains readable and reachable. | Pending |
| P04 | Offline test map | Select Test map, disconnect networking, and confirm the schematic and fictional aircraft remain usable. Do not call the schematic an offline road map. | Pending |
| P05 | Network recovery | Reconnect and select Street map. Confirm the hosted style reloads without an app reinstall. | Pending |
| P06 | Map and aircraft controls | Exercise zoom, recenter, nearest, show/hide, pause/resume, and reset. Record any control that does not behave as labeled. | Pending |
| P07 | Traffic limits and reference origin | Confirm no more than five aircraft appear. Verify the 25 NM boundary with fixtures and label distance as measured from the fixed Austin origin, not current GPS. | Pending |
| P08 | Stale positions | Run Stale test. Confirm expired positions disappear and can be restored. Do not infer freshness from the HTTP request time. | Pending |
| P09 | Source-switch race | Switch sources while a bridge request is pending. Confirm the old response cannot replace the newly selected source. | Pending |
| P10 | Lifecycle and layout | Check portrait, landscape, day/night, large text, system bars, background/resume, and process recreation. Keep an observation for each subcase. | Pending |
| P11 | About and notices | Open About and inspect license, attribution, and source links. Confirm no credentials are displayed or bundled. | Pending |


## Connect the Desktop Head Unit

I install the Android Auto Desktop Head Unit Emulator using Android Studio's SDK Manager. I update Android Auto on the phone, complete initial setup, enable its developer mode, and start the head-unit server. The phone stays unlocked for the initial connection. [Official DHU setup][1].

The runner prepares the ADB connection with `--dhu`. The equivalent command is:

```powershell
adb -s SERIAL forward --no-rebind tcp:5277 tcp:5277
```

The fixture bridge uses a different direction and port:

```powershell
adb -s SERIAL reverse --no-rebind tcp:8765 tcp:8765
```

The runner does not replace existing tunnels. If a port is already in use, I inspect `adb forward --list` or `adb -s SERIAL reverse --list`. I remove only a tunnel I own before recreating it:

```powershell
adb -s SERIAL forward --remove tcp:5277
adb -s SERIAL reverse --remove tcp:8765
```

These cleanup commands are not part of a normal first run. I do not remove another device's tunnel.

### Start the DHU on Windows

From the project root, I save the project path and run the DHU from its installation directory so its bundled libraries can be found:

```powershell
$project = (Get-Location).Path
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
Push-Location "$sdk\extras\google\auto"
.\desktop-head-unit.exe -c "$project\validation\dhu\touch.ini"
Pop-Location
```

I use my actual SDK location if it differs from the Windows default. I repeat with `rotary.ini` and `wide.ini`. On Linux/macOS I run `./desktop-head-unit -c /absolute/project/validation/dhu/touch.ini` from the SDK's `extras/google/auto` directory.

The optional `cluster-probe.ini` can be added as a second `-c` argument on a DHU version supporting a secondary cluster display. It is a probe, not an instruction to enable cluster support in the app. I confirm no aircraft content appears there. A lack of cluster support is recorded as unsupported rather than an observed rendering pass.

### Observe the current app

I launch AeroDrive manually before entering these commands in the DHU console. They change the host state, not AeroDrive's eligibility:

```text
restrict none
day
screenshot aerodrive-parked-day.png
night
screenshot aerodrive-parked-night.png
restrict all
screenshot aerodrive-restricted.png
restrict none
```

I wait for each state to settle and attempt the relevant controls before capturing it. I move the captures from the DHU working directory into the matching run's evidence folder. I record screen video separately when a still image cannot demonstrate an action being blocked or a surface reconnecting. The DHU supports these console commands and the profile settings used here. [DHU reference][1].

I never substitute `restrict none` as a test of driving restrictions. A setup connection, a screenshot file, or an APK install is not evidence that all behavior passed.

## DHU observations

| ID | Check | Procedure and expected observation | Status |
| --- | --- | --- | --- |
| D01 | Launch and surface | Open AeroDrive from Android Auto in the DHU. Confirm the real map surface appears rather than a blank surface or only a template. | Pending |
| D02 | Map controls | Check pan mode, zoom, recenter, and host-supported pinch. Record unsupported gestures instead of marking them passed. | Pending |
| D03 | Touch and rotary | Repeat key actions in touch and rotary profiles. Confirm focus is visible, stable, and recoverable with Back. | Pending |
| D04 | Parked-only actions | With restrict none, exercise Nearest and Show/Hide. With restrict all, attempt them again and confirm the host prevents their actions. | Pending |
| D05 | Shared state | While parked, change map, simulation, and source settings on the phone. Confirm the car receives the selected state. | Pending |
| D06 | Day/night and attribution | Run day and night. Confirm both the map and attribution remain readable and do not collide with host controls. | Pending |
| D07 | Map safe areas | Use standard and wide profiles. Check visible/stable area handling and confirm host panels do not hide required map content. | Pending |
| D08 | Reconnect | Disconnect and reconnect the DHU three times. Confirm there are no crashes, persistent blank surfaces, duplicate subscriptions, or stuck controls. | Pending |
| D09 | Lifecycle resources | Inspect logs and profiling while stopping the screen and destroying its surface. Confirm subscriptions stop and only app-owned rendering resources are released. | Pending |
| D10 | Updates and focus | Observe an active simulation for five minutes. Check template refresh behavior, UI focus, and responsiveness while aircraft positions update. | Pending |
| D11 | Distance wording | Confirm any displayed distance is a snapshot from the fixed Austin origin. No field should imply live GPS navigation or a real ETA. | Pending |
| D12 | Overlay behavior while restricted | Under restrict all, record whether aircraft remain visible, move, or can be selected. This is an observation for Google, not a policy pass for aircraft overlays. | Pending |


## Fixture bridge

I use the bridge commands documented in the original `bridge/README.md`. This add-on does not change its command-line interface or assume a new server filename.

| ID | Check | Procedure and expected observation | Status |
| --- | --- | --- | --- |
| B01 | Fixture bridge | Start the bridge in its documented default fixture mode, prepare the reverse tunnel, and select it on the phone. | Pending |
| B02 | Source identification | Confirm bridge fixtures cannot be mistaken for live OpenSky traffic. | Pending |
| B03 | Bridge unavailable | Stop the bridge. Confirm an error is visible while the map remains usable. | Pending |
| B04 | Freshness expiry | Leave the bridge unavailable past the documented freshness window. Confirm old aircraft disappear. | Pending |
| B05 | Stale and empty fixtures | Restart the bridge with its documented stale and empty scenarios. Record both outcomes. | Pending |
| B06 | Cache and backoff | Repeatedly switch simulation and bridge sources. Confirm this does not bypass cache or rate-limit backoff. | Pending |
| B07 | Tunnel recovery | Remove and recreate only the selected device's bridge tunnel. Confirm recovery without reinstalling the app. | Pending |


## Completion and release

I close the build/phone/DHU roadmap item only after a successful real Android build and all required observations have evidence. I keep fixture integration results separate from a live-data test.

A Google response closes the feedback-received item only when its scope and required changes are recorded. A rejection, unclear reply, or unsupported category keeps the affected release blocked. Even favorable conceptual feedback does not replace the applicable Google Play review or the remaining navigation and production work. [Distribution review][2].

I perform driving-state tests with the DHU or another controlled test setup. I do not operate test controls while driving a real vehicle.

[1]: https://developer.android.com/training/cars/testing/dhu
[2]: https://developer.android.com/training/cars/distribute

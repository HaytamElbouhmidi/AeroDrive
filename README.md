# AeroDrive

I'm building a navigation app that can tell me what's flying nearby and play local ATC audio.

**Status:** Planning. No working release yet.

## Planned features

- Turn-by-turn driving directions with destination search, route previews, ETA, and rerouting.
- Live nearby aircraft shown on the map without covering the route.
- Aircraft details, including callsign, operator, model, altitude, and distance when available.
- Spoken identification for a selected aircraft, with optional automatic announcements.
- Local ATC playback from authorized feeds.
- Automatic switching between relevant ATC feeds as I drive, with short audio fades between them.
- A channel lock to stay on the current feed instead of switching automatically.
- Navigation instructions taking priority over aircraft announcements and ATC audio.
- Android and Android Auto first, followed by iPhone and Apple CarPlay.

## Roadmap

This is the order I plan to build things in. I'll check tasks off as they're finished and tested. Provider permissions can be worked out alongside the prototype, but live features need those permissions before release.

### 1. Check feasibility and set up the project

- [ ] Compare navigation SDKs, flight-data providers, authorized ATC sources, and their costs and usage terms.
- [ ] Pick an initial test region and set up the Android project.
- [ ] Build a small Android Auto prototype to check the map, simulated aircraft, and supported controls.

### 2. Get navigation working

- [ ] Add destination search, route preview, and start/stop navigation.
- [ ] Add turn-by-turn directions, ETA, remaining distance, and rerouting.
- [ ] Keep the phone and Android Auto navigation sessions in sync.
- [ ] Handle reconnects and keep navigation usable with all aviation features turned off.

### 3. Add aircraft identification with test data

- [ ] Put simulated aircraft on the map and limit how many are visible at once.
- [ ] Add aircraft details and an "Identify nearby aircraft" control.
- [ ] Read the selected aircraft aloud without interrupting directions.
- [ ] Submit an early Android Auto test build for review feedback before expanding the car interface.

### 4. Connect live aircraft data

- [ ] Confirm permitted access and connect the chosen flight-data provider.
- [ ] Keep private provider credentials out of the app and public repository.
- [ ] Handle missing details, stale positions, rate limits, and connection failures.
- [ ] Add opt-in automatic announcements with cooldowns and no repeated callouts for the same aircraft.

### 5. Get ATC playback working

- [ ] Confirm permission to use the selected streams and build a small feed directory.
- [ ] Start with one working feed, then add manual feed selection and play/pause controls.
- [ ] Handle calls, navigation announcements, connection loss, and playback recovery.
- [ ] Show when no local feed is available without affecting navigation or aircraft tracking.

### 6. Add automatic ATC switching

- [ ] Choose the most relevant available feed using location and coverage information.
- [ ] Prevent repeated switching near coverage boundaries or when GPS accuracy is poor.
- [ ] Check that the next feed works before fading to it, without overlapping conversations or turn instructions.
- [ ] Add "Stay on this channel" and test what happens when a selected feed goes offline.

### 7. Test and release on Android

- [ ] Test simulated drives, physical head units, different screen sizes, and supported input methods.
- [ ] Test GPS loss, weak cellular coverage, long drives, audio interruptions, and Android Auto reconnects.
- [ ] Measure battery use, data use, and ongoing provider costs, then fix the issues found during beta testing.
- [ ] Finish privacy disclosures, attribution, provider permissions, and platform review before publishing.

### 8. Add iPhone and CarPlay

- [ ] Confirm provider support, usage rights, and CarPlay eligibility for the Apple version.
- [ ] Reuse the backend and data formats where practical, then build the iPhone and CarPlay interfaces.
- [ ] Run a separate test and review cycle before releasing on Apple platforms.

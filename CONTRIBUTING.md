# Contributing

Keep changes focused. For a bug, include what happened, what should have happened, and the smallest set of steps that reproduces it. For Android Auto issues, include the phone, host version, display size, and input method.

Run the bridge tests, `tools/check-project.py`, and either the standalone core checks or `:core:test`. App changes also need a debug build and lint run when the Android SDK is available. Say which checks were not run instead of marking the whole change tested.

Keep credentials, real provider responses, signed APKs, and personal device settings out of commits. Use the fictional fixtures for tests. Do not remove upstream attribution or add a public endpoint without checking its usage terms.

Comments should explain a decision or a non-obvious limitation, not repeat the code. Update the README or the relevant setup page when behavior, dependencies, or commands change. Do not check off phone/head-unit work based only on a unit test.

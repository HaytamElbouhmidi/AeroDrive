# Aircraft overlay platform review

**Owner:** Haytam Elbouhmidi, Mechanical Engineer  
**Checked:** October 15, 2026  
**State:** Draft prepared, not submitted. No platform response or approval is recorded.

## Decision needed

I need Google to clarify whether an optional aircraft layer belongs in the Android Auto driving experience. The choice of MapLibre and OpenSky does not resolve app-category or driver-distraction requirements.

My current source prototype is not a navigation release. It does not provide turn-by-turn directions. I do not disguise it as a complete navigation app, classify moving aircraft as driving POIs, or assume parked-only actions make the whole layer acceptable.

## Platform constraints

Google's navigation category requires actual turn-by-turn functionality under NF-1. NF-2 limits the custom surface to map content and drive-relevant information. SA-1 also makes marker motion worth asking about explicitly. I found no explicit allowance for AeroDrive's aircraft overlay. That absence is an unresolved question, not proof of either approval or categorical prohibition. [Car app quality][1].

Cluster output has narrower content limits. I do not plan to place aviation markers on the instrument cluster. [Navigation guidance][2].

The Car App Library's supported categories and templates do not themselves authorize a use case. [Car App Library][3].

## Ask before expanding the interface

I submit the text in [aircraft-overlay-question.md](review/aircraft-overlay-question.md) through Play Console Help and support as a policy/category clarification request. I ask for routing to the team responsible for Android Auto car-app review. I do not assume that support offers advance approval. [Play developer support][5].

There is no submission from this validation session. An authenticated Play Console action is still required. A public technical issue or a discussion comment would not replace a release review.

I attach a short screen recording from the actual phone/DHU run once available, the debug build's version and hash, the visible controls, and a clear description of marker updates in restricted mode. I share fictional traffic only. A draft or mockup is labeled as such and never presented as a running build.

## Route for actual car-app feedback

Internal sharing and internal testing do not receive car form-factor review. Closed testing provides non-blocking car-app feedback. Open testing and production use blocking review. I inspect the actual car-app findings rather than interpreting an accepted closed-track submission as approval of the aircraft feature. [Distribution review][4].

A debug APK installed with ADB is for local development. Before a Play submission, I need a properly signed, non-debuggable review artifact and production-appropriate host validation. This add-on does not enable release builds, introduce a review variant, or substitute an allow-all host validator. I also need a reviewable, honest implementation of the selected category.

## Evidence required from Google

| Field | Value |
| --- | --- |
| Submitted through | Not submitted |
| Case ID or review identifier | Pending |
| Submission date | Pending |
| Exact reviewed build and APK/AAB hash | Pending |
| Screenshot/video evidence | Pending real capture |
| Response date | Pending |
| Reviewed surface and driving state | Pending |
| Aircraft update/selection conditions | Pending |
| Required changes | Pending |
| Outcome | Unresolved |

I keep the original response privately and add a redacted copy to the evidence record when permitted. A response must cover the actual behavior, not just the app name or the use of the map library.

## Conservative release conditions

Until the question is resolved, the aircraft layer stays an experimental development feature. Any future production car experience must remain usable with aviation disabled. I treat a phone-only aviation layer or a genuinely parked-only layer as options to ask about, not already-approved alternatives.

A usable car release still needs real navigation, proper host trust, provider permissions for live data, privacy and attribution checks, and device validation. This document is a release checklist, not an implemented Gradle release guard. No source-level enforcement was verified in this session.

## References

[1]: https://developer.android.com/docs/quality-guidelines/car-app-quality
[2]: https://developer.android.com/training/cars/apps/navigation
[3]: https://developer.android.com/training/cars/apps/library
[4]: https://developer.android.com/training/cars/distribute
[5]: https://support.google.com/googleplay/android-developer/gethelp

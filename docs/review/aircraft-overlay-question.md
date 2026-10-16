# Draft request for Android Auto guidance

**Subject:** AeroDrive aircraft map overlay, navigation-category and driver-distraction clarification

Hello,

I'm Haytam Elbouhmidi, a mechanical engineer building AeroDrive, an open-source Android app using MapLibre Native for maps and OpenSky Network as the intended aircraft-data provider. Core features will be free, with optional one-time cosmetic purchases planned later.

I'm requesting guidance before I expand or publish the Android Auto interface. The current source prototype is a map and simulated-traffic experiment centered on Austin, Texas. It is not a finished navigation app. Turn-by-turn navigation is not implemented yet, and phone/DHU validation is still pending.

The prototype design limits the map to five fictional aircraft within 25 nautical miles of a fixed Austin origin. It provides map pan, zoom, and recenter controls. Aircraft selection and show/hide actions are intended to use parked-only host controls. I need to assess the aircraft layer itself separately from those buttons, including whether aircraft remain visible or receive position updates while driving restrictions are active.

For a future version that also implements actual turn-by-turn driving navigation, could the Android Auto review team clarify the following?

1. Would an optional, passive aircraft-position layer on the main navigation map be permitted while driving under NF-2, or is it outside the permitted drive-relevant content?
2. Would periodic position updates raise an additional issue under SA-1, even without trails, pulsing symbols, or aircraft announcements?
3. If the driving overlay is not permitted, could a navigation app offer an aviation view only while parked, with the entire aviation layer removed when driving restrictions apply? This is a proposed alternative, not a claim about implemented or approved behavior.
4. Would a single parked-only nearest-aircraft action with concise host-rendered details be acceptable, or should aircraft exploration remain exclusively on the phone?
5. What evidence and testing-track route should I use to obtain feedback on the exact implementation once the app has a reviewable navigation feature set?

I do not intend to put aircraft content on the instrument cluster, present this as flight-navigation equipment, use aircraft as driving destinations, or bypass the host's restrictions. Live-provider access and ATC playback are outside this prototype review request.

I understand that conceptual guidance does not replace a Google Play car-app review. Please identify any conditions that would affect the design and whether the aviation features need to remain phone-only.

Thank you,

Haytam Elbouhmidi  
Mechanical Engineer

## Submission preparation

This draft has not been sent. Before submitting, I update the testing status above and attach only evidence that exists: the build identifier/hash, a description of the exact displayed controls, and actual screenshots or video from the phone/DHU run. I record the resulting case ID and response in the review log.

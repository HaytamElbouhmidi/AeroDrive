package com.haytam.aerodrive.core

class SimulatedAircraftSource {
    private data class Track(val id: String, val model: String?, val center: GeoPoint,
        val radiusNm: Double, val periodSeconds: Double, val phase: Double, val altitude: Int?)
    private val tracks = listOf(
        Track("DEMO101", "A320 test fixture", Austin.airport, 5.0, 650.0, 310.0, 7_800),
        Track("DEMO202", "B737 test fixture", Austin.origin, 8.0, 950.0, 25.0, 11_000),
        Track("DEMO303", "C172 test fixture", Austin.executive, 2.8, 700.0, 190.0, 3_500),
        Track("DEMO404", "Helicopter test fixture", Austin.origin, 1.2, 550.0, 180.0, 1_800),
        Track("DEMO505", "E175 test fixture", Austin.airport, 10.0, 1100.0, 135.0, 14_000),
        Track("DEMO606", null, Austin.origin, 12.0, 1300.0, 270.0, null)
    )
    fun sample(elapsedSeconds: Double, nowMillis: Long): List<Aircraft> {
        require(elapsedSeconds.isFinite() && elapsedSeconds >= 0)
        return tracks.map { track ->
            val angle = (track.phase + elapsedSeconds / track.periodSeconds * 360) % 360
            val position = Geo.destination(track.center, angle, track.radiusNm)
            val next = Geo.destination(track.center, angle + 0.1, track.radiusNm)
            Aircraft(track.id, track.id, track.model, position, track.altitude,
                Geo.bearing(position, next), nowMillis, true)
        } + listOf(
            Aircraft("DEMO_STALE", null, null, Austin.origin, null, null, nowMillis - 90_000, true),
            Aircraft("DEMO_NO_POSITION", null, null, null, null, null, nowMillis, true)
        )
    }
}


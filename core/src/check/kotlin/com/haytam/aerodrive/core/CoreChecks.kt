package com.haytam.aerodrive.core

import kotlin.math.abs

fun runChecks() {
    var count = 0
    fun test(name: String, block: () -> Unit) {
        try {
            block()
            count++
            println("PASS $name")
        } catch (error: Throwable) {
            throw AssertionError(name, error)
        }
    }
    fun close(actual: Double, expected: Double, tolerance: Double = 0.001) {
        check(abs(actual - expected) <= tolerance) { "$actual != $expected" }
    }
    fun invalid(block: () -> Unit) {
        var rejected = false
        try { block() } catch (_: IllegalArgumentException) { rejected = true }
        check(rejected)
    }
    val now = 1_800_000_000_000L
    fun aircraft(id: String = "test", position: GeoPoint? = Austin.origin,
                 age: Long = 0, ground: Boolean = false) =
        Aircraft(id, null, null, position, null, null, now - age, true, ground)
    fun vector(): MutableList<Any?> = mutableListOf(
        "abc123", " DEMO101 ", "Simulation", now / 1000, now / 1000,
        -97.7, 30.25, 1000.0, false, 90.0, 90.0, 0.0, null, 1100.0, "1200", false, 0
    )
    val selector = AircraftSelector()
    test("identical coordinates have zero distance") { close(Geo.distanceNm(Austin.origin, Austin.origin), 0.0) }
    test("distance is symmetric") { close(Geo.distanceNm(Austin.origin, Austin.airport), Geo.distanceNm(Austin.airport, Austin.origin)) }
    test("destination preserves the requested distance") {
        close(Geo.distanceNm(Austin.origin, Geo.destination(Austin.origin, 40.0, 25.0)), 25.0)
    }
    test("bearing and destination agree") { close(Geo.bearing(Austin.origin, Geo.destination(Austin.origin, 123.0, 20.0)), 123.0) }
    test("longitude wraps at the dateline") {
        val point = Geo.destination(GeoPoint(0.0, 179.9), 90.0, 20.0)
        check(point.longitude in -180.0..-179.0)
    }
    test("invalid coordinates are rejected") {
        invalid { GeoPoint(Double.NaN, 0.0) }; invalid { GeoPoint(91.0, 0.0) }; invalid { GeoPoint(0.0, -181.0) }
    }
    test("blank identifiers are rejected") { invalid { aircraft(id = " ") } }
    test("callsigns are trimmed") { check(aircraft().copy(callsign = " TEST ").displayName == "TEST") }
    test("missing callsign uses the address") { check(aircraft("abc123").displayName == "abc123") }
    test("missing positions are excluded") { check(selector.nearby(Austin.origin, listOf(aircraft(position = null)), now).isEmpty()) }
    test("old positions are excluded") { check(selector.nearby(Austin.origin, listOf(aircraft(age = 60_001)), now).isEmpty()) }
    test("freshness boundary is inclusive") { check(selector.nearby(Austin.origin, listOf(aircraft(age = 60_000)), now).size == 1) }
    test("future positions beyond clock tolerance are excluded") {
        check(selector.nearby(Austin.origin, listOf(aircraft(age = -5_001)), now).isEmpty())
    }
    test("ground reports are excluded") { check(selector.nearby(Austin.origin, listOf(aircraft(ground = true)), now).isEmpty()) }
    test("aircraft outside the radius are excluded") {
        check(selector.nearby(Austin.origin, listOf(aircraft(position = Geo.destination(Austin.origin, 0.0, 26.0))), now).isEmpty())
    }
    test("only five aircraft are displayed") {
        val rows = (1..10).map { aircraft(it.toString(), Geo.destination(Austin.origin, 0.0, it.toDouble())) }
        val selected = selector.nearby(Austin.origin, rows.reversed(), now)
        check(selected.size == 5 && selected.first().aircraft.id == "1")
    }
    test("newest duplicate wins") {
        val selected = selector.nearby(Austin.origin, listOf(aircraft(age = 10_000), aircraft(age = 0)), now)
        check(selected.size == 1 && selected.first().aircraft.positionTimeMillis == now)
    }
    test("new ground report replaces an airborne duplicate") {
        check(selector.nearby(Austin.origin, listOf(aircraft(age = 10_000), aircraft(ground = true)), now).isEmpty())
    }
    test("invalid selector settings fail early") { invalid { AircraftSelector(limit = 0) }; invalid { AircraftSelector(radiusNm = -1.0) } }
    test("OpenSky latitude and longitude use the correct columns") {
        val result = checkNotNull(OpenSkyDecoder.decode(vector()))
        check(result.position == GeoPoint(30.25, -97.7))
    }
    test("OpenSky meters are converted to barometric feet") {
        val result = checkNotNull(OpenSkyDecoder.decode(vector()))
        check(result.altitudeFeet == 3281 && result.altitudeLabel.endsWith("barometric"))
    }
    test("OpenSky does not invent aircraft model or operator") { check(OpenSkyDecoder.decode(vector())?.model == null) }
    test("OpenSky null heading stays unknown") {
        val row = vector().apply { this[10] = null }
        check(OpenSkyDecoder.decode(row)?.headingDegrees == null)
    }
    test("heading 360 is normalized to north") { check(OpenSkyDecoder.decode(vector().apply { this[10] = 360.0 })?.headingDegrees == 0.0) }
    test("invalid OpenSky coordinates are rejected") { check(OpenSkyDecoder.decode(vector().apply { this[6] = 91.0 }) == null) }
    test("nonfinite OpenSky coordinates are rejected") { check(OpenSkyDecoder.decode(vector().apply { this[5] = Double.NaN }) == null) }
    test("missing position time is not replaced with last contact") { check(OpenSkyDecoder.decode(vector().apply { this[3] = null }) == null) }
    test("last contact does not refresh a stale position") {
        val row = vector().apply { this[3] = now / 1000 - 120 }
        val result = checkNotNull(OpenSkyDecoder.decode(row))
        check(selector.nearby(Austin.origin, listOf(result), now).isEmpty())
    }
    test("short state vectors are skipped") { check(OpenSkyDecoder.decode(listOf("abc123")) == null) }
    test("invalid transponder addresses are skipped") { check(OpenSkyDecoder.decode(vector().apply { this[0] = "not-an-address" }) == null) }
    test("null altitude is preserved") { check(OpenSkyDecoder.decode(vector().apply { this[7] = null })?.altitudeFeet == null) }
    test("ground flag must be a boolean") { check(OpenSkyDecoder.decode(vector().apply { this[8] = "false" }) == null) }
    test("fixture records remain visibly simulated") {
        val result = checkNotNull(OpenSkyDecoder.decode(vector(), simulated = true))
        check(result.simulated && result.altitudeLabel.endsWith("(simulated)"))
    }
    test("simulation is deterministic") {
        val source = SimulatedAircraftSource()
        check(source.sample(20.0, now) == source.sample(20.0, now))
    }
    test("simulated positions move") {
        val source = SimulatedAircraftSource()
        check(source.sample(0.0, now).first().position != source.sample(20.0, now).first().position)
    }
    test("pause freezes simulation time") {
        val engine = TrafficEngine(); engine.advance(10.0); engine.togglePaused(); engine.advance(20.0)
        check(engine.elapsedSeconds == 10.0)
    }
    test("negative and nonfinite simulation steps are rejected") {
        invalid { TrafficEngine().advance(-1.0) }; invalid { TrafficEngine().advance(Double.NaN) }
    }
    test("hiding aircraft also clears selection") {
        val engine = TrafficEngine(); engine.selectNearest(now); check(engine.snapshot(now).selected != null)
        engine.toggleAircraft(); check(engine.snapshot(now).visible.isEmpty() && engine.snapshot(now).selected == null)
    }
    test("stale fixture clears selection and markers") {
        val engine = TrafficEngine(); engine.selectNearest(now); engine.toggleStale()
        check(engine.snapshot(now).nearby.isEmpty() && engine.snapshot(now).selected == null)
    }
    test("switching to OpenSky does not pass simulated tracks as live") {
        val engine = TrafficEngine(); engine.setMode(FeedMode.OPENSKY)
        check(engine.snapshot(now).nearby.isEmpty() && engine.snapshot(now).mode == FeedMode.OPENSKY)
    }
    test("late live result is ignored after returning to simulation") {
        val engine = TrafficEngine(); engine.setMode(FeedMode.OPENSKY); engine.setMode(FeedMode.SIMULATION)
        engine.accept(listOf(aircraft("late-result")), "OpenSky", "Ready")
        check(engine.snapshot(now).nearby.none { it.aircraft.id == "late-result" })
    }
    test("live positions expire after an error") {
        val engine = TrafficEngine(); engine.setMode(FeedMode.OPENSKY)
        engine.accept(listOf(aircraft()), "OpenSky", "Ready"); engine.failure("Offline")
        check(engine.snapshot(now).nearby.size == 1)
        check(engine.snapshot(now + 60_001).nearby.isEmpty())
    }
    test("OpenSky mode disables simulation controls") {
        val engine = TrafficEngine(); engine.setMode(FeedMode.OPENSKY)
        engine.togglePaused(); engine.toggleStale(); engine.advance(20.0)
        check(!engine.paused && !engine.staleTest && engine.elapsedSeconds == 0.0)
    }
    test("test map and aircraft source are independent") {
        val engine = TrafficEngine(); engine.toggleTestMap(); engine.setMode(FeedMode.OPENSKY)
        check(engine.snapshot(now).testMap)
    }
    test("reset restores simulation without changing the map choice") {
        val engine = TrafficEngine(); engine.toggleTestMap(); engine.setMode(FeedMode.OPENSKY); engine.reset()
        check(engine.snapshot(now).mode == FeedMode.SIMULATION && engine.snapshot(now).testMap)
    }

    test("unknown safe areas use the surface bounds") {
        check(MapViewport.safeBounds(800, 480, null, null) == MapBounds(0, 0, 800, 480))
    }
    test("empty visible area means unknown") {
        val stable = MapBounds(300, 40, 800, 440)
        check(MapViewport.safeBounds(800, 480, MapBounds.EMPTY, stable) == stable)
    }
    test("empty stable area does not hide a known visible area") {
        val visible = MapBounds(0, 40, 800, 440)
        check(MapViewport.safeBounds(800, 480, visible, MapBounds.EMPTY) == visible)
    }
    test("two empty callbacks do not hide the map credits") {
        check(MapViewport.safeBounds(800, 480, MapBounds.EMPTY, MapBounds.EMPTY) == MapBounds(0, 0, 800, 480))
    }
    test("visible and stable areas are intersected") {
        check(MapViewport.safeBounds(800, 480, MapBounds(0, 30, 750, 460), MapBounds(250, 0, 800, 430)) ==
            MapBounds(250, 30, 750, 430))
    }
    test("safe areas are clipped to the actual surface") {
        check(MapViewport.safeBounds(800, 480, MapBounds(-20, -10, 900, 600), null) == MapBounds(0, 0, 800, 480))
    }
    test("disjoint known areas have no usable intersection") {
        check(MapViewport.safeBounds(800, 480, MapBounds(0, 0, 200, 480), MapBounds(300, 0, 800, 480)).isEmpty)
    }
    test("invalid surface dimensions have no usable area") {
        check(MapViewport.safeBounds(0, 480, null, null).isEmpty)
        check(MapViewport.safeBounds(800, -1, null, null).isEmpty)
    }
    test("surface resize recomputes the safe area") {
        val visible = MapBounds(100, 20, 1200, 700)
        check(MapViewport.safeBounds(800, 480, visible, null) == MapBounds(100, 20, 800, 480))
    }
    test("negative host focal coordinates fall back to centered zoom") {
        check(!MapViewport.hasFocus(-1f, 100f))
        check(!MapViewport.hasFocus(100f, -1f))
    }
    test("nonfinite host focal coordinates are unavailable") {
        check(!MapViewport.hasFocus(Float.NaN, 0f))
        check(!MapViewport.hasFocus(0f, Float.POSITIVE_INFINITY))
    }
    test("zero focal coordinates are valid") { check(MapViewport.hasFocus(0f, 0f)) }
    test("doubling scale adds one zoom level") { close(MapViewport.zoomDelta(2f)!!, 1.0) }
    test("halving scale removes one zoom level") { close(MapViewport.zoomDelta(0.5f)!!, -1.0) }
    test("unit scale leaves zoom unchanged") { close(MapViewport.zoomDelta(1f)!!, 0.0) }
    test("invalid scale is ignored") {
        for (scale in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            check(MapViewport.zoomDelta(scale) == null)
        }
    }
    println("$count core checks passed")
}

fun main() = runChecks()

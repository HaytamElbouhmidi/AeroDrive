package com.haytam.aerodrive.core

enum class FeedMode { SIMULATION, OPENSKY }

data class TrafficSnapshot(
    val nearby: List<NearbyAircraft>,
    val selectedId: String?,
    val showAircraft: Boolean,
    val paused: Boolean,
    val staleTest: Boolean,
    val elapsedSeconds: Double,
    val mode: FeedMode,
    val sourceLabel: String,
    val message: String,
    val testMap: Boolean
) {
    val selected: NearbyAircraft? get() = nearby.firstOrNull { it.aircraft.id == selectedId }
    val visible: List<NearbyAircraft> get() = if (showAircraft) nearby else emptyList()
}

class TrafficEngine {
    private val simulation = SimulatedAircraftSource()
    private val selector = AircraftSelector()
    private var samples = emptyList<Aircraft>()
    private var selectedId: String? = null
    private var sourceLabel = "Simulation"
    private var message = "Fictional aircraft. Fixed Austin origin."
    var mode = FeedMode.SIMULATION
        private set
    var paused = false
        private set
    var showAircraft = true
        private set
    var staleTest = false
        private set
    var testMap = false
        private set
    var elapsedSeconds = 0.0
        private set

    fun advance(seconds: Double) {
        require(seconds.isFinite() && seconds >= 0)
        if (!paused && mode == FeedMode.SIMULATION) elapsedSeconds += seconds
    }
    fun togglePaused() { if (mode == FeedMode.SIMULATION) paused = !paused }
    fun toggleAircraft() {
        showAircraft = !showAircraft
        if (!showAircraft) selectedId = null
    }
    fun toggleStale() {
        if (mode == FeedMode.SIMULATION) {
            staleTest = !staleTest
            selectedId = null
        }
    }
    fun toggleTestMap() { testMap = !testMap }
    fun setMode(next: FeedMode) {
        mode = next
        paused = false
        staleTest = false
        selectedId = null
        samples = emptyList()
        sourceLabel = if (next == FeedMode.SIMULATION) "Simulation" else "OpenSky bridge"
        message = if (next == FeedMode.SIMULATION) "Fictional aircraft. Fixed Austin origin."
            else "Waiting for the local bridge."
    }
    fun accept(aircraft: List<Aircraft>, source: String, status: String) {
        if (mode != FeedMode.OPENSKY) return
        samples = aircraft
        sourceLabel = source
        message = status
    }
    fun failure(status: String) {
        if (mode == FeedMode.OPENSKY) message = status
    }
    fun reset() {
        elapsedSeconds = 0.0
        showAircraft = true
        setMode(FeedMode.SIMULATION)
    }
    fun selectNearest(nowMillis: Long) {
        selectedId = if (showAircraft) snapshot(nowMillis).nearby.firstOrNull()?.aircraft?.id else null
    }
    fun snapshot(nowMillis: Long): TrafficSnapshot {
        val data = if (mode == FeedMode.SIMULATION) simulation.sample(elapsedSeconds, nowMillis) else samples
        val timed = if (staleTest) data.map { it.copy(positionTimeMillis = nowMillis - 90_000) } else data
        val nearby = selector.nearby(Austin.origin, timed, nowMillis)
        if (nearby.none { it.aircraft.id == selectedId }) selectedId = null
        return TrafficSnapshot(nearby, selectedId, showAircraft, paused, staleTest, elapsedSeconds,
            mode, sourceLabel, message, testMap)
    }
}

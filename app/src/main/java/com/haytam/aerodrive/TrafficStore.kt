package com.haytam.aerodrive

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.haytam.aerodrive.core.FeedMode
import com.haytam.aerodrive.core.TrafficEngine
import com.haytam.aerodrive.core.TrafficSnapshot
import com.haytam.aerodrive.data.BridgeException
import com.haytam.aerodrive.data.OpenSkyBridgeClient
import java.util.concurrent.Executors

class TrafficStore {
    private val engine = TrafficEngine()
    private val bridge = OpenSkyBridgeClient()
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "opensky-bridge").apply { isDaemon = true }
    }
    private val observers = linkedSetOf<(TrafficSnapshot) -> Unit>()
    private var lastTick = 0L
    private var nextPoll = 0L
    private var requestRunning = false
    private var generation = 0
    val snapshot: TrafficSnapshot get() = engine.snapshot(System.currentTimeMillis())

    private val ticker = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            engine.advance((now - lastTick).coerceIn(0, 2_000) / 1_000.0)
            lastTick = now
            publish()
            poll(now)
            if (observers.isNotEmpty()) handler.postDelayed(this, 500)
        }
    }

    fun observe(observer: (TrafficSnapshot) -> Unit) {
        checkMainThread()
        val first = observers.isEmpty()
        observers.add(observer)
        observer(snapshot)
        if (first) {
            lastTick = SystemClock.elapsedRealtime()
            handler.post(ticker)
        }
    }
    fun remove(observer: (TrafficSnapshot) -> Unit) {
        checkMainThread()
        observers.remove(observer)
        if (observers.isEmpty()) handler.removeCallbacks(ticker)
    }
    fun nearest() = change { selectNearest(System.currentTimeMillis()) }
    fun toggleAircraft() = change { toggleAircraft() }
    fun togglePaused() = change { togglePaused() }
    fun toggleStale() = change { toggleStale() }
    fun toggleTestMap() = change { toggleTestMap() }
    fun toggleSource() {
        generation++
        change { setMode(if (mode == FeedMode.SIMULATION) FeedMode.OPENSKY else FeedMode.SIMULATION) }
    }
    fun reset() {
        generation++
        change { reset() }
    }
    private fun change(action: TrafficEngine.() -> Unit) {
        checkMainThread()
        engine.action()
        lastTick = SystemClock.elapsedRealtime()
        publish()
    }
    private fun publish() {
        val current = snapshot
        observers.toList().forEach { it(current) }
    }
    private fun poll(now: Long) {
        if (engine.mode != FeedMode.OPENSKY || requestRunning || now < nextPoll || observers.isEmpty()) return
        requestRunning = true
        val requestGeneration = generation
        worker.execute {
            val result = runCatching { bridge.fetch() }
            handler.post {
                requestRunning = false
                val retrySeconds = result.getOrNull()?.retryAfterSeconds
                    ?: (result.exceptionOrNull() as? BridgeException)?.retryAfterSeconds ?: 30
                // Toggling the source must not bypass a server cooldown.
                nextPoll = SystemClock.elapsedRealtime() + retrySeconds.coerceIn(30, 604_800) * 1_000
                if (requestGeneration != generation || engine.mode != FeedMode.OPENSKY) return@post
                result.fold(
                    onSuccess = { engine.accept(it.aircraft, it.sourceLabel, it.message) },
                    onFailure = { error ->
                        engine.failure((error as? BridgeException)?.message
                            ?: "Bridge unavailable. Check the server and adb reverse.")
                    }
                )
                publish()
            }
        }
    }
    private fun checkMainThread() = check(Looper.myLooper() == Looper.getMainLooper())
}

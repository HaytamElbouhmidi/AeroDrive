package com.haytam.aerodrive.map

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.haytam.aerodrive.BuildConfig
import com.haytam.aerodrive.R
import com.haytam.aerodrive.core.MapViewport
import com.haytam.aerodrive.core.Austin
import com.haytam.aerodrive.core.TrafficSnapshot
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

class MapPanel(context: Context, private val car: Boolean, savedState: Bundle? = null) {
    val view = FrameLayout(context)
    private val options = MapLibreMapOptions.createFromAttributes(context)
        .textureMode(car)
        .compassEnabled(!car).logoEnabled(!car).attributionEnabled(!car)
        .rotateGesturesEnabled(false).tiltGesturesEnabled(false)
    private val mapView = MapView(context, options)
    private val credits = TextView(context).apply {
        textSize = 12f
        setTextColor(context.getColor(R.color.app_text))
        setBackgroundColor(context.getColor(R.color.app_surface))
        setPadding(dp(8), dp(4), dp(8), dp(4))
        maxLines = 3
    }
    private val handler = Handler(Looper.getMainLooper())
    private val layer = AircraftLayer()
    private var map: MapLibreMap? = null
    private var state: TrafficSnapshot? = null
    private var night = false
    private var testMap = false
    private var fallback = false
    private var destroyed = false
    private var started = false
    private var resumed = false
    private var styleGeneration = 0
    private var safeArea: Rect? = null
    private val styleTimeout = Runnable { useFallback() }

    init {
        view.addView(mapView, FrameLayout.LayoutParams(-1, -1))
        view.addView(credits, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.START))
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateSafeArea() }
        mapView.onCreate(savedState)
        mapView.addOnDidFailLoadingMapListener {
            handler.post { if (!destroyed && !testMap && !fallback) useFallback() }
        }
        mapView.getMapAsync { ready ->
            if (!destroyed) {
                map = ready
                ready.setMinZoomPreference(7.0)
                ready.setMaxZoomPreference(17.0)
                loadStyle()
                if (savedState == null) recenter()
                updateSafeArea()
            }
        }
    }

    fun update(snapshot: TrafficSnapshot) {
        state = snapshot
        if (snapshot.testMap != testMap) {
            testMap = snapshot.testMap
            loadStyle()
        }
        layer.update(snapshot)
    }
    fun night(enabled: Boolean) {
        if (night == enabled) return
        night = enabled
        loadStyle()
    }
    private fun loadStyle() {
        val current = map ?: return
        if (destroyed) return
        handler.removeCallbacks(styleTimeout)
        fallback = false
        layer.clear()
        val generation = ++styleGeneration
        val builder = if (testMap) localStyle() else Style.Builder().fromUri(
            if (night) BuildConfig.MAP_NIGHT_STYLE_URL else BuildConfig.MAP_STYLE_URL
        )
        credits.text = if (testMap) "Austin test layout | Not a road map" else BuildConfig.MAP_ATTRIBUTION
        if (!testMap) handler.postDelayed(styleTimeout, 10_000)
        current.setStyle(builder) { style ->
            if (!destroyed && generation == styleGeneration) {
                handler.removeCallbacks(styleTimeout)
                layer.attach(style)
                state?.let(layer::update)
            }
        }
    }
    private fun localStyle(): Style.Builder {
        val name = if (night) "austin-night.json" else "austin-day.json"
        return Style.Builder().fromJson(view.context.assets.open(name).bufferedReader().use { it.readText() })
    }
    private fun useFallback() {
        val current = map ?: return
        if (destroyed || testMap || fallback) return
        fallback = true
        handler.removeCallbacks(styleTimeout)
        layer.clear()
        val generation = ++styleGeneration
        credits.text = "Map unavailable | Austin test layout | Not a road map"
        current.setStyle(localStyle()) { style ->
            if (!destroyed && generation == styleGeneration) {
                layer.attach(style)
                state?.let(layer::update)
            }
        }
    }
    fun zoom(delta: Double, focus: PointF? = null) {
        val current = map ?: return
        if (!delta.isFinite()) return
        val zoom = (current.cameraPosition.zoom + delta).coerceIn(7.0, 17.0)
        if (focus == null) current.moveCamera(CameraUpdateFactory.zoomTo(zoom))
        else current.setZoom(zoom, focus, 0)
    }
    fun scale(focusX: Float, focusY: Float, factor: Float) {
        val delta = MapViewport.zoomDelta(factor) ?: return
        val focus = if (MapViewport.hasFocus(focusX, focusY)) PointF(focusX, focusY) else null
        zoom(delta, focus)
    }
    fun scroll(x: Float, y: Float) {
        if (x.isFinite() && y.isFinite()) map?.scrollBy(-x, -y, 0)
    }
    fun recenter() {
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
            LatLng(Austin.origin.latitude, Austin.origin.longitude), 10.0
        ))
    }
    fun safeArea(area: Rect) {
        safeArea = Rect(area)
        updateSafeArea()
    }
    private fun updateSafeArea() {
        val area = safeArea ?: return
        if (area.isEmpty) {
            map?.setPadding(0, 0, 0, 0)
            credits.visibility = View.INVISIBLE
            return
        }
        credits.visibility = View.VISIBLE
        val right = (view.width - area.right).coerceAtLeast(0)
        val bottom = (view.height - area.bottom).coerceAtLeast(0)
        map?.setPadding(area.left, area.top, right, bottom)
        val current = credits.layoutParams as FrameLayout.LayoutParams
        if (current.width != area.width() || current.leftMargin != area.left || current.bottomMargin != bottom) {
            credits.layoutParams = FrameLayout.LayoutParams(area.width(), -2, Gravity.BOTTOM or Gravity.START).apply {
                leftMargin = area.left
                bottomMargin = bottom
            }
        }
    }
    private fun dp(value: Int) = (value * view.resources.displayMetrics.density).toInt()

    fun start() { if (!started && !destroyed) { mapView.onStart(); started = true } }
    fun resume() { if (!resumed && !destroyed) { mapView.onResume(); resumed = true } }
    fun pause() { if (resumed) { mapView.onPause(); resumed = false } }
    fun stop() { pause(); if (started) { mapView.onStop(); started = false } }
    fun saveState(out: Bundle) { mapView.onSaveInstanceState(out) }
    fun lowMemory() { if (!destroyed) mapView.onLowMemory() }
    fun destroy() {
        if (destroyed) return
        stop()
        destroyed = true
        handler.removeCallbacksAndMessages(null)
        layer.clear()
        map = null
        mapView.onDestroy()
    }
}

package com.haytam.aerodrive.car

import android.app.Presentation
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.haytam.aerodrive.core.TrafficSnapshot
import com.haytam.aerodrive.map.MapPanel

// The virtual-display approach follows MapLibre's Android Auto sample.
// Its MIT notice is in third_party/maplibre-android-auto-sample.txt.
class CarMap(private val context: CarContext) : SurfaceCallback {
    private var container: SurfaceContainer? = null
    private var display: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var panel: MapPanel? = null
    private var state: TrafficSnapshot? = null
    private var visible: Rect? = null
    private var stable: Rect? = null
    private var started = false
    private var night = false
    private var destroyed = false

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        if (destroyed) return
        val surface = surfaceContainer.surface ?: return
        if (!surface.isValid || surfaceContainer.width <= 0 || surfaceContainer.height <= 0) return
        releaseDisplay()
        container = surfaceContainer
        visible = null
        stable = null
        try {
            val manager = context.getSystemService(DisplayManager::class.java)
            val target = checkNotNull(manager.createVirtualDisplay(
                "AeroDrive map", surfaceContainer.width, surfaceContainer.height,
                surfaceContainer.dpi.coerceAtLeast(160), surface, 0
            ))
            display = target
            val window = Presentation(context, target.display)
            presentation = window
            val map = MapPanel(window.context, car = true)
            panel = map
            window.setContentView(map.view)
            window.show()
            map.night(night)
            state?.let(map::update)
            map.view.post { updateSafeArea() }
            if (started) { map.start(); map.resume() }
        } catch (error: RuntimeException) {
            Log.e("CarMap", "Could not attach the map surface", error)
            releaseDisplay()
        }
    }
    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        releaseDisplay()
    }
    override fun onVisibleAreaChanged(visibleArea: Rect) {
        visible = Rect(visibleArea)
        updateSafeArea()
    }
    override fun onStableAreaChanged(stableArea: Rect) {
        stable = Rect(stableArea)
        updateSafeArea()
    }
    private fun updateSafeArea() {
        val current = container ?: return
        val area = Rect(0, 0, current.width, current.height)
        visible?.let { if (!area.intersect(it)) area.setEmpty() }
        stable?.let { if (!area.intersect(it)) area.setEmpty() }
        panel?.safeArea(area)
    }
    override fun onScroll(distanceX: Float, distanceY: Float) { panel?.scroll(distanceX, distanceY) }
    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        panel?.scale(focusX, focusY, scaleFactor)
    }
    fun update(snapshot: TrafficSnapshot) { state = snapshot; panel?.update(snapshot) }
    fun zoom(delta: Double) { panel?.zoom(delta) }
    fun recenter() { panel?.recenter() }
    fun night(enabled: Boolean) { night = enabled; panel?.night(enabled) }
    fun start() { started = true; panel?.start(); panel?.resume() }
    fun stop() { started = false; panel?.stop() }
    fun destroy() { destroyed = true; stop(); releaseDisplay() }
    private fun releaseDisplay() {
        panel?.destroy()
        panel = null
        try { presentation?.dismiss() } catch (error: RuntimeException) {
            Log.w("CarMap", "Presentation already detached", error)
        }
        presentation = null
        display?.release()
        display = null
        // The host owns the Surface. Only the display and map belong to this app.
        container = null
    }
}

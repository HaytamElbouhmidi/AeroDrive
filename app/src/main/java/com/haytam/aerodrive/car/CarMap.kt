package com.haytam.aerodrive.car

import android.app.Presentation
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.haytam.aerodrive.core.MapBounds
import com.haytam.aerodrive.core.MapViewport
import com.haytam.aerodrive.core.TrafficSnapshot
import com.haytam.aerodrive.map.MapPanel

// MapLibre's car sample is a reference, not a drop-in renderer.
// This implementation uses a Presentation on a virtual display.
class CarMap(private val context: CarContext) : SurfaceCallback {
    private var container: SurfaceContainer? = null
    private var display: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var panel: MapPanel? = null
    private var state: TrafficSnapshot? = null
    private var visible: MapBounds? = null
    private var stable: MapBounds? = null
    private var started = false
    private var night = false
    private var destroyed = false

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val surface = surfaceContainer.surface ?: return
        if (destroyed || !surface.isValid || surfaceContainer.width <= 0 || surfaceContainer.height <= 0) {
            if (surface === container?.surface) releaseDisplay() else surface.release()
            return
        }
        // A callback can resize the same Surface instance.
        releaseDisplay(releaseSurface = surface !== container?.surface)
        container = surfaceContainer
        visible = null
        stable = null
        try {
            val manager = checkNotNull(context.getSystemService(DisplayManager::class.java)) {
                "Display service unavailable"
            }
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
            if (started) {
                map.start()
                map.resume()
            }
        } catch (error: RuntimeException) {
            Log.e("CarMap", "Could not attach the map surface", error)
            releaseDisplay()
        }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        val activeSurface = container?.surface
        releaseDisplay()
        // The destroy callback can contain a separate local Surface handle.
        surfaceContainer.surface?.takeUnless { it === activeSurface }?.release()
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        visible = visibleArea.toBounds()
        updateSafeArea()
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        stable = stableArea.toBounds()
        updateSafeArea()
    }

    private fun Rect.toBounds() = MapBounds(left, top, right, bottom)

    private fun updateSafeArea() {
        val current = container ?: return
        val area = MapViewport.safeBounds(current.width, current.height, visible, stable)
        panel?.safeArea(Rect(area.left, area.top, area.right, area.bottom))
    }

    override fun onScroll(distanceX: Float, distanceY: Float) {
        panel?.scroll(distanceX, distanceY)
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        panel?.scale(focusX, focusY, scaleFactor)
    }

    fun update(snapshot: TrafficSnapshot) {
        state = snapshot
        panel?.update(snapshot)
    }

    fun zoom(delta: Double) { panel?.zoom(delta) }
    fun recenter() { panel?.recenter() }

    fun night(enabled: Boolean) {
        night = enabled
        panel?.night(enabled)
    }

    fun start() {
        started = true
        panel?.start()
        panel?.resume()
    }

    fun stop() {
        started = false
        panel?.stop()
    }

    fun destroy() {
        destroyed = true
        started = false
        releaseDisplay()
    }

    private fun releaseDisplay(releaseSurface: Boolean = true) {
        val oldPanel = panel
        val oldPresentation = presentation
        val oldDisplay = display
        val oldSurface = container?.surface
        panel = null
        presentation = null
        display = null
        container = null
        visible = null
        stable = null

        // Cleanup must continue even when one of the resources is already detached.
        dispose("map") { oldPanel?.destroy() }
        dispose("presentation") { oldPresentation?.dismiss() }
        dispose("display") { oldDisplay?.release() }
        if (releaseSurface) dispose("surface") { oldSurface?.release() }
    }

    private fun dispose(name: String, close: () -> Unit) {
        try {
            close()
        } catch (error: RuntimeException) {
            Log.w("CarMap", "Could not release $name", error)
        }
    }
}

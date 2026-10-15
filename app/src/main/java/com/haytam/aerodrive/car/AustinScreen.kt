package com.haytam.aerodrive.car

import android.content.res.Configuration
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.haytam.aerodrive.AeroDriveApp
import com.haytam.aerodrive.R
import com.haytam.aerodrive.core.TrafficSnapshot
import java.util.Locale

class AustinScreen(context: CarContext) : Screen(context), DefaultLifecycleObserver {
    private val store = (context.applicationContext as AeroDriveApp).traffic
    private val map = CarMap(context)
    private var state = store.snapshot
    private var selection = state.selected
    private fun key(value: TrafficSnapshot) = listOf(
        value.selectedId, value.showAircraft, value.sourceLabel, value.message, value.staleTest, value.testMap, value.visible.isEmpty()
    )
    private var templateKey = key(state)
    private val observer: (TrafficSnapshot) -> Unit = { snapshot ->
        state = snapshot
        map.update(snapshot)
        val nextKey = key(snapshot)
        if (nextKey != templateKey) {
            templateKey = nextKey
            selection = snapshot.selected
            invalidate()
        }
    }
    init {
        lifecycle.addObserver(this)
        map.night(carContext.isDarkMode)
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(map)
    }
    override fun onStart(owner: LifecycleOwner) { map.start(); store.observe(observer) }
    override fun onStop(owner: LifecycleOwner) { store.remove(observer); map.stop() }
    override fun onDestroy(owner: LifecycleOwner) {
        store.remove(observer)
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(null)
        map.destroy()
    }
    fun configurationChanged(configuration: Configuration) {
        map.night(carContext.isDarkMode)
        invalidate()
    }
    override fun onGetTemplate(): Template {
        val pane = Pane.Builder().addRow(Row.Builder()
            .setTitle("Austin | ${state.sourceLabel}")
            .addText("Map test only. No route or ATC audio.")
            .addText(if (state.showAircraft && state.visible.isEmpty()) "No fresh aircraft positions in range." else state.message).build())
        selection?.let { selected ->
            val aircraft = selected.aircraft
            pane.addRow(Row.Builder()
                .setTitle("${aircraft.displayName}${if (aircraft.simulated) " | SIMULATED" else ""}")
                .addText(aircraft.altitudeLabel)
                .addText(String.format(Locale.US, "%.1f nm from fixed origin at selection", selected.distanceNm))
                .build())
        }
        pane.addAction(Action.Builder().setTitle("Nearest")
            .setOnClickListener(ParkedOnlyOnClickListener.create {
                store.nearest()
                selection = store.snapshot.selected
                invalidate()
            }).build())
        pane.addAction(Action.Builder().setTitle(if (state.showAircraft) "Hide aircraft" else "Show aircraft")
            .setOnClickListener(ParkedOnlyOnClickListener.create { store.toggleAircraft() }).build())
        val content = PaneTemplate.Builder(pane.build()).setTitle("AeroDrive")
            .setHeaderAction(Action.APP_ICON).build()
        val controls = ActionStrip.Builder().addAction(Action.PAN)
            .addAction(mapAction(R.drawable.ic_plus) { map.zoom(1.0) })
            .addAction(mapAction(R.drawable.ic_minus) { map.zoom(-1.0) })
            .addAction(mapAction(R.drawable.ic_center) { map.recenter() }).build()
        return MapWithContentTemplate.Builder().setContentTemplate(content)
            .setMapController(MapController.Builder().setMapActionStrip(controls).build()).build()
    }
    private fun mapAction(icon: Int, click: () -> Unit) = Action.Builder()
        .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, icon)).build())
        .setOnClickListener { click() }.build()
}

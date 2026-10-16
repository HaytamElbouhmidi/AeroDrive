package com.haytam.aerodrive.phone

import android.app.Activity
import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.haytam.aerodrive.AeroDriveApp
import com.haytam.aerodrive.BuildConfig
import com.haytam.aerodrive.R
import com.haytam.aerodrive.core.FeedMode
import com.haytam.aerodrive.core.TrafficSnapshot
import com.haytam.aerodrive.map.MapPanel
import java.util.Locale

class MainActivity : Activity() {
    private val store get() = (application as AeroDriveApp).traffic
    private lateinit var map: MapPanel
    private lateinit var status: TextView
    private lateinit var aircraft: Button
    private lateinit var pause: Button
    private lateinit var stale: Button
    private lateinit var source: Button
    private lateinit var testMap: Button
    private val observer: (TrafficSnapshot) -> Unit = { render(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        map = MapPanel(this, car = false, savedState = savedInstanceState?.getBundle("map"))
        map.night(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.app_background))
        }
        val body = LinearLayout(this).apply {
            orientation = if (landscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        }
        root.addView(TextView(this).apply {
            text = "AeroDrive | Austin | ${BuildConfig.VERSION_NAME}"
            textSize = 20f
            setPadding(dp(12), dp(8), dp(12), dp(8))
        })
        val mapLayout = if (landscape) LinearLayout.LayoutParams(0, -1, 0.60f)
            else LinearLayout.LayoutParams(-1, 0, 0.55f)
        body.addView(map.view, mapLayout)
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.app_surface))
        }
        status = TextView(this).apply {
            textSize = 14f
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }
        controls.addView(status)
        fun row(vararg buttons: Button) {
            controls.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                buttons.forEach { addView(it, LinearLayout.LayoutParams(0, -2, 1f)) }
            })
        }
        aircraft = button("Hide aircraft") { store.toggleAircraft() }
        pause = button("Pause") { store.togglePaused() }
        stale = button("Stale test") { store.toggleStale() }
        source = button("Use OpenSky bridge") {
            if (store.snapshot.mode == FeedMode.OPENSKY) store.toggleSource()
            else AlertDialog.Builder(this).setTitle("Use the local OpenSky bridge?")
                .setMessage("The bridge must be running on the computer connected over USB. " +
                    "Fixture mode uses fictional data. Live mode needs OpenSky permission and server-side credentials. " +
                    "Change this only while parked.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Connect") { _, _ -> store.toggleSource() }.show()
        }
        testMap = button("Test map") { store.toggleTestMap() }
        row(button("Nearest") { store.nearest() }, aircraft, pause)
        row(button("Zoom +") { map.zoom(1.0) }, button("Zoom -") { map.zoom(-1.0) }, button("Center") { map.recenter() })
        row(source, testMap)
        row(stale, button("Reset") {
            store.reset()
            map.recenter()
        }, button("About") { showAbout() })
        controls.addView(TextView(this).apply {
            text = "Fixed test origin, not your location. No directions or ATC."
            textSize = 12f
            setPadding(dp(12), 0, dp(12), dp(8))
        })
        val controlsLayout = if (landscape) LinearLayout.LayoutParams(0, -1, 0.40f)
            else LinearLayout.LayoutParams(-1, 0, 0.45f)
        body.addView(ScrollView(this).apply { addView(controls) }, controlsLayout)
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
    }
    private fun button(title: String, click: () -> Unit) = Button(this).apply {
        text = title
        isAllCaps = false
        setOnClickListener { click() }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun render(snapshot: TrafficSnapshot) {
        map.update(snapshot)
        aircraft.text = if (snapshot.showAircraft) "Hide aircraft" else "Show aircraft"
        pause.text = if (snapshot.paused) "Resume" else "Pause"
        pause.isEnabled = snapshot.mode == FeedMode.SIMULATION
        stale.isEnabled = snapshot.mode == FeedMode.SIMULATION
        stale.text = if (snapshot.staleTest) "Restore data" else "Stale test"
        source.text = if (snapshot.mode == FeedMode.OPENSKY) "Use simulation" else "Use OpenSky bridge"
        testMap.text = if (snapshot.testMap) "Street map" else "Test map"
        val selected = snapshot.selected
        val details = selected?.let {
            val plane = it.aircraft
            "${plane.displayName}${if (plane.simulated) " | SIMULATED" else ""} | " +
                plane.altitudeLabel + String.format(Locale.US, " | %.1f nm", it.distanceNm)
        } ?: "${snapshot.visible.size} aircraft shown. Select Nearest for details."
        status.text = "${snapshot.sourceLabel} | $details\n" +
            if (snapshot.staleTest) "Old positions removed." else snapshot.message
    }
    private fun showAbout() {
        val text = assets.open("notices.txt").bufferedReader().use { it.readText() }
        AlertDialog.Builder(this).setTitle("AeroDrive ${BuildConfig.VERSION_NAME}")
            .setMessage(text).setPositiveButton("Close", null).show()
    }
    override fun onStart() {
        super.onStart()
        map.start()
        store.observe(observer)
    }
    override fun onResume() {
        super.onResume()
        map.resume()
    }
    override fun onPause() {
        map.pause()
        super.onPause()
    }
    override fun onStop() {
        store.remove(observer)
        map.stop()
        super.onStop()
    }
    override fun onDestroy() {
        map.destroy()
        super.onDestroy()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle("map", Bundle().also(map::saveState))
        super.onSaveInstanceState(outState)
    }
    override fun onLowMemory() {
        super.onLowMemory()
        if (::map.isInitialized) map.lowMemory()
    }
}

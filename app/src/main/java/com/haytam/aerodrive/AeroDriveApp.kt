package com.haytam.aerodrive

import android.app.Application
import org.maplibre.android.MapLibre

class AeroDriveApp : Application() {
    val traffic by lazy { TrafficStore() }

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }
}

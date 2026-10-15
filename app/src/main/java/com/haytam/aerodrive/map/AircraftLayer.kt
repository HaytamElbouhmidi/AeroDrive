package com.haytam.aerodrive.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.haytam.aerodrive.core.Austin
import com.haytam.aerodrive.core.TrafficSnapshot
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

class AircraftLayer {
    private var source: GeoJsonSource? = null
    private var lastData: String? = null

    fun attach(style: Style) {
        lastData = null
        source = GeoJsonSource("aerodrive-aircraft", collection(JSONArray())).also(style::addSource)
        style.addImage("aerodrive-plane", icon(0xFF007A92.toInt(), true))
        style.addImage("aerodrive-selected", icon(0xFFD45B00.toInt(), true))
        style.addImage("aerodrive-unknown", icon(0xFF007A92.toInt(), false))
        style.addImage("aerodrive-unknown-selected", icon(0xFFD45B00.toInt(), false))
        style.addLayer(SymbolLayer("aerodrive-aircraft-icons", "aerodrive-aircraft").withProperties(
            iconImage(get("icon")), iconRotate(get("heading")), iconSize(0.7f),
            iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
            iconAllowOverlap(true), iconIgnorePlacement(true)
        ))
        val origin = point(Austin.origin.longitude, Austin.origin.latitude, JSONObject())
        style.addSource(GeoJsonSource("aerodrive-origin", collection(JSONArray().put(origin))))
        style.addLayer(CircleLayer("aerodrive-origin-dot", "aerodrive-origin").withProperties(
            circleRadius(6f), circleColor(Color.BLACK), circleStrokeColor(Color.WHITE), circleStrokeWidth(2f)
        ))
    }
    fun update(snapshot: TrafficSnapshot) {
        val features = JSONArray()
        snapshot.visible.forEach { nearby ->
            val aircraft = nearby.aircraft
            val location = aircraft.position ?: return@forEach
            val selected = aircraft.id == snapshot.selectedId
            val icon = if (aircraft.headingDegrees == null) {
                if (selected) "aerodrive-unknown-selected" else "aerodrive-unknown"
            } else if (selected) "aerodrive-selected" else "aerodrive-plane"
            features.put(point(location.longitude, location.latitude, JSONObject()
                .put("icon", icon).put("heading", aircraft.headingDegrees ?: 0.0)))
        }
        val json = collection(features)
        if (json != lastData) {
            source?.setGeoJson(json)
            lastData = json
        }
    }
    fun clear() { source = null; lastData = null }
    private fun point(lon: Double, lat: Double, properties: JSONObject) = JSONObject()
        .put("type", "Feature").put("properties", properties)
        .put("geometry", JSONObject().put("type", "Point")
            .put("coordinates", JSONArray().put(lon).put(lat)))
    private fun collection(features: JSONArray) = JSONObject()
        .put("type", "FeatureCollection").put("features", features).toString()

    private fun icon(color: Int, headingKnown: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (!headingKnown) {
            paint.color = Color.WHITE
            canvas.drawCircle(32f, 32f, 15f, paint)
            paint.color = color
            canvas.drawCircle(32f, 32f, 11f, paint)
            return bitmap
        }
        val outline = Path().apply {
            moveTo(32f, 5f); lineTo(37f, 26f); lineTo(58f, 39f)
            lineTo(58f, 46f); lineTo(37f, 39f); lineTo(36f, 50f)
            lineTo(44f, 56f); lineTo(44f, 60f); lineTo(32f, 56f)
            lineTo(20f, 60f); lineTo(20f, 56f); lineTo(28f, 50f)
            lineTo(27f, 39f); lineTo(6f, 46f); lineTo(6f, 39f)
            lineTo(27f, 26f); close()
        }
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawPath(outline, paint)
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawPath(outline, paint)
        return bitmap
    }
}

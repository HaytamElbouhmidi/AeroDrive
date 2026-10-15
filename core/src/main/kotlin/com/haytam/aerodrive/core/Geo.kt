package com.haytam.aerodrive.core

import kotlin.math.*

data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}
object Geo {
    private const val EARTH_RADIUS_METERS = 6_371_008.8
    const val METERS_PER_NM = 1_852.0

    fun distanceNm(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = (sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2))
            .coerceIn(0.0, 1.0)
        return 2 * EARTH_RADIUS_METERS * atan2(sqrt(h), sqrt(1 - h)) / METERS_PER_NM
    }

    fun destination(start: GeoPoint, bearingDegrees: Double, distanceNm: Double): GeoPoint {
        require(bearingDegrees.isFinite() && distanceNm.isFinite() && distanceNm >= 0)
        val angle = distanceNm * METERS_PER_NM / EARTH_RADIUS_METERS
        val bearing = Math.toRadians(bearingDegrees)
        val latitude = Math.toRadians(start.latitude)
        val longitude = Math.toRadians(start.longitude)
        val endLat = asin((sin(latitude) * cos(angle) + cos(latitude) * sin(angle) * cos(bearing))
            .coerceIn(-1.0, 1.0))
        val endLon = longitude + atan2(sin(bearing) * sin(angle) * cos(latitude),
            cos(angle) - sin(latitude) * sin(endLat))
        return GeoPoint(Math.toDegrees(endLat), (Math.toDegrees(endLon) + 540) % 360 - 180)
    }

    fun bearing(a: GeoPoint, b: GeoPoint): Double {
        val p1 = Math.toRadians(a.latitude)
        val p2 = Math.toRadians(b.latitude)
        val dl = Math.toRadians(b.longitude - a.longitude)
        return (Math.toDegrees(atan2(sin(dl) * cos(p2),
            cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl))) + 360) % 360
    }
}
object Austin {
    val origin = GeoPoint(30.2672, -97.7431)
    val airport = GeoPoint(30.1975, -97.6664)
    val executive = GeoPoint(30.3975, -97.5664)
    const val RADIUS_NM = 25.0
    const val MAX_AIRCRAFT = 5
}

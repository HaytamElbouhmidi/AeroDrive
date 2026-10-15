package com.haytam.aerodrive.core

import kotlin.math.roundToInt

data class Aircraft(
    val id: String,
    val callsign: String?,
    val model: String?,
    val position: GeoPoint?,
    val altitudeFeet: Int?,
    val headingDegrees: Double?,
    val positionTimeMillis: Long,
    val simulated: Boolean,
    val onGround: Boolean = false
) {
    init {
        require(id.isNotBlank())
        require(headingDegrees == null ||
            (headingDegrees.isFinite() && headingDegrees >= 0 && headingDegrees < 360))
    }
    val displayName: String get() = callsign?.trim()?.takeIf { it.isNotEmpty() } ?: id
    val altitudeLabel: String get() = altitudeFeet?.let {
        "$it ft ${if (simulated) "(simulated)" else "barometric"}"
    } ?: "Altitude unavailable"
}

data class NearbyAircraft(val aircraft: Aircraft, val distanceNm: Double)

class AircraftSelector(
    private val radiusNm: Double = Austin.RADIUS_NM,
    private val limit: Int = Austin.MAX_AIRCRAFT,
    private val staleAfterMillis: Long = 60_000L
) {
    init { require(radiusNm.isFinite() && radiusNm > 0 && limit > 0 && staleAfterMillis > 0) }

    fun nearby(origin: GeoPoint, aircraft: List<Aircraft>, nowMillis: Long): List<NearbyAircraft> =
        aircraft.asSequence()
            .filter { it.positionTimeMillis in (nowMillis - staleAfterMillis)..(nowMillis + 5_000) }
            .sortedByDescending { it.positionTimeMillis }
            .distinctBy { it.id }
            .filterNot { it.onGround }
            .mapNotNull { item -> item.position?.let { NearbyAircraft(item, Geo.distanceNm(origin, it)) } }
            .filter { it.distanceNm <= radiusNm }
            .sortedWith(compareBy<NearbyAircraft> { it.distanceNm }.thenBy { it.aircraft.id })
            .take(limit)
            .toList()
}

object OpenSkyDecoder {
    private val address = Regex("[0-9a-fA-F]{6}")

    fun decode(row: List<Any?>, simulated: Boolean = false): Aircraft? {
        if (row.size < 17) return null
        val id = (row[0] as? String)?.trim()?.takeIf { address.matches(it) }?.lowercase() ?: return null
        fun number(index: Int) = (row[index] as? Number)?.toDouble()?.takeIf { it.isFinite() }
        val latitude = number(6)?.takeIf { it in -90.0..90.0 } ?: return null
        val longitude = number(5)?.takeIf { it in -180.0..180.0 } ?: return null
        // A recent radio message does not make an old position fresh.
        val positionTime = number(3)?.takeIf { it > 0 && it <= 10_000_000_000 && it % 1.0 == 0.0 }
            ?: return null
        val callsign = (row[1] as? String)?.filter { !it.isISOControl() }?.trim()
            ?.take(16)?.takeIf { it.isNotEmpty() }
        val altitude = number(7)?.takeIf { it in -2_000.0..100_000.0 }
            ?.let { (it / 0.3048).roundToInt() }
        val heading = number(10)?.takeIf { it in 0.0..360.0 }?.rem(360)
        val onGround = row[8] as? Boolean ?: return null
        return Aircraft(id, callsign, null, GeoPoint(latitude, longitude), altitude, heading,
            (positionTime * 1_000).toLong(), simulated, onGround)
    }
}

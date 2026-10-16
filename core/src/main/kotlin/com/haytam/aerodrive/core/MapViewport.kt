package com.haytam.aerodrive.core

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

data class MapBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val isEmpty: Boolean get() = right <= left || bottom <= top

    fun intersect(other: MapBounds): MapBounds = MapBounds(
        max(left, other.left), max(top, other.top), min(right, other.right), min(bottom, other.bottom)
    ).let { if (it.isEmpty) EMPTY else it }

    companion object {
        val EMPTY = MapBounds(0, 0, 0, 0)
    }
}

object MapViewport {
    fun safeBounds(width: Int, height: Int, visible: MapBounds?, stable: MapBounds?): MapBounds {
        if (width <= 0 || height <= 0) return MapBounds.EMPTY
        var bounds = MapBounds(0, 0, width, height)
        // An empty host rectangle means unknown, not fully occluded.
        for (area in listOfNotNull(visible, stable)) {
            if (!area.isEmpty) bounds = bounds.intersect(area)
        }
        return bounds
    }

    fun zoomDelta(scale: Float): Double? =
        scale.takeIf { it.isFinite() && it > 0f }?.let { ln(it.toDouble()) / ln(2.0) }

    fun hasFocus(x: Float, y: Float): Boolean =
        x.isFinite() && y.isFinite() && x >= 0f && y >= 0f
}

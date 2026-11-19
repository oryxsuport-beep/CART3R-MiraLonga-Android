package com.cart3r.aimlinedemo

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

data class AimPoint(val x: Float, val y: Float)
data class TableBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val valid: Boolean get() = right > left && bottom > top
    fun contains(p: AimPoint): Boolean = valid && p.x >= left && p.x <= right && p.y >= top && p.y <= bottom
}

/** Ideal point-ray reflections. Not a simulation of balls, force, spin, or pockets. */
object BounceGeometry {
    fun trace(origin: AimPoint, towards: AimPoint, bounds: TableBounds, rebounds: Int): List<AimPoint> {
        if (!bounds.contains(origin) || rebounds !in 0..8) return emptyList()
        var vx = towards.x - origin.x
        var vy = towards.y - origin.y
        val len = hypot(vx, vy)
        if (len < 0.0001f) return emptyList()
        vx /= len
        vy /= len
        var position = origin
        val path = mutableListOf(origin)
        repeat(rebounds + 1) { step ->
            val tx = when {
                vx > 0.0001f -> (bounds.right - position.x) / vx
                vx < -0.0001f -> (bounds.left - position.x) / vx
                else -> Float.POSITIVE_INFINITY
            }
            val ty = when {
                vy > 0.0001f -> (bounds.bottom - position.y) / vy
                vy < -0.0001f -> (bounds.top - position.y) / vy
                else -> Float.POSITIVE_INFINITY
            }
            val t = min(tx, ty)
            if (!t.isFinite() || t < -0.0001f) return path
            val hit = AimPoint(
                (position.x + vx * t).coerceIn(bounds.left, bounds.right),
                (position.y + vy * t).coerceIn(bounds.top, bounds.bottom)
            )
            path.add(hit)
            if (step == rebounds) return path
            if (abs(t - tx) < 0.01f) vx = -vx
            if (abs(t - ty) < 0.01f) vy = -vy
            position = hit
        }
        return path
    }
}

package com.cart3r.aimlinedemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BounceGeometryTest {
    private val table = TableBounds(0f, 0f, 100f, 100f)
    @Test fun horizontalRebounds() {
        val p = BounceGeometry.trace(AimPoint(50f,50f), AimPoint(60f,50f), table, 2)
        assertEquals(4, p.size)
        assertEquals(100f, p[1].x, 0.001f)
        assertEquals(0f, p[2].x, 0.001f)
        assertEquals(100f, p[3].x, 0.001f)
    }
    @Test fun cornerReflection() {
        val p = BounceGeometry.trace(AimPoint(50f,50f), AimPoint(60f,60f), table, 1)
        assertEquals(3, p.size)
        assertEquals(100f, p[1].x, 0.001f)
        assertEquals(0f, p[2].y, 0.001f)
    }
    @Test fun invalidInputs() {
        assertTrue(BounceGeometry.trace(AimPoint(-5f,50f), AimPoint(10f,50f), table, 2).isEmpty())
        assertTrue(BounceGeometry.trace(AimPoint(50f,50f), AimPoint(50f,50f), table, 2).isEmpty())
    }
}

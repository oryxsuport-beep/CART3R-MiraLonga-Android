package com.cart3r.aimlinedemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PocketPlannerTest {
    private val table=TableBounds(0f,0f,100f,60f)

    @Test fun sixPocketsAndSixDirectRays() {
        val holes=PocketPlanner.pockets(table)
        assertEquals(6,holes.size)
        assertEquals(50f,holes[1].x,0.001f)
        val rays=PocketPlanner.direct(AimPoint(40f,25f),table)
        assertEquals(6,rays.size)
        assertTrue(rays.all{it.points.size==2})
    }

    @Test fun backCushionBankIsOnRail() {
        val ray=PocketPlanner.oneBank(
            AimPoint(45f,20f),
            AimPoint(50f,0f),
            table,
            Cushion.BOTTOM
        )
        assertEquals(3,ray.size)
        assertEquals(60f,ray[1].y,0.001f)
    }

    @Test fun fanIncludesIdealBankRoutes() {
        val rays=PocketPlanner.fan(AimPoint(50f,30f),table,true)
        assertEquals(12,rays.size)
        assertEquals(6,rays.count{it.bank==null})
        assertEquals(6,rays.count{it.bank!=null})
    }

    @Test fun invalidPositionHasNoRoutes() {
        assertTrue(PocketPlanner.fan(AimPoint(-1f,25f),table,true).isEmpty())
    }
}

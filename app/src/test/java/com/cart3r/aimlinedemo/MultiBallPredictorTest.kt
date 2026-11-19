package com.cart3r.aimlinedemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiBallPredictorTest {
    private val table=TableBounds(0f,0f,100f,100f)
    @Test fun closestObstacleHitWins() {
        val p=MultiBallPredictor.predict(
            AimPoint(10f,50f),AimPoint(90f,50f),
            listOf(AimPoint(70f,50f),AimPoint(40f,50f)),
            5f,table,0
        )
        assertEquals(1,p.hitBall)
        assertEquals(30f,p.impact!!.x,0.02f)
    }
    @Test fun detectsImpactAfterOneBank() {
        val p=MultiBallPredictor.predict(
            AimPoint(40f,50f),AimPoint(95f,50f),
            listOf(AimPoint(30f,50f)),5f,table,1
        )
        assertEquals(0,p.hitBall)
        assertEquals(95f,p.incoming[1].x,0.01f)
        assertEquals(40f,p.impact!!.x,0.01f)
    }
    @Test fun showsSecondObjectCollision() {
        val p=MultiBallPredictor.predict(
            AimPoint(10f,50f),AimPoint(90f,50f),
            listOf(AimPoint(40f,50f),AimPoint(70f,50f)),
            5f,table,0
        )
        assertEquals(0,p.hitBall)
        assertEquals(1,p.secondaryBall)
        assertEquals(60f,p.secondaryImpact!!.x,0.02f)
    }
    @Test fun noCollisionDrawsRailPath() {
        val p=MultiBallPredictor.predict(
            AimPoint(30f,30f),AimPoint(90f,30f),
            listOf(AimPoint(50f,70f)),5f,table,1
        )
        assertNull(p.hitBall)
        assertEquals(3,p.incoming.size)
        assertTrue(p.cueAfter.isEmpty())
    }
    @Test fun invalidBallCoordinatesIgnored() {
        val p=MultiBallPredictor.predict(
            AimPoint(10f,30f),AimPoint(90f,30f),
            listOf(AimPoint(-99f,-99f)),5f,table,0
        )
        assertNull(p.hitBall)
    }
}

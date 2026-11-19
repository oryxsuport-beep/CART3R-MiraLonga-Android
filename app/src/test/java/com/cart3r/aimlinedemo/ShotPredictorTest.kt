package com.cart3r.aimlinedemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotPredictorTest {
    private val rails=TableBounds(0f,0f,100f,100f)

    @Test fun headOnCollisionTransfersNormalVelocity() {
        val p=ShotPredictor.predict(
            AimPoint(15f,50f),AimPoint(90f,50f),AimPoint(60f,50f),5f,rails,0
        )
        assertTrue(p.collides)
        assertEquals(50f,p.cueIncoming.last().x,0.01f)
        assertTrue(p.cueOutgoing.isEmpty())
        assertEquals(95f,p.objectOutgoing.last().x,0.01f)
    }

    @Test fun glancingCollisionSplitsTrajectories() {
        val p=ShotPredictor.predict(
            AimPoint(15f,45f),AimPoint(90f,45f),AimPoint(60f,50f),5f,rails,0
        )
        assertTrue(p.collides)
        assertTrue(p.cueOutgoing.size>1)
        assertTrue(p.objectOutgoing.size>1)
        assertTrue(p.cueOutgoing[1].y<45f)
        assertTrue(p.objectOutgoing[1].y>50f)
    }

    @Test fun missedShotStillDrawsLongAim() {
        val p=ShotPredictor.predict(
            AimPoint(15f,25f),AimPoint(90f,25f),AimPoint(60f,65f),5f,rails,0
        )
        assertFalse(p.collides)
        assertEquals(2,p.cueIncoming.size)
        assertEquals(95f,p.cueIncoming.last().x,0.01f)
    }

    @Test fun overlappingBallsDoNotGiveFalseImpact() {
        val p=ShotPredictor.predict(
            AimPoint(15f,25f),AimPoint(90f,25f),AimPoint(16f,25f),5f,rails,1
        )
        assertFalse(p.collides)
    }

    @Test fun moreThanThreeReboundsIsRejected() {
        val p=ShotPredictor.predict(
            AimPoint(15f,25f),AimPoint(90f,25f),null,5f,rails,4
        )
        assertTrue(p.cueIncoming.isEmpty())
    }
}

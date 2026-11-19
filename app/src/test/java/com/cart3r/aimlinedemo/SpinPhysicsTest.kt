package com.cart3r.aimlinedemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpinPhysicsTest {
    @Test fun stunMatchesUserReference(){
        val p=SpinPhysics.transition(3f,0f)
        assertEquals(0.437f,p.slideTimeSeconds,0.002f)
        assertEquals(2.143f,p.rollSpeedMetersPerSecond,0.002f)
        assertEquals(1.123f,p.slideDistanceMeters,0.005f)
    }
    @Test fun pureRollingOffsetIsPointFourR(){
        val v=3f
        val k=SpinPhysics.spinRatioFromCueHeight(0.4f)
        val p=SpinPhysics.transition(v,k*v)
        assertEquals(0f,p.slideTimeSeconds,0.0001f)
    }
    @Test fun drawAndFollowCauseOppositeSignedTravel(){
        val draw=SpinPhysics.transition(0f,-1.25f*3f)
        val follow=SpinPhysics.transition(0f,1.25f*3f)
        assertTrue(draw.totalSignedDistanceMeters<0f)
        assertTrue(follow.totalSignedDistanceMeters>0f)
    }
    @Test fun cushionRestitutionChangesOutgoingAngle(){
        val t=TableBounds(0f,0f,100f,100f)
        val path=BounceGeometry.trace(
            AimPoint(50f,50f),AimPoint(60f,70f),t,1,0.75f
        )
        assertEquals(3,path.size)
        assertEquals(100f,path[1].y,0.001f)
        assertEquals(62.5f,path[2].y,0.001f)
    }
}

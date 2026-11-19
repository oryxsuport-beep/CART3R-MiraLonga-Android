package com.cart3r.aimlinedemo

import kotlin.math.abs
import kotlin.math.sign

/** Educational analytical spin-to-roll transition using the user's reference model. */
data class RollTransition(
    val slideTimeSeconds: Float,
    val rollSpeedMetersPerSecond: Float,
    val slideDistanceMeters: Float,
    val totalSignedDistanceMeters: Float
)
object SpinPhysics {
    const val GRAVITY=9.81f
    const val SLIDING_FRICTION=0.20f
    const val ROLLING_FRICTION=0.010f
    fun spinRatioFromCueHeight(offsetInRadii:Float):Float=2.5f*offsetInRadii
    /** 1D rigid-sphere model; values are not calibrated to any video game. */
    fun transition(
        initialSpeed:Float,
        angularRimSpeed:Float,
        slidingFriction:Float=SLIDING_FRICTION,
        rollingFriction:Float=ROLLING_FRICTION
    ):RollTransition {
        require(slidingFriction>0f&&rollingFriction>0f)
        val slip=initialSpeed-angularRimSpeed
        val signSlip=sign(slip)
        val slideTime=if(abs(slip)<1e-6f)0f
            else 2f*abs(slip)/(7f*slidingFriction*GRAVITY)
        val rollSpeed=initialSpeed-signSlip*slidingFriction*GRAVITY*slideTime
        val slideDistance=initialSpeed*slideTime -
            0.5f*signSlip*slidingFriction*GRAVITY*slideTime*slideTime
        val afterRoll=sign(rollSpeed)*rollSpeed*rollSpeed/
            (2f*rollingFriction*GRAVITY)
        return RollTransition(slideTime,rollSpeed,slideDistance,slideDistance+afterRoll)
    }
}

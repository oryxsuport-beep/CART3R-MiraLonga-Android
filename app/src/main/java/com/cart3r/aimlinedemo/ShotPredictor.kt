package com.cart3r.aimlinedemo

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/** Idealized billiards model for manually placed balls, not real game physics. */
data class ShotPreview(
    val cueIncoming: List<AimPoint>,
    val cueOutgoing: List<AimPoint>,
    val objectOutgoing: List<AimPoint>,
    val ghostBallCenter: AimPoint?,
    val collides: Boolean
)

object ShotPredictor {
    fun predict(
        cue: AimPoint, direction: AimPoint, objectBall: AimPoint?,
        radius: Float, table: TableBounds, rebounds: Int
    ): ShotPreview {
        val none = ShotPreview(emptyList(),emptyList(),emptyList(),null,false)
        if (radius <= 0f || rebounds !in 0..3 || !table.valid) return none
        val play = TableBounds(table.left+radius,table.top+radius,table.right-radius,table.bottom-radius)
        if (!play.contains(cue)) return none
        val dx = direction.x-cue.x; val dy=direction.y-cue.y
        val length = hypot(dx,dy)
        if(length<0.0001f)return none
        val vx=dx/length;val vy=dy/length
        val ray = AimPoint(cue.x+vx,cue.y+vy)
        val path=BounceGeometry.trace(cue,ray,play,rebounds)
        val missed=ShotPreview(path,emptyList(),emptyList(),null,false)
        if(objectBall==null||!play.contains(objectBall))return missed
        val ox=objectBall.x-cue.x;val oy=objectBall.y-cue.y
        val projected=ox*vx+oy*vy
        if(projected<=0f)return missed
        val lateral2=max(0f,ox*ox+oy*oy-projected*projected)
        val diameter=2f*radius
        if(lateral2>=diameter*diameter)return missed
        val travel=projected-sqrt(max(0f,diameter*diameter-lateral2))
        if(travel<=0f)return missed
        val first=BounceGeometry.trace(cue,ray,play,0)
        if(first.size<2)return missed
        if(travel>=hypot(first[1].x-cue.x,first[1].y-cue.y)-0.01f)return missed
        val impact=AimPoint(cue.x+vx*travel,cue.y+vy*travel)
        val nx=(objectBall.x-impact.x)/diameter
        val ny=(objectBall.y-impact.y)/diameter
        val transfer=(vx*nx+vy*ny).coerceIn(0f,1f)
        if(transfer<0.0001f)return missed
        val cx=vx-transfer*nx;val cy=vy-transfer*ny
        val cueAfter=if(hypot(cx,cy)>0.02f)
            BounceGeometry.trace(impact,AimPoint(impact.x+cx,impact.y+cy),play,rebounds)
        else emptyList()
        val objAfter=BounceGeometry.trace(
            objectBall,AimPoint(objectBall.x+nx,objectBall.y+ny),play,rebounds
        )
        return ShotPreview(listOf(cue,impact),cueAfter,objAfter,impact,true)
    }
}

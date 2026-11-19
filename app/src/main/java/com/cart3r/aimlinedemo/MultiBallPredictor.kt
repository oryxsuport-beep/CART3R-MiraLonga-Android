package com.cart3r.aimlinedemo

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/** Manual approximation of elastic billiard-ball contacts, including cushion paths. */
data class CollisionPreview(
    val incoming: List<AimPoint>,
    val cueAfter: List<AimPoint>,
    val ballAfter: List<AimPoint>,
    val hitBall: Int?,
    val impact: AimPoint?,
    val secondaryBall: Int?,
    val secondaryImpact: AimPoint?
)

object MultiBallPredictor {
    private data class Contact(val ball: Int,val impact: AimPoint,val distance: Float)

    /** Earliest circle collision along a finite segment, excluding overlaps. */
    private fun firstContact(
        start: AimPoint, end: AimPoint, balls: List<AimPoint>,
        radius: Float, exclude: Int = -1
    ): Contact? {
        val dx = end.x-start.x
        val dy = end.y-start.y
        val length = hypot(dx,dy)
        if(length<=0.0001f)return null
        val ux=dx/length
        val uy=dy/length
        val diameter=2f*radius
        var best:Contact?=null
        for ((index,center) in balls.withIndex()) {
            if(index==exclude)continue
            val ox=center.x-start.x
            val oy=center.y-start.y
            val squared=ox*ox+oy*oy
            if(squared<diameter*diameter-0.001f)continue
            val projection=ox*ux+oy*uy
            if(projection<0f)continue
            val lateral=max(0f,squared-projection*projection)
            if(lateral>=diameter*diameter)continue
            val distance=projection-sqrt(max(0f,diameter*diameter-lateral))
            if(distance<=0.001f||distance>length+0.0001f)continue
            if(best==null||distance<best.distance)
                best=Contact(index,AimPoint(start.x+ux*distance,start.y+uy*distance),distance)
        }
        return best
    }

    private fun closestContact(
        path: List<AimPoint>, balls: List<AimPoint>, radius: Float, exclude: Int = -1
    ): Pair<Int,Contact>? {
        for(i in 1 until path.size) {
            val hit=firstContact(path[i-1],path[i],balls,radius,exclude)
            if(hit!=null)return Pair(i,hit)
        }
        return null
    }

    /** Computes the first hit (including after a bank) and up to one secondary object hit. */
    fun predict(
        cue:AimPoint, direction:AimPoint, balls:List<AimPoint>,
        radius:Float, table:TableBounds, rebounds:Int
    ):CollisionPreview {
        val empty=CollisionPreview(emptyList(),emptyList(),emptyList(),null,null,null,null)
        if(radius<=0f||!table.valid||rebounds !in 0..3)return empty
        val play=TableBounds(table.left+radius,table.top+radius,table.right-radius,table.bottom-radius)
        if(!play.contains(cue))return empty
        val all=balls.filter { play.contains(it) }
        // Retain input ball indices: invalid off-table points are ignored in contact search.
        val validated=balls.map { if(play.contains(it)) it else AimPoint(-100000f,-100000f) }
        val path=BounceGeometry.trace(cue,direction,play,rebounds)
        val miss=CollisionPreview(path,emptyList(),emptyList(),null,null,null,null)
        if(path.size<2||all.isEmpty())return miss
        val found=closestContact(path,validated,radius)?:return miss
        val i=found.first
        val hit=found.second
        val incoming=path.take(i)+hit.impact
        val before=path[i-1]
        val end=path[i]
        val magnitude=hypot(end.x-before.x,end.y-before.y)
        if(magnitude<0.0001f)return miss
        val vx=(end.x-before.x)/magnitude
        val vy=(end.y-before.y)/magnitude
        val center=balls[hit.ball]
        val nx=(center.x-hit.impact.x)/(2f*radius)
        val ny=(center.y-hit.impact.y)/(2f*radius)
        val normal=(vx*nx+vy*ny).coerceIn(0f,1f)
        if(normal<=0.0001f)return miss
        // Equal-mass perfectly elastic collision, no friction or spin.
        val afterX=vx-normal*nx
        val afterY=vy-normal*ny
        val cueAfter=if(hypot(afterX,afterY)>0.015f) {
            BounceGeometry.trace(
                hit.impact,
                AimPoint(hit.impact.x+afterX,hit.impact.y+afterY),
                play,rebounds
            )
        } else emptyList()
        val objectPath=BounceGeometry.trace(
            center,AimPoint(center.x+nx,center.y+ny),play,rebounds
        )
        val second=closestContact(objectPath,validated,radius,hit.ball)
        val objectAfter=if(second==null)objectPath else
            objectPath.take(second.first)+second.second.impact
        return CollisionPreview(
            incoming,cueAfter,objectAfter,hit.ball,hit.impact,
            second?.second?.ball,second?.second?.impact
        )
    }
}

package com.cart3r.aimlinedemo

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Circle candidate detected only from a user-authorized screen-capture frame. */
data class VisionBall(val center: AimPoint, val radius: Float, val cueScore: Float)

/**
 * Lightweight, on-device segmentation. It requires manually calibrated
 * table boundaries and ball radius. No network or native vision library.
 *
 * Algorithm: estimate the felt's median RGB, classify non-felt pixels on
 * a coarse grid, find connected components, and retain ball-sized blobs.
 * Intentionally conservative: ambiguous positions are not auto-applied.
 */
object BallVision {
    private fun rgbDistSq(a:Int,b:Int):Int {
        val r=(a ushr 16 and 255)-(b ushr 16 and 255)
        val g=(a ushr 8 and 255)-(b ushr 8 and 255)
        val bl=(a and 255)-(b and 255)
        return r*r+g*g+bl*bl
    }

    private fun isGuideColor(pixel:Int):Boolean {
        val r=pixel ushr 16 and 255
        val g=pixel ushr 8 and 255
        val b=pixel and 255
        return (r>170&&g<115&&b<135) ||
               (g>165&&b>170&&r<120) ||
               (r>160&&b>150&&g<130)
    }

    /** Coordinates refer to the screenshot pixels (not the overlay view). */
    fun detect(
        pixels:IntArray,
        width:Int,
        height:Int,
        roi:TableBounds,
        expectedRadius:Float,
        limit:Int=16
    ):List<VisionBall> {
        if(width<30||height<30||pixels.size<width*height||
            !roi.valid||expectedRadius<2f||limit<1)return emptyList()
        val pad=max(expectedRadius*1.4f,3f)
        val left=(roi.left+pad).toInt().coerceIn(0,width-1)
        val right=(roi.right-pad).toInt().coerceIn(0,width-1)
        val top=(roi.top+pad).toInt().coerceIn(0,height-1)
        val bottom=(roi.bottom-pad).toInt().coerceIn(0,height-1)
        if(right-left<expectedRadius*4||bottom-top<expectedRadius*4)return emptyList()

        val samples=ArrayList<Int>(160)
        for(yi in 1..10)for(xi in 1..15) {
            val x=(left+(right-left)*xi/16f).toInt()
            val y=(top+(bottom-top)*yi/11f).toInt()
            samples.add(pixels[y*width+x])
        }
        fun medianChannel(shift:Int):Int {
            val channel=samples.map{it ushr shift and 255}.sorted()
            return channel[channel.size/2]
        }
        val felt=(medianChannel(16) shl 16) or
                 (medianChannel(8) shl 8) or medianChannel(0)

        val step=max(2,(expectedRadius/4.4f).roundToInt())
        val gw=(right-left)/step+1
        val gh=(bottom-top)/step+1
        if(gw*gh>500000)return emptyList()
        val mask=BooleanArray(gw*gh)
        val pixelCache=IntArray(gw*gh)
        for(y in 0 until gh)for(x in 0 until gw){
            val sx=(left+x*step).coerceAtMost(right)
            val sy=(top+y*step).coerceAtMost(bottom)
            val p=pixels[sy*width+sx]
            val idx=y*gw+x
            pixelCache[idx]=p
            val r=p ushr 16 and 255
            val g=p ushr 8 and 255
            val b=p and 255
            val white=(r>150&&g>150&&b>150&&
                max(r,max(g,b))-min(r,min(g,b))<75)
            mask[idx]=!isGuideColor(p) &&
                (rgbDistSq(p,felt)>62*62 || (white&&rgbDistSq(p,felt)>40*40))
        }
        val seen=BooleanArray(mask.size)
        val queue=IntArray(mask.size)
        val gridR=expectedRadius/step
        val areaR=PI*gridR*gridR
        val results=ArrayList<VisionBall>()
        for(seed in mask.indices){
            if(!mask[seed]||seen[seed])continue
            var first=0
            var last=0
            queue[last++]=seed
            seen[seed]=true
            var count=0
            var sumX=0.0
            var sumY=0.0
            var minX=gw
            var maxX=0
            var minY=gh
            var maxY=0
            var whitish=0
            while(first<last){
                val cur=queue[first++]
                val x=cur%gw
                val y=cur/gw
                count++
                sumX+=x
                sumY+=y
                minX=min(minX,x);maxX=max(maxX,x)
                minY=min(minY,y);maxY=max(maxY,y)
                val p=pixelCache[cur]
                val r=p ushr 16 and 255
                val g=p ushr 8 and 255
                val b=p and 255
                if(r>175&&g>175&&b>175&&
                    max(r,max(g,b))-min(r,min(g,b))<60)whitish++
                for(dy in -1..1)for(dx in -1..1){
                    if(dx==0&&dy==0)continue
                    val xx=x+dx;val yy=y+dy
                    if(xx<0||xx>=gw||yy<0||yy>=gh)continue
                    val next=yy*gw+xx
                    if(mask[next]&&!seen[next]){
                        seen[next]=true
                        queue[last++]=next
                    }
                }
            }
            if(count<max(3.0,areaR*0.14).toInt()||
                count>areaR*3.1)continue
            val bw=maxX-minX+1
            val bh=maxY-minY+1
            if(bw<gridR*0.9||bh<gridR*0.9||
                bw>gridR*3.5||bh>gridR*3.5)continue
            val aspect=bw.toFloat()/bh
            if(aspect<0.55f||aspect>1.85f)continue
            val cx=left+(sumX/count*step).toFloat()
            val cy=top+(sumY/count*step).toFloat()
            if(!roi.contains(AimPoint(cx,cy)))continue
            val measuredR=max(2f,((bw+bh)*step/4f))
            results.add(VisionBall(AimPoint(cx,cy),measuredR,
                whitish.toFloat()/count))
        }
        // Keep candidates near the expected diameter. Deduplicate overlapping blobs.
        val sorted=results.sortedWith(
            compareBy<VisionBall> { abs(it.radius-expectedRadius) }
                .thenByDescending { it.cueScore }
        )
        val distinct=ArrayList<VisionBall>()
        for(ball in sorted){
            if(distinct.none{
                hypot(it.center.x-ball.center.x,it.center.y-ball.center.y)<expectedRadius*1.5f
            })distinct.add(ball)
            if(distinct.size>=limit)break
        }
        return distinct
    }
}

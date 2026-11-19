package com.cart3r.aimlinedemo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class BallVisionTest {
    private val width=420
    private val height=210
    private val felt=0xFF246C3B.toInt()
    private fun frame():IntArray=IntArray(width*height){felt}
    private fun circle(a:IntArray,cx:Int,cy:Int,r:Int,color:Int){
        for(y in (cy-r)..(cy+r))for(x in (cx-r)..(cx+r)){
            if(x !in 0 until width||y !in 0 until height)continue
            if((x-cx)*(x-cx)+(y-cy)*(y-cy)<=r*r)a[y*width+x]=color
        }
    }
    private val roi=TableBounds(0f,0f,width.toFloat()-1,height.toFloat()-1)

    @Test fun detectsSeparateWhiteAndColoredBalls() {
        val a=frame()
        circle(a,105,85,11,0xFFF0F0F0.toInt())
        circle(a,245,110,11,0xFFF7D323.toInt())
        val balls=BallVision.detect(a,width,height,roi,11f)
        assertTrue("expected two balls, found ${balls.size}",balls.size>=2)
        assertTrue(balls.any{it.cueScore>0.7f&&
            kotlin.math.abs(it.center.x-105f)<8f})
        assertTrue(balls.any{kotlin.math.abs(it.center.x-245f)<8f})
    }
    @Test fun ignoresEmptyFelt(){
        assertTrue(BallVision.detect(frame(),width,height,roi,11f).isEmpty())
    }
    @Test fun rejectsInvalidFrames(){
        assertTrue(BallVision.detect(IntArray(0),width,height,roi,11f).isEmpty())
        assertTrue(BallVision.detect(frame(),width,height,roi,0f).isEmpty())
    }
    @Test fun ignoresLongRedGuideLines(){
        val a=frame()
        for(x in 40..340)a[90*width+x]=0xFFFA2547.toInt()
        assertTrue(BallVision.detect(a,width,height,roi,11f).isEmpty())
    }
}

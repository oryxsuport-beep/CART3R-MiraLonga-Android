package com.cart3r.aimlinedemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.max

/** No screen recording or game data access. Handles are adjusted manually. */
class AimLineView(context: Context) : View(context) {
    enum class Mode { LOCKED, AIM, TABLE }
    private val origin = PointF()
    private val target = PointF()
    private var bounds = TableBounds(0f,0f,1f,1f)
    private var initialized = false
    private var mode = Mode.LOCKED
    private var activeHandle = -1
    private var rebounds = 0
    private val density = resources.displayMetrics.density
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255,52,83)
        strokeWidth = 2.5f*density
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(17f*density,8f*density),0f)
    }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255,180,30)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f*density
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    fun setMode(newMode: Mode) { mode=newMode; activeHandle=-1; invalidate() }
    fun setRebounds(value: Int) { rebounds=value.coerceIn(0,3); invalidate() }

    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) {
        super.onSizeChanged(w,h,oldw,oldh)
        if (!initialized) {
            origin.set(w*.35f,h*.6f)
            target.set(w*.6f,h*.4f)
            bounds=TableBounds(w*.1f,h*.16f,w*.9f,h*.84f)
            initialized=true
        } else if(oldw>0&&oldh>0) {
            val sx=w.toFloat()/oldw; val sy=h.toFloat()/oldh
            origin.set(origin.x*sx,origin.y*sy)
            target.set(target.x*sx,target.y*sy)
            bounds=TableBounds(bounds.left*sx,bounds.top*sy,bounds.right*sx,bounds.bottom*sy)
        }
    }

    override fun onDraw(canvas:Canvas) {
        if (!initialized) return
        val a=AimPoint(origin.x,origin.y)
        val b=AimPoint(target.x,target.y)
        val points=if(rebounds>0) BounceGeometry.trace(a,b,bounds,rebounds) else {
            val dx=b.x-a.x; val dy=b.y-a.y
            if(hypot(dx,dy)<2f) emptyList() else {
                var t=Float.POSITIVE_INFINITY
                if(dx>0f)t=minOf(t,(width-a.x)/dx)
                if(dx<0f)t=minOf(t,-a.x/dx)
                if(dy>0f)t=minOf(t,(height-a.y)/dy)
                if(dy<0f)t=minOf(t,-a.y/dy)
                if(t.isFinite()&&t>0) listOf(a,AimPoint(a.x+t*dx,a.y+t*dy)) else emptyList()
            }
        }
        for (i in 1 until points.size)
            canvas.drawLine(points[i-1].x,points[i-1].y,points[i].x,points[i].y,stroke)
        if (rebounds>0||mode==Mode.TABLE)
            canvas.drawRect(bounds.left,bounds.top,bounds.right,bounds.bottom,outline)
        if (mode==Mode.AIM) {
            drawDot(canvas,origin.x,origin.y,Color.WHITE)
            drawDot(canvas,target.x,target.y,Color.rgb(255,50,83))
        }
        if (mode==Mode.TABLE) {
            drawDot(canvas,bounds.left,(bounds.top+bounds.bottom)/2,Color.YELLOW)
            drawDot(canvas,bounds.right,(bounds.top+bounds.bottom)/2,Color.YELLOW)
            drawDot(canvas,(bounds.left+bounds.right)/2,bounds.top,Color.YELLOW)
            drawDot(canvas,(bounds.left+bounds.right)/2,bounds.bottom,Color.YELLOW)
        }
    }
    private fun drawDot(c:Canvas,x:Float,y:Float,color:Int) {
        dot.color=Color.BLACK; c.drawCircle(x,y,12f*density,dot)
        dot.color=color; c.drawCircle(x,y,9f*density,dot)
    }
    override fun onTouchEvent(ev:MotionEvent):Boolean {
        if (mode==Mode.LOCKED)return false
        val x=ev.x.coerceIn(0f,width.toFloat())
        val y=ev.y.coerceIn(0f,height.toFloat())
        when(ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val p=if(mode==Mode.AIM) listOf(AimPoint(origin.x,origin.y),AimPoint(target.x,target.y))
                  else listOf(
                    AimPoint(bounds.left,(bounds.top+bounds.bottom)/2),
                    AimPoint(bounds.right,(bounds.top+bounds.bottom)/2),
                    AimPoint((bounds.left+bounds.right)/2,bounds.top),
                    AimPoint((bounds.left+bounds.right)/2,bounds.bottom)
                  )
                activeHandle=p.indices.minByOrNull { hypot(x-p[it].x,y-p[it].y) } ?: -1
                updateHandle(x,y)
                return true
            }
            MotionEvent.ACTION_MOVE -> { updateHandle(x,y); return true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { activeHandle=-1; return true }
        }
        return true
    }
    private fun updateHandle(x:Float,y:Float) {
        if(mode==Mode.AIM) {
            if(activeHandle==0)origin.set(x,y)
            if(activeHandle==1)target.set(x,y)
        } else if(mode==Mode.TABLE) {
            val minWidth=max(width*.2f,60f*density)
            val minHeight=max(height*.2f,60f*density)
            bounds=when(activeHandle) {
                0 -> bounds.copy(left=x.coerceAtMost(bounds.right-minWidth))
                1 -> bounds.copy(right=x.coerceAtLeast(bounds.left+minWidth))
                2 -> bounds.copy(top=y.coerceAtMost(bounds.bottom-minHeight))
                3 -> bounds.copy(bottom=y.coerceAtLeast(bounds.top+minHeight))
                else -> bounds
            }
        }
        invalidate()
    }
}

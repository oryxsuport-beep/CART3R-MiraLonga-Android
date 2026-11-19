package com.cart3r.aimlinedemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.view.View
import kotlin.math.max

/**
 * Rendering-only overlay. It NEVER receives touches. The interactive markers
 * are separate small windows so touches outside those windows reach the game.
 */
class AimLineView(context: Context):View(context) {
    enum class Mode { LOCKED, AIM, TABLE, BALL, EXTRA }
    private val white=PointF()
    private val direction=PointF()
    private val targetBall=PointF()
    private val extras=mutableListOf<PointF>()
    private var table=TableBounds(0f,0f,1f,1f)
    private var initialized=false
    private var mode=Mode.LOCKED
    private var targetEnabled=true
    private var fanEnabled=true
    private var banksEnabled=true
    private var rebounds=0
    private val dp=resources.displayMetrics.density
    private var radius=9f*dp
    private val dotted=Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color=Color.rgb(255,42,76)
        strokeWidth=2f*dp
        style=Paint.Style.STROKE
        pathEffect=DashPathEffect(floatArrayOf(12f*dp,6f*dp),0f)
    }
    private val cueAfter=Paint(dotted).apply{color=Color.rgb(255,200,53)}
    private val objectAfter=Paint(dotted).apply{color=Color.rgb(43,223,238)}
    private val secondaryAfter=Paint(dotted).apply{color=Color.rgb(224,96,252)}
    private val directPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color=Color.argb(150,255,38,62)
        strokeWidth=1.45f*dp
        style=Paint.Style.STROKE
    }
    private val bankPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color=Color.argb(95,255,70,72)
        strokeWidth=1.1f*dp
        style=Paint.Style.STROKE
    }
    private val outline=Paint(Paint.ANTI_ALIAS_FLAG).apply{
        color=Color.rgb(255,180,50)
        strokeWidth=1.3f*dp
        style=Paint.Style.STROKE
    }
    private val dot=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.FILL}

    fun setMode(value:Mode){mode=value;invalidate()}
    fun setRebounds(value:Int){rebounds=value.coerceIn(0,3);invalidate()}
    fun setBallEnabled(value:Boolean){targetEnabled=value;invalidate()}
    fun setFanEnabled(value:Boolean){fanEnabled=value;invalidate()}
    fun setBanksEnabled(value:Boolean){banksEnabled=value;invalidate()}
    fun radiusDp():Int=(radius/dp).toInt()
    fun toggleRadius():Int {
        radius=(if(radius/dp>7.5f)6f else 9f)*dp
        invalidate()
        return radiusDp()
    }
    fun obstacleCount():Int=extras.size
    fun addObstacle():Boolean {
        if(extras.size>=7)return false
        val i=extras.size
        extras.add(PointF(width*(0.50f+0.06f*(i%4)),height*(0.36f+0.08f*(i/4))))
        invalidate()
        return true
    }
    fun removeObstacle():Boolean {
        if(extras.isEmpty())return false
        extras.removeAt(extras.lastIndex)
        invalidate()
        return true
    }
    fun handles(which:Mode):List<AimPoint> = when(which){
        Mode.AIM->listOf(AimPoint(white.x,white.y),AimPoint(direction.x,direction.y))
        Mode.BALL->listOf(AimPoint(targetBall.x,targetBall.y))
        Mode.EXTRA->extras.map{AimPoint(it.x,it.y)}
        Mode.TABLE->listOf(
            AimPoint(table.left,(table.top+table.bottom)/2f),
            AimPoint(table.right,(table.top+table.bottom)/2f),
            AimPoint((table.left+table.right)/2f,table.top),
            AimPoint((table.left+table.right)/2f,table.bottom)
        )
        Mode.LOCKED->emptyList()
    }
    fun moveHandle(which:Mode,index:Int,rawX:Float,rawY:Float) {
        if(width<1||height<1)return
        val x=rawX.coerceIn(0f,width.toFloat())
        val y=rawY.coerceIn(0f,height.toFloat())
        when(which){
            Mode.AIM->if(index==0)white.set(x,y) else if(index==1)direction.set(x,y)
            Mode.BALL->if(index==0)targetBall.set(x,y)
            Mode.EXTRA->if(index in extras.indices)extras[index].set(x,y)
            Mode.TABLE->{
                val minW=max(width*.25f,60f*dp)
                val minH=max(height*.25f,60f*dp)
                table=when(index){
                    0->table.copy(left=x.coerceAtMost(table.right-minW))
                    1->table.copy(right=x.coerceAtLeast(table.left+minW))
                    2->table.copy(top=y.coerceAtMost(table.bottom-minH))
                    3->table.copy(bottom=y.coerceAtLeast(table.top+minH))
                    else->table
                }
            }
            Mode.LOCKED->{}
        }
        invalidate()
    }

    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){
        super.onSizeChanged(w,h,oldw,oldh)
        if(!initialized){
            white.set(w*.35f,h*.6f)
            direction.set(w*.6f,h*.4f)
            targetBall.set(w*.7f,h*.47f)
            table=TableBounds(w*.09f,h*.16f,w*.91f,h*.83f)
            initialized=true
        }else if(oldw>0&&oldh>0){
            val sx=w.toFloat()/oldw
            val sy=h.toFloat()/oldh
            white.set(white.x*sx,white.y*sy)
            direction.set(direction.x*sx,direction.y*sy)
            targetBall.set(targetBall.x*sx,targetBall.y*sy)
            extras.forEach{it.set(it.x*sx,it.y*sy)}
            table=TableBounds(table.left*sx,table.top*sy,table.right*sx,table.bottom*sy)
        }
    }
    private fun path(c:Canvas,pts:List<AimPoint>,paint:Paint){
        for(i in 1 until pts.size)c.drawLine(
            pts[i-1].x,pts[i-1].y,pts[i].x,pts[i].y,paint
        )
    }
    private fun circle(c:Canvas,x:Float,y:Float,r:Float,color:Int){
        dot.color=Color.argb(80,Color.red(color),Color.green(color),Color.blue(color))
        c.drawCircle(x,y,r,dot)
        dot.color=color
        c.drawCircle(x,y,r,outline.apply{this.color=color})
    }

    override fun onDraw(canvas:Canvas){
        if(!initialized)return
        val balls=buildList {
            if(targetEnabled)add(AimPoint(targetBall.x,targetBall.y))
            extras.forEach{add(AimPoint(it.x,it.y))}
        }
        val predicted=MultiBallPredictor.predict(
            AimPoint(white.x,white.y),AimPoint(direction.x,direction.y),
            balls,radius,table,rebounds
        )
        if(targetEnabled&&fanEnabled){
            val routes=PocketPlanner.fan(AimPoint(targetBall.x,targetBall.y),table,banksEnabled)
            routes.forEach { path(canvas,it.points,if(it.bank==null)directPaint else bankPaint) }
            dot.color=Color.rgb(255,60,78)
            PocketPlanner.pockets(table).forEach{canvas.drawCircle(it.x,it.y,3f*dp,dot)}
        }
        path(canvas,predicted.incoming,dotted)
        path(canvas,predicted.cueAfter,cueAfter)
        path(canvas,predicted.ballAfter,objectAfter)
        path(canvas,predicted.secondaryAfter,secondaryAfter)
        if(predicted.impact!=null){
            outline.color=Color.YELLOW
            canvas.drawCircle(predicted.impact.x,predicted.impact.y,radius,outline)
        }
        if(predicted.secondaryImpact!=null){
            outline.color=Color.MAGENTA
            canvas.drawCircle(predicted.secondaryImpact.x,predicted.secondaryImpact.y,radius,outline)
        }
        if(mode==Mode.TABLE||rebounds>0){
            outline.color=Color.rgb(255,180,50)
            canvas.drawRect(table.left,table.top,table.right,table.bottom,outline)
        }
        if(targetEnabled)circle(canvas,targetBall.x,targetBall.y,radius,Color.CYAN)
        extras.forEach{circle(canvas,it.x,it.y,radius,Color.MAGENTA)}
        if(mode==Mode.AIM) {
            dot.color=Color.WHITE
            canvas.drawCircle(white.x,white.y,5f*dp,dot)
        }
    }
}

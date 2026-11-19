package com.cart3r.aimlinedemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.abs

/**
 * Separates an always non-touchable drawing layer from a few small touchable
 * windows. This preserves gameplay touches everywhere except the panel
 * and the visible marker circles.
 */
class AimOverlayService:Service() {
    private val manager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private lateinit var line:AimLineView
    private lateinit var panel:LinearLayout
    private lateinit var panelBody:ScrollView
    private lateinit var panelParams:WindowManager.LayoutParams
    private var mode=AimLineView.Mode.LOCKED
    private var targetEnabled=true
    private var fanEnabled=true
    private var banksEnabled=true
    private var rebounds=0
    private var minimized=false
    private var started=false
    private val activeMarkers=mutableListOf<Pair<View,WindowManager.LayoutParams>>()
    private lateinit var aimButton:TextView
    private lateinit var tableButton:TextView
    private lateinit var ballButton:TextView
    private lateinit var extraButton:TextView
    private lateinit var radiusButton:TextView
    private lateinit var bounceButton:TextView
    private lateinit var fanButton:TextView
    private lateinit var banksButton:TextView
    private lateinit var panelHeader:TextView

    private fun dp(px:Int)=(px*resources.displayMetrics.density).toInt()
    private fun windowFlags() =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

    override fun onCreate(){
        super.onCreate()
        val notificationManager=getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel("aim_overlay","Mira visual",NotificationManager.IMPORTANCE_LOW)
        )
        val pending=PendingIntent.getActivity(
            this,0,Intent(this,MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val note=Notification.Builder(this,"aim_overlay")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("CART3R Mira Longa V5")
            .setContentText("Mira ativa. Toques no jogo liberados.")
            .setContentIntent(pending).setOngoing(true).build()
        if(Build.VERSION.SDK_INT>=34)
            startForeground(1001,note,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1001,note)
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY}
        if(!started)startOverlay()
        return START_NOT_STICKY
    }
    private fun button(text:String):TextView=TextView(this).apply{
        this.text=text
        textSize=11.5f
        setTextColor(Color.WHITE)
        gravity=Gravity.CENTER_VERTICAL
        setPadding(dp(10),dp(6),dp(6),dp(6))
        minHeight=dp(31)
        background=GradientDrawable().apply{
            setColor(Color.rgb(35,14,23))
            setStroke(dp(1),Color.rgb(75,30,44))
            cornerRadius=dp(5).toFloat()
        }
    }
    private fun startOverlay(){
        line=AimLineView(this)
        val overlayParams=WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity=Gravity.TOP or Gravity.START
            // Under Android 12's 0.8 maximum obscuring-opacity threshold.
            alpha=0.70f
        }

        panel=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            background=GradientDrawable().apply{
                setColor(Color.argb(246,18,9,15))
                cornerRadius=dp(9).toFloat()
                setStroke(dp(1),Color.rgb(225,38,75))
            }
        }
        panelHeader=button("C3  /  arraste ou minimize  ▾")
        panel.addView(panelHeader)
        panelBody=ScrollView(this).apply{
            isFillViewport=false
            isVerticalScrollBarEnabled=true
        }
        val options=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        aimButton=button("✏  Mover mira / branca")
        tableButton=button("▣  Calibrar 4 bordas")
        ballButton=button("●  Mover bola-alvo")
        extraButton=button("✚  Adicionar colisão")
        radiusButton=button("◯  Raio: 9 (6/9)")
        bounceButton=button("↗  Rebotes: 0")
        fanButton=button("✦  Seis caçapas: ON")
        banksButton=button("⌁  Tabelas extras: ON")
        val remove=button("−  Remover última colisão")
        val hideTarget=button("◌  Mostrar/ocultar alvo")
        val lock=button("✓  Liberar toques")
        val stop=button("×  Encerrar mira")
        listOf(
            aimButton,tableButton,ballButton,extraButton,remove,
            radiusButton,bounceButton,fanButton,banksButton,hideTarget,lock,stop
        ).forEach{options.addView(it)}
        panelBody.addView(options)
        panel.addView(panelBody,LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,dp(228)
        ))
        panelParams=WindowManager.LayoutParams(
            dp(194),WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            windowFlags(),PixelFormat.TRANSLUCENT
        ).apply {
            gravity=Gravity.TOP or Gravity.START
            x=dp(10);y=dp(54)
        }

        aimButton.setOnClickListener{selectMode(AimLineView.Mode.AIM)}
        tableButton.setOnClickListener{selectMode(AimLineView.Mode.TABLE)}
        ballButton.setOnClickListener {
            targetEnabled=true
            line.setBallEnabled(true)
            selectMode(AimLineView.Mode.BALL)
        }
        extraButton.setOnClickListener{
            if(line.addObstacle())selectMode(AimLineView.Mode.EXTRA)
            else extraButton.text="✚  Limite: 7 bolas extras"
        }
        remove.setOnClickListener {
            line.removeObstacle()
            if(line.obstacleCount()==0)selectMode(AimLineView.Mode.LOCKED)
            else selectMode(AimLineView.Mode.EXTRA)
        }
        hideTarget.setOnClickListener{
            targetEnabled=!targetEnabled
            line.setBallEnabled(targetEnabled)
            if(!targetEnabled&&mode==AimLineView.Mode.BALL)selectMode(AimLineView.Mode.LOCKED)
        }
        radiusButton.setOnClickListener{
            val radius=line.toggleRadius()
            radiusButton.text="◯  Raio: $radius (6/9)"
        }
        bounceButton.setOnClickListener{
            rebounds=(rebounds+1)%4
            line.setRebounds(rebounds)
            bounceButton.text="↗  Rebotes: $rebounds"
        }
        fanButton.setOnClickListener{
            fanEnabled=!fanEnabled
            line.setFanEnabled(fanEnabled)
            fanButton.text=if(fanEnabled)"✦  Seis caçapas: ON" else "✦  Seis caçapas: OFF"
        }
        banksButton.setOnClickListener{
            banksEnabled=!banksEnabled
            line.setBanksEnabled(banksEnabled)
            banksButton.text=if(banksEnabled)"⌁  Tabelas extras: ON" else "⌁  Tabelas extras: OFF"
        }
        lock.setOnClickListener{selectMode(AimLineView.Mode.LOCKED)}
        stop.setOnClickListener{stopSelf()}

        var downX=0f
        var downY=0f
        var oldX=0
        var oldY=0
        var moved=false
        panelHeader.setOnTouchListener{_,event->
            when(event.actionMasked){
                MotionEvent.ACTION_DOWN->{
                    downX=event.rawX;downY=event.rawY
                    oldX=panelParams.x;oldY=panelParams.y
                    moved=false
                    true
                }
                MotionEvent.ACTION_MOVE->{
                    val dx=event.rawX-downX
                    val dy=event.rawY-downY
                    if(abs(dx)>dp(6)||abs(dy)>dp(6))moved=true
                    if(moved){
                        panelParams.x=(oldX+dx).toInt().coerceAtLeast(0)
                        panelParams.y=(oldY+dy).toInt().coerceAtLeast(0)
                        manager.updateViewLayout(panel,panelParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP->{
                    if(!moved){
                        minimized=!minimized
                        panelBody.visibility=if(minimized)View.GONE else View.VISIBLE
                        panelHeader.text=if(minimized)"C3  ▴" else "C3  /  arraste ou minimize  ▾"
                        manager.updateViewLayout(panel,panelParams)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL->true
                else->false
            }
        }
        manager.addView(line,overlayParams)
        try {
            manager.addView(panel,panelParams)
            started=true
        }catch(e:Exception){
            manager.removeView(line)
            throw e
        }
        line.post { if(started)refreshMarkers() }
    }

    private fun selectMode(requested:AimLineView.Mode) {
        mode=if(mode==requested)AimLineView.Mode.LOCKED else requested
        line.setMode(mode)
        refreshMarkers()
        aimButton.text=if(mode==AimLineView.Mode.AIM)"✓  Concluir ajuste da mira" else "✏  Mover mira / branca"
        tableButton.text=if(mode==AimLineView.Mode.TABLE)"✓  Concluir bordas" else "▣  Calibrar 4 bordas"
        ballButton.text=if(mode==AimLineView.Mode.BALL)"✓  Concluir bola-alvo" else "●  Mover bola-alvo"
    }

    private fun removeMarkers() {
        for((view,_) in activeMarkers) {
            if(view.isAttachedToWindow)manager.removeView(view)
        }
        activeMarkers.clear()
    }

    private fun refreshMarkers(){
        removeMarkers()
        if(!started||mode==AimLineView.Mode.LOCKED)return
        val modeAtCreation=mode
        val positions=line.handles(modeAtCreation)
        if(positions.isEmpty())return
        val markerSize=dp(34)
        positions.forEachIndexed{index,position->
            val marker=TextView(this).apply{
                text=when(modeAtCreation){
                    AimLineView.Mode.AIM->if(index==0)"◉" else "✛"
                    AimLineView.Mode.TABLE->"↔"
                    AimLineView.Mode.BALL->"●"
                    AimLineView.Mode.EXTRA->"✚"
                    else->"◉"
                }
                gravity=Gravity.CENTER
                textSize=19f
                setTextColor(Color.WHITE)
                background=GradientDrawable().apply{
                    shape=GradientDrawable.OVAL
                    setColor(Color.argb(240,115,18,48))
                    setStroke(dp(2),Color.rgb(255,90,107))
                }
            }
            val params=WindowManager.LayoutParams(
                markerSize,markerSize,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                windowFlags(),PixelFormat.TRANSLUCENT
            ).apply {
                gravity=Gravity.TOP or Gravity.START
                x=(position.x-markerSize/2f).toInt().coerceAtLeast(0)
                y=(position.y-markerSize/2f).toInt().coerceAtLeast(0)
            }
            var downX=0f
            var downY=0f
            var oldX=0
            var oldY=0
            marker.setOnTouchListener{_,event->
                when(event.actionMasked){
                    MotionEvent.ACTION_DOWN->{
                        downX=event.rawX;downY=event.rawY
                        oldX=params.x;oldY=params.y
                        true
                    }
                    MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP->{
                        val maxX=(line.width-markerSize).coerceAtLeast(0)
                        val maxY=(line.height-markerSize).coerceAtLeast(0)
                        params.x=(oldX+event.rawX-downX).toInt().coerceIn(0,maxX)
                        params.y=(oldY+event.rawY-downY).toInt().coerceIn(0,maxY)
                        manager.updateViewLayout(marker,params)
                        line.moveHandle(modeAtCreation,index,
                            params.x+markerSize/2f,params.y+markerSize/2f)
                        // Table-edge handles move on their constrained axis.
                        true
                    }
                    MotionEvent.ACTION_CANCEL->true
                    else->false
                }
            }
            manager.addView(marker,params)
            activeMarkers.add(Pair(marker,params))
        }
    }

    override fun onDestroy(){
        removeMarkers()
        if(::panel.isInitialized&&panel.isAttachedToWindow)manager.removeView(panel)
        if(::line.isInitialized&&line.isAttachedToWindow)manager.removeView(line)
        super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null
}

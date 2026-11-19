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
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class AimOverlayService: Service() {
    private val wm by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private lateinit var line: AimLineView
    private lateinit var controls: LinearLayout
    private lateinit var lineParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams
    private lateinit var modeButton: TextView
    private lateinit var tableButton: TextView
    private lateinit var bounceButton: TextView
    private lateinit var ballButton: TextView
    private lateinit var radiusButton: TextView
    private var ballEnabled = false
    private var mode = AimLineView.Mode.LOCKED
    private var rebounds = 0
    private var started = false

    override fun onCreate() {
        super.onCreate()
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("aim_overlay","Mira visual ativa",NotificationManager.IMPORTANCE_LOW))
        val intent=PendingIntent.getActivity(
            this,0,Intent(this,MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification=Notification.Builder(this,"aim_overlay")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("CART3R Mira Longa")
            .setContentText("Mira manual ativa. Volte ao app para parar.")
            .setContentIntent(intent).setOngoing(true).build()
        if(Build.VERSION.SDK_INT>=34)
            startForeground(1001,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1001,notification)
    }

    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY}
        if(!started)showOverlay()
        return START_NOT_STICKY
    }

    private fun chip(text:String)=TextView(this).apply {
        this.text=text; textSize=12f; setTextColor(Color.WHITE)
        gravity=Gravity.CENTER; setPadding(dp(8),dp(10),dp(8),dp(10))
    }
    private fun showOverlay() {
        line=AimLineView(this)
        lineParams=WindowManager.LayoutParams(
            -1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity=Gravity.TOP or Gravity.START; alpha=0.75f }

        controls=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background=GradientDrawable().apply {
                setColor(Color.argb(240,25,8,16))
                cornerRadius=dp(12).toFloat()
                setStroke(dp(1),Color.rgb(255,44,79))
            }
        }
        val handle=chip("✥ CART3R / arrastar")
        modeButton=chip("✏ Ajustar mira")
        tableButton=chip("▣ Calibrar mesa")
        bounceButton=chip("↗ Rebotes: 0")
        ballButton=chip("● Colocar bola")
        radiusButton=chip("◯ Raio: 12")
        val hideBall=chip("× Ocultar bola")
        controls.addView(handle)
        controls.addView(modeButton)
        controls.addView(tableButton)
        controls.addView(bounceButton)
        controls.addView(ballButton)
        controls.addView(radiusButton)
        controls.addView(hideBall)
        panelParams=WindowManager.LayoutParams(
            dp(154),-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity=Gravity.TOP or Gravity.START; x=dp(12); y=dp(58) }
        modeButton.setOnClickListener {
            setMode(if(mode==AimLineView.Mode.AIM) AimLineView.Mode.LOCKED else AimLineView.Mode.AIM)
        }
        tableButton.setOnClickListener {
            setMode(if(mode==AimLineView.Mode.TABLE) AimLineView.Mode.LOCKED else AimLineView.Mode.TABLE)
        }
        ballButton.setOnClickListener {
            ballEnabled=true
            line.setBallEnabled(true)
            setMode(if(mode==AimLineView.Mode.BALL)AimLineView.Mode.LOCKED else AimLineView.Mode.BALL)
        }
        hideBall.setOnClickListener {
            ballEnabled=false
            line.setBallEnabled(false)
            setMode(AimLineView.Mode.LOCKED)
        }
        radiusButton.setOnClickListener {
            val size=line.cycleRadius()
            radiusButton.text="◯ Raio: $size"
        }
        bounceButton.setOnClickListener {
            rebounds=(rebounds+1)%4
            line.setRebounds(rebounds)
            bounceButton.text="↗ Rebotes: $rebounds"
        }
        var startX=0f; var startY=0f; var oldX=0; var oldY=0
        handle.setOnTouchListener { _,event ->
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX=event.rawX;startY=event.rawY
                    oldX=panelParams.x;oldY=panelParams.y;true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx=event.rawX-startX;val dy=event.rawY-startY
                    if(abs(dx)>2f||abs(dy)>2f){
                        panelParams.x=(oldX+dx).toInt().coerceAtLeast(0)
                        panelParams.y=(oldY+dy).toInt().coerceAtLeast(0)
                        wm.updateViewLayout(controls,panelParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        wm.addView(line,lineParams)
        try {
            wm.addView(controls,panelParams)
            started=true
        } catch(e:Exception) {
            wm.removeView(line); throw e
        }
    }
    private fun setMode(newMode:AimLineView.Mode) {
        mode=newMode
        line.setMode(newMode)
        val base=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        lineParams.flags=if(newMode==AimLineView.Mode.LOCKED)
            base or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else base
        wm.updateViewLayout(line,lineParams)
        modeButton.text=if(newMode==AimLineView.Mode.AIM) "✓ Travar mira" else "✏ Ajustar mira"
        tableButton.text=if(newMode==AimLineView.Mode.TABLE) "✓ Travar mesa" else "▣ Calibrar mesa"
        ballButton.text=if(newMode==AimLineView.Mode.BALL) "✓ Travar bola"
            else if(ballEnabled) "● Ajustar bola" else "● Colocar bola"
    }
    override fun onDestroy() {
        if(::controls.isInitialized&&controls.isAttachedToWindow)wm.removeView(controls)
        if(::line.isInitialized&&line.isAttachedToWindow)wm.removeView(line)
        super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}

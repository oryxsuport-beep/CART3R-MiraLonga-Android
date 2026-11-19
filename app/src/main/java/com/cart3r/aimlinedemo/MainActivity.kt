package com.cart3r.aimlinedemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity:Activity(){
    companion object{
        const val ACTION_REQUEST_CAPTURE="com.cart3r.aimlinedemo.REQUEST_CAPTURE"
        private const val REQUEST_SCREEN_CAPTURE=3917
    }
    private lateinit var status:TextView

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val layout=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            gravity=Gravity.CENTER_HORIZONTAL
            setPadding(dp(22),dp(28),dp(22),dp(20))
            setBackgroundColor(Color.rgb(7,7,10))
        }
        fun label(text:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply{
            this.text=text;textSize=size;setTextColor(color)
            gravity=Gravity.CENTER
            if(bold)setTypeface(Typeface.DEFAULT,Typeface.BOLD)
            setPadding(0,dp(9),0,dp(9))
        }
        fun button(title:String,onTap:()->Unit){
            val b=Button(this).apply{
                text=title;isAllCaps=false;setTextColor(Color.WHITE)
                backgroundTintList=ColorStateList.valueOf(Color.rgb(164,26,55))
                setOnClickListener{onTap()}
            }
            layout.addView(b,LinearLayout.LayoutParams(-1,dp(52)).apply{
                bottomMargin=dp(9)
            })
        }
        layout.addView(label("CART3R / MIRA LONGA V6",21f,Color.rgb(255,64,99),true))
        layout.addView(label("Detecção de bolas opcional, local e experimental",13f,Color.LTGRAY))
        status=label("Verificando permissão...",12f,Color.LTGRAY)
        layout.addView(status)
        button("1. Permitir aparecer sobre outros apps"){
            if(!Settings.canDrawOverlays(this)){
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            }else refreshStatus()
        }
        button("2. Iniciar linhas manuais"){
            if(!Settings.canDrawOverlays(this))status.text="Autorize a sobreposição primeiro."
            else{
                startForegroundService(Intent(this,AimOverlayService::class.java))
                status.text="Overlay iniciado. Abra seu jogo."
            }
        }
        button("3. Detectar bolas automaticamente"){
            requestCapture()
        }
        button("Parar detecção (sem fechar a mira)"){
            startService(Intent(this,AimOverlayService::class.java).apply{
                action=AimOverlayService.ACTION_STOP_AUTO
            })
            status.text="Detecção interrompida."
        }
        button("Encerrar mira e detecção"){
            stopService(Intent(this,AimOverlayService::class.java))
            status.text="Overlay encerrado."
        }
        layout.addView(label(
            "Escolha compartilhar a tela inteira e confirme o pedido do Android. " +
            "A análise fica no celular; nenhuma imagem é salva ou enviada.",
            12f,Color.rgb(175,164,175)
        ))
        layout.addView(label(
            "Calibre a mesa e o raio 6/9. O detector é experimental e pode " +
            "confundir bolas com elementos da tela. O ajuste manual continua disponível.",
            12f,Color.rgb(175,164,175)
        ))
        setContentView(layout)
        if(intent?.action==ACTION_REQUEST_CAPTURE)requestCapture()
    }
    override fun onNewIntent(intent:Intent){
        super.onNewIntent(intent)
        setIntent(intent)
        if(intent.action==ACTION_REQUEST_CAPTURE)requestCapture()
    }
    private fun requestCapture(){
        if(!Settings.canDrawOverlays(this)){
            status.text="Autorize primeiro a sobreposição."
            return
        }
        val manager=getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val request=if(Build.VERSION.SDK_INT>=34){
            manager.createScreenCaptureIntent(
                MediaProjectionConfig.createConfigForDefaultDisplay())
        }else manager.createScreenCaptureIntent()
        @Suppress("DEPRECATION")
        startActivityForResult(request,REQUEST_SCREEN_CAPTURE)
    }
    @Deprecated("Android legacy activity result callback")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode!=REQUEST_SCREEN_CAPTURE)return
        if(resultCode!=RESULT_OK||data==null){
            status.text="Permissão de captura não concedida."
            return
        }
        startForegroundService(Intent(this,AimOverlayService::class.java).apply{
            action=AimOverlayService.ACTION_START_AUTO
            putExtra(AimOverlayService.EXTRA_RESULT_CODE,resultCode)
            putExtra(AimOverlayService.EXTRA_RESULT_DATA,data)
        })
        status.text="Detecção iniciada com autorização. Volte à mesa."
    }
    override fun onResume(){
        super.onResume()
        if(::status.isInitialized)refreshStatus()
    }
    private fun refreshStatus(){
        status.text=if(Settings.canDrawOverlays(this))"Sobreposição autorizada"
            else "Permissão da sobreposição pendente"
    }
    private fun dp(px:Int)=(px*resources.displayMetrics.density).toInt()
}

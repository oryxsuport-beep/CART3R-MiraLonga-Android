package com.cart3r.aimlinedemo

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(42), dp(22), dp(24))
            setBackgroundColor(Color.rgb(7,7,10))
        }
        fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setPadding(0, dp(12), 0, dp(12))
            }
        fun button(title: String, onTap: () -> Unit) {
            val b = Button(this).apply {
                text = title
                isAllCaps = false
                setTextColor(Color.WHITE)
                backgroundTintList = ColorStateList.valueOf(Color.rgb(164,26,55))
                setOnClickListener { onTap() }
            }
            layout.addView(b, LinearLayout.LayoutParams(-1,dp(55)).apply { bottomMargin = dp(12) })
        }
        layout.addView(label("CART3R / MIRA LONGA V5", 21f, Color.rgb(255,64,99), true))
        layout.addView(label("Camada de mira que não bloqueia a tela. Sem root.", 13f, Color.LTGRAY))
        status = label("Verificando permissão...", 13f, Color.LTGRAY)
        layout.addView(status)
        button("1. Permitir aparecer sobre outros apps") {
            if (!Settings.canDrawOverlays(this))
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            else refreshStatus()
        }
        button("2. Iniciar mira longa") {
            if (!Settings.canDrawOverlays(this)) status.text = "Autorize a sobreposição primeiro."
            else {
                startForegroundService(Intent(this, AimOverlayService::class.java))
                status.text = "Sobreposição iniciada. Abra seu aplicativo para praticar."
            }
        }
        button("Parar mira") {
            stopService(Intent(this, AimOverlayService::class.java))
            status.text = "Sobreposição encerrada."
        }
        layout.addView(label(
            "Ajustar mira: arraste os 2 pontos. Calibrar mesa: ajuste as 4 bordas. " +
            "Use os pontos móveis e continue jogando. Raio 9 ou 6. Adicione até 7 bolas extras para prever colisões.",
            12f, Color.rgb(170,163,170)
        ))
        layout.addView(label(
            "Ajustes manuais. Colisões aproximadas, sem visão automática ou bypass.",
            12f, Color.rgb(170,163,170)
        ))
        setContentView(layout)
    }
    override fun onResume() { super.onResume(); if (::status.isInitialized) refreshStatus() }
    private fun refreshStatus() {
        status.text = if (Settings.canDrawOverlays(this)) "Sobreposição autorizada" else "Permissão pendente"
    }
    private fun dp(px: Int) = (px * resources.displayMetrics.density).toInt()
}

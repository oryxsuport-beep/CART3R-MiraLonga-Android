package com.cart3r.aimlinedemo

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

/**
 * User-consented, ephemeral screen capture, processed completely on-device.
 * Does not save frames, send traffic or interact with other app processes.
 *
 * On Android 14+ the caller must start a mediaProjection-type foreground
 * service and obtain fresh user consent for each session.
 */
class ScreenCaptureController(
    private val context:Context,
    private val onFrame:(pixels:IntArray,width:Int,height:Int)->Unit,
    private val onStopped:()->Unit
) {
    private val running=AtomicBoolean(false)
    private val thread=HandlerThread("CART3R-ball-vision")
    private lateinit var worker:Handler
    private var projection:MediaProjection?=null
    private var display:VirtualDisplay?=null
    private var reader:ImageReader?=null
    private var lastFrame=0L

    private val callback=object:MediaProjection.Callback(){
        override fun onStop(){
            stopInternal(notify=true,stopProjection=false)
        }
    }
    fun start(resultCode:Int,resultData:Intent){
        if(!running.compareAndSet(false,true))return
        thread.start()
        worker=Handler(thread.looper)
        try{
            val metrics=DisplayMetrics()
            @Suppress("DEPRECATION")
            val displayManager=context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            displayManager.defaultDisplay.getRealMetrics(metrics)
            val factor=min(1f,960f/metrics.widthPixels)
            val width=(metrics.widthPixels*factor).toInt().coerceAtLeast(80)
            val height=(metrics.heightPixels*factor).toInt().coerceAtLeast(80)
            val mgr=context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mp=mgr.getMediaProjection(resultCode,resultData)
                ?:throw IllegalStateException("Screen-capture permission unavailable")
            projection=mp
            mp.registerCallback(callback,worker)
            val captureReader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2)
            reader=captureReader
            captureReader.setOnImageAvailableListener({source->
                val image=try{source.acquireLatestImage()}catch(_:IllegalStateException){null}
                if(image!=null){
                    try {
                        if(running.get()){
                            val now=SystemClock.elapsedRealtime()
                            if(now-lastFrame>=650L) {
                                lastFrame=now
                                val pixels=readPixels(image)
                                if(pixels!=null&&running.get())onFrame(pixels,image.width,image.height)
                            }
                        }
                    }catch(_:Exception){
                        // Invalid or protected frames are simply ignored.
                    }finally{image.close()}
                }
            },worker)
            display=mp.createVirtualDisplay(
                "CART3R-live-ball-detection",
                width,height,metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                captureReader.surface,null,worker
            )
        }catch(e:Exception){
            stopInternal(notify=false,stopProjection=true)
            throw e
        }
    }
    private fun readPixels(image:Image):IntArray?{
        if(image.planes.isEmpty())return null
        val p=image.planes[0]
        if(p.pixelStride<4)return null
        val buffer=p.buffer
        val width=image.width
        val height=image.height
        if(width<80||height<80||width*height>5000000)return null
        val result=IntArray(width*height)
        for(y in 0 until height){
            val base=y*p.rowStride
            for(x in 0 until width){
                val offset=base+x*p.pixelStride
                if(offset+3>=buffer.limit())return null
                val r=buffer.get(offset).toInt() and 255
                val g=buffer.get(offset+1).toInt() and 255
                val b=buffer.get(offset+2).toInt() and 255
                result[y*width+x]=0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            }
        }
        return result
    }
    fun stop(){stopInternal(notify=false,stopProjection=true)}
    private fun stopInternal(notify:Boolean,stopProjection:Boolean){
        if(!running.getAndSet(false))return
        reader?.setOnImageAvailableListener(null,null)
        display?.release()
        display=null
        reader?.close()
        reader=null
        val activeProjection=projection
        projection=null
        activeProjection?.unregisterCallback(callback)
        if(stopProjection)activeProjection?.stop()
        if(thread.isAlive)thread.quitSafely()
        if(notify)onStopped()
    }
}

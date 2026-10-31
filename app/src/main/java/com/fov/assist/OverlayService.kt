// lang: Kotlin / file: OverlayService.kt / target: Pixel Assist
package com.fov.assist

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var canvasView: AimView? = null
    private var menu: LinearLayout? = null
    private var reopen: Button? = null
    private var fov = 15; private var yOffset = 0
    private var cross = true; private var diagonals = true; private var box = true; private var tracer = true; private var marker = true

    override fun onCreate() { super.onCreate(); wm = getSystemService(WINDOW_SERVICE) as WindowManager; makeChannel() }

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        if (i?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if (i == null) return START_NOT_STICKY
        val n = notification()
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFY_ID,n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(NOTIFY_ID,n)
        val code = i.getIntExtra(EXTRA_CODE, Activity.RESULT_CANCELED)
        val data = if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(EXTRA_DATA, Intent::class.java) else @Suppress("DEPRECATION") (i.getParcelableExtra(EXTRA_DATA) as? Intent)
        if (data == null) { stopSelf(); return START_NOT_STICKY }
        beginCapture(code,data); addOverlay()
        return START_NOT_STICKY
    }

    private fun beginCapture(code: Int, data: Intent) {
        try {
            val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = mgr.getMediaProjection(code,data)
            val m = resources.displayMetrics
            reader = ImageReader.newInstance(m.widthPixels,m.heightPixels,PixelFormat.RGBA_8888,2)
            projection?.registerCallback(object : MediaProjection.Callback() { override fun onStop() { stopSelf() } }, Handler(mainLooper))
            display = projection?.createVirtualDisplay("PixelAssist",m.widthPixels,m.heightPixels,m.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader?.surface,null,null)
        } catch (_: Exception) { stopSelf() }
    }

    private fun addOverlay() {
        canvasView = AimView(this).also { sync(it) }
        val p = WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT)
        wm.addView(canvasView,p)
        makeMenu(); showMenu()
    }

    private fun sync(v: AimView) { v.fov=fov; v.yOffset=yOffset; v.crosshair=cross; v.diagonals=diagonals; v.box=box; v.tracer=tracer; v.marker=marker; v.invalidate() }

    private fun makeMenu() {
        menu = LinearLayout(this).apply {
            orientation = 1; setPadding(12,8,12,8); setBackgroundColor(0xE8202832.toInt())
            addView(TextView(this@OverlayService).apply { text="PIXEL ASSIST • VISUAL TEST"; setTextColor(-1); textSize=14f })
            toggle("Crosshair",cross) { cross=it; refresh() }
            toggle("Diagonals",diagonals) { diagonals=it; refresh() }
            toggle("Test box",box) { box=it; refresh() }
            toggle("Tracer",tracer) { tracer=it; refresh() }
            toggle("Synthetic test marker",marker) { marker=it; refresh() }
            slider("FOV 5–35%",5,35,fov) { fov=it; refresh() }
            slider("Marker Y offset",-20,20,yOffset) { yOffset=it; refresh() }
            addView(Button(this@OverlayService).apply { text="Hide menu"; setOnClickListener { hideMenu() } })
        }
    }

    private fun LinearLayout.toggle(name:String, initial:Boolean, update:(Boolean)->Unit) {
        var enabled=initial
        addView(Button(this@OverlayService).apply {
            text="$name: ${if(enabled) "ON" else "OFF"}"
            setOnClickListener { enabled=!enabled; text="$name: ${if(enabled) "ON" else "OFF"}"; update(enabled) }
        })
    }

    private fun LinearLayout.slider(name:String,min:Int,max:Int,initial:Int,update:(Int)->Unit) {
        val label = TextView(this@OverlayService).apply { text="$name: $initial"; setTextColor(-1) }
        addView(label)
        addView(SeekBar(this@OverlayService).apply {
            this.max=max-min; progress=initial-min
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s:SeekBar?,p:Int,fromUser:Boolean) {
                    val v=p+min; label.text="$name: $v"; update(v)
                }
                override fun onStartTrackingTouch(s:SeekBar?) {}
                override fun onStopTrackingTouch(s:SeekBar?) {}
            })
        })
    }

    private fun refresh() { canvasView?.let(::sync) }

    private fun showMenu() {
        val v=menu ?: return
        if(v.parent==null) wm.addView(v,WindowManager.LayoutParams((resources.displayMetrics.widthPixels*.75f).toInt(),-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.START; x=12; y=70 })
        reopen?.let { try { wm.removeView(it) } catch (_:Exception) {} }; reopen=null
    }

    private fun hideMenu() {
        menu?.let { try { wm.removeView(it) } catch (_:Exception) {} }
        reopen=Button(this).apply { text="PA"; setOnClickListener { showMenu() } }
        wm.addView(reopen,WindowManager.LayoutParams(56,56,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.END; x=10; y=150 })
    }

    private fun makeChannel() {
        if(Build.VERSION.SDK_INT>=26) (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(CHANNEL,"Pixel Assist",NotificationManager.IMPORTANCE_LOW))
    }
    private fun notification(): Notification {
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop=PendingIntent.getService(this,1,Intent(this,OverlayService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Pixel Assist running").setContentText("Visual test overlay active").setOngoing(true).setContentIntent(open).addAction(android.R.drawable.ic_media_pause,"Stop",stop).build()
    }

    override fun onDestroy() {
        try { display?.release() } catch (_:Exception) {}; display=null
        try { reader?.close() } catch (_:Exception) {}; reader=null
        try { projection?.stop() } catch (_:Exception) {}; projection=null
        listOf(canvasView,menu,reopen).forEach { v -> if(v!=null) try { wm.removeView(v) } catch (_:Exception) {} }
        canvasView=null; menu=null; reopen=null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null

    companion object {
        const val EXTRA_CODE="capture_code"; const val EXTRA_DATA="capture_data"
        private const val CHANNEL="pixel_assist_channel"; private const val NOTIFY_ID=701
        private const val ACTION_STOP="com.fov.assist.STOP"
    }
}

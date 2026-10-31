package com.fov.assist

import android.app.*
import android.content.*
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import rikka.shizuku.Shizuku
import kotlin.math.max

class MainActivity : Activity() {
    private val captureCode = 4102
    private val shizukuCode = 4103
    private val imageCode = 4104
    private val prefs by lazy { getSharedPreferences("pixel_assist", MODE_PRIVATE) }
    private var target: String
        get() = prefs.getString("target_package", "") ?: ""
        set(value) { prefs.edit().putString("target_package", value).apply() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = 1
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 30, 24, 20)
        }
        root.addView(TextView(this).apply {
            text = "PIXEL ASSIST"
            textSize = 26f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Visual QA overlay • demo-only object tracking"
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 18)
        })
        button(root, "1. Grant overlay permission") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else toast("Overlay permission granted")
        }
        button(root, "2. Check / request Shizuku permission") {
            try {
                if (Shizuku.isPreV11() || !Shizuku.pingBinder()) toast("Start Shizuku first")
                else if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) toast("Shizuku permission granted")
                else Shizuku.requestPermission(shizukuCode)
            } catch (e: Exception) {
                toast("Shizuku unavailable: ${e.message}")
            }
        }
        button(root, "3. Select installed app") { chooseApp() }
        button(root, "4. Analyze an image") { chooseImage() }
        button(root, "5. Start visual overlay") {
            if (!Settings.canDrawOverlays(this)) {
                toast("Grant overlay permission first")
            } else {
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
                startActivityForResult(manager.createScreenCaptureIntent(), captureCode)
            }
        }
        button(root, "Stop overlay") {
            stopService(Intent(this, OverlayService::class.java))
            toast("Overlay stopped")
        }
        root.addView(TextView(this).apply {
            text = "Selected: ${target.ifBlank { "None" }}\nImage Analyzer reports dimensions and sampled brightness. Demo Detection/Tracking uses an app-drawn test object only."
            setPadding(0, 18, 0, 0)
            textSize = 12f
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun button(root: LinearLayout, label: String, action: () -> Unit) {
        root.addView(Button(this).apply {
            text = label
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 7
        })
    }

    private fun chooseApp() {
        val pm = packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString() }
        AlertDialog.Builder(this)
            .setTitle("Choose app")
            .setItems(apps.map { "${it.loadLabel(pm)} • ${it.activityInfo.packageName}" }.toTypedArray()) { _, i ->
                target = apps[i].activityInfo.packageName
                render()
                toast("App selected")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, imageCode)
    }

    private fun analyzeImage(uri: Uri) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                toast("Could not read this image")
                return
            }
            val sample = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, 512)
            }
            val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, sample) }
            if (bitmap == null) {
                toast("Could not decode this image")
                return
            }
            var total = 0.0
            var count = 0
            val stepX = max(1, bitmap.width / 64)
            val stepY = max(1, bitmap.height / 64)
            for (y in 0 until bitmap.height step stepY) {
                for (x in 0 until bitmap.width step stepX) {
                    val pixel = bitmap.getPixel(x, y)
                    val red = (pixel shr 16) and 0xFF
                    val green = (pixel shr 8) and 0xFF
                    val blue = pixel and 0xFF
                    total += 0.299 * red + 0.587 * green + 0.114 * blue
                    count++
                }
            }
            val brightness = if (count > 0) (total / count).toInt() else 0
            bitmap.recycle()
            AlertDialog.Builder(this)
                .setTitle("Image analysis")
                .setMessage("Resolution: ${bounds.outWidth} × ${bounds.outHeight}\nSampled average brightness: $brightness / 255\n\nThis tool measures basic image properties; it does not identify people or game opponents.")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            toast("Image analysis failed: ${e.message}")
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= maxSide || height / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    @Deprecated("Platform activity result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == imageCode && resultCode == RESULT_OK) {
            data?.data?.let(::analyzeImage)
            return
        }
        if (requestCode == captureCode && resultCode == RESULT_OK && data != null) {
            val service = Intent(this, OverlayService::class.java).apply {
                putExtra(OverlayService.EXTRA_CODE, resultCode)
                putExtra(OverlayService.EXTRA_DATA, data)
            }
            androidx.core.content.ContextCompat.startForegroundService(this, service)
            if (target.isNotBlank()) packageManager.getLaunchIntentForPackage(target)?.let(::startActivity)
            toast("Overlay started")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == shizukuCode) {
            toast(if (results.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED) "Shizuku granted" else "Shizuku denied")
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}

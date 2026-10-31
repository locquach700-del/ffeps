// lang: Kotlin / file: MainActivity.kt / target: Pixel Assist
package com.fov.assist

import android.app.*
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private val captureCode = 4102
    private val shizukuCode = 4103
    private val prefs by lazy { getSharedPreferences("pixel_assist", MODE_PRIVATE) }
    private var target: String
        get() = prefs.getString("target_package", "") ?: ""
        set(value) { prefs.edit().putString("target_package", value).apply() }

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }

    private fun render() {
        val root = LinearLayout(this).apply { orientation = 1; gravity = Gravity.CENTER_HORIZONTAL; setPadding(24, 30, 24, 20) }
        root.addView(TextView(this).apply { text = "PIXEL ASSIST"; textSize = 26f; gravity = Gravity.CENTER })
        root.addView(TextView(this).apply { text = "Visual QA overlay • no automated game input"; textSize = 13f; gravity = Gravity.CENTER; setPadding(0,8,0,18) })
        button(root, "1. Grant overlay permission") {
            if (!Settings.canDrawOverlays(this)) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            else toast("Overlay permission granted")
        }
        button(root, "2. Request Shizuku permission") {
            try {
                if (Shizuku.isPreV11() || !Shizuku.pingBinder()) toast("Start Shizuku first")
                else if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) toast("Shizuku permission granted")
                else Shizuku.requestPermission(shizukuCode)
            } catch (e: Exception) { toast("Shizuku unavailable: ${e.message}") }
        }
        button(root, "3. Select installed app") { chooseApp() }
        button(root, "4. Start visual overlay") {
            if (!Settings.canDrawOverlays(this)) toast("Grant overlay permission first")
            else startActivityForResult((getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager).createScreenCaptureIntent(), captureCode)
        }
        button(root, "Stop overlay") { stopService(Intent(this, OverlayService::class.java)); toast("Overlay stopped") }
        root.addView(TextView(this).apply { text = "Selected: ${target.ifBlank { "None" }}\nShizuku is permission-status only. No swipe commands are sent."; setPadding(0,18,0,0); textSize = 12f })
        setContentView(root)
    }

    private fun button(root: LinearLayout, label: String, action: () -> Unit) {
        root.addView(Button(this).apply { text = label; setOnClickListener { action() } },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 7 })
    }

    private fun chooseApp() {
        val pm = packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .distinctBy { it.activityInfo.packageName }.sortedBy { it.loadLabel(pm).toString() }
        AlertDialog.Builder(this).setTitle("Choose app").setItems(apps.map { "${it.loadLabel(pm)} • ${it.activityInfo.packageName}" }.toTypedArray()) { _, i ->
            target = apps[i].activityInfo.packageName; render(); toast("App selected")
        }.setNegativeButton("Cancel", null).show()
    }

    @Deprecated("Platform MediaProjection result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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
        if (requestCode == shizukuCode) toast(if (results.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED) "Shizuku granted" else "Shizuku denied")
    }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}

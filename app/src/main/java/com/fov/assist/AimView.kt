package com.fov.assist

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import kotlin.math.min
import kotlin.math.sin

class AimView(context: Context) : View(context) {
    var fov = 15
    var yOffset = 0
    var crosshair = true
    var crosshairScale = 1
    var diagonals = true
    var box = true
    var tracer = true
    var marker = true
    var demoDetection = false
    var demoTracking = false

    private val white = Paint(3).apply { color = 0xCCFFFFFF.toInt(); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val green = Paint(3).apply { color = 0xFF35E0A1.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE }
    private val cyan = Paint(3).apply { color = 0xFF43C7FF.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE }
    private val label = Paint(3).apply { color = 0xFF35E0A1.toInt(); textSize = 30f; style = Paint.Style.FILL }
    private val demoLabel = Paint(3).apply { color = 0xFF43C7FF.toInt(); textSize = 27f; style = Paint.Style.FILL }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        val x = w / 2f
        val y = h / 2f
        val r = min(w, h) * (fov.coerceIn(5, 35) / 100f)

        if (diagonals) {
            c.drawLine(0f, 0f, w, h, white)
            c.drawLine(w, 0f, 0f, h, white)
        }
        c.drawCircle(x, y, r, green)

        if (crosshair) {
            val arm = 12f * crosshairScale.coerceIn(1, 4)
            c.drawLine(x - arm, y, x + arm, y, green)
            c.drawLine(x, y - arm, x, y + arm, green)
            c.drawCircle(x, y, 3f, green)
        }

        if (marker) {
            val tx = x + r * .45f
            val ty = y + r * (.12f + yOffset / 100f)
            if (tracer) c.drawLine(x, y, tx, ty, green)
            if (box) c.drawRect(RectF(tx - 24, ty - 34, tx + 24, ty + 34), green)
            c.drawCircle(tx, ty, 5f, green)
            c.drawText("TEST", tx + 30, ty, label)
        }

        if (demoDetection || demoTracking) {
            val phase = SystemClock.uptimeMillis() / 850.0
            val demoX = if (demoTracking) x + (sin(phase) * r * .48).toFloat() else x + r * .38f
            val demoY = if (demoTracking) y + (sin(phase * .72) * r * .30).toFloat() else y - r * .30f
            val halfW = 34f
            val halfH = 48f
            c.drawRoundRect(RectF(demoX - halfW, demoY - halfH, demoX + halfW, demoY + halfH), 8f, 8f, cyan)
            c.drawCircle(demoX, demoY, 5f, cyan)
            c.drawText(if (demoTracking) "DEMO TRACK" else "DEMO OBJECT", demoX + 42f, demoY, demoLabel)
            if (demoTracking) postInvalidateDelayed(33)
        }
    }
}

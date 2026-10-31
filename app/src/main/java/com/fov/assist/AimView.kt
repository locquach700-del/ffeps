// lang: Kotlin / file: AimView.kt / target: Pixel Assist
package com.fov.assist

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.min

class AimView(context: Context) : View(context) {
    var fov = 15
    var yOffset = 0
    var crosshair = true
    var diagonals = true
    var box = true
    var tracer = true
    var marker = true
    private val white = Paint(3).apply { color = 0xCCFFFFFF.toInt(); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val green = Paint(3).apply { color = 0xFF35E0A1.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE }
    private val label = Paint(3).apply { color = 0xFF35E0A1.toInt(); textSize = 30f; style = Paint.Style.FILL }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        val x = w / 2f; val y = h / 2f
        val r = min(w, h) * (fov.coerceIn(5,35) / 100f)
        if (diagonals) { c.drawLine(0f,0f,w,h,white); c.drawLine(w,0f,0f,h,white) }
        c.drawCircle(x,y,r,green)
        if (crosshair) { c.drawLine(x-12,y,x+12,y,green); c.drawLine(x,y-12,x,y+12,green) }
        if (marker) {
            val tx = x + r * .45f; val ty = y + r * (.12f + yOffset / 100f)
            if (tracer) c.drawLine(x,y,tx,ty,green)
            if (box) c.drawRect(RectF(tx-24,ty-34,tx+24,ty+34),green)
            c.drawCircle(tx,ty,5f,green); c.drawText("TEST",tx+30,ty,label)
        }
    }
}

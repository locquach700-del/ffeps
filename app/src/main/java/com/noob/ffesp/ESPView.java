// lang: Java | file: ESPView.java | target: Android 12-13
// Custom View — vẽ ESP box (hộp) + snapline (tia) cho mỗi entity địch

package com.noob.ffesp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ESPView extends View {

    // ── Enemy data ────────────────────────────────────────────────────────
    public static class Enemy {
        public float x, y;       // screen X,Y của đầu (top-left box)
        public float w, h;       // width, height của box
        public float footX, footY; // screen pos chân — điểm kéo line tới
        public float hp;         // 0.0 - 100.0
        public int   distance;   // meters, ước tính
        public boolean visible;  // đang trong LOS hay không
    }

    // ── Paints ────────────────────────────────────────────────────────────
    private final Paint pBox     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBoxFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLine    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pHpBg    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pHpFg    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pText    = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── State ─────────────────────────────────────────────────────────────
    private final List<Enemy> enemies = new ArrayList<>();
    private final Object      lock    = new Object();
    private boolean espEnabled  = true;
    private boolean showLines   = true;
    private boolean showBoxes   = true;
    private boolean showHp      = true;
    private boolean showDist    = true;

    private final Handler renderHandler = new Handler(Looper.getMainLooper());
    private boolean rendering = false;

    private static final long FRAME_MS = 16; // ~60fps

    public ESPView(Context ctx) {
        super(ctx);
        setWillNotDraw(false);
        initPaints();
    }

    private void initPaints() {
        // Box outline — đỏ
        pBox.setColor(Color.RED);
        pBox.setStyle(Paint.Style.STROKE);
        pBox.setStrokeWidth(2.5f);

        // Box fill — đỏ rất mờ
        pBoxFill.setColor(0x15FF0000);
        pBoxFill.setStyle(Paint.Style.FILL);

        // Snapline — vàng, từ bottom-center màn hình đến chân địch
        pLine.setColor(Color.YELLOW);
        pLine.setStyle(Paint.Style.STROKE);
        pLine.setStrokeWidth(1.2f);
        pLine.setAlpha(180);

        // HP bar background — xám tối
        pHpBg.setColor(0xFF333333);
        pHpBg.setStyle(Paint.Style.FILL);

        // HP bar fill — xanh lá → đỏ theo % HP
        pHpFg.setStyle(Paint.Style.FILL);

        // Text — khoảng cách, HP số
        pText.setColor(Color.WHITE);
        pText.setTextSize(22f);
        pText.setTypeface(Typeface.MONOSPACE);
        pText.setShadowLayer(3f, 1f, 1f, Color.BLACK);
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setEnemies(List<Enemy> newList) {
        synchronized (lock) {
            enemies.clear();
            enemies.addAll(newList);
        }
    }

    public void setESPEnabled(boolean v)  { espEnabled  = v; invalidate(); }
    public void setShowLines(boolean v)   { showLines   = v; }
    public void setShowBoxes(boolean v)   { showBoxes   = v; }
    public void setShowHp(boolean v)      { showHp      = v; }
    public void setShowDist(boolean v)    { showDist    = v; }
    public boolean isESPEnabled()         { return espEnabled; }

    public void startRendering() {
        rendering = true;
        scheduleFrame();
    }

    public void stopRendering() {
        rendering = false;
        renderHandler.removeCallbacksAndMessages(null);
    }

    // ── Render loop ───────────────────────────────────────────────────────

    private void scheduleFrame() {
        if (!rendering) return;
        renderHandler.postDelayed(() -> {
            invalidate();
            scheduleFrame();
        }, FRAME_MS);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!espEnabled) return;

        float screenW = getWidth();
        float screenH = getHeight();
        float originX = screenW / 2f; // điểm gốc snapline — bottom center
        float originY = screenH;

        List<Enemy> snapshot;
        synchronized (lock) {
            snapshot = new ArrayList<>(enemies);
        }

        for (Enemy e : snapshot) {
            if (!e.visible) continue;

            // ── Snapline — vàng ───────────────────────────────────────────
            if (showLines) {
                canvas.drawLine(originX, originY, e.footX, e.footY, pLine);
            }

            // ── Box fill ──────────────────────────────────────────────────
            if (showBoxes) {
                canvas.drawRect(e.x, e.y, e.x + e.w, e.y + e.h, pBoxFill);
                canvas.drawRect(e.x, e.y, e.x + e.w, e.y + e.h, pBox);

                // Corner accents — bo góc box bằng 4 đường ngắn
                float cLen = e.w * 0.2f;
                float stroke = 4f;
                Paint pCorner = new Paint(pBox);
                pCorner.setStrokeWidth(stroke);
                pCorner.setColor(0xFFFF4444);

                // Top-left
                canvas.drawLine(e.x, e.y, e.x + cLen, e.y,            pCorner);
                canvas.drawLine(e.x, e.y, e.x,         e.y + cLen,    pCorner);
                // Top-right
                canvas.drawLine(e.x+e.w, e.y, e.x+e.w - cLen, e.y,   pCorner);
                canvas.drawLine(e.x+e.w, e.y, e.x+e.w, e.y + cLen,   pCorner);
                // Bottom-left
                canvas.drawLine(e.x, e.y+e.h, e.x+cLen, e.y+e.h,     pCorner);
                canvas.drawLine(e.x, e.y+e.h, e.x, e.y+e.h - cLen,   pCorner);
                // Bottom-right
                canvas.drawLine(e.x+e.w, e.y+e.h, e.x+e.w-cLen, e.y+e.h, pCorner);
                canvas.drawLine(e.x+e.w, e.y+e.h, e.x+e.w, e.y+e.h-cLen, pCorner);
            }

            // ── HP bar — bên trái box ─────────────────────────────────────
            if (showHp) {
                float barX  = e.x - 8f;
                float barH  = e.h;
                float fillH = barH * (e.hp / 100f);

                // Background
                canvas.drawRect(barX - 4f, e.y, barX, e.y + barH, pHpBg);

                // Fill color: xanh → vàng → đỏ
                int hpColor;
                if (e.hp > 60)      hpColor = 0xFF00FF44;
                else if (e.hp > 30) hpColor = 0xFFFFFF00;
                else                hpColor = 0xFFFF2222;

                pHpFg.setColor(hpColor);
                canvas.drawRect(barX - 4f,
                    e.y + barH - fillH,
                    barX,
                    e.y + barH,
                    pHpFg);

                // HP text
                canvas.drawText(
                    (int) e.hp + "",
                    barX - 3f,
                    e.y - 4f,
                    pText);
            }

            // ── Distance label ────────────────────────────────────────────
            if (showDist && e.distance > 0) {
                canvas.drawText(
                    e.distance + "m",
                    e.x + e.w / 2f - 14f,
                    e.y - 6f,
                    pText);
            }
        }
    }
}

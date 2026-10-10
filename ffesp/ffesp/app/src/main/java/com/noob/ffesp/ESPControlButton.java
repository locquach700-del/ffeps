// lang: Java | file: ESPControlButton.java | target: Android 12-13
// Nút toggle nổi — kéo được, tap để bật/tắt ESP

package com.noob.ffesp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class ESPControlButton {

    private final Context     ctx;
    private final ESPView     espView;
    private final WindowManager wm;
    private TextView          btn;
    private WindowManager.LayoutParams params;

    // Drag state
    private int initX, initY;
    private float initTouchX, initTouchY;

    public ESPControlButton(Context ctx, ESPView espView, WindowManager wm) {
        this.ctx     = ctx;
        this.espView = espView;
        this.wm      = wm;
    }

    public void attach() {
        btn = new TextView(ctx);
        btn.setText("ESP\nON");
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(11f);
        btn.setGravity(android.view.Gravity.CENTER);
        btn.setBackgroundColor(0xCC1A1A2E);
        btn.setPadding(16, 12, 16, 12);

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 200;

        btn.setOnTouchListener(new View.OnTouchListener() {
            long lastDown = 0;
            boolean dragged = false;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initX      = params.x;
                        initY      = params.y;
                        initTouchX = e.getRawX();
                        initTouchY = e.getRawY();
                        lastDown   = System.currentTimeMillis();
                        dragged    = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int)(e.getRawX() - initTouchX);
                        int dy = (int)(e.getRawY() - initTouchY);
                        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) dragged = true;
                        params.x = initX + dx;
                        params.y = initY + dy;
                        wm.updateViewLayout(btn, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!dragged &&
                            System.currentTimeMillis() - lastDown < 300) {
                            // Toggle ESP
                            boolean now = !espView.isESPEnabled();
                            espView.setESPEnabled(now);
                            btn.setText(now ? "ESP\nON" : "ESP\nOFF");
                            btn.setBackgroundColor(
                                now ? 0xCC1A1A2E : 0xCC2E1A1A);
                        }
                        return true;
                }
                return false;
            }
        });

        wm.addView(btn, params);
    }

    public void detach() {
        if (btn != null) {
            wm.removeView(btn);
            btn = null;
        }
    }
}

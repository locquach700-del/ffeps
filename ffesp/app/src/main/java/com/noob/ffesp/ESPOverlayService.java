// lang: Java | file: ESPOverlayService.java | target: Android 12-13
// Foreground service — attach ESPView lên WindowManager, giữ overlay sống

package com.noob.ffesp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.WindowManager;

public class ESPOverlayService extends Service {

    public static boolean isRunning = false;
    private static final String CH_ID = "esp_ch";

    private WindowManager      wm;
    private ESPView            espView;
    private ESPControlButton   controlBtn;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

        createChannel();
        startForeground(42, buildNotif());

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        // ── Overlay ESP canvas ────────────────────────────────────────────
        espView = new ESPView(this);
        WindowManager.LayoutParams espParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE  |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE  |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        wm.addView(espView, espParams);

        // ── Control button — góc trên phải, tap để toggle ESP ─────────────
        controlBtn = new ESPControlButton(this, espView, wm);
        controlBtn.attach();

        // Bắt đầu render loop + fake data feed (thay bằng real mem read khi root)
        espView.startRendering();
        MockDataFeed.start(espView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        MockDataFeed.stop();
        espView.stopRendering();
        controlBtn.detach();
        wm.removeView(espView);
    }

    @Override
    public IBinder onBind(Intent i) { return null; }

    private void createChannel() {
        NotificationChannel ch = new NotificationChannel(
            CH_ID, "ESP Overlay", NotificationManager.IMPORTANCE_LOW);
        ch.setSound(null, null);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    private Notification buildNotif() {
        return new Notification.Builder(this, CH_ID)
            .setContentTitle("FF ESP Active")
            .setContentText("Overlay đang chạy — tap nút để toggle")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build();
    }
}

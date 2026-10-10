// lang: Java | file: MainActivity.java | target: Android 12-13
// UI chính — xin permission overlay, chọn package FF, launch overlay service

package com.noob.ffesp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 1001;

    // Free Fire package names
    private static final String[] FF_PACKAGES = {
        "com.dts.freefireth",      // FF Global
        "com.dts.freefiremax",     // FF MAX Global
        "com.garena.game.fftw",    // FF TW
        "com.garena.game.ffvn",    // FF VN
    };

    private Button  btnStart, btnStop;
    private TextView tvStatus, tvVersion;
    private boolean espRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart  = findViewById(R.id.btnStart);
        btnStop   = findViewById(R.id.btnStop);
        tvStatus  = findViewById(R.id.tvStatus);
        tvVersion = findViewById(R.id.tvVersion);

        tvVersion.setText("FF ESP OB55 | by Noob");

        btnStart.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission();
            } else {
                launchESP();
            }
        });

        btnStop.setOnClickListener(v -> stopESP());
        setUI(false);
    }

    private void requestOverlayPermission() {
        Intent i = new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName()));
        startActivityForResult(i, REQ_OVERLAY);
        Toast.makeText(this,
            "Bật 'Hiển thị trên ứng dụng khác' rồi quay lại",
            Toast.LENGTH_LONG).show();
    }

    private void launchESP() {
        // Tìm và mở Free Fire
        String ffPkg = findFFPackage();
        if (ffPkg != null) {
            Intent ff = getPackageManager().getLaunchIntentForPackage(ffPkg);
            if (ff != null) {
                ff.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(ff);
            }
        } else {
            Toast.makeText(this, "Không tìm thấy Free Fire", Toast.LENGTH_SHORT).show();
        }

        // Start ESP overlay service
        Intent svc = new Intent(this, ESPOverlayService.class);
        startForegroundService(svc);

        espRunning = true;
        tvStatus.setText("● ESP ĐANG CHẠY");
        tvStatus.setTextColor(0xFF00FF44);
        setUI(true);
    }

    private void stopESP() {
        stopService(new Intent(this, ESPOverlayService.class));
        espRunning = false;
        tvStatus.setText("● ESP TẮT");
        tvStatus.setTextColor(0xFFFF4444);
        setUI(false);
    }

    private String findFFPackage() {
        for (String pkg : FF_PACKAGES) {
            try {
                getPackageManager().getPackageInfo(pkg, 0);
                return pkg;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void setUI(boolean running) {
        btnStart.setEnabled(!running);
        btnStop.setEnabled(running);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                launchESP();
            } else {
                Toast.makeText(this, "Permission bị từ chối", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cập nhật trạng thái khi quay lại từ settings
        if (espRunning && !ESPOverlayService.isRunning) {
            setUI(false);
            tvStatus.setText("● ESP TẮT");
            tvStatus.setTextColor(0xFFFF4444);
            espRunning = false;
        }
    }
}

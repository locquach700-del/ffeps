// lang: Java | file: MockDataFeed.java | target: Android 12-13
// Giả lập enemy data — thay thế bằng MemReader.java khi có root
// Dùng để test UI, overlay, và box/line render

package com.noob.ffesp;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MockDataFeed {

    private static HandlerThread thread;
    private static Handler       handler;
    private static boolean       running = false;

    private static final Random rng = new Random();

    public static void start(ESPView view) {
        thread  = new HandlerThread("MockFeed");
        thread.start();
        handler = new Handler(thread.getLooper());
        running = true;
        scheduleTick(view);
    }

    public static void stop() {
        running = false;
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (thread  != null) thread.quitSafely();
    }

    private static void scheduleTick(ESPView view) {
        if (!running) return;
        handler.postDelayed(() -> {
            view.setEnemies(generateEnemies(view.getWidth(), view.getHeight()));
            scheduleTick(view);
        }, 50); // 20Hz update — đủ mượt cho test
    }

    // Sinh 2-5 địch ngẫu nhiên trên màn hình
    private static List<ESPView.Enemy> generateEnemies(int sw, int sh) {
        List<ESPView.Enemy> list = new ArrayList<>();
        if (sw == 0 || sh == 0) return list;

        int count = 2 + rng.nextInt(4);
        for (int i = 0; i < count; i++) {
            ESPView.Enemy e = new ESPView.Enemy();

            // Box size tỉ lệ với khoảng cách giả định
            e.distance = 20 + rng.nextInt(200);
            float scale = 1f - (e.distance / 300f);
            e.w = Math.max(40f, sw * 0.06f * scale);
            e.h = e.w * 2.8f; // tỉ lệ người đứng

            // Vị trí ngẫu nhiên trong safe zone
            e.x = 40 + rng.nextFloat() * (sw - e.w - 80);
            e.y = 40 + rng.nextFloat() * (sh - e.h - 120);

            // Chân = bottom-center của box
            e.footX = e.x + e.w / 2f;
            e.footY = e.y + e.h;

            // HP random, nhảy nhẹ mỗi tick
            e.hp      = 10 + rng.nextFloat() * 90f;
            e.visible = true;

            list.add(e);
        }
        return list;
    }
}

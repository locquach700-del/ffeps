// lang: Java | file: MemReader.java | target: Android 12-13 ROOT
// Memory reader qua /proc/pid/mem — chỉ hoạt động khi root
// Thay MockDataFeed bằng class này khi device đã root

package com.noob.ffesp;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MemReader {

    // ── Offsets OB55 — re-derive sau mỗi patch ────────────────────────────
    // Dùng il2cppdumper + Ghidra để lấy offset mới
    private static final long OFF_GAME_MANAGER = 0x123456L; // placeholder
    private static final long OFF_PLAYER_LIST  = 0x78L;
    private static final long OFF_LOCAL_PLAYER = 0x80L;
    private static final long OFF_POSITION     = 0x90L;
    private static final long OFF_HEALTH        = 0x1A4L;
    private static final long OFF_TEAM_ID       = 0x108L;
    private static final long OFF_IS_ALIVE      = 0x160L;
    private static final long OFF_BONE_HEAD     = 0x230L;
    private static final long OFF_BONE_CHEST    = 0x254L;
    private static final long OFF_BONE_HIP      = 0x278L;

    private RandomAccessFile memFile;
    private final int pid;

    public MemReader(int pid) throws Exception {
        this.pid = pid;
        memFile  = new RandomAccessFile(
            new File("/proc/" + pid + "/mem"), "r");
    }

    // ── Đọc N bytes từ địa chỉ addr ──────────────────────────────────────
    public byte[] read(long addr, int size) {
        try {
            memFile.seek(addr);
            byte[] buf = new byte[size];
            memFile.readFully(buf);
            return buf;
        } catch (Exception e) {
            return new byte[size];
        }
    }

    public long readLong(long addr) {
        return ByteBuffer.wrap(read(addr, 8))
            .order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    public float readFloat(long addr) {
        return ByteBuffer.wrap(read(addr, 4))
            .order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public int readInt(long addr) {
        return ByteBuffer.wrap(read(addr, 4))
            .order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    // ── Tìm PID của Free Fire ─────────────────────────────────────────────
    public static int findFFPid() {
        File proc = new File("/proc");
        String[] targets = {
            "com.dts.freefireth",
            "com.dts.freefiremax",
            "com.garena.game.ffvn"
        };
        for (File f : proc.listFiles()) {
            try {
                int pid = Integer.parseInt(f.getName());
                File cmdline = new File("/proc/" + pid + "/cmdline");
                if (!cmdline.exists()) continue;
                RandomAccessFile raf = new RandomAccessFile(cmdline, "r");
                byte[] buf = new byte[256];
                int n = raf.read(buf);
                raf.close();
                String name = new String(buf, 0, n).replace('\0', ' ').trim();
                for (String t : targets)
                    if (name.contains(t)) return pid;
            } catch (Exception ignored) {}
        }
        return -1;
    }

    // ── Đọc base address của libil2cpp.so từ /proc/pid/maps ──────────────
    public static long findModuleBase(int pid, String module) {
        try {
            File maps = new File("/proc/" + pid + "/maps");
            RandomAccessFile raf = new RandomAccessFile(maps, "r");
            String line;
            while ((line = raf.readLine()) != null) {
                if (line.contains(module) && line.contains("r-xp")) {
                    String addrPart = line.split("-")[0];
                    raf.close();
                    return Long.parseLong(addrPart, 16);
                }
            }
            raf.close();
        } catch (Exception ignored) {}
        return 0L;
    }

    public void close() {
        try { if (memFile != null) memFile.close(); }
        catch (Exception ignored) {}
    }

    // ── Hướng dẫn re-derive offset sau patch ─────────────────────────────
    // 1. Pull libil2cpp.so từ APK mới: adb pull /data/app/.../libil2cpp.so
    // 2. Chạy il2cppdumper → dump.cs + script.json
    // 3. Load Ghidra, apply Il2CppInspector script
    // 4. Tìm class: EntityManager, PlayerEntity, LocalPlayer, BoneSystem
    // 5. Lấy field offset từ struct viewer
    // 6. Cập nhật các hằng OFF_* ở trên
}

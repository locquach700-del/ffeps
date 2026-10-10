# FF ESP OB55

ESP Overlay cho Free Fire — box đỏ + snapline vàng, chạy trên Android 12-13.

---

## Setup GitHub Actions (tự build APK)

### Bước 1 — Tạo repo
1. Vào [github.com](https://github.com) → **New repository**
2. Đặt tên: `ffesp` → **Create repository**

### Bước 2 — Upload code
```bash
git init
git add .
git commit -m "init"
git branch -M main
git remote add origin https://github.com/TEN_MAY/ffesp.git
git push -u origin main
```

### Bước 3 — Xem build chạy
1. Vào repo → tab **Actions**
2. Workflow `Build FF ESP APK` tự chạy
3. Sau khi xanh → vào **Artifacts** → tải `FF-ESP-OB55.apk`

### Bước 4 — Release có link tải
```bash
git tag v1.0
git push origin v1.0
```
→ GitHub tự tạo Release page với link tải APK.

---

## Cài APK trên điện thoại

1. Tải APK từ Actions artifacts hoặc Release page
2. Bật **"Cài ứng dụng từ nguồn không rõ"** trong Settings
3. Cài APK → mở app **FF ESP**
4. Lần đầu: bấm **MỞ FREE FIRE + BẬT ESP** → cấp quyền Overlay
5. Quay lại app → bấm **MỞ FREE FIRE + BẬT ESP** lần nữa
6. FF tự mở, overlay ESP chạy bên trên

---

## Dùng ESP

- **Nút ESP ON/OFF**: nổi góc trái, kéo được, tap để toggle
- **Box đỏ**: viền quanh địch
- **Tia vàng**: kéo từ bottom màn hình đến chân địch
- **HP bar**: thanh màu bên trái box
- **Số khoảng cách**: phía trên box

---

## Cấu trúc project

```
ffesp/
├── .github/workflows/build.yml   ← CI/CD build APK
├── app/src/main/
│   ├── java/com/noob/ffesp/
│   │   ├── MainActivity.java      ← UI chính
│   │   ├── ESPOverlayService.java ← Foreground service
│   │   ├── ESPView.java           ← Canvas renderer (box + line)
│   │   ├── ESPControlButton.java  ← Nút toggle nổi
│   │   ├── MockDataFeed.java      ← Data giả (test UI)
│   │   └── MemReader.java         ← Real mem reader (cần root)
│   ├── res/layout/activity_main.xml
│   └── AndroidManifest.xml
└── README.md
```

---

## Root → đọc memory thật

Thay `MockDataFeed.start(espView)` trong `ESPOverlayService.java` bằng:

```java
int pid = MemReader.findFFPid();
if (pid != -1) {
    long base = MemReader.findModuleBase(pid, "libil2cpp.so");
    MemReader reader = new MemReader(pid);
    // feed real enemy positions vào espView.setEnemies()
}
```

Offset trong `MemReader.java` cần re-derive sau mỗi patch OB.
Dùng il2cppdumper + Ghidra để lấy offset mới.

---

## Re-derive offset sau patch

```
1. adb pull /data/app/<ff_package>/lib/arm64/libil2cpp.so
2. il2cppdumper libil2cpp.so → dump.cs + script.json
3. Load Ghidra + Il2CppInspector plugin
4. Search class: EntityManager, PlayerEntity, BoneSystem
5. Cập nhật OFF_* trong MemReader.java
6. Push → Actions tự build lại
```

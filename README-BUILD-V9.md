# KasirPOS V9 — build APK

## Opsi 1: Android Studio (Windows)
1. Buka folder `AndroidStudio-KasirPOS` menggunakan Android Studio versi baru.
2. Pastikan internet aktif untuk mengunduh Android SDK 35, Gradle, dan dependencies AndroidX.
3. Edit `app/build.gradle` bagian `POS_URL` sesuai lokasi hosting HTTPS yang benar (default `/POS/pos.php`).
4. Klik Build > Build App Bundle(s) / APK(s) > Build APK(s).
5. Ambil APK dari `app/build/outputs/apk/debug/app-debug.apk`.
6. Instal di Android untuk pengujian saja. Debug APK bukan paket rilis yang ditandatangani secara aman.

## Opsi 2: GitHub Actions (tanpa Android Studio lokal)
1. Buat repo GitHub khusus Android KasirPOS; upload **isi** folder `AndroidStudio-KasirPOS` ke root repo, termasuk folder `.github`.
2. Pastikan Actions aktif; buka tab Actions lalu pilih `Build KasirPOS Android APK` > `Run workflow`.
3. Setelah workflow sukses, unduh artifact `KasirPOS-V9-debug-apk`, ekstrak, instal `app-debug.apk`.
4. Sesuaikan POS_URL **sebelum** upload/build.

**PENTING**: Tidak ada APK prebuilt di paket V9 ini; build belum dapat diuji pada environment pembuat paket karena Android SDK/Gradle tidak terpasang. Bukan sertifikasi siap produksi.

## Uji wajib sebelum digunakan
- Jalankan login saat online, muat menu/katalog, offline lalu buat transaksi percobaan.
- Pastikan status pending muncul dan transaksi tidak hilang setelah aplikasi ditutup/dibuka kembali.
- Hubungkan internet, tunggu WorkManager/tekan sinkron manual; periksa hanya satu transaksi di hosting.
- Uji ulang dengan sesi login kedaluwarsa dan respons server error.
- Hindari reset aplikasi/clear data selama ada transaksi pending.
- Pastikan Google Drive media/backup berfungsi melalui OAuth sesuai konfigurasi MOD Checklist.

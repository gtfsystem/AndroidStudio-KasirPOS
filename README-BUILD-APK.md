# KasirPOS V8 Android — Build APK

1. Instal Android Studio versi terbaru dengan Android SDK 35 dan JDK 17.
2. Buka folder `AndroidStudio-KasirPOS` dan tunggu Gradle sync selesai. Proyek membutuhkan internet untuk dependensi AndroidX WorkManager.
3. Ubah `POS_URL` di `app/build.gradle` ke `https://domain-anda/POS/pos.php` (harus HTTPS).
4. Build > Build APK(s), lalu instal APK debug untuk pengujian. Untuk distribusi gunakan APK release yang ditandatangani.
5. Upload folder `KasirPOS` V8 ke hosting HTTPS; backup database sebelum mengganti versi.

## Apa yang baru

Antrean transaksi tetap disimpan di IndexedDB; pada aplikasi Android, payload juga dicerminkan ke penyimpanan internal privat aplikasi untuk dikirim oleh WorkManager saat internet tersedia. WorkManager mengulang maksimal sesuai kebijakan Android dan tidak menjamin sinkronisasi seketika. Jadwal periodik minimal 15 menit, serta permintaan satu kali saat transaksi baru masuk.

Server membutuhkan `client_txn_id` unik untuk idempotensi (sudah ada sejak V3). Jangan hapus data aplikasi Android ketika ada transaksi pending. Jika sesi login berakhir, buka aplikasi dan login lagi, lalu tekan sinkronisasi.

**Batasan:** Source ini belum diuji pada perangkat Android; belum ada APK hasil kompilasi. Antrean tersimpan dalam area privat aplikasi tetapi belum dienkripsi di level aplikasi. Jangan gunakan pada transaksi produksi sebelum uji offline, login kedaluwarsa, koneksi intermiten, reboot, dan transaksi paralel. Background sync menggunakan cookie sesi WebView; efektivitasnya bergantung pada sesi dan kebijakan Android.

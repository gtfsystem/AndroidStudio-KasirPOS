@echo off
setlocal
echo KasirPOS V9 - Build Debug APK
where gradle >nul 2>&1
if errorlevel 1 (
  echo Gradle belum tersedia. Buka folder ini di Android Studio ^> Build ^> Build APK(s).
  pause
  exit /b 1
)
call gradle :app:assembleDebug
if errorlevel 1 (
  echo Build GAGAL. Periksa error di atas.
  pause
  exit /b 1
)
echo APK: app\build\outputs\apk\debug\app-debug.apk
pause

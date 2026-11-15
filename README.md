# Glass TV – Android TV / Fire TV launcher

Liquid-glass home screen with floating bubbles, 5 minimal wallpapers that blend light to dark with the sun,
big IST clock + Ghaziabad (Govindpuram) weather, full app drawer, minimal screensaver and a black-and-white boot logo.

## Get the APK (pick one)

**A. No Android Studio (GitHub Actions)**
1. Create a GitHub repo, upload this folder, push to `main`.
2. Open the *Actions* tab, run *Build APK*, download the `GlassTV-debug-apk` artifact (contains `app-debug.apk`).

**B. Android Studio**
1. Open this folder, let Gradle sync, then *Build > Build APK(s)*.
2. APK: `app/build/outputs/apk/debug/app-debug.apk`

**C. Command line** (JDK 17 + Android SDK 34): `./gradlew assembleDebug`

## Install over ADB
```
adb connect <tv-ip>:5555        # enable ADB debugging in Developer options first
adb install -r app-debug.apk
```
Open **Glass TV** from your apps row. First launch plays the boot logo.

## Screensaver
- Built in: shows after 1/2/5/10 min idle on the Glass TV home screen (Settings > Screensaver after). Works on Fire TV.
- System screensaver (Android TV / Google TV), optional:
  `adb shell settings put secure screensaver_components com.glasstv.launcher/.ClockDreamService`

## Using it
- Home: OK opens an app, long-press OK on a dock app removes it from Home.
- All Apps: long-press OK pins/unpins an app on Home.
- Settings: Left/Right (or OK) changes values: wallpaper, theme, clock font/size, 12/24h, °C/°F, location, bubbles, screensaver delay.
- Custom clock font: copy `clock_font.ttf` to `Android/data/com.glasstv.launcher/files/`, choose *Custom file*.

## Notes
- Time, date and sunrise/sunset are pinned to Asia/Kolkata. Weather (Open-Meteo, no key) defaults to Ghaziabad (Govindpuram).
- Fire TV: the Home button always returns to Amazon's launcher unless you use a separate tool to change it; this app is launched from the apps row.
- The boot mark is a simple 12-ray burst drawn in code (see `BootLogoView.kt`); swap in your own asset if you prefer.

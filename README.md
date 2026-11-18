# Coumpanion

Coumpanion shows updates from your AI apps in a small island while you use another Android app. Watch apps like Claude and ChatGPT directly on your phone. Pair Coumpanion Server for DeepSeek chat and task progress. Compatible desktop integrations also support approvals.

[![Download the Coumpanion APK](https://img.shields.io/badge/Download-Coumpanion%20APK-146c5b?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Neyoiooo/coumpanion/releases/download/coumpanion-v0.5.0-preview.5/Coumpanion-debug.apk)

**Android 11 or later.** Preview 5 adds **Watch AI apps**. You can install it over Preview 4. [Release notes](https://github.com/Neyoiooo/coumpanion/releases/tag/coumpanion-v0.5.0-preview.5)

## Watch AI apps on your phone

1. Download and open the APK. If Android asks, allow your browser or file manager to install it.
2. Open **Coumpanion** and tap **Choose AI apps**. Turn on the installed apps you want to watch.
3. Tap **Allow notification access** and grant it for Coumpanion in Android Settings. If **Allow the island** appears when you return, tap it and enable **Display over other apps**.
4. Switch apps. When a selected app posts a new notification, Coumpanion shows an **Update** pill. Tap it to open that app or hold it to see the notification title.

[Phone setup and permission help](android/README.md#watch-ai-apps)

## Build from source

Use JDK 17, Android SDK platform 35, and Node.js.

```bash
cd android
./gradlew test assembleDebug lint
node scripts/gen-strings.mjs --check
```

See the [Android guide](android/README.md) for pairing, settings, and development. The [phone link protocol](docs/ANDROID_LINK.md) documents how the app connects to a compatible host.

Start Coumpanion Server on a cloud host from your phone, then copy its private link into the app. The server holds your DeepSeek API key. [Phone setup guide](android/server/README.md#from-your-phone)

## Credits

Coumpanion is based on Louis Raillé’s Coucou and Rhytam Biswas’s Android port. The code is open source under the MIT License. [Upstream project](https://github.com/Louis-CFM/coucou) · [MIT license](LICENSE) · [Android license](android/LICENSE) · [Upstream artwork notice](LICENSE-ASSETS.md)

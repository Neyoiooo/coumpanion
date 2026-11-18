# Coumpanion

Start an AI task, then go back to scrolling. Coumpanion puts a small island over your other Android apps so you can see when your assistant is working, done, or waiting for your approval. The goal is to keep the task running on your server even when you switch apps or lose your connection.

[![Download the Coumpanion APK](https://img.shields.io/badge/Download-Coumpanion%20APK-146c5b?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Neyoiooo/coumpanion/releases/download/coumpanion-v0.5.0-preview.2/Coumpanion-debug.apk)

**Android 11 or later.** Preview 2 adds a smaller island, a glow that follows your AI provider’s colors, and Glint the firefly as the launcher icon. You can try the offline demo without a computer. The phone-only AI server is in development. [Release notes](https://github.com/Neyoiooo/coumpanion/releases/tag/coumpanion-v0.5.0-preview.2)

## Try it on your phone

1. Download and open the APK. If Android asks, allow your browser or file manager to install it.
2. Open **Coumpanion** and tap **Try demo mode**.
3. Grant **Display over other apps** for Coumpanion, then switch to another app. Watch the island while the demo runs.
4. When the demo asks for approval, tap **Deny** or use your fingerprint or screen lock to **Allow**.

## Build from source

Use JDK 17, Android SDK platform 35, and Node.js.

```bash
cd android
./gradlew test assembleDebug lint
node scripts/gen-strings.mjs --check
```

See the [Android guide](android/README.md) for pairing, settings, and development. The [phone link protocol](docs/ANDROID_LINK.md) documents how the app connects to a compatible host.

## Credits

Coumpanion is based on Louis Raillé’s Coucou and Rhytam Biswas’s Android port. The code is open source under the MIT License. [Upstream project](https://github.com/Louis-CFM/coucou) · [MIT license](LICENSE) · [Android license](android/LICENSE) · [Upstream artwork notice](LICENSE-ASSETS.md)

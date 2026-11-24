# Coumpanion

**All your AI. One island.**

Coumpanion keeps local AI task markers in a Dynamic Island while you use other Android apps. Tap the island to see elapsed time and manage sessions. **Standard** supports manual tracking and **Full** adds optional notification watching. Desktop pairing and DeepSeek through the optional Coumpanion Server are available in both editions.

**Android 11 or later.** Find available downloads on [GitHub Releases](https://github.com/Neyoiooo/coumpanion/releases). See the [Android setup and installation guide](android/README.md) for edition details, update compatibility and Android permissions.

## Build from source

Use JDK 17, Android SDK platform 36, and Node.js.

```bash
cd android
./gradlew test assembleDebug lint
node scripts/gen-strings.mjs --check
```

See the [Android guide](android/README.md) for pairing, settings, and development. The [phone link protocol](docs/ANDROID_LINK.md) documents how the app connects to a compatible host.

Start Coumpanion Server on a cloud host from your phone, then copy its private link into the app. The server holds your DeepSeek API key. [Phone setup guide](android/server/README.md#from-your-phone)

## Credits

Coumpanion is based on Louis Raillé’s Coucou and Rhytam Biswas’s Android port. The code is open source under the MIT License. [Upstream project](https://github.com/Louis-CFM/coucou) · [MIT license](LICENSE) · [Android license](android/LICENSE) · [Upstream artwork notice](LICENSE-ASSETS.md)

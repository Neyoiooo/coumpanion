# Coumpanion

<p align="center">
  <img src="docs/media/coupi/banner.png" width="708" alt="Coumpanion in mint-green pixel lettering on a night sky">
</p>

<p align="center"><strong>All your AI. One island.</strong></p>

Coumpanion keeps your AI sessions in a Dynamic Island on Android. Start a timer, switch apps, and tap the island whenever you want to check elapsed time, reopen a provider, or stop a session. Manual tracking runs on your phone without an account, API key, or server.

<p align="center">
  <a href="https://github.com/Neyoiooo/coumpanion/releases/tag/coumpanion-v0.6.0-preview.1"><strong>Get the Android preview</strong></a>
  ·
  <a href="android/README.md">Setup and installation</a>
</p>

## <img src="docs/media/coupi/idle.gif" width="26" alt=""> Meet Coupi

Coupi is the little robot that lives in your island. Equip an AI and Coupi takes on its colors. The round badge on its belly shows which AI it has equipped while you track that session. ChatGPT wears mint, Claude wears terracotta, Gemini wears blue-violet, and Codex wears indigo. When several sessions are open, expand the island to see who’s equipped with what.

The gallery previews Coupi’s effects as animated pixel stickers. The avatar artwork is pending, and the linked APKs are the existing signed preview.

## <img src="docs/media/coupi/completed.gif" width="26" alt=""> Coupi’s states

| Pixel sticker | State | What the island is telling you |
| :---: | --- | --- |
| <img src="docs/media/coupi/idle.gif" width="50" height="50" alt="A mint pixel light gently bobs"> | **Idle** · mint | Coupi is at rest. |
| <img src="docs/media/coupi/tracking.gif" width="50" height="50" alt="Three mint pixels orbit a small tile"> | **Tracking** · ChatGPT mint | Your manual timer is running. Only elapsed tracking time is confirmed. |
| <img src="docs/media/coupi/thinking.gif" width="50" height="50" alt="Three terracotta dots take turns rising"> | **Thinking** · Claude terracotta | A paired desktop agent reported that it is thinking. |
| <img src="docs/media/coupi/working.gif" width="50" height="50" alt="Blue-violet pixels move along processing bars"> | **Working** · Gemini blue-violet | A paired desktop agent reported work in progress. |
| <img src="docs/media/coupi/completed.gif" width="50" height="50" alt="Sparkles flash around an indigo check mark"> | **Completed** · Codex indigo | A paired desktop agent reported completion. Coupi celebrates briefly and returns to idle. |
| <img src="docs/media/coupi/approval.gif" width="50" height="50" alt="A lavender question mark gently pulses"> | **Waiting for approval** · lavender | A paired desktop agent needs your response. |
| <img src="docs/media/coupi/error.gif" width="50" height="50" alt="An orange warning triangle shakes"> | **Error** · orange | A paired desktop agent reported a problem. |
| <img src="docs/media/coupi/disconnected.gif" width="50" height="50" alt="Silver pixel Zs drift upward"> | **Disconnected** · silver | The desktop link is offline. |

The colors are examples. Any equipped provider can use the states supported by its activity source. Full edition’s optional notification watching shows updates reported by the source app.

## <img src="docs/media/coupi/tracking.gif" width="26" alt=""> Install on Android

Both editions require **Android 11 or later**. Pick an AI in **Choose AI apps**, start tracking, then give Coumpanion permission to display the island over other apps.

| Edition | What’s included | Signed preview |
| --- | --- | --- |
| **Standard** | Manual tracking, Dynamic Island, widgets and optional desktop pairing | [Download Standard APK](https://github.com/Neyoiooo/coumpanion/releases/download/coumpanion-v0.6.0-preview.1/Coumpanion-Android-0.6.0-Standard.apk) |
| **Full** | Standard features plus optional notification watching | [Download Full APK](https://github.com/Neyoiooo/coumpanion/releases/download/coumpanion-v0.6.0-preview.1/Coumpanion-Android-0.6.0-Full.apk) |

Both editions can use DeepSeek through the optional Coumpanion Server. The server holds your DeepSeek API key. [Set up the server from your phone](android/server/README.md#from-your-phone) · [Browse releases](https://github.com/Neyoiooo/coumpanion/releases)

## <img src="docs/media/coupi/working.gif" width="26" alt=""> Build from source

Use JDK 17, Android SDK platform 36, and Node.js.

```bash
cd android
./gradlew test assembleDebug lint
node scripts/gen-strings.mjs --check
```

[Android development and avatar system](android/README.md) · [Phone link protocol](docs/ANDROID_LINK.md)

## Credits

Coumpanion is based on Louis Raillé’s Coucou and Rhytam Biswas’s Android port. The code is open source under the MIT License. [Upstream project](https://github.com/Louis-CFM/coucou) · [MIT license](LICENSE) · [Android license](android/LICENSE) · [Upstream artwork notice](LICENSE-ASSETS.md)

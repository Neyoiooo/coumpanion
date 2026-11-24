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

Coupi is the little robot that lives in your island. Coupi looks around and smiles when idle. During every other state, its head shows a status symbol. Equip an AI to give Coupi the provider’s colors and its logo in the belly badge.

The gallery shows the avatar system on `main`. The download links below point to the signed Android preview.

## <img src="docs/media/coupi/completed.gif" width="26" alt=""> Coupi’s status display

| Default Coupi | State | What the island is telling you |
| :---: | --- | --- |
| <img src="docs/media/coupi/idle.gif" width="104" height="104" alt="Coupi blinks, looks around, and moves its mouth"> | **Idle** | Coupi is at rest. |
| <img src="docs/media/coupi/tracking.gif" width="104" height="104" alt="A clock ticks on Coupi’s display"> | **Tracking** | Your manual timer is running. Only elapsed tracking time is confirmed. |
| <img src="docs/media/coupi/thinking.gif" width="104" height="104" alt="A thought bubble with animated dots appears on Coupi’s display"> | **Thinking** | A paired desktop agent reported that it is thinking. |
| <img src="docs/media/coupi/working.gif" width="104" height="104" alt="A blue segment moves across Coupi’s activity indicator"> | **Working** | A paired desktop agent reported work in progress. |
| <img src="docs/media/coupi/completed.gif" width="104" height="104" alt="A green pixel heart pulses on Coupi’s display"> | **Completed** | A paired desktop agent reported completion. Coupi pulses a green heart and then holds it. |
| <img src="docs/media/coupi/approval.gif" width="104" height="104" alt="A purple question mark appears on Coupi’s display"> | **Question or approval** | A paired desktop agent needs your response. |
| <img src="docs/media/coupi/error.gif" width="104" height="104" alt="Coupi’s display turns red with a yellow warning symbol"> | **Error** | An integration reported a problem. |
| <img src="docs/media/coupi/disconnected.gif" width="104" height="104" alt="A floating Z appears on Coupi’s display"> | **Disconnected** | The desktop link is offline. |

The island shows states supported by each session’s activity source. Full edition’s optional notification watching shows updates reported by the source app.

## <img src="docs/media/coupi/idle.gif" width="26" alt=""> Coupi’s AI personality

At rest, Coupi looks left and right, blinks, and makes a little “o” before smiling again. Equip a provider to see the same idle loop in its colors.

| <img src="android/app/src/main/assets/avatars/badges/chatgpt.png" width="22" alt=""> ChatGPT | <img src="android/app/src/main/assets/avatars/badges/claude.png" width="22" alt=""> Claude | <img src="android/app/src/main/assets/avatars/badges/gemini.png" width="22" alt=""> Gemini |
| :---: | :---: | :---: |
| <img src="docs/media/coupi/chatgpt.gif" width="112" height="112" alt="ChatGPT Coupi looks around and blinks"> | <img src="docs/media/coupi/claude.gif" width="112" height="112" alt="Claude Coupi looks around and blinks"> | <img src="docs/media/coupi/gemini.gif" width="112" height="112" alt="Gemini Coupi looks around and blinks"> |

| <img src="android/app/src/main/assets/avatars/badges/deepseek.png" width="22" alt=""> DeepSeek | <img src="android/app/src/main/assets/avatars/badges/perplexity.png" width="22" alt=""> Perplexity | <img src="android/app/src/main/assets/avatars/badges/muse.png" width="22" alt=""> Muse |
| :---: | :---: | :---: |
| <img src="docs/media/coupi/deepseek.gif" width="112" height="112" alt="DeepSeek Coupi looks around and blinks"> | <img src="docs/media/coupi/perplexity.gif" width="112" height="112" alt="Perplexity Coupi looks around and blinks"> | <img src="docs/media/coupi/muse.gif" width="112" height="112" alt="Muse Coupi looks around and blinks"> |

| <img src="android/app/src/main/assets/avatars/badges/copilot.png" width="22" alt=""> Copilot | <img src="android/app/src/main/assets/avatars/badges/grok.png" width="22" alt=""> Grok | <img src="android/app/src/main/assets/avatars/badges/codex.png" width="22" alt=""> Codex |
| :---: | :---: | :---: |
| <img src="docs/media/coupi/copilot.gif" width="112" height="112" alt="Copilot Coupi looks around and blinks"> | <img src="docs/media/coupi/grok.gif" width="112" height="112" alt="Grok Coupi looks around and blinks"> | <img src="docs/media/coupi/codex.gif" width="112" height="112" alt="Codex Coupi looks around and blinks"> |

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

Coumpanion is based on Louis Raillé’s Coucou and Rhytam Biswas’s Android port. The code is open source under the MIT License. [Upstream project](https://github.com/Louis-CFM/coucou) · [MIT license](LICENSE) · [Android license](android/LICENSE) · [Artwork and provider logo notices](LICENSE-ASSETS.md)

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

<p align="center">
  <img src="docs/media/coupi/tracking.gif" width="126" alt="Mint ChatGPT Coupi floats with three orbiting pixels">
  <img src="docs/media/coupi/idle.gif" width="126" alt="Silver Coupi breathes and blinks with its badge empty">
  <img src="docs/media/coupi/thinking.gif" width="126" alt="Terracotta Claude Coupi tilts with animated thinking dots">
</p>

Coupi is the little robot that lives in your island. Equip an AI and Coupi takes on its colors. The round badge on its belly shows which AI it has equipped. When several sessions are open, expand the island to see each one.

The gallery shows the avatar system on `main`. The download links below point to the signed Android preview.

## <img src="docs/media/coupi/completed.gif" width="26" alt=""> Coupi’s states

| Coupi | State and equipped AI | What the island is telling you |
| :---: | --- | --- |
| <img src="docs/media/coupi/idle.gif" width="88" height="88" alt="Coupi breathes and blinks"> | **Idle** · Unequipped | Coupi is at rest. |
| <img src="docs/media/coupi/tracking.gif" width="88" height="88" alt="Mint Coupi sways under three orbiting pixels"> | **Tracking** · <img src="android/app/src/main/assets/avatars/badges/chatgpt.png" width="22" height="22" alt=""> ChatGPT | Your manual timer is running. Only elapsed tracking time is confirmed. |
| <img src="docs/media/coupi/thinking.gif" width="88" height="88" alt="Terracotta Coupi tilts above animated thinking dots"> | **Thinking** · <img src="android/app/src/main/assets/avatars/badges/claude.png" width="22" height="22" alt=""> Claude | A paired desktop agent reported that it is thinking. |
| <img src="docs/media/coupi/working.gif" width="88" height="88" alt="Blue-violet Coupi bounces between processing pixels"> | **Working** · <img src="android/app/src/main/assets/avatars/badges/gemini.png" width="22" height="22" alt=""> Gemini | A paired desktop agent reported work in progress. |
| <img src="docs/media/coupi/completed.gif" width="88" height="88" alt="Indigo Coupi jumps and throws off pixel sparkles"> | **Completed** · <img src="android/app/src/main/assets/avatars/badges/codex.png" width="22" height="22" alt=""> Codex | A paired desktop agent reported completion. Coupi celebrates briefly and returns to idle. |
| <img src="docs/media/coupi/approval.gif" width="88" height="88" alt="Lavender Coupi pulses under a question mark"> | **Waiting for approval** · <img src="android/app/src/main/assets/avatars/badges/muse.png" width="22" height="22" alt=""> Muse | A paired desktop agent needs your response. |
| <img src="docs/media/coupi/error.gif" width="88" height="88" alt="Ocean-blue Coupi shakes beside a warning"> | **Error** · <img src="android/app/src/main/assets/avatars/badges/deepseek.png" width="22" height="22" alt=""> DeepSeek | An integration reported a problem. |
| <img src="docs/media/coupi/disconnected.gif" width="88" height="88" alt="Silver Coupi rests beneath drifting pixel Zs"> | **Disconnected** · <img src="android/app/src/main/assets/avatars/badges/grok.png" width="22" height="22" alt=""> Grok | The desktop link is offline. |

<p align="center">
  <img src="docs/media/coupi/perplexity.gif" width="96" alt="Turquoise Coupi equipped with Perplexity">
  <img src="docs/media/coupi/copilot.gif" width="96" alt="Pastel rainbow Coupi equipped with Copilot">
  <br>
  <strong>
    <img src="android/app/src/main/assets/avatars/badges/perplexity.png" width="22" height="22" alt=""> Perplexity
    ·
    <img src="android/app/src/main/assets/avatars/badges/copilot.png" width="22" height="22" alt=""> Copilot
  </strong>
</p>

The pairings illustrate how the palettes and motions combine. The island shows states supported by each session’s activity source. Full edition’s optional notification watching shows updates reported by the source app.

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

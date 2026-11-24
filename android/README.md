# Coumpanion for Android

Coumpanion shows your agents, lets you answer approval requests and supports chat through a compatible link. The Android app is based on [Coucou](https://github.com/Louis-CFM/coucou) by Louis Raillé and the Android port by Rhytam Biswas. The code remains [MIT-licensed](LICENSE).

The offline demo runs on Android 11 or later. [Coumpanion Server](server/README.md) provides a real DeepSeek connection over a private network. Compatible desktop links can also be used for pairing and chat.

## Choose an edition

| Edition | Application ID | Features |
| --- | --- | --- |
| Standard | `app.coumpanion.android.standard` | Manual tracking, Dynamic Island, widgets, desktop pairing and DeepSeek through the optional server |
| Full | `app.coumpanion.android` | Standard features plus optional notification watching |

Both editions support Android 11 and later. Full retains the existing application ID. Android only upgrades an existing install when the signatures match. If you have a debug-signed preview, install Standard alongside it or back up what you need before replacing the preview with production-signed Full. Android clears app data and permissions on uninstall. The editions have separate local histories, settings and pairing.

## Manual task tracking

Open **Choose AI apps** on Home and turn on manual tracking for each installed AI app you want to track. Grant **Display over other apps** when prompted and switch to another app. Tap the island to see elapsed tracking time, open the source app, or stop each session. You can also stop sessions from Home or the app picker. Sessions restore when you reopen Coumpanion. Manual tracking is an unverified marker and runs entirely on the phone without notification access or pairing. Desktop approvals and questions take priority.

In **Settings → Display**, choose Camera, Center, Left or Right for the island position and turn on **Solid island background** if you prefer a static background. Android may hide or terminate the overlay under its system policies. Reopen Coumpanion to restore saved manual sessions after a force stop or reboot.

## Watch AI apps

1. In Full, tap **Choose AI apps** on Home or open the AI apps picker from Settings.
2. Select the installed apps you want to watch and grant notification access in Android Settings.
3. Grant **Display over other apps** if you want the island, then switch to another app.

A new notification shows an **Update** for a few seconds. Tap to open the source app, or hold to see the shortened title and any recognized event. A matching provider-specific title may report a completion or an issue. Other titles show a generic update. These are notification reports. Verified desktop progress comes through the paired desktop link.

Android grants notification access for the device. Coumpanion checks the selected package before reading its title and keeps titles in memory for the island. Its bounded local history stores only the provider, receipt time and event category. Clear the history from Home or the app picker. Turning off a watched app clears its entries, and revoking notification access clears the log. Android keeps the original notification in the shade.

Android's notification access screen may label the toggle **Restricted setting** for sideloaded apps. See [Android's guidance for restricted settings](https://support.google.com/android/answer/12623953).

## Muse web investigation

[Meta documents Muse Code](https://dev.meta.ai/docs/muse-code) as a terminal and CI agent and uses the browser for authentication. The existing desktop hook can forward Muse Code activity through phone pairing. No supported Muse Code web activity feed for Android was found. Generic browser notifications do not give Coumpanion a dependable, portable way to identify and follow a Muse Code task across browsers. Automatic Muse web tracking remains unavailable.

## AI avatars

Coupi uses a single transparent master at `app/src/main/assets/avatars/robot.png`, prepared as a cutout from the supplied mascot image. Equipping a provider tints the shell and adds its installed app icon to the belly badge when available. A pixel monogram or spark covers providers without a local icon. The face, outlines and dither remain part of the source artwork. The renderer uses nearest-neighbor sampling and caches the resulting bitmaps. Status marks provide a fallback when the master is unavailable.

Compact and expanded islands, the AI app picker, and active session cards use the same renderer. The Solo, List, Team, and Agent Overview widgets use static renders. Manual sessions use the Tracking pose, and notification reports use Idle. Thinking, Working, and Completed use confirmed desktop session states.

**Settings → Display → Animated AI avatars** appears in builds with the master and switches avatar motion on or off. Avatar motion also pauses with battery saver, reduced motion, the screen off, or the island closed.

Run `python scripts/render-coupi-showcase.py` from the repository root with Pillow installed to regenerate the README GIFs from the same master. The GIFs use pixel badges so they can be generated without Android app icons.

## Build and test

Needs JDK 17 and the Android SDK (platform 36). From this folder:

```bash
./gradlew test assembleDebug lint
node scripts/gen-strings.mjs --check      # res/values/strings.xml is generated from i18n/app-strings.json
```

`assembleRelease` builds the two R8-optimized APKs. `lint` checks the resulting APKs for debug logs and verifies that the listener is declared only in Full. For a local release-signed build, supply `COUCOU_KEYSTORE_FILE`, `COUCOU_KEYSTORE_PASSWORD`, `COUCOU_KEY_ALIAS` and, if it differs, `COUCOU_KEY_PASSWORD` through the environment or your user Gradle properties. Keep the key and password out of the repository.

### Production distribution

Use the **Android release** workflow on `main` to build and sign both APKs and a Standard Android App Bundle. The workflow expects `ANDROID_KEYSTORE_BASE64` and `ANDROID_KEYSTORE_PASSWORD` for the production key with alias `coucou`. Keep a secure backup and reuse the signing identity for future APK updates. The workflow verifies signatures and 16 KB native-library alignment, records APK checksums, and checks that both signed APKs install, reinstall and launch on Android 16 with 16 KB pages. A manual run prepares artifacts. An `android-v0.6.0` tag publishes the APKs after those checks pass.

Both editions target Android 16 (API 36), the current submission requirement for phone apps on Google Play. For Google Play, register the Standard application ID, enroll in Play App Signing and test the bundle on an internal track. If you distribute the same package through both Google Play and GitHub, configure the same app-signing identity for both channels so they can update each other. If Play Console uses a separate upload key, sign the unsigned Standard bundle with that registered key before upload. [Android app signing guide](https://developer.android.com/studio/publish/app-signing)

For GitHub distribution, verify the release APKs against `SHA256SUMS` and register the application IDs and signing certificates where Android developer verification applies. If Google Play Protect blocks a signed release, record the exact warning and affected device, follow Google's developer guidance and submit an appeal if the classification appears incorrect. Signing verifies the APK's origin and integrity. Play Protect independently evaluates the distribution channel and permissions. [Android developer verification](https://developer.android.com/developer-verification) · [Play Protect developer guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)

To run the offline demo, open **Connect to your server** and tap **Try demo mode**. Tap **Allow** next to the overlay permission notice on Home and enable **Display over other apps** for Coumpanion. Return to Coumpanion, then switch to another app while the demo runs. The island shows progress and the demo approval. Tap **Review request** to see the exact command. The first **Allow** or **Send answer** uses your PIN, pattern, or password. That check is shared while the screen stays on. **Deny** works directly.

For a separate test over Wi-Fi, run the fake desktop:

```bash
node tools/dev-desktop.mjs --host <LAN IP>     # prints a pairing link; flags: --details --answers --diffs --usage --services --fake-chat
```

The desktop side is in `windows/src-tauri/src/phone_link`; the protocol is `docs/ANDROID_LINK.md`, the internet relay
`docs/RELAY_LINK.md` (deploy and test it with `docs/RELAY_DEPLOY.md`).

## What it does

- **Home**: the focused agent as a black hero panel (a status mark in the agent’s colour, a plain sentence), the other agents below, plan
  usage and service cards when the computer offers them, and *Recent* (your own Allow/Deny decisions, kept only on the phone).
  Raw tool names become plain words (`core/ToolLabels.kt`); a command or path is never shown without a deliberate tap (`core/SafeText.kt`).
- **Languages** (Settings > Display > Language, or Android 13 and later: Android's own Apps > Coumpanion > Language): the phone's own, English, French (informal "tu", like the rest of Coucou), Spanish, Brazilian Portuguese, Simplified Chinese, Hindi, Bengali, Russian, Indonesian, Arabic (written right to left, the layout and the arrows mirror) and Turkish. The English source is `i18n/app-strings.json` (strings and plurals); each language is a file in `i18n/translations/`; `node scripts/gen-strings.mjs` writes the resources (`values-<language>`), `res/xml/locales_config.xml` and the table `app/TextIds.kt`, and `--check` (run by CI) fails on a missing key, a placeholder that does not match, a missing plural form or a stale file. Texts the Mac and Windows apps already have in a language are reused; the rest was written for Android and is listed in `i18n/REVIEW.md` so a native speaker can check it. The words that the pure rules produce ("Running a command", "2 agents are working", the Widgets' state words) go through `core/Texts.kt`, so they are translated too. Before Android 13 there is no system per-app language, so the choice is kept in the app (the Widgets' fixed texts, drawn by the launcher, then follow the phone's language). Not translated yet: the "Can't connect?" report and its steps, and the demo's made-up sessions.
- **Theme** (Settings > Display): System (the default), Light or Dark, and an optional *Use my phone's colours* (Material You, Android 12 and later, off by default). The hero panel, the sheets and the file-change view take their colours from `core/Tokens.kt` (`DARK` and `LIGHT`, every text pair checked for WCAG AA in `TokensTest` and `LightThemeTest`); the island remains dark and has its own background setting, and the camera screen stays black. The agent and state colours are the same in both.
- **Accessibility**: TalkBack says an agent as "Claude Code, waiting for your OK" (name, state in words, then the line), a tab with an alert as "Home, a request is waiting", and a request sheet or the island is announced when it appears (`core/SpokenLabels.kt`, `core/WidgetSpoken.kt`: every tapped area of every Widget speaks one sentence built from what it shows). Every control is at least 48 dp, sheets scroll and status lines may wrap so the app holds at a font scale of 1.3, and Settings > Display > *Reduce motion* (System, On, Off; System follows Android's "Remove animations") turns the island's and sheets' springs into a fade (`core/MotionPolicy.kt`).
- **Request alerts** (Settings > Permission requests): *Don't wake the screen for a request* (off by default, the screen wakes as before) and the vibration (Standard is the old pattern, Short, or Off). A request is never muted or hidden by either: it still arrives with an approval tone and its notification. The vibration is a notification channel setting that Android cannot change later, so each choice has its own channel id and the old one is deleted (`core/ApprovalAlerts.kt`).
- **Home says why the link was lost** (not on Wi-Fi, the computer closed the connection, or it cannot be reached: Coucou closed there, phone link off, another network), and the ongoing notification is posted again only when its text changes (`core/OngoingGate.kt`). Predictive back is on (`enableOnBackInvokedCallback`).
- **Requests**: a sheet rises over any screen. Allow uses the screen-lock check once while the screen stays on, Deny works directly, taps in the first 600 ms
  are ignored, and each request expires after 120 s. Questions from Claude Code can be answered the same way (the computer must allow it).
- **Island over other apps** (on by default, with an option to turn it off): a dark island positioned below the status bar drops while an agent works, shows
  the result for 10 s and goes back up with the computer's motion. Needs Android's "Display over other apps". Rules are pure and
  tested (`core/Island*.kt`); `ui/IslandOverlay.kt` draws.
- **Chat** (a tab, when the computer offers it), **session details**, **file changes** and **plan usage** are opt-in switches on the
  computer (Settings > Android phone), all off by default; the phone is told only what it asks for and the computer allows.
- **Pairing**: scan the QR code with the in-app camera (CameraX + zxing, offline) or paste the link; a scan or a `coucou://pair`
  link always asks to confirm. The certificate is pinned. If the computer's address changes the phone finds it again by itself (mDNS,
  checked against the pinned certificate before the token is sent), or you can type the address.
- **Away from home Wi-Fi** (optional): through your own Cloudflare relay, end-to-end encrypted; the direct link is always tried first.
- **Keeping the link alive with the screen off**: an alarm every few minutes checks that the computer still answers and reconnects if not (Doze allows nothing more); Home says when and why the link was lost; Settings > Computer > *Can't connect?* checks the network, address, discovery, certificate, pairing code and relay; a calm hint suggests the battery setting ("Unrestricted", with the Samsung steps) only if it would help. Details and limits: `docs/ANDROID_LINK.md`, "Keeping the link alive".
- **Widget, quick-settings tile, ongoing notification**: agent names and state words only. Updated when something changes, never on a timer.
- **Widgets**: ten to pick from on the home screen, all on one card (24dp corners, a thin border, dark by default and light when the system is in day mode) with a gradient, a 1dp border and a glow in the colour of the state, the same palette for every Widget (`WidgetTone` in `core/WidgetData.kt`, `res/values/colors.xml`): green when an agent has finished, blue while it works, orange while something waits for you, soft grey when all is calm, and a muted grey with the "not connected" icon when the computer is not there. With several agents the most urgent one sets the colour (waiting, then working, then happy, then idle). Plan usage has a palette of its own so it never looks like a state: teal, yellow from 75% and red from 90% (`PlanUsageLook`); only from 90% is the percentage big and bold, with "Near limit" and the clock time it resets at ("resets at 14:30"). In the Widget picker each one shows a large, realistic preview with sample data (Android 12 and later; Android 11 shows a still picture of the card), its name and a one-line description. The previews are static: vector status marks in `res/drawable/preview_*`, layouts in `res/layout/widget_preview_*`.
  - *Agents summary* (3x2, the first Widget, same receiver as before): its text and counts are unchanged, now on the shared card with a small mark in the state of your most urgent agent, a headline with a badge for what waits for you and up to three short lines with a state dot each. A tap anywhere opens the app; it has no buttons.
  - *Agent at a glance* (Solo, 2x2): your most urgent agent, its name and a state word.
  - *All your agents* (List, 4x3): one row of at least 48dp per agent (status mark, name, state word, dot), up to five, with a badge for what waits for you; with no agent, a centred empty state ("Waiting for your computer" / "No agents running").
  - *Agent overview* (4x4): on the left, a status mark followed by the agent’s name and state; on the right your plan usage (Claude and Codex, each with a thin bar that is teal, yellow from 75% and red from 90%, and a small percentage that becomes big and bold with "Near limit" from 90%), or "Plan usage is off" (a tap opens Settings) or "Not connected". Under them rounded buttons: two rows of three (Home, Chat, Settings, Recent, Top agent, Pair again; tinted orange while something waits), one row of four when medium, none when small.
  - *One-line status* (Status Pill, 4x1): one slim line with a dot, the agent's name and its state word, and four round buttons that open Home, Chat, Settings and Recent decisions. Narrower, the state word goes first, then the name; the text never runs under the buttons.
  - *Approvals waiting* (Waiting Alert, 2x2): calm when nothing waits, a bright orange "N waiting" when something does. A tap opens the app and the sheet rises by itself.
  - *Plan usage* (Usage Ring, 2x2): a ring for your Claude or Codex plan usage; "Turn on plan usage in the app" when it is off.
  - *Finished today* (Today, 2x2): agent tasks finished today, counted on this phone only and started again at local midnight by one inexact alarm (no exact-alarm permission, no wake-ups).
  - *Status indicator* (1x1): the most urgent agent’s status colour.
  - *Your team* (Team, 4x2): up to four agents you pick (a choice screen opens when you add it, and again from its long-press menu "Reconfigure"; skip it and it shows the busiest ones), two by two, each with status mark, name and state word; an agent you picked that is not running says "Not running". The choice is kept per Widget on the phone only (`app/TeamStore.kt`, `core/TeamChoice.kt`) and goes with the Widget. A tap opens that agent, or the app.

  The buttons only open a screen of the app, from a closed list (`core/LaunchScreen.kt`) through the non-exported `LaunchActivity`; none approves, denies or sends anything, and `MainActivity` still reads no intent extra. A tap on Solo, Agent overview, Status Pill, Status indicator or a List row opens that agent in the app. A request is decided in the app. Widgets show agent names, state words and colours only, are redrawn when something changes (`updatePeriodMillis` stays 0) and a redraw that would look the same is skipped. On Android 12 and later they adapt to their size (small, medium, large). Code: `core/WidgetData.kt` (pure, tested), `app/WidgetHub.kt`, one class per Widget in `app/`, `app/StatusMarkBitmap.kt`, `app/RingBitmap.kt`.
- **History**: *Weekly recap* (optional, needs Coucou with the `recap` capability on the computer; Settings → Android phone → "Show my weekly recap on the phone", off by default): a card on Home with last week's time with agents, sessions, files, lines, commands, permissions allowed and denied and the agent used most, counts only (`link/Protocol.kt`, `core/RecapText.kt`, `ui/RecapPanel.kt`; held in memory, never stored). *Activity* (Settings > More) has a search box and *Export as text*, which shares the same wording the list shows (never the commands) through Android's share sheet (`core/HistoryExport.kt`). Settings > Notices > *Mute an agent's notices* silences the finished / failed / limit notices of the agents you pick; requests and questions are never muted (`core/AgentMute.kt`, rule in `StatusPolicy.announce`).
- **Quick actions**: long-press the app icon for *Chat*, *Recent decisions* and *Pair again* (`res/xml/shortcuts.xml`). They start the private `LaunchActivity` with the key of one screen of the closed list, like the Widget buttons: they only open a screen. *Share to Coumpanion*: in another app, Share > Coumpanion chat puts the text in the chat box as a draft, cut to the chat's limit and stripped of control characters (`core/ShareDraft.kt`, `app/ShareActivity.kt`). It is never sent by itself; if the computer does not offer chat the text is dropped with a short message, and while the computer is not reachable yet it waits for up to two minutes.
- **Looking at every state without a computer** (debug builds only): `adb shell am broadcast -n app.coumpanion.android/com.coucou.android.app.WidgetPreviewReceiver -a com.coucou.android.debug.WIDGET_STATE --es state <disconnected|empty|working|waiting|done|live>`; `live` goes back to the real state. The receiver is in `app/src/debug` and does not exist in a release build.

## Getting the phone link running on Windows

1. Download the portable build from the repository's **Windows** workflow run (Actions, Windows, the `Coucou-Windows-portable`
   artifact). It holds `coucou.exe` and `coucou-hook.exe`.
2. **Close any other Coucou first.** An installed copy (for example in `C:\Program Files\Coucou`) starts at login and has no
   phone link, so the phone would say "Connecting…" forever. Then start the portable `coucou.exe`.
3. Settings > **Android phone** > turn **Phone link** on. It is off by default and listens on your local network only. If Windows
   asks about the firewall, allow Coucou on **private** networks (a network marked *Public* blocks the phone).
4. **Show pairing code**, then scan the QR code with the app (or paste the link). The phone and the computer must be on the same Wi-Fi.
5. Optional, each with its own switch on the same page: session details, answers to questions, file changes, chat through the
   computer's own keys, plan usage, services.

What to know when you try approvals from the phone:

- Only **Claude Code** (the command-line tool), Codex, Copilot CLI and Muse Code can be approved from Coucou or the phone. Requests
  from the **Claude Desktop app** are asked in that app only: the computer declines them at once (`windows/src/island/agents.ts`,
  `APPROVAL_AGENTS`; the same rule as on the Mac).
- Claude Code must be in a mode that **asks**. In auto or bypass mode it never asks, so nothing reaches the phone. To test:
  `claude --permission-mode default "…"`, or press Shift+Tab until it says manual mode.
- The plan usage numbers also come from Claude Code's status line, so they update from the command-line tool and not from the
  Claude Desktop app.
- In Windows PowerShell, `claude` can be blocked by the script policy (`claude.ps1 cannot be loaded`). Run `claude.cmd`, use
  Command Prompt, or allow local scripts for your user with `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

For testing without a cable once the phone has been connected by USB once: `adb tcpip 5555`, then
`adb connect <phone address>:5555`. It resets when the phone restarts or its address changes.

## Getting the phone link running on a Mac

1. Use a Coucou for Mac that carries the Android phone link (the GitHub build; it is not in the App Store build). Update Coucou first if
   Settings has no **Android phone** box.
2. Settings > **Android phone** > turn **Link with Coucou for Android** on. It is off by default and listens on your local network only.
   If macOS asks to find devices on your local network, allow it.
3. **Show pairing code**, then scan the QR code with the app (or paste the link). The phone and the Mac must be on the same Wi-Fi.
4. Optional, each with its own switch on the same page: session details, answers to Claude Code's questions, plan usage. Chat, file
   changes, services, the weekly recap and the relay are not available from a Mac yet.

The first Allow or Answer uses your screen lock on the phone. Turning the screen off or restarting the app resets that check. Only the request still waiting on the Mac can be answered.
Turning the switch off stops the listener and the announcement at once. As on Windows, Claude Code must be in a mode that asks for
permission, or nothing reaches the phone.

## Security rules the tests guard

Certificate pinning and the pairing token; approval fingerprints, 120 s expiry and decide-once; the unlocked approval session for Allow and Answer;
`MainActivity` ignores intent extras (only the non-exported `LaunchActivity` can ask for the Allow prompt); `FLAG_SECURE` on pairing,
chat, approval and question screens; secrets only in the Android Keystore blob; no logging of tokens, answers or relay keys; no ad,
billing or analytics library.

## Layout

- `.../link` protocol, pairing, TLS client, WebSocket client, relay, transport choice, demo link
- `.../core` pure rules (island, navigation, safe text, home text, glance); `.../ui` Compose screens; `.../app` model, notifications, service, storage
- `relay/` the relay (Cloudflare Worker + Node twin); `tools/` the pretend computer; `i18n/` the English strings
- Tests check the protocol and matching agent colours, including the fake desktop link.

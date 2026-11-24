# Coumpanion — Android-first product roadmap

**All your AI. One little companion.**

## Product vision

A free, standalone Android companion that displays activity from supported AI tools in a responsive, top-attached island. Coupi is the mascot and visual identity. The experience should be useful on a phone without a computer, account with Coumpanion, companion server, Termux or paid API key.

The core experience is genuine AI task activity where a supported source exists. Manual sessions are an optional fallback, not the product's main promise.

## Product boundaries

- Android is the only actively developed product target. Preserve upstream license and attribution; do not erase release history.
- Remove desktop pairing, QR/paste pairing, desktop remote approvals, desktop chat, desktop plan usage, desktop services and desktop-specific widgets from the Android product. Audit dependencies before deleting code; retain generic session, overlay, avatar and storage components. Do not remove unrelated Android share/launch functions.
- No mandatory notification reading, accessibility service, Usage Access, Termux, external server, subscription, paid API or analytics. Do not ship notification listening as the primary experience. Plan a single no-listener edition after safe migration; retain already published releases, signatures and appealed APK unchanged.
- No simulated AI progress. A native consumer AI app without a supported activity interface cannot be tracked automatically. Browser signals, manual timers, provider-reported events and confirmed task events must remain distinct.
- Use authorized interfaces and user consent. Do not use private endpoints, extract credentials, intercept other apps' network traffic or bypass Android security.

## Intended user experience

1. Open Coumpanion, meet Coupi, choose supported AI tools and enable the island if desired.
2. A provider card explains whether it supports a real activity integration, a browser-based integration, or only launch/manual tracking.
3. Start a task using a supported integration; Coupi appears below the safe status-bar/camera edge, showing provider, actual task status and a subtle provider gradient.
4. Tap to expand for current sessions, details and available actions. On a confirmed completion, Coupi celebrates and retracts. Unknown or disconnected activity never displays fake progress.
5. Home contains the mascot, provider launcher and active activity; Activity contains local history; Settings contains appearance, privacy, diagnostics and supported integrations. No desktop pairing screen or connection-first onboarding.

## Milestones and release gates

### 0.7 — Standalone Android foundation
- Remove desktop-facing navigation and pairing entry points, then desktop-only services, state and permissions where no longer needed. Verify clean installs and upgrades and preserve local user data.
- Finish the mobile-first home, top-attached responsive island, provider gradients, peeking Coupi icon and unified pixel-art avatar system. Regenerate matching showcase assets.
- Keep manual sessions clearly labeled and optional. Keep launcher, local history, diagnostics, supported share handoff, motion controls and useful Android widgets.
- Run Android unit tests, lint, both existing variants while migration is in progress, and physical-device checks. No new release until signed install and upgrade checks pass.

### 0.8 — Validate real phone-only activity
- Research documented provider integrations and supported Android browser extension mechanisms. Select one feasible source without mandatory payment or external hosting.
- Build a minimal end-to-end experiment before expanding the feature: start a task, leave its interface, receive actual supported state changes, handle completion, interruption and disconnect.
- If using browser signals, identify them as web activity, not native-app events. Validate permissions, battery impact, website changes and explicit consent. Do not read conversation content unnecessarily.
- **Go/no-go:** if no genuine free phone-only integration is verified, do not label the app an automatic native AI tracker. Reconsider the product direction before investing in additional tracking architecture.

### 0.9 — Coupi Connect
- Only after a successful experiment, implement a small authenticated activity interface for supported phone-only sources. Prefer local communication where supported; no unauthenticated listener or mandatory relay.
- Normalize provider, session identity, event sequence, timestamps and supported state; support revocation, stale state, deduplication and bounded local history.
- Expand to additional AI providers one at a time based on tested capabilities, not desired logos. Maintain a public compatibility matrix and easy setup.
- Test source labeling, disconnect behavior, permission revocation and overlay reliability.

### 1.0 — Public launch
- Consolidate into one standalone edition without the notification listener, subject to a safe package/signing/data migration plan. Do not overwrite or invalidate existing signed releases.
- Verify physical-device behavior, signed install/upgrade, privacy policy, asset licenses, battery performance, accessibility and distribution readiness.
- Publish accurate compatibility information and Play Store screenshots from actual builds. Preserve the appealed APK unchanged.

These are proposed release targets, not claims of existing functionality. The roadmap does not itself remove source code or create an integration. Only publish releases after the required checks pass.

# Coumpanion Android changelog

## Unreleased

- Added the Codex web shortcut and Coupi badge to the AI Launcher. Codex supports local manual tracking.
- Manual timers now show elapsed seconds on visible tracking screens and in the island.

## 0.6.1 Preview 2

- Removed Android Demo Mode and its simulated sessions and approval requests. Open the AI Launcher for real timers or pair with your desktop for verified agent activity.

## 0.6.1 Preview 1

- The AI Launcher opens native apps or HTTPS providers. Custom providers can have an imported logo, accent, and launch-tracking preference. Desktop pairing stays optional.
- Manual timers support pause, resume, restart, manual completion, cancellation, expiration, and reminders. They use elapsed realtime and restore local records from Preview 1.
- Session History separates manual results, notifications, and verified activity observed through a paired link. Filter records by provider, date, or status and delete individual records or the full history.
- Quick Settings shows the active timer count or desktop connection state. Share accepts plain text and requires an explicit handoff, clipboard copy, or desktop draft action.
- The island has three sizes and solid or translucent backgrounds. The expanded session list includes elapsed time, pause, stop, and manual completion. Coupi's badge and status display share the body's motion.
- Diagnostics Center shows edition, permissions, battery saver, link health, and avatar readiness. Its optional clipboard report contains local status fields only.
- Full continues to watch only selected supported packages. Exact-title categories are notification reports. Other titles appear as generic updates.

The signed 0.6.0 Preview 1 is a separate release. The v0.6.1 release workflow can build signed candidates without publishing them.

### Release checks

Physical-device coverage should include switching apps, overlay permission changes, lock and unlock, battery saver, force stop and reopening, a reboot with running and paused timers, multiple providers, notification listener revocation in Full, and desktop reconnection across network changes. Test the signed upgrade from Preview 1 and confirm the launcher, Quick Settings, Share, and Settings shortcuts on supported Android versions. The existing preview's Play Protect appeal is tracked separately.

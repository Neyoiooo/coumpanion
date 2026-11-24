# Coumpanion product roadmap

**All your AI. One little companion.**

Coumpanion is a free Android companion that shows activity from connected AI tools while the user works in another app. Coupi is the visual identity; the top-attached live task pill is the primary experience. No mandatory account, paid API, cloud server, Termux or notification access.

## Product experience

- Home: Coupi, selected AI providers, and active sessions. Launch an AI or connect an actual activity source.
- Activity: local history with clear source labels and session controls.
- Connections: supported agent links and optional integrations; show capability and connection status before setup.
- Settings: appearance, privacy, diagnostics and motion controls.
- Overlay: notch-aware, two-line task pill, provider gradients, animated Coupi, expandable details and existing approval controls.

## Activity contract

Manual timers are user-started markers. Notification reports are reports, not verified generation. Paired agent events can show the task/status actually reported by the agent. An unknown or disconnected source must never imply active generation. No accessibility, private endpoints, or fake progress to fill gaps.

## Milestones and release gates

1. **Product identity (v0.7 target):** top-attached safe-edge pill, consistent mobile-first Home and Connections, original assets, on-device usability and accessibility checks. Preserve existing functionality until replacements work.
2. **Coupi Connect (v0.8 target):** reuse the authenticated phone link; document an event contract, then test one real agent integration end-to-end. Define session identity, sequencing, expiration, authentication, revocation, local data minimization and unknown/disconnected state. Do not expose an unauthenticated receiver.
3. **Phone-only integration validation (v0.9 target):** investigate supported provider/browser event interfaces. Ship only a tested integration with explicit user consent and accurate source labels. A browser-specific indicator is not a native-app hook. No assumption that consumer AI apps emit webhooks. If no phone-only genuine source is available, retain the honest fallback and reconsider positioning.
4. **Public launch (v1.0 target):** physical-device testing, signed install/upgrade CI, privacy and asset audit, clear onboarding, and distribution readiness. Preserve the appealed APK and its checksum; never overwrite prior release assets.

These are targets, not claims of implemented functionality. Do not publish a release until its required checks pass.

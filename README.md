# Hamdel

Hamdel is a modern Android relationship and marriage assistant prototype built with Kotlin, Jetpack Compose, MVVM, Room, DataStore-ready architecture, WorkManager-ready background jobs, and a replaceable AI engine interface.

The app calls real online models with a priority fallback chain: **gapgpt.app first** (`gpt-5-nano` then `gpt-4o-mini`), and **Liara AI Gateway second** (`openai/gpt-5-nano`, `openai/gpt-4o-mini`, `google/gemini-2.0-flash-001`) if every gapgpt attempt fails. If both remote providers are unreachable (offline, no keys yet), the app transparently falls back to a local demo AI implementation so it never breaks. Encrypted cloud sync, biometric gates, and full speech processing UI can still be added behind the existing service and repository boundaries; a ready-to-use gapgpt speech-to-text/text-to-speech client (`GapgptAudioClient`) is already included for the Sessions feature.

## Features in this MVP

- Material 3 Compose interface with light and dark theme support
- Bottom navigation: Dashboard, Conversations, Sessions, Analysis, Assistant, Profile
- Relationship dashboard with compatibility, respect, intimacy, stress, warnings, and daily suggestions
- Profile and consent surface for two-person relationship analysis
- Conversation analysis demo for respect, empathy, sarcasm risk, control risk, and emotional support
- Session summary screen for speech-to-text and call-analysis workflows
- Long-term relationship memory timeline
- Smart assistant chat and message simulator
- Room database seed data and repository layer
- GitHub Actions workflow for public debug APK builds

## Build

On GitHub, the included workflow builds the debug APK on every push or pull request.

Locally, install Android Studio or Gradle plus Android SDK 35, then run:

```bash
gradle :app:assembleDebug
```

## AI keys and privacy notes

No provider API key is ever committed or hardcoded. At startup the app downloads a small encrypted `keys.txt` from a remote URL, decrypts it in memory (AES-256-GCM, key derived via PBKDF2-HMAC-SHA256 from a password), and uses the resulting gapgpt/Liara keys for that session. A backup URL is tried automatically if the primary one is unreachable. The decrypted keys are cached only inside `EncryptedSharedPreferences` on-device (for offline reuse), never written in plaintext.

To build locally:
1. Copy `secrets.properties.example` to `secrets.properties` (already gitignored) and set `KEYS_DECRYPT_PASSWORD` to the password used with `encrypt_keys.py`.
2. Build normally — if the password is left empty (e.g. CI builds), the app just keeps using the local demo AI engine instead of failing.

**Security note:** the gapgpt keys shown in the shared documentation files are plaintext example keys — since they were pasted into a chat/doc, treat them as compromised and rotate them at gapgpt.app rather than relying on them long-term. The production key material should only ever live in the encrypted remote `keys.txt`, never in a doc or in source.

The app is designed around explicit mutual consent; production releases should add biometric unlock, encrypted backup, data export, and full data deletion flows before handling real sensitive data.

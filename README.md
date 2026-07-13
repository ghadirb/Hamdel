# Hamdel

Hamdel is a modern Android relationship and marriage assistant built with Kotlin, Jetpack Compose, MVVM, Room, encrypted local key caching, and online AI provider fallback.

The app tries **GapGPT first** (`gpt-5-nano`, then `gpt-4o-mini`) and falls back to **Liara AI Gateway** (`openai/gpt-5-nano`, `openai/gpt-4o-mini`, `google/gemini-2.0-flash-001`) if GapGPT fails. If both providers are unavailable, the app keeps working through a local fallback engine.

## Features

- Material 3 Compose UI with bottom navigation
- Dashboard, Conversations, Sessions, Analysis, Assistant, and Profile tabs
- Runtime encrypted key loading from the configured public key bundle URLs
- GapGPT/Liara online analysis for conversations, assistant answers, and message simulation
- Audio recording/import with GapGPT Whisper transcription and automatic session analysis
- Editable user profiles with explicit consent toggles
- Relationship memory timeline and dashboard metrics that update after real analyses
- Startup information dialog loaded from `https://abrehamrahi.ir/o/public/NdnIkby5/`
- GitHub Actions workflow for public debug APK builds

## Build

On GitHub, the included workflow builds the debug APK on every push or pull request.

Locally, install Android Studio or Gradle plus Android SDK 35 and JDK 17, then run:

```bash
gradle :app:assembleDebug
```

## AI Keys

No provider API key is committed or hardcoded. At startup the app downloads the encrypted key bundle from:

```text
https://abrehamrahi.ir/o/public/eUFcsXOX
https://gist.githubusercontent.com/ghadirb/626a804df3009e49045a2948dad89fe5/raw/c93c06d1b2f38c65ee30f092c134a89998326d12/keys.txt
```

The bundle is decrypted locally with the same AES-GCM/PBKDF2 method used by `encrypt_keys.py`. The public build defaults to the password used for the published encrypted bundle, so online providers can work without committing raw API keys. To rotate the password locally, copy `secrets.properties.example` to `secrets.properties` and set `KEYS_DECRYPT_PASSWORD`.

## Startup Message JSON

Upload `startup_message.json` to this direct URL:

```text
https://abrehamrahi.ir/o/public/NdnIkby5/
```

The app falls back to a built-in message if the URL is unavailable.

## Privacy

The app is designed around explicit mutual consent. It does not make final decisions for users and is not a replacement for a human counselor, psychologist, emergency service, or legal/medical advice. Production releases should add biometric unlock, encrypted backup, data export, and full data deletion before handling real sensitive data at scale.

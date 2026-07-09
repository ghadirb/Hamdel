# Hamdel

Hamdel is a modern Android relationship and marriage assistant prototype built with Kotlin, Jetpack Compose, MVVM, Room, DataStore-ready architecture, WorkManager-ready background jobs, and a replaceable AI engine interface.

The public build uses a local demo AI implementation so the repository can compile without API keys. Online LLM, encrypted cloud sync, biometric gates, and speech processing can be added behind the existing service and repository boundaries.

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

## AI and privacy notes

No private key is committed. Add online model providers through `RelationshipAiEngine` and read credentials from secure runtime configuration. The app is designed around explicit mutual consent; production releases should add biometric unlock, encrypted backup, data export, and full data deletion flows before handling real sensitive data.

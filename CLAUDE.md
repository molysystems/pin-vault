# PinVault — Claude Project Context

## What This App Does
App-aware credential overlay for Android. Stores app-specific PINs/passwords/recovery codes (which traditional password managers handle poorly due to URL-centric design). Key feature: floating overlay appears automatically when a linked app is opened.

## Tech Stack
- Language: Kotlin
- Min SDK: 26, Target SDK: 34
- UI: Jetpack Compose (main app) + XML Views (overlay)
- Architecture: MVVM + Room + Hilt
- Encryption: AndroidKeyStore + AES-256-GCM
- Build: Gradle Kotlin DSL

## Package
`com.molysystems.pinvault`

## Key Design Decisions
- No INTERNET permission (zero network access, verifiable by users)
- Hardware-backed encryption via AndroidKeyStore
- UsageStatsManager for foreground app detection (not AccessibilityService)
- Overlay uses XML views (not Compose) for WindowManager compatibility
- English-only UI, Material Design 3, dark mode default

## Permissions Required
- SYSTEM_ALERT_WINDOW — overlay display
- FOREGROUND_SERVICE — persistent services
- PACKAGE_USAGE_STATS — foreground app detection

## What NOT to Build Yet
- Cloud sync / backup
- Autofill service
- Password generator
- OTP capture via NotificationListenerService (Step 6, lower priority)

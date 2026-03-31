# Contributing to Project Asteria

Thanks for your interest in contributing! This document explains how to set up the project, propose changes, and follow standards that keep the codebase healthy.

## 🛠 Quick Start
1. Fork the repo & clone your fork.
2. Create a feature branch: `git checkout -b feat/your-feature-name`.
3. Add a NASA API key (see `API_SETUP.md`).
4. Run the app: use Android Studio or `./gradlew installDebug`.
5. Commit using conventional commits (see below) and open a PR.

## ✅ Prerequisites
- Android Studio (latest stable)
- JDK 17 or 11 (match Gradle config)
- Kotlin + Jetpack Compose familiarity
- A valid NASA APOD API key

## 🧱 Architecture & Conventions
- UI: Jetpack Compose + Material 3.
- Data: Repository pattern wrapping Retrofit (Moshi) calls.
- Models: Immutable Kotlin data classes, nullable only when API may omit a field.
- State: Prefer `State`, `remember`, and unidirectional data flow.
- Avoid leaking coroutine scopes—use `viewModelScope` when adding ViewModels.

## 🧪 Testing (Planned)
If adding tests:
- Put unit tests under `app/src/test/...`
- Use JUnit + Kotlin test libs.
- Keep tests deterministic; mock network.

## 📝 Commit Message Format (Conventional Commits)
```
<type>(optional scope): <description>

(optional body)
(optional footer)
```
Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`.
Examples:
- `feat(ui): add immersive fullscreen image viewer`
- `fix(api): handle null url from APOD response`

## 🔀 Pull Request Guidelines
- Keep PRs focused (logical set of changes).
- Link related issue(s) if they exist.
- Include before/after screenshots for UI changes.
- Note any follow‑ups or tech debt.

## 🧹 Code Style
- Kotlin official style (Android Studio default formatting).
- No wildcard imports.
- Prefer explicit visibility.
- Avoid premature optimization.

## 🌐 Networking
- Use the central `NasaApodService` factory.
- Handle nullable fields defensively.
- Avoid hardcoding API keys (use `local.properties`).

## 🔐 Secrets
Never commit real API keys. Use `local.properties` and the provided template.

## 🐛 Reporting Bugs
Open an issue with:
- Steps to reproduce
- Expected vs actual behavior
- Logs / stack trace (trimmed)
- Device/emulator info

## 💡 Feature Requests
Describe the use case and value. Mockups or sketches help.

## 🚫 Ground Rules
- Follow the `CODE_OF_CONDUCT.md`.
- No spammy or low‑effort PRs (they’ll be closed).

## 📦 Release Process (Future)
1. Bump version in `app/build.gradle.kts`.
2. Tag release: `git tag -a vX.Y.Z -m "Release vX.Y.Z"`.
3. Attach signed APK / bundle (future CI).

## 🧭 Roadmap (Indicative)
- Offline caching
- Theming & dynamic color polish
- Notifications scheduling refinement
- More NASA endpoints (e.g., Mars Rover photos)

## 🙌 Recognition
Meaningful contributions (features, fixes, docs) may be credited in README.

## ❓ Questions
Open a Discussion or Issue—maintainers will respond when available.

Happy hacking! ✨

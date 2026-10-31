# Contributing to Still

Thanks for helping. Still does one thing, slow breathing; contributions that keep it small, private and calm are the most welcome.

## Get started

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleDebug && python3 scripts/checks.py
```

You need JDK 21 and the Android SDK with platform 36. Test haptics on a real phone: the emulator does not vibrate.

## Ground rules

- **No network, ever.** A pull request that adds the INTERNET permission, directly or through a dependency, will not be merged. CI fails if the merged manifest contains it.
- **No new dependency** without a strong reason. Today the app uses AndroidX Compose, Activity and Kotlin coroutines.
- **One permission.** Vibration. Anything else must be optional, asked at the moment it is needed, and explained in the UI.
- **Only sessions and settings on disk.** Sessions are start time, mode and minutes. Nothing else is stored.
- **No pressure.** No streaks, badges, scores or reminders the user did not set. See [NON_GOALS.md](NON_GOALS.md).
- **Quran only** in Trust: Arabic and a public domain translation. No hadith, no commentary.
- Keep it simple. If a feature needs a settings screen of its own, it probably belongs in another app.

## Pull requests

- One topic per pull request, with a short description of what changes for the user.
- Add or update unit tests in `app/src/test` for any change to `Patterns`, `stateAt` or `Store`.
- Still is English only, in sentence case. Strings go in `values/strings.xml`; lint fails on hard-coded text. One verb per button, never "OK", no exclamation marks.
- Colors, sizes and type come from `Tokens.kt`. Never write a color, a `.dp` or a `.sp` in a screen: add a token. `scripts/checks.py` fails on a literal anywhere else, and `ContrastTest` fails if a text and background pair drops below WCAG AA.
- Update `CHANGELOG.md` under "Unreleased".

## Reporting bugs

Open an issue with your phone model, Android version (and ROM, e.g. GrapheneOS), the mode you used, and what you expected.

# Still

**Breathe slowly. Calm down, or fall asleep.**

Pick a mode, tap Begin, and follow the orb: it grows as you breathe in, rests when you hold, and shrinks as you breathe out. Soft tones and a gentle vibration keep you in time, even with your eyes closed. When it is over, Still says so, tells you how many minutes you breathed today, and lets you go.

## Three modes

| Mode | What it does | Length |
|---|---|---|
| **Calm** | Cardiac coherence: in for 5 seconds, out for 5, six breaths a minute. | 3, 5 or 10 minutes |
| **Sleep** | Starts at in 4, out 6, and slows down over 8 minutes to 4-7-8 (in 4, hold 7, out 8). Black screen, a barely visible orb, haptics only by default. Stops by itself. | 10, 15 or 20 minutes |
| **Trust** | One verse of the Quran about trusting God, to read calmly, then 3 minutes of Calm. | 3 minutes |

Trust shows the Quran only: the Arabic text and Pickthall's English translation (1930, public domain). No hadith, no commentary.

## Why trust it

| | |
|---|---|
| No internet permission | The app cannot open a network connection. Check `AndroidManifest.xml`, or the permissions screen once installed. Sounds and verses ship inside the app. |
| Only your sessions are stored | Date, mode and minutes of each session, in a small file in the app's private storage. Nothing else. |
| Yours to take or erase | Export your sessions to a JSON file saved where you choose, or erase everything at once, from Settings. |
| One permission | Vibration, for the breath haptics. Nothing to grant, nothing to ask. |
| No account, no tracking | No analytics, no ads, no cloud backup, no device-to-device transfer. No streaks, no badges, no reminders. |
| Checkable | About shows the signing certificate of the installed app, to compare with [SECURITY.md](SECURITY.md). Last security review: 31 Oct 2026, internal, no external audit yet ([docs/security](docs/security/), [threat model](docs/THREAT_MODEL.md)). |

## Features

- **The orb is the interface.** It moves on springs, never in a straight line, and follows every phase of the breath.
- **Haptics that breathe.** A slow rising texture as you breathe in, stillness on a hold, a slow falling texture as you breathe out. They follow your phone's media vibration setting, and taps follow its touch feedback setting.
- **Quiet by default when it should be.** Sounds play only when your phone is not on silent or vibrate. An optional ambience: wind, rain or night. Sound and haptics each have their own switch in Settings.
- **Sleep without a service.** The screen stays black (OLED pixels off) and the session ends on its own, then the phone sleeps normally. No background service, no wake lock.
- Its own color: night blue, the same as the icon, in light and dark. Flat, rounded, set in Bricolage Grotesque and Figtree. English.

What Still will never do: [NON_GOALS.md](NON_GOALS.md).

## Install

Android 12 or newer. Built and tested for GrapheneOS, works on any Android.

- [Obtainium](https://github.com/ImranR98/Obtainium): add `https://github.com/sidalidev/still`. Obtainium installs Still from the releases and keeps it up to date.
- Or download the APK from [Releases](https://github.com/sidalidev/still/releases) and check its SHA-256 against the `.sha256` file published next to it:

  ```bash
  shasum -a 256 still-0.1.0.apk
  ```

Still cannot update itself, by design: it has no internet access.

## How it works

1. `Patterns` (in `Engine.kt`) turns a mode and a length into the full timeline of the session, a list of steps: in, hold, out.
2. `Engine` walks that timeline about every frame and publishes a `BreathState`; the orb draws it.
3. At the start of each phase, `Haptics` and `Sound` get a call and play in time with the orb.
4. When the session ends, `Store` adds it to the JSON file and the end screen shows today's minutes.
5. `Verses` holds the Trust verses and picks one for the day.

Read `Breath.kt` first: it is the shared language of the whole app.

## Build

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
python3 scripts/checks.py
```

Release signing reads `~/.still/keystore.properties` if present; otherwise the release APK is unsigned (`-Punsigned` forces it).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Security issues: [SECURITY.md](SECURITY.md). Privacy: [PRIVACY.md](PRIVACY.md).

## License

[Apache-2.0](LICENSE). Fonts, icon, verses and sounds: see [ASSETS.md](ASSETS.md).

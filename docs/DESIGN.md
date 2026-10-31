# Still: design

Still helps you calm down and fall asleep by breathing slowly. Everything happens on the phone: no internet permission, no account, no tracking.

## The three modes

| Mode | Pattern | Length | Guide |
|---|---|---|---|
| **Calm** (cardiac coherence, "365") | in 5 s, out 5 s (6 breaths a minute) | 5 min (3, 5 or 10) | orb, soft sound, haptics |
| **Sleep** | starts at in 4 / out 6 and slows down over 8 minutes to 4-7-8 (in 4, hold 7, out 8) | 10 min (10, 15, 20), then stops by itself | black screen, very dim orb, haptics only by default |
| **Trust** | one verse of the Quran about trusting God, read calmly, then Calm for 3 min | 3 min | verse card, then orb |

Trust shows the Quran only (Arabic and an English translation in the public domain, Pickthall 1930). No hadith, no commentary.

## Feel

- One action per screen. Home: the orb breathing slowly at rest, three mode cards, one big "Begin".
- The orb is the interface: it grows on the in-breath, rests on a hold, shrinks on the out-breath, with springs, never linear.
- Haptics follow the breath: a slow rising texture while breathing in, nothing on a hold, a slow falling texture while breathing out (`VibrationEffect.Composition`, `PRIMITIVE_SLOW_RISE`, `PRIMITIVE_QUICK_FALL`, `PRIMITIVE_LOW_TICK`), with a simple waveform fallback. The breath textures use the media vibration usage, so the system media vibration setting applies; taps go through the view, so the touch feedback setting applies.
- Sleep mode keeps the screen on but fully black (OLED off pixels) with a barely visible orb, so it never needs a background service or a wake lock. The session ends on its own and the screen is released.
- Sounds: soft in and out tones and an optional ambience (wind, rain, night), made once, shipped as `.ogg`. Only when the phone is not on silent or vibrate. Sound and Haptics are two switches in Settings.
- End: "That's it." and the minutes breathed today. No streaks, no badges, no reminders unless the user sets one.

## Data

Only the user's sessions (date, mode, minutes), in a small JSON file in the app's private storage. Export to a JSON file saved where the user chooses (system file picker), erase everything in one gesture. Nothing else is stored.

## Colour

Night blue `#34508A` (cream text `#FBF7EF` on it), on the shared cream and night-pine backgrounds. Icon: a rounded crescent above a soft breath wave, cream on flat night blue.

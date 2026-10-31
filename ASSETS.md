# Assets

| Asset | Where | License |
|---|---|---|
| Bricolage Grotesque (variable), titles | `app/src/main/res/font/bricolage_grotesque.ttf`, from [google/fonts](https://github.com/google/fonts/tree/main/ofl/bricolagegrotesque) | SIL Open Font License 1.1, [`app/src/main/assets/licenses/BricolageGrotesque-OFL.txt`](app/src/main/assets/licenses/BricolageGrotesque-OFL.txt) |
| Amiri Quran, the Arabic verses | `app/src/main/res/font/amiri_quran.ttf`, `AmiriQuran.ttf` from the [Amiri 1.003 release](https://github.com/aliftype/amiri/releases/tag/1.003) by the Amiri Project Authors | SIL Open Font License 1.1, [`app/src/main/assets/licenses/AmiriQuran-OFL.txt`](app/src/main/assets/licenses/AmiriQuran-OFL.txt) |
| Figtree (variable), text | `app/src/main/res/font/figtree.ttf`, from [google/fonts](https://github.com/google/fonts/tree/main/ofl/figtree) | SIL Open Font License 1.1, [`app/src/main/assets/licenses/Figtree-OFL.txt`](app/src/main/assets/licenses/Figtree-OFL.txt) |
| App icon: a rounded crescent above a soft breath wave | `app/src/main/res/drawable/` | Drawn for Still, Apache-2.0 like the code |
| Quran verses, Arabic | `Verses.kt` | Uthmani text from the [Tanzil project](https://tanzil.net), Creative Commons Attribution 3.0, text unchanged |
| Quran verses, English | `Verses.kt` | Marmaduke Pickthall, *The Meaning of the Glorious Koran* (1930), public domain |

The fonts are unmodified. Their licenses ship inside the APK, in `assets/licenses/`.

Trust shows the Quran only. No hadith, no commentary.

Colors: the sidali.dev palette. Still is night blue `#34508A`, with cream `#FBF7EF` on it; the page is `#FBF7EF` (light) or `#12201A` (dark). All values live in `Palette` in `Tokens.kt`.

## Verses

`app/src/main/java/dev/sidali/still/Verses.kt`. Quran only, no hadith, no commentary. 9 short passages: 13:28, 65:3 (part), 94:5-6, 2:286 (part), 3:173 (part), 9:51, 2:152, 39:53 (part), 20:46.

- Arabic: Uthmani text from the [Tanzil project](https://tanzil.net), Creative Commons Attribution 3.0, used verbatim as Tanzil requires. Retrieved through api.alquran.cloud, edition `quran-uthmani`.
- English: Marmaduke Pickthall, *The Meaning of the Glorious Koran* (1930), public domain. Retrieved through api.alquran.cloud, edition `en.pickthall`. Wording and punctuation unchanged, so the translation keeps its own exclamation marks: it is quoted scripture, not interface text.
- Partial verses are marked "(part)" and cut at the same natural boundary in both languages.

## Sounds

All in `app/src/main/res/raw/`, made by [`scripts/gen-sounds.mjs`](scripts/gen-sounds.mjs). Mono Ogg, silence trimmed, short fades on the one-shots, loudness normalised around -18 LUFS (ffmpeg `loudnorm`). Ambiences are made seamless by crossfading their last 2 s into their first 2 s, then loop in `MediaPlayer` at a low volume.

| File | What | Length | Loudness | Size |
|---|---|---|---|---|
| `breath_in.ogg` | warm chord rising slightly in pitch, with soft air | 3.1 s | -17.2 LUFS | 31 KB |
| `breath_out.ogg` | the same chord falling, darker air | 3.1 s | -17.4 LUFS | 29 KB |
| `chime.ogg` | one low wooden bell (G3, inharmonic partials) | 2.0 s | -18.3 LUFS | 16 KB |
| `amb_wind.ogg` | wind in pines, loop | 20.0 s | -19.2 LUFS | 88 KB |
| `amb_rain.ogg` | steady rain on a roof, loop | 20.0 s | -19.8 LUFS | 89 KB |
| `amb_night.ogg` | quiet night with distant crickets, loop | 20.0 s | -17.9 LUFS | 103 KB |

The script has two sources:

- **ElevenLabs Sound Effects** (default): `ELEVENLABS_API_KEY=... node scripts/gen-sounds.mjs`. The key comes from the environment only and needs the `sound_generation` permission. Before a public release with these sounds, confirm that the ElevenLabs plan in use allows commercial redistribution of the sound effects it makes.
- **Offline synthesis** (`--local`): sines and filtered noise built with ffmpeg alone, no network, no third-party material. The sounds shipped today come from this mode, so they are original to this repository and fall under its license.

Encoding: Ogg Vorbis 44.1 kHz when ffmpeg has `libvorbis`, otherwise Ogg Opus 48 kHz (64 kbit/s one-shots, 40 kbit/s loops), which Android decodes natively from Android 10 (the app needs Android 12).

Volumes in the app (`Tokens.Mix` in `Tokens.kt`): breath tones 0.55, chime 0.7, ambience 0.28 with a 1.8 s fade. Sounds play only when the phone is in ringer mode (not silent, not vibrate) and the Sound setting is on; audio focus is taken transiently only while an ambience plays.

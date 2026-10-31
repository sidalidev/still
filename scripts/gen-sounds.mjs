#!/usr/bin/env node
// Makes the sounds in app/src/main/res/raw/ with the ElevenLabs Sound Effects API, then shapes them
// with ffmpeg: mono Ogg (Vorbis 44.1 kHz, or Opus when libvorbis is missing), silence trimmed, short fades, about -18 LUFS.
// Ambiences are turned into seamless loops by crossfading their tail into their head.
//
//   ELEVENLABS_API_KEY=... node scripts/gen-sounds.mjs            (all sounds)
//   ELEVENLABS_API_KEY=... node scripts/gen-sounds.mjs chime       (only some)
//   node scripts/gen-sounds.mjs --local                            (synthesised with ffmpeg, no API)
//
// --local builds the same six sounds from sines and filtered noise, fully offline. It is the
// fallback when no ElevenLabs key with the sound_generation permission is at hand.
// The key is read from the environment only; it is never written anywhere.
// Needs Node 18+ (fetch) and ffmpeg with libvorbis.

import { execFileSync } from "node:child_process";
import { mkdtempSync, writeFileSync, rmSync, statSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const OUT = join(ROOT, "app/src/main/res/raw");

const SOUNDS = [
  { name: "breath_in", seconds: 3, influence: 0.6, quality: 4,
    text: "very soft airy rising tone, like a slow inhale through the nose turned into a warm pad, gentle, no voice" },
  { name: "breath_out", seconds: 3, influence: 0.6, quality: 4,
    text: "very soft descending warm pad, like a slow exhale, gentle, no voice" },
  { name: "chime", seconds: 2, influence: 0.6, quality: 4,
    text: "a single soft low wooden bell, gentle, one strike, calm decay" },
  { name: "amb_wind", seconds: 22, influence: 0.5, quality: 0, loop: true,
    text: "calm night wind in mountain pines, steady, seamless loop, no music" },
  { name: "amb_rain", seconds: 22, influence: 0.5, quality: 0, loop: true,
    text: "gentle steady rain on a roof, seamless loop, no thunder, no music" },
  { name: "amb_night", seconds: 22, influence: 0.5, quality: 0, loop: true,
    text: "quiet summer night, distant crickets, calm, seamless loop, no music" },
];

const args = process.argv.slice(2);
const local = args.includes("--local");
const key = process.env.ELEVENLABS_API_KEY;
if (!local && !key) {
  console.error("ELEVENLABS_API_KEY is not set.");
  process.exit(1);
}

async function generate(s) {
  const body = { text: s.text, duration_seconds: s.seconds, prompt_influence: s.influence };
  const call = (extra) =>
    fetch("https://api.elevenlabs.io/v1/sound-generation?output_format=mp3_44100_128", {
      method: "POST",
      headers: { "xi-api-key": key, "Content-Type": "application/json", Accept: "audio/mpeg" },
      body: JSON.stringify({ ...body, ...extra }),
    });
  // Ask for a native loop when the model supports it; fall back to a plain request otherwise.
  let res = await call(s.loop ? { loop: true } : {});
  if (!res.ok && s.loop && res.status >= 400 && res.status < 500) res = await call({});
  if (!res.ok) throw new Error(`${s.name}: HTTP ${res.status} ${(await res.text()).slice(0, 300)}`);
  return Buffer.from(await res.arrayBuffer());
}

// Offline recipes: ffmpeg filter graphs that end in [out].
const PAD = (f, glide) => {
  // A warm chord on f (root, fifth, octave, a faint ninth), its pitch gliding by `glide` per second.
  const ph = `(t+${glide / 2}*t*t)`;
  return `aevalsrc='0.5*sin(2*PI*${f}*${ph})+0.33*sin(2*PI*${f * 1.4983}*${ph})+0.22*sin(2*PI*${f * 2}*${ph})+0.06*sin(2*PI*${f * 2.2449}*${ph})':s=44100:d=3`;
};
const RECIPES = {
  breath_in:
    `${PAD(110, 0.02)},lowpass=f=1400[t];` +
    `anoisesrc=d=3:c=pink:a=0.5:r=44100,bandpass=f=1500:w=1400,volume=0.35[n];` +
    `[t][n]amix=inputs=2:normalize=0,afade=t=in:d=2.4:curve=qsin,afade=t=out:st=2.4:d=0.6:curve=qsin[out]`,
  breath_out:
    `${PAD(110, -0.02)},lowpass=f=1100[t];` +
    `anoisesrc=d=3:c=pink:a=0.5:r=44100,bandpass=f=900:w=900,volume=0.35[n];` +
    `[t][n]amix=inputs=2:normalize=0,afade=t=in:d=0.3:curve=qsin,afade=t=out:st=0.3:d=2.7:curve=qsin[out]`,
  chime:
    `aevalsrc='min(t/0.004,1)*(0.6*sin(2*PI*196*t)*exp(-2.2*t)+0.3*sin(2*PI*540.9*t)*exp(-4.5*t)+0.12*sin(2*PI*1058*t)*exp(-8*t)+0.05*sin(2*PI*1745*t)*exp(-14*t))':s=44100:d=2,lowpass=f=3000[out]`,
  amb_wind:
    `anoisesrc=d=22:c=brown:a=0.8:r=44100,highpass=f=70,lowpass=f=700,` +
    `volume='0.55+0.3*sin(2*PI*t/11)+0.15*sin(2*PI*t/3.7+1)':eval=frame[w];` +
    `anoisesrc=d=22:c=pink:a=0.3:r=44100,bandpass=f=1800:w=1500,volume='0.06+0.05*sin(2*PI*t/11+0.6)':eval=frame[h];` +
    `[w][h]amix=inputs=2:normalize=0[out]`,
  amb_rain:
    `anoisesrc=d=22:c=pink:a=0.6:r=44100,highpass=f=250,lowpass=f=5000,equalizer=f=2500:t=q:w=1:g=-4[r];` +
    `anoisesrc=d=22:c=brown:a=0.5:r=44100,lowpass=f=300,volume=0.5[roof];` +
    `[r][roof]amix=inputs=2:normalize=0[out]`,
  amb_night:
    `anoisesrc=d=22:c=brown:a=0.4:r=44100,lowpass=f=400,volume=0.6[bed];` +
    `aevalsrc='0.05*sin(2*PI*4400*t)*gt(sin(2*PI*30*t),0.2)*lt(mod(t,0.93),0.13)+0.03*sin(2*PI*4720*t)*gt(sin(2*PI*26*t),0.2)*lt(mod(t+0.4,1.17),0.11)':s=44100:d=22,` +
    `highpass=f=3000,volume='0.7+0.3*sin(2*PI*t/7)':eval=frame[crickets];` +
    `[bed][crickets]amix=inputs=2:normalize=0[out]`,
};

function synthesise(s, raw) {
  ffmpeg(["-filter_complex", RECIPES[s.name], "-map", "[out]", "-ac", "1", "-ar", "44100", raw]);
}

// Ogg Vorbis when ffmpeg has libvorbis, otherwise Ogg Opus (also .ogg, decoded natively from Android 10).
const VORBIS = execFileSync("ffmpeg", ["-hide_banner", "-encoders"]).toString().includes("libvorbis");

function ffmpeg(args) {
  execFileSync("ffmpeg", ["-hide_banner", "-loglevel", "error", "-y", ...args], { stdio: "inherit" });
}

function shape(s, raw) {
  const out = join(OUT, `${s.name}.ogg`);
  const loud = "loudnorm=I=-18:TP=-2:LRA=11";
  let filter;
  if (s.loop) {
    // head = first 2 s, body = the rest; crossfading the end of body into head makes body's
    // last sample continue exactly where body's first sample starts.
    filter =
      `[0:a]aformat=channel_layouts=mono,aresample=44100,asplit=2[a][b];` +
      `[a]atrim=0:2,asetpts=PTS-STARTPTS[head];` +
      `[b]atrim=start=2,asetpts=PTS-STARTPTS[body];` +
      `[body][head]acrossfade=d=2:c1=qsin:c2=qsin,${loud},aresample=44100[out]`;
  } else {
    const trim = "silenceremove=start_periods=1:start_threshold=-55dB:start_silence=0.02";
    filter =
      `[0:a]aformat=channel_layouts=mono,aresample=44100,${trim},areverse,${trim},areverse,` +
      `afade=t=in:d=0.02,areverse,afade=t=in:d=0.25,areverse,${loud},aresample=44100[out]`;
  }
  const codec = VORBIS
    ? ["-ar", "44100", "-c:a", "libvorbis", "-q:a", String(s.quality)]
    : ["-ar", "48000", "-c:a", "libopus", "-b:a", s.loop ? "40k" : "64k", "-application", "audio"];
  ffmpeg(["-i", raw, "-filter_complex", filter, "-map", "[out]", "-ac", "1", ...codec, "-map_metadata", "-1", out]);
  return out;
}

const wanted = args.filter((a) => !a.startsWith("--"));
const work = mkdtempSync(join(tmpdir(), "still-sounds-"));
try {
  for (const s of SOUNDS.filter((s) => wanted.length === 0 || wanted.includes(s.name))) {
    const raw = join(work, `${s.name}.${local ? "wav" : "mp3"}`);
    if (local) synthesise(s, raw);
    else writeFileSync(raw, await generate(s));
    const out = shape(s, raw);
    console.log(`${s.name}.ogg  ${Math.round(statSync(out).size / 1024)} KB`);
  }
} finally {
  rmSync(work, { recursive: true, force: true });
}

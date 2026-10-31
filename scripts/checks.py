#!/usr/bin/env python3
"""
Release checks. CI runs them after the release build; run them locally the same way:

    ./gradlew :app:assembleRelease -Punsigned && python3 scripts/checks.py

Fails when:
  - a merged release manifest asks for INTERNET, or for a permission not in docs/permissions.txt;
  - a color, dp or sp literal appears in Kotlin outside Tokens.kt;
  - a UI string says "OK" or has an exclamation mark;
  - a repository text file (tracked, or new and not ignored) contains a long or medium dash, a
    personal path, a forbidden word, or a leftover from the app template;
  - the latest review in docs/security/ is missing or more than 6 months old;
  - CHANGELOG.md or the store changelog has no entry for the version in app/build.gradle.kts.

Without a release build, the permission check is skipped with a warning (pass --strict to fail).
"""
import datetime
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ANDROID_NAME = "{http://schemas.android.com/apk/res/android}name"
STRICT = "--strict" in sys.argv
errors: list[str] = []
warnings: list[str] = []


def permissions() -> None:
    allowed = {line.strip() for line in (ROOT / "docs/permissions.txt").read_text().splitlines() if line.strip()}
    if "android.permission.INTERNET" in allowed:
        errors.append("docs/permissions.txt lists INTERNET. Still never asks for it.")
    manifests = sorted(ROOT.glob("app/build/intermediates/merged_manifest/release/*/AndroidManifest.xml"))
    if not manifests:
        (errors if STRICT else warnings).append("No merged release manifest: build the release first.")
    for m in manifests:
        for tag in ("uses-permission", "uses-permission-sdk-23"):
            for e in ET.parse(m).getroot().iter(tag):
                name = e.get(ANDROID_NAME)
                if name == "android.permission.INTERNET":
                    errors.append(f"{m.relative_to(ROOT)}: INTERNET permission, never allowed")
                elif name not in allowed:
                    errors.append(f"{m.relative_to(ROOT)}: {name} is not listed in docs/permissions.txt")


# Design values live in Tokens.kt only.
LITERAL = re.compile(r"\b\d+(\.\d+)?f?\.(dp|sp)\b|Color\(0x|Color\.(White|Black|Red|Green|Blue|Gray)\b")


def tokens_only() -> None:
    for path in sorted((ROOT / "app/src/main/java").rglob("*.kt")):
        if path.name == "Tokens.kt":
            continue
        for n, line in enumerate(path.read_text().splitlines(), 1):
            if LITERAL.search(line):
                errors.append(f"{path.relative_to(ROOT)}:{n}: design literal, use a token from Tokens.kt")


def ui_strings() -> None:
    for path in sorted((ROOT / "app/src/main/res").glob("values*/strings.xml")):
        for e in ET.parse(path).getroot().iter("string"):
            text = "".join(e.itertext()).strip()
            if "!" in text:
                errors.append(f"{path.relative_to(ROOT)}: {e.get('name')} has an exclamation mark")
            if re.search(r"\bOK\b", text, re.IGNORECASE):
                errors.append(f"{path.relative_to(ROOT)}: {e.get('name')} says OK, use a verb")


# Built from pieces so this file does not match itself.
DASHES = re.compile("[\\u2013\\u2014]")
PERSONAL = re.compile("/" + "Users/|/" + "home/[a-z]")
FORBIDDEN = re.compile(
    r"\bcl[a]ude\b|\banthr[o]pic\b|\bchat[g]pt\b|\bopen[a]i\b|\bcop[i]lot\b|\bass[i]stants?\b"
    r"|\bgener[a]ted\b|co-[a]uthored-by",
    re.IGNORECASE,
)
SHORT_FORM = re.compile(r"\bA[I]\b")
LEFTOVER = re.compile(r"\bEch[o]\b|dev\.sidali\.ech[o]\b|sidalidev/ech[o]\b|\.ech[o]/")
# Third-party texts kept exactly as they ship: the Gradle wrapper and the Apache License.
VENDOR = {"gradlew", "gradlew.bat", "LICENSE"}


def repository_files() -> list[str]:
    out = subprocess.run(
        ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"],
        cwd=ROOT, capture_output=True, check=True,
    ).stdout
    return sorted({f.decode() for f in out.split(b"\0") if f})


def repository_text() -> None:
    for f in repository_files():
        path = ROOT / f
        if not path.is_file():
            continue
        data = path.read_bytes()
        if b"\0" in data:
            continue  # binary
        rules = [(DASHES, "long or medium dash"), (PERSONAL, "personal path"), (LEFTOVER, "template leftover")]
        if f not in VENDOR:
            rules += [(FORBIDDEN, "forbidden word"), (SHORT_FORM, "forbidden word")]
        for n, line in enumerate(data.decode("utf-8", errors="replace").splitlines(), 1):
            for rx, what in rules:
                m = rx.search(line)
                if m:
                    errors.append(f"{f}:{n}: {what} ({m.group(0)!r})")


def add_months(d: datetime.date, months: int) -> datetime.date:
    y, m = divmod(d.month - 1 + months, 12)
    year, month = d.year + y, m + 1
    for day in (d.day, 30, 29, 28):
        try:
            return datetime.date(year, month, day)
        except ValueError:
            continue
    raise ValueError(d)


def security_review() -> None:
    dates = sorted(
        datetime.date.fromisoformat(p.stem)
        for p in (ROOT / "docs/security").glob("*.md")
        if re.fullmatch(r"\d{4}-\d{2}-\d{2}", p.stem)
    )
    if not dates:
        errors.append("No dated review in docs/security/.")
    elif add_months(dates[-1], 6) < datetime.date.today():
        errors.append(f"The latest security review ({dates[-1]}) is more than 6 months old.")


def changelog() -> None:
    gradle = (ROOT / "app/build.gradle.kts").read_text()
    name = re.search(r'versionName = "([^"]+)"', gradle).group(1)
    code = re.search(r"versionCode = (\d+)", gradle).group(1)
    if not re.search(rf"^## {re.escape(name)}\s*$", (ROOT / "CHANGELOG.md").read_text(), re.MULTILINE):
        errors.append(f"CHANGELOG.md has no '## {name}' entry.")
    if not (ROOT / f"fastlane/metadata/android/en-US/changelogs/{code}.txt").is_file():
        errors.append(f"No store changelog fastlane/metadata/android/en-US/changelogs/{code}.txt.")


for check in (permissions, tokens_only, ui_strings, repository_text, security_review, changelog):
    check()
for w in warnings:
    print(f"::warning::{w}")
for e in errors:
    print(f"::error::{e}")
if errors:
    sys.exit(1)
print("Release checks passed.")

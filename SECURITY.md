# Security policy

## Reporting a vulnerability

Please do not open a public issue. Use GitHub's private vulnerability reporting: [report a vulnerability](https://github.com/sidalidev/still/security/advisories/new) (also in the app: About, "Report a security issue"). If you cannot use GitHub, write to contact@sidali.dev. You will get an answer within a week.

Include what an attacker can do, the steps to reproduce, and the version or commit.

## Scope

In scope: anything that lets another app or a remote party read your sessions or settings, makes Still use the network, keeps data after Erase everything, writes data outside the app's private storage without an export you started, or keeps the screen or the phone awake after a session ends.

Out of scope: a rooted or compromised phone, and what you do with an exported file.

What is protected and what is not: [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md). Reviews: [docs/security/](docs/security/) (internal, no external audit yet).

## Design notes

- No INTERNET permission. CI checks the merged manifest against [docs/permissions.txt](docs/permissions.txt).
- One permission: vibration.
- Only sessions (start time, mode, minutes) and settings are stored, in the app's private storage.
- Sleep mode keeps the window on with a flag, never a wake lock or a background service, and releases it when the session ends.
- Backups and device-to-device transfers of app data are disabled.

## If something goes wrong

1. A GitHub security advisory within 72 hours of a confirmed issue, and a fixed release as soon as it is ready, which Obtainium picks up.
2. A dated post-mortem in `docs/security/incidents/` within 14 days.

## Release signing

APKs are signed with a single key. Its certificate SHA-256 is:

`d09543509523f019649e34f28e1bb557455cead0be54e8d51be576d20e3d72b4`

The app shows the certificate of the installed APK in About, "Signing certificate", read from Android at run time. If it differs from the one above, the app was not built by us. Obtainium and Android refuse an update signed with any other key.

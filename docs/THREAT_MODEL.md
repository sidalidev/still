# Threat model

Still guides slow breathing with an orb, haptics and soft sounds. It has no internet permission, no account and no server.

**What is protected**

- Your sessions never leave the phone through Still: without the INTERNET permission, the app cannot open a connection. CI fails if the permission ever appears.
- Only sessions (start time, mode, minutes) and settings are stored, in the app's private storage, which other apps cannot read.
- Data is excluded from cloud backups and device-to-device transfers. Export is the only way out, and only when you start it.
- Erase everything deletes your sessions and settings at once.
- Sleep mode never holds a wake lock or runs a background service: the screen is released when the session ends.
- Updates must be signed with the same key (fingerprint in SECURITY.md and in About).

**Against whom**: other apps on the phone, a network observer (there is no traffic to observe), and a modified APK (different certificate).

**What is not protected**

- Someone holding your unlocked phone can open Still and see the minutes you breathed today.
- An exported file is yours: wherever you save or share it, it is no longer protected by the app.
- A rooted or compromised phone can read any app's storage, Still's included.
- A compromise of the developer's GitHub account or signing key. The key is kept outside the repository; moving it to hardware is planned.
- No external audit has been done. Internal reviews are in `docs/security/`.

**Post-quantum**: Still uses no cryptography of its own and sends nothing, so there is nothing to break later.

**Cyber Resilience Act**: Still is free, open source and not sold, so it is outside the commercial scope.

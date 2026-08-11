# Free Guitar — a free Simply-Guitar-style app

Play-along songs, chord library, tuner, and metronome. Listens to your real
guitar through the mic and tells you when you hit the right chord. 100% free,
no ads, no subscriptions.

## Install on your phone (no USB needed)

### Option A — grab the built APK right now
The APK is at `FreeGuitar.apk`. Send it to your phone (email / cloud drive /
bluetooth), open it, and allow "install unknown apps". That's it.

### Option B — auto-build & auto-release from GitHub (free forever)
1. Create a free GitHub account, then create a new repository.
2. Upload this whole `FreeGuitar` folder to it (or push via git).
3. The included workflow (`.github/workflows/build.yml`) builds the APK
   automatically on every push. On the repo's **Actions** tab you'll see the
   build. Download `FreeGuitar-APK` artifact on your phone's browser.
4. To get a clean release page: create a git tag `v1.0`, push it, and GitHub
   publishes the APK as a release you can open directly on your phone.

## Auto-update new songs & app versions (the "Updates" tab)
The app can pull new songs and new app builds from any free JSON URL.

1. Host `example-manifest.json` (rename to `manifest.json`) somewhere free —
   GitHub Pages or any static file host.
2. In the app: **Updates** tab → paste the URL → **Check for Updates**.
3. New songs appear in your song library. New APK builds can be downloaded and
   installed right from the app.
4. Leave "Check for new songs when the app opens" ON to get real-time song
   updates automatically every launch.

### Manifest format
```json
{
  "app": {
    "versionCode": 2,
    "versionName": "1.1",
    "apkUrl": "https://YOUR-HOST/free-guitar-1.1.apk",
    "notes": "What's new"
  },
  "songs": [
    {
      "title": "My Song",
      "artist": "Artist",
      "bpm": 100,
      "strum": "↓ ↓ ↑ ↓ ↑",
      "progression": [ { "chord": "G", "beats": 4 }, { "chord": "D", "beats": 4 } ]
    }
  ]
}
```

## About F-Droid
F-Droid only accepts open-source apps with a public source repository and goes
through their review process. If you want that, publish the source on GitHub,
then follow their submission guide. Until then, Option B gives you the same
"download straight to my phone" experience with zero cost.

## Build locally
```
gradlew :app:assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

# Interval Timer (Android)

A simple interval timer for workouts. No accounts, no internet, no ads.

## What it does

- **Workouts** are a named sequence of:
  - **Start** (warm-up, optional)
  - one or more **blocks**, run in order. Each block has an optional name, a
    number of **sets**, and any number of **stages** with their own name,
    duration and type (**Work** = red, **Rest** = green). Every set runs the
    block's stages in order. For example "Circuit: 3 sets of Squats 0:40,
    Push-ups 0:40, Plank 0:40, Rest 1:00".
  - **End** (cool-down, optional)
- Rest stages at the very end of the last set of the last block are skipped.
- Create, edit, copy and delete workouts; add, remove and reorder (↑/↓) blocks and stages.
- **Run screen**: big MM:SS countdown, stage (Start / Work / Rest / End) with its
  own color, "Set 3/5 · Circuit 2/3", total time left, and what comes next.
  Buttons: Prev, Pause/Resume, Skip, Stop. The screen stays on while running.
- **Sound**: short beep at 5-4-3-2-1 seconds, a longer, higher tone when a new stage
  starts. Music (Spotify, YouTube Music, ...) only gets quieter for the beep and keeps playing.
- **Vibration** on every stage change. Sound and vibration can be switched off in Settings.
- Keeps running with the **screen off or the app in the background**: an ongoing
  notification shows the stage and seconds left, with Pause and Stop buttons.

## Web app (any phone, no install file)

There is also a web version: **<https://amiteze.github.io/Time-Intervals/>**

- Open it in Chrome (Android) or Safari (iPhone). To get an app icon:
  Chrome **⋮ → Add to Home screen / Install app**; Safari **Share → Add to Home Screen**.
  After the first visit it also works offline.
- Same workouts editor, run screen, colors, beeps and vibration as the Android app.
  Workouts are saved in that browser only (not shared with the Android app).
- **Keep the screen on and the page open while training.** The page keeps
  the screen awake by itself. Browsers pause web pages when the screen is off
  or you switch apps, so beeps and vibration can stop then. If you need the
  timer to run with the screen off, use the Android app.
- Beeps play on top of your music, but browsers can't make the music quieter
  for a moment like the Android app does.
- Vibration works on Android only (iPhones don't allow it for websites).
- The code is in `web/` (plain HTML, CSS and JavaScript, no build step).
  Every push to `main` that changes `web/` publishes it again in about a minute
  (Actions tab → "Deploy web app"). Changes only in `web/` don't build a new APK.

## Install the APK on your phone

1. On your phone, open this repo's **Releases** page:
   <https://github.com/amitEze/time-intervals/releases>
   (the newest one is at the top; the one marked **Latest** comes from `main`).
2. Under **Assets**, tap `IntervalTimer-1.0.N.apk` to download it.
3. Open the download (from the notification or the *Files* / *Downloads* app).
4. The first time, Android says installing from this source isn't allowed.
   Tap **Settings** → turn on **Allow from this source** for your browser
   (or Files app) → go back and tap **Install**.
   (You can find this later under *Settings → Apps → Special app access → Install unknown apps*.)
5. If Play Protect warns about an unknown app, tap **More details → Install anyway**.
6. Open the app and allow **notifications** when asked, so you see the timer
   while the screen is off.

**Updating:** just install the newer APK the same way. It installs over the old
one and keeps your workouts, because every build is signed with the same key.

Tip for reliable background running: on some phones (Samsung, Xiaomi, ...)
go to *Settings → Apps → Interval Timer → Battery* and choose **Unrestricted**.

## Edit the code from your phone and get a new build

Every push to `main` builds a new APK automatically (GitHub Actions) and
publishes it as a new Release, about 5 minutes later.

**Option A: browser (github.dev)**
1. Open the repo in your browser and press `.` (or change `github.com` to
   `github.dev` in the address). This opens an editor.
2. Edit files, then use the *Source Control* panel: write a message → **Commit & Push** to `main`.

**Option B: GitHub mobile app or github.com**
1. Open a file → tap the ✏️ (edit) button → change it → **Commit changes** to `main`.

**Then:** open the **Actions** tab to watch the "Build APK" run. Green ✔ = a new
Release with the APK is ready. Red ✖ = tap the run → the failed step to see the
error (or ask Claude Code to fix it, see below).

Each run also has the APK as a downloadable **artifact** (at the bottom of the run page).

### Where things are (all small files)

| File | What it controls |
|---|---|
| `app/src/main/java/.../timer/StageBuilder.kt` | How a workout becomes a list of stages |
| `app/src/main/java/.../timer/TimerEngine.kt` | Countdown, pause, skip, previous (pure Kotlin, unit tested) |
| `app/src/main/java/.../service/TimerService.kt` | Background service, notification, when beeps happen |
| `app/src/main/java/.../service/Beeper.kt` | Beep pitch and length, audio focus / ducking |
| `app/src/main/java/.../ui/StageColors.kt` | Run screen colors |
| `app/src/main/java/.../ui/RunScreen.kt` | Run screen layout |
| `app/src/main/java/.../ui/EditorScreen.kt` | Workout editor |
| `app/src/main/java/.../data/Storage.kt` | Saving workouts and settings, example workouts |
| `app/src/test/...` | Unit tests (run in CI before every build) |
| `.github/workflows/android.yml` | The APK build + release workflow |
| `web/js/*.js` | Web app: `engine.js`/`stages.js` timer logic, `views.js` screens, `sound.js` beeps |
| `web/style.css` | Web app colors and layout |
| `.github/workflows/web.yml` | Publishes the web app to GitHub Pages |

## How to ask Claude Code to change things

Open Claude Code (claude.ai/code or the Claude app), pick the `time-intervals` repo and describe
the change in plain words. Good requests are specific and say how you'll check it:

- "Make the countdown beeps start at 3 seconds instead of 5."
- "Add a 'Get ready' 10-second countdown before every workout."
- "Change the Rest color to dark blue."
- "The last build failed. Check the Actions run, fix it and push to main."

Ask it to **push to `main`** (or to merge its branch) when you want a new APK;
pushes to other branches create *pre-release* builds you can also install.

## Signing key

The APK is signed with the key in `keystore/release.jks` (password
`intervaltimer`). It is committed so the build works with zero setup and
every new APK installs over the old one.
Note: because it's in a public repo, anyone could sign an app with it. That's
fine for a personal sideloaded app; just only install APKs from this repo's Releases.

**Optional, more private: use GitHub Secrets instead.** The workflow uses these
secrets automatically if they exist. Changing to a *different* key means
uninstalling the app once (this deletes saved workouts). To keep the *same*
key but stop depending on the file in the repo, add these four secrets:

1. On the phone browser open
   `https://github.com/amitEze/time-intervals/settings/secrets/actions`
   (repo → *Settings* → *Secrets and variables* → *Actions*; on the phone use
   the browser's "Desktop site" if the menu is hidden).
2. Tap **New repository secret** for each:

   | Name | Value |
   |---|---|
   | `SIGNING_KEYSTORE_BASE64` | the base64 text of a `.jks` file (see below) |
   | `SIGNING_STORE_PASSWORD` | the keystore password |
   | `SIGNING_KEY_ALIAS` | the key alias |
   | `SIGNING_KEY_PASSWORD` | the key password |

   For the current key: alias `intervaltimer`, both passwords `intervaltimer`,
   and the base64 text is `base64 -w0 keystore/release.jks` (ask Claude Code to
   print it for you, then paste it into the secret).

# Lumen

A fast, private home screen for Android TV and Google TV. No ads, no account, no tracking.

Built with Kotlin and Compose for TV. Minimum Android 8.0 (API 26).

## Features (v0.1)

- **Apps grid** with TV banners (icon fallback). Sort by your own order, A to Z, or most used. Hold OK on an app to add it to Favourites, move it, hide it, open App info or uninstall.
- **Favourites row** for apps and **link tiles**: pin a show, YouTube channel, playlist or web page by its link.
- **Continue watching** from the Android TV Watch Next list, with progress bars. Google ends this API in the second half of 2027, so treat it as best effort.
- **Inputs row** for HDMI and other physical inputs.
- **Quick settings panel** (Menu key or the header button): Wi-Fi, Bluetooth, picture, sound, sleep timer, input switching, all settings.
- **Home button takeover** on Google TV through an accessibility service. Short press Home opens Lumen; hold Home for the Google TV screen.
- **Strong mode** over ADB: make Lumen the system home and let it keep its Home service on after updates.
- **Recovery mode**: after 3 crashes within a minute, Lumen opens a safe screen instead of looping.
- **Backup and restore** of the whole layout to `Download/Lumen/lumen-backup.json`.
- Wallpaper and accent presets, 12 or 24-hour clock.

## Install

1. Download `app-release.apk` from the latest [GitHub Actions run](../../actions) or [release](../../releases).
2. Sideload it, for example with the Downloader app, `adb install`, or "Send files to TV".
3. Open Lumen and choose **Set up** on the banner at the top.

## Making Home open Lumen

| TV | What to do |
| --- | --- |
| Plain Android TV (Shield, Sony, Philips) | Settings › Home button setup › Choose default home screen › Lumen |
| Google TV (Chromecast, Google TV Streamer, TCL, Hisense) | Turn on **Lumen Home button** in Accessibility settings |
| Either, for the most reliable result | Run the strong-mode ADB commands below |

```sh
adb shell cmd package set-home-activity com.nikhil.lumen/.MainActivity
adb shell pm grant com.nikhil.lumen android.permission.WRITE_SECURE_SETTINGS
# Android 13+ sideloads, if the accessibility switch is greyed out:
adb shell appops set com.nikhil.lumen ACCESS_RESTRICTED_SETTINGS allow
```

Lumen never disables the Google TV launcher (`launcherx`). On some TVs that causes boot loops. Uninstalling Lumen always returns the TV to its stock home.

## Permissions

| Permission | Why |
| --- | --- |
| `READ_TV_LISTINGS`, `READ_EPG_DATA` | Continue watching row. Asked at runtime, optional. |
| `INTERNET` | Only to download poster images that apps publish for Continue watching. |
| `REQUEST_DELETE_PACKAGES` | Uninstall from the app menu. |
| `WRITE_SECURE_SETTINGS` | Only if you grant it over ADB. Keeps the Home service on. |
| Accessibility service | Reads Home key presses and which app window is in front. It does not read screen content. |

## Build

```sh
./gradlew assembleDebug        # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # JVM tests for ordering, backup format and crash guard
```

CI builds debug and release APKs on every push. Release builds are signed with the debug key so they install; use your own keystore before publishing to a store.

## Roadmap

- On-TV strong mode with Shizuku (no computer needed)
- Continue watching from Trakt, Jellyfin and Plex, ahead of the 2027 Watch Next shutdown
- Profiles and kids mode with app limits and bedtime
- Phone companion over the local network: wallpaper upload, settings, keyboard
- Screensaver, icon packs, remote key remapping

## Licence

MIT. See [LICENSE](LICENSE).

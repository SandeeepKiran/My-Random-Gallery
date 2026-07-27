# My Random Gallery

A native Android gallery for **random discovery** of your own photos and videos. Pick the folders you care about, shuffle through media, favourite what you love, run slideshows, and optionally play several videos at once.

Built with **Kotlin**, **Jetpack Compose**, and **Material 3**. Runs on **Android 11+** (API 30); compiled and targeted for **API 36** (Android 16).

| | |
|---|---|
| **Author** | Sandeep Kiran (Mousy) |
| **Package** | `com.mousy.myrandomgallery` |
| **Min SDK** | 30 (Android 11) |
| **Target / Compile SDK** | 36 (Android 16) |
| **UI** | Jetpack Compose + Material 3 |
| **License** | [MIT](LICENSE) |

> **Design source of truth:** the interactive wireframe in [`Design_Files/`](Design_Files/) defines screens, navigation, gestures, settings, and visual tokens. This app ports that spec to native Android.

---

## Table of contents

1. [Screenshots](#screenshots)
2. [Features](#features)
3. [Quick start (PowerShell)](#quick-start-powershell)
4. [Build from source](#build-from-source)
5. [First-run guide](#first-run-guide)
6. [Architecture](#architecture)
7. [Android 16 vs 11–15](#android-16-vs-1115)
8. [Permissions & privacy](#permissions--privacy)
9. [DEVICE-ONLY features](#device-only-features)
10. [Design files](#design-files)
11. [Project structure](#project-structure)
12. [Troubleshooting](#troubleshooting)
13. [Publishing to GitHub](#publishing-to-github)
14. [Contributing](#contributing)
15. [License & author](#license--author)

---

## Screenshots

| Gallery | Favourites | Slideshow | Settings |
|---------|------------|-----------|----------|
| _Add device screenshots here_ | _Add device screenshots here_ | _Add device screenshots here_ | _Add device screenshots here_ |

Tip: capture on a real phone after granting folder access — empty states are intentional until you pick sources.

---

## Features

### Tabs (bottom navigation)

Bottom nav is **icons only** (no text labels under tabs). Accessibility labels still come from each tab’s name.

| Tab | Description | Default |
|-----|-------------|---------|
| **Gallery** | Random shuffled grid of selected media | Always on; app opens here |
| **Favourites** | Favourited items with type + time filters | On |
| **Recent** | Recently added files (7–365 day windows) | On |
| **Slideshow** | Opens fullscreen viewer / autoplay | On |
| **Videos (Multi-Video)** | Play 1 / 2 / 4 videos or audio files at once | Off (enable in Settings) |
| **Albums** | Browse selected folders as albums | Off (enable in Settings) |
| **More** | Settings, appearance, storage tools | Always on |

Default order: Favourites → Recent → Gallery → Slideshow → More. Tab order and visibility are configurable in **More → Tabs & Layout** (Gallery and More cannot be hidden).

### Gestures (from wireframe)

| Gesture | Where | Action |
|---------|-------|--------|
| Double-tap | Grid tile | Toggle favourite |
| Long-press (~2.5s) | Grid tile | Enter multi-select |
| Swipe L/R or U/D | Gallery (swipe mode) | New random set (up/left) or history back (down/right) |
| Pinch | Grid | Change columns (1–6) |
| Tap | Fullscreen viewer / slideshow | Toggle chrome (top bar **and** bottom tabs) |
| Swipe L/R | Viewer | Previous / next item |
| Swipe up | Viewer photos | Delete after confirmation (if safety toggles allow) |

### Appearance

- Light / dark mode
- **AMOLED black** surfaces in dark mode
- Six accent palettes: **Rose**, **Lavender**, **Mint**, **Peach**, **Sky**, **Sand**
- Material You **dynamic color** on Android 12+ (blended with accent)

### Playback & safety

- Slideshow speeds: 1s → 5min, Custom, or Off (videos always play full duration)
- Don’t loop at end of list
- Disable swipe-up-to-delete, or disable every delete action app-wide
- Haptic feedback for confirmed actions and padded/edge-to-edge grid tiles
- Viewer media controls are separate from slideshow playback; video and audio autoplay when opened

### Storage & data

- Source folders via **SAF** (Storage Access Framework) + MediaStore discovery
- File-type filters detected from selected folders
- Optional favourites folder sync (copy on favourite / remove copy on unfavourite)
- Hidden folders dialog
- Export / import settings
- Reset all settings while keeping favourites, and share a captured crash log
- Download favourites as **`.zip`** (copies originals — never moves them)

---

## Quick start (PowerShell)

You must **rebuild after code changes**. Editing Kotlin/Compose/resources does nothing on the phone until you run `assembleDebug` again and reinstall (or re-run from Android Studio).

### Prerequisites

- JDK 17+ (Android Studio’s embedded JBR is fine)
- Android SDK with Platform **API 36**, Build-Tools, and **Platform-Tools** (`adb`)
- Phone or emulator on **Android 11+** with **USB debugging** enabled

### Build and install (from project root)

In **Windows PowerShell**:

```powershell
cd C:\Users\SandeepKiran\Videos\My_Random_Gallery
.\gradlew.bat assembleDebug
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

**PowerShell note:** use `.\gradlew.bat` (with `.\`). Bare `gradlew.bat` is rejected because the current directory is not on PowerShell’s command search path.

**APK output:**

```text
app\build\outputs\apk\debug\app-debug.apk
```

### Optional: confirm the device

```powershell
adb devices
```

You should see your phone listed as `device` (not `unauthorized`). If `adb` is not recognized, it lives under the Android SDK **platform-tools** folder, for example:

```text
%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
```

Add that folder to your PATH, or call `adb.exe` with the full path.

### After install

Open **My Random Gallery** → allow media access when prompted → go to **More** → **Add folder (SAF)** and pick at least one folder.

> Debug APKs use the Android debug keystore. For a signed release build, see [Release APK](#release-apk-signed).

### Launcher icon not updating?

Icon / adaptive-icon changes sometimes stick after a plain reinstall. Uninstall the old app from the phone, then install again (`adb install` without relying on a stale launcher cache), or use `adb uninstall com.mousy.myrandomgallery` before `adb install`.

---

## Build from source

### Prerequisites

| Tool | Notes |
|------|--------|
| **Android Studio** | Ladybug (2024.2+) or newer recommended |
| **JDK** | 17 or newer (21 works; project `jvmTarget` is 17) |
| **Android SDK** | Platform **API 36**, Build-Tools 36.x, Platform-Tools |
| **Device / emulator** | API **30+** (Android 11+) |

Create `local.properties` in the project root (gitignored):

```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

### Android Studio

1. **File → Open** the `My_Random_Gallery` folder
2. Wait for Gradle sync
3. Select a device / emulator (API 30+)
4. Run **app**

Studio rebuilds and installs for you. From the terminal, remember: **rebuild after every code change** before expecting the phone to show updates.

### Command line (PowerShell)

From the project root:

```powershell
.\gradlew.bat assembleDebug
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Clean rebuild:

```powershell
.\gradlew.bat clean assembleDebug
```

**macOS / Linux**

```bash
chmod +x gradlew
./gradlew assembleDebug
```

### Release APK (signed)

1. Create a keystore (once):

```powershell
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias myrandomgallery
```

2. Add signing config to `app/build.gradle.kts` (or use Android Studio **Build → Generate Signed Bundle / APK**). Prefer storing passwords in `keystore.properties` (gitignored) — **never commit keystores or passwords**.

3. Build:

```powershell
.\gradlew.bat assembleRelease
```

Output: `app\build\outputs\apk\release\app-release.apk`

### CI

GitHub Actions workflow: [`.github/workflows/android.yml`](.github/workflows/android.yml) runs `assembleDebug` on push/PR.

---

## First-run guide

1. **Grant media permission** when Android asks (full or limited — both work; SAF folders also work independently).
2. Open **More** (settings).
3. Tap **Add folder (SAF)** and select folders that contain photos/videos (e.g. `DCIM`, `Pictures`, `Download`).
4. Optionally enable **Multi-Video** / **Album** tabs under **Tabs & Layout**.
5. Return to **Gallery** — media appears in a shuffled grid.
6. **Double-tap** to favourite · **long-press** to multi-select · **pinch** to change density.

If the grid is empty: no folders selected, or no matching file types — adjust **Source Folders** and **File Types** in Settings.

---

## Architecture

```text
SettingsRepository (DataStore)
        │
        ▼
 GalleryViewModel  ←── MediaRepository (MediaStore + SAF)
        │                 FavouritesExporter / FavouritesFolderSync
        ▼
   GalleryUiState
        │
        ▼
 Compose UI (icon-only tabs, grids, viewer, dialogs)
```

| Layer | Responsibility |
|-------|----------------|
| **UI** | Compose screens matching wireframe layouts & Material 3 |
| **ViewModel** | Port of wireframe state machine (tabs, gestures, slideshow, multi-video) |
| **Preferences** | Persistent settings via DataStore |
| **Media** | DEVICE-ONLY scanning, playback, copy/zip |
| **Version gate** | API 30–36 branching for permissions & privacy APIs |

**Stack:** Compose · Material 3 · ViewModel · DataStore · Media3 ExoPlayer · Coil 3 · DocumentFile / SAF

---

## Android 16 vs 11–15

The app **targets Android 16 (API 36)** and uses newer privacy APIs when available. On older devices it switches to compatible paths.

| Concern | Android 11–12 (30–32) | Android 13–15 (33–35) | Android 16+ (36) |
|---------|----------------------|------------------------|------------------|
| Storage permission | `READ_EXTERNAL_STORAGE` | `READ_MEDIA_IMAGES` / `VIDEO` (+ audio on 13) | Granular media + `READ_MEDIA_VISUAL_USER_SELECTED` |
| Partial photo access | N/A | API 34+ partial / user-selected | Preferred privacy path + Photo Picker |
| Dynamic color | API 31+ | Yes | Yes |
| Predictive back | Legacy | API 34+ | Ready |
| Folder picking | SAF `OpenDocumentTree` | Same | Same |
| Video | Media3 ExoPlayer | Same | Same |

Implementation: [`AndroidVersionGate.kt`](app/src/main/java/com/mousy/myrandomgallery/util/AndroidVersionGate.kt) and [`MediaPermissions.kt`](app/src/main/java/com/mousy/myrandomgallery/data/media/MediaPermissions.kt).

You do **not** need Android 16 on your phone — **Android 11 is enough**. API 36 is the compile/target baseline so modern privacy features activate when present.

---

## Permissions & privacy

| Permission | Max / when | Why |
|------------|------------|-----|
| `READ_EXTERNAL_STORAGE` | maxSdk 32 | Legacy media read on Android 11–12 |
| `READ_MEDIA_IMAGES` | 33+ | Images via MediaStore |
| `READ_MEDIA_VIDEO` | 33+ | Videos via MediaStore |
| `READ_MEDIA_AUDIO` | 33+ path | Audio types if enabled in filters |
| `READ_MEDIA_VISUAL_USER_SELECTED` | 34+ | Limited / selected photo access |

- **No internet permission** — the app does not phone home.
- **No analytics / ads** in this codebase.
- Favourites and settings stay on-device unless **you** export them.
- Zip export **copies** favourites; it never moves or deletes originals for that feature.

---

## DEVICE-ONLY features

These need a real device (or emulator with storage) and are marked `DEVICE-ONLY` in source:

| Feature | APIs | Primary files |
|---------|------|----------------|
| Folder access | SAF `OpenDocumentTree`, persistable URI permissions, MediaStore | `MediaRepository.kt`, `GalleryApp.kt` |
| Video/audio playback | Media3 `ExoPlayer` | `FullscreenViewer.kt`, `MultiVideoScreen.kt` |
| Landscape multi-video | `ActivityInfo` orientation lock | `MainActivity.kt` |
| Pinch columns | `detectTransformGestures` | `MediaGrid.kt` |
| Favourites zip | `ZipOutputStream`, `FileProvider` | `FavouritesExporter.kt` |
| Favourites folder sync | SAF copy/delete | `FavouritesFolderSync.kt` |

Full details: [`docs/DEVICE_ONLY.md`](docs/DEVICE_ONLY.md).

---

## Design files

| Asset | Path |
|-------|------|
| Main interactive wireframe | [`Design_Files/My Random Gallery app Wireframe/My Random Gallery.dc.html`](Design_Files/My%20Random%20Gallery%20app%20Wireframe/My%20Random%20Gallery.dc.html) |
| Empty folders state | [`Design_Files/My Random Gallery app Wireframe/Empty.dc.html`](Design_Files/My%20Random%20Gallery%20app%20Wireframe/Empty.dc.html) |
| Bundled branding page | [`Design_Files/My Random Gallery.html`](Design_Files/My%20Random%20Gallery.html) |

When behavior or UI conflicts with code, **prefer the wireframe** unless a platform limitation is documented under DEVICE-ONLY.

---

## Project structure

```text
My_Random_Gallery/
├── Design_Files/                 # Interactive HTML wireframe (source of truth)
├── docs/
│   └── DEVICE_ONLY.md            # Device API notes
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mousy/myrandomgallery/
│       │   ├── MainActivity.kt
│       │   ├── data/             # models, DataStore, MediaStore/SAF
│       │   ├── viewmodel/        # GalleryViewModel
│       │   ├── ui/               # Compose screens & theme
│       │   └── util/             # AndroidVersionGate
│       └── res/
├── gradle/libs.versions.toml
├── .github/workflows/android.yml
├── README.md
└── LICENSE
```

---

## Troubleshooting

| Problem | What to try |
|---------|-------------|
| Empty Gallery | Add at least one SAF folder in **More**; check **File Types** |
| Permission denied | Re-open app → system settings → allow Photos/Videos; or rely on SAF folders |
| Videos won’t play | Confirm codec support; try another file; check Multi-Video cell has a selection |
| Gradle sync fails | Install SDK Platform 36 + Build-Tools; set `sdk.dir` in `local.properties` |
| `jdk` errors | Use JDK 17+; Android Studio’s embedded JBR is fine |
| `gradlew.bat` not found / not runnable | From project root use `.\gradlew.bat` in PowerShell |
| `adb` not found | Install Platform-Tools; add `%LOCALAPPDATA%\Android\Sdk\platform-tools` to PATH |
| Phone not listed | Enable USB debugging; run `adb devices`; accept the RSA prompt on the phone |
| Code changes missing on phone | Rebuild (`.\gradlew.bat assembleDebug`) then `adb install -r ...` |
| Launcher icon stale | Uninstall the app, then install again |
| Install blocked | Enable unknown sources / use `adb install` |
| Delete disabled | Check **Disable all delete options** in More → Playback & Safety |

---

## Publishing to GitHub

```bash
cd My_Random_Gallery
git init
git add .
git commit -m "Initial commit: My Random Gallery (Compose + Material 3)"
gh repo create My_Random_Gallery --public --source=. --remote=origin --push
```

**Do not commit:**

- `local.properties`
- `*.jks` / `*.keystore` / `keystore.properties`
- `app/build/` or `.gradle/`

These are already covered by [`.gitignore`](.gitignore).

### Tag a GitHub Release (debug APK)

CI already uploads `app-debug.apk` as an Actions artifact on every push. To also attach it to a **GitHub Release**, push a version tag:

```powershell
git tag v1.0.1
git push origin v1.0.1
```

That triggers [`.github/workflows/release.yml`](.github/workflows/release.yml), which builds the debug APK and attaches it to the release for tag `v*`.

Suggested repo topics: `android`, `kotlin`, `jetpack-compose`, `material3`, `gallery`, `mediastore`.

---

## Contributing

1. Fork the repository
2. Create a branch: `git checkout -b feature/my-change`
3. Match existing Kotlin/Compose style and wireframe behavior
4. Keep DEVICE-ONLY comments when touching storage, video, pinch, or zip/copy
5. On Windows PowerShell: `.\gradlew.bat assembleDebug` (rebuild after changes)
6. Open a PR with summary + test plan (device API level, folders used)

Issues and ideas welcome via GitHub Issues.

---

## License & author

**MIT** — see [LICENSE](LICENSE).

**Sandeep Kiran (Mousy)** — Made with ❤️ for random media discovery.

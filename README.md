# Elias Live TV

![Android Build](https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue?style=flat-square&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose)
![Media3](https://img.shields.io/badge/AndroidX%20Media3-ExoPlayer%201.5.1-orange?style=flat-square)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)

---

## 📱 Direct Android APK Download (মোবাইলে সরাসরি ডাউনলোড লিঙ্ক)

[![Download APK](https://img.shields.io/badge/Download_APK-Elias%20Live%20TV%20(v1.0.0)-00E5FF?style=for-the-badge&logo=android&logoColor=black)](Elias-Live-TV.apk?raw=true)

👉 **[📥 এখানে ক্লিক করে সরাসরি Elias-Live-TV.apk ডাউনলোড করুন (Direct Download Link)](Elias-Live-TV.apk?raw=true)**

> **মোবাইল ব্যবহারকারীদের জন্য নির্দেশনা:**
> 1. উপরের লিংকে চাপ দিলেই **`Elias-Live-TV.apk`** (মাত্র ২১ MB) ফাইলটি আপনার মোবাইলে ডাউনলোড হয়ে যাবে।
> 2. মোবাইলের Notification bar অথবা `Downloads` ফোল্ডার থেকে ফাইলটিতে ট্যাপ করে **Install** করুন।
> 3. প্রথমবারের জন্য *"Allow installation from unknown sources"* পারমিশন চাইলে চালু করে দিন।

---

**Elias Live TV** is a high-performance, modern native Android application for importing, organizing, and streaming M3U/M3U8 IPTV playlists. Built using Jetpack Compose, Material Design 3, Room Database, and AndroidX Media3 ExoPlayer, it delivers a smooth streaming experience with low latency, zero telemetry, and complete privacy.

> **Developer Credit:**  
> *This App Develop by Elias Hossain.*

---

## 🌟 Key Features

- **Robust M3U & M3U8 Playlist Parsing**:
  - Handles `#EXTM3U` and `#EXTINF` metadata directives.
  - Automatically extracts channel names, IDs (`tvg-id`), logos (`tvg-logo`), and categories (`group-title`).
  - Supports UTF-8 encoding with or without Byte Order Mark (BOM).
  - Tolerates varied line endings (`\r\n`, `\n`), interspersed comment directives (`#EXTGRP`, `#EXTVLCOPT`), and malformed entries.
  - Safe relative URL resolution against base URLs.
- **AndroidX Media3 ExoPlayer Engine**:
  - Native playback for HTTP Live Streaming (HLS `.m3u8`), progressive MP4, and direct HTTP/HTTPS streams.
  - Aspect ratio toggle (Fit, Zoom/Fill, Stretch).
  - Picture-in-picture friendly lifecycle management and audio focus handling.
  - Live broadcast indicator (`LIVE` badge), play/pause, prev/next channel controls, and channel drawer overlay.
  - Detailed, actionable playback error diagnostics and one-tap reconnection.
- **Channel Browsing & Discovery**:
  - Dual layout modes: Adaptive Grid View and compact List View.
  - Instant channel and group search.
  - Automatic category filtering with dynamic channel counts per group.
  - Alphabetical (A-Z / Z-A) and default playlist order sorting.
- **Favorites & Watch History**:
  - One-tap channel bookmarking (`Favorites` screen).
  - Automatic watch history tracking with relative timestamps for instant replay.
- **Playlist Management**:
  - Manage multiple saved playlists locally.
  - Switch active playlists or delete old ones with safety confirmation dialogs.
  - Storage Access Framework integration: imports local `.m3u` files without broad storage permissions.
  - Android Sharesheet support: open `.m3u` files directly from file managers and browser downloads.
- **Privacy & Security First**:
  - **100% Local Storage**: Playlists, channels, and watch history remain strictly on the user's device inside Room Database.
  - Zero external tracking, zero analytics, zero data harvesting.
  - Permissive yet scoped cleartext network support for external HTTP stream providers.
- **Included Demo Channels**:
  - Ready-to-play with curated, free, legal public broadcast channels (NASA TV HD, ISS Live, Bloomberg TV, DW English, France 24, Red Bull TV) for immediate playback verification.

---

## 📸 Screenshots

*(Place real device screenshots here when publishing to GitHub)*

| Home (Grid View) | Channels (List View) | Media3 Live Player | Settings & About |
|:---:|:---:|:---:|:---:|
| `[Screenshot Placeholder: Home]` | `[Screenshot Placeholder: List]` | `[Screenshot Placeholder: Player]` | `[Screenshot Placeholder: Settings]` |

---

## 📋 System Requirements

- **Target OS**: Android 7.0 (API Level 24) or higher
- **Target SDK**: Android 15 / 16 (API Level 36)
- **Compile SDK**: 36
- **Architecture**: ARM64-v8a, ARMeabi-v7a, x86_64
- **Build Tools**: Gradle 8+, JDK 17 or JDK 21

---

## 🛠️ Technology Stack

| Layer | Library / Tool |
|---|---|
| **Language** | Kotlin 2.2.10 |
| **UI Framework** | Jetpack Compose with Material 3 |
| **Media Engine** | AndroidX Media3 ExoPlayer (`1.5.1`) + HLS Extension |
| **Image Loading** | Coil Compose (`2.7.0`) |
| **Local Database** | Room Database (`2.7.0`) with KSP |
| **Architecture** | MVVM (Model-View-ViewModel) + StateFlow & Coroutines |
| **Build Automation** | Gradle Kotlin DSL (`build.gradle.kts`) |
| **CI/CD** | GitHub Actions (Automated Debug & Release APK generation) |

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/<YOUR_GITHUB_USERNAME>/elias-live-tv.git
cd elias-live-tv
```

*(Note: Replace `<YOUR_GITHUB_USERNAME>` with your GitHub username or organization).*

### 2. Open in Android Studio

1. Launch **Android Studio Ladybug (2024.2+)** or later.
2. Select **Open** and choose the cloned `elias-live-tv` root folder.
3. Allow Gradle to sync dependencies automatically.

### 3. Build the APK Locally

Using Gradle from the terminal:

```bash
# On Linux / macOS:
./gradlew assembleDebug

# On Windows:
gradlew.bat assembleDebug
```

The compiled APK will be available at:
`app/build/outputs/apk/debug/app-debug.apk`

### 4. Run Automated Tests

To execute JVM Unit and Robolectric tests:

```bash
./gradlew testDebugUnitTest
```

---

## 📥 How to Import an M3U Playlist

1. Launch **Elias Live TV** on your Android device.
2. On the main screen, tap **Import Playlist**.
3. Choose your preferred import method:
   - **Pick File**: Tap **Select File** to browse your device's downloads or file system using the native Android Storage Access Framework.
   - **Paste Text**: Paste raw M3U text directly into the text field.
   - **Open via File Manager**: Tap any `.m3u` or `.m3u8` file in your preferred file manager, and choose **Elias Live TV** from the "Open With" dialog.
   - **Load Demo Channels**: Tap **Load Free Public Channels** on the home screen to instantly test playback with authorized legal web streams.
4. The app parses the file in the background, categorizes channels, and displays the import summary with channel counts.

---

## 🤖 GitHub Actions CI/CD & Automated APK Downloads

This repository includes two GitHub Actions workflows:

### 1. Continuous Integration (`.github/workflows/android-build.yml`)
- Triggers on every push and pull request to `main` or `master`.
- Sets up JDK 17 with Gradle caching.
- Runs all unit and parser tests.
- Builds the debug APK and attaches it as a downloadable GitHub Actions artifact (`Elias-Live-TV-debug`).

### 2. Release & APK Distribution (`.github/workflows/android-release.yml`)
- Triggers automatically when a version tag (e.g., `v1.0.0`) is pushed, or manually via **workflow_dispatch**.
- Executes tests and builds the release APK.
- Generates a SHA-256 checksum for binary integrity verification.
- Automatically creates a GitHub Release and attaches:
  - `Elias-Live-TV-v1.0.0.apk`
  - `Elias-Live-TV-v1.0.0.apk.sha256`

---

## 🔑 Configuring Signed Release Builds (Optional)

To publish an APK signed with your production keystore, configure the following **Repository Secrets** in GitHub (**Settings > Secrets and variables > Actions**):

| Secret Name | Description |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded string of your `.jks` or `.keystore` file (`base64 -w 0 upload-key.jks`) |
| `ANDROID_KEYSTORE_PASSWORD` | Password for your keystore |
| `ANDROID_KEY_ALIAS` | Key alias name (e.g., `upload` or `mykey`) |
| `ANDROID_KEY_PASSWORD` | Password for the key alias |

*If these secrets are not configured, the workflow safely falls back to generating an installable build so your automated releases never break.*

---

## 🏷️ How to Trigger a Release (Step-by-Step)

To create a new release (e.g., `v1.0.0`):

```bash
git tag -a v1.0.0 -m "Release v1.0.0: Initial public release of Elias Live TV"
git push origin v1.0.0
```

Alternatively:
1. Navigate to your repository on GitHub.
2. Go to the **Actions** tab.
3. Select **Build & Publish Release APK**.
4. Click **Run workflow**, enter `v1.0.0`, and click **Run workflow**.
5. Once completed, download your APK from the repository's **Releases** page!

---

## 🛡️ Privacy & Security Statement

- **Zero Data Collection**: Elias Live TV does not collect, log, track, or share your personal data, IP address, browsing behavior, or imported playlist content.
- **Local Persistence**: All playlists, channel lists, favorites, and watch history are stored strictly on-device in a local SQLite/Room database.
- **Safe Network Permissions**: Requests only `INTERNET` and `ACCESS_NETWORK_STATE` to stream remote video channels and display channel logos.

---

## ⚖️ Legal Disclaimer

Elias Live TV is a media player application. It does not provide, host, bundle, distribute, or sell any playlists, subscriptions, television channels, or video streams. Users must provide their own playlists and must have legitimate authorization or subscription access to all streams they choose to play. The author and contributors are not responsible for any misuse of the application.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Developer Attribution

**This App Develop by Elias Hossain.**

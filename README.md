# s'Dart App 🎯

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin_Multiplatform-v2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-v1.7+-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: GPL v3](https://img.shields.io/badge/Code_License-GPLv3-blue.svg)](LICENSE)
[![Model Weights: CC BY-NC 4.0](https://img.shields.io/badge/Model_Weights-CC_BY--NC_4.0-orange.svg)](LICENSE-MODEL)
[![Platform](https://img.shields.io/badge/Platform-Desktop_%7C_Android_%7C_iOS-lightgrey.svg)](#-supported-platforms)

**s'Dart App** is a cross-platform dart scoring app with camera-based automatic scoring – and **everything runs locally on your own devices**.

No cloud, no account, no subscription, no tracking. Dart detection runs on-device, game data stays on your device, and companion devices talk to each other directly over your local Wi-Fi. The app works fully offline.

### 🔒 100% Local Processing

- **On-device AI:** Dartboard and dart detection run directly on your PC or smartphone (ONNX Runtime / NCNN). Camera images never leave your network.
- **No cloud services:** No servers, no user accounts, no analytics, no ads. The app makes no connections to the internet.
- **Local network only:** Companion devices, live scoreboards and tournament sharing communicate exclusively within your own Wi-Fi.
- **Your data stays yours:** Players, statistics and game history are stored locally on the device.

The app combines classic scorekeeping (X01, Cricket, tournament modes) with camera-based dartboard detection, automatic calibration and multi-device companion support. Built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**.

---

## 📸 Screenshots

### Desktop

| | |
|---|---|
| ![Overview](docs/screenshots/dashboard.png) | ![New game](docs/screenshots/games.png) |
| ![Players & statistics](docs/screenshots/players.png) | ![Tournament setup](docs/screenshots/tournament.png) |
| ![Connect devices](docs/screenshots/connect.png) | ![X01 game](docs/screenshots/game_x01.png) |
| ![Cricket](docs/screenshots/game_cricket.png) | ![All Fives](docs/screenshots/game_allfives.png) |
| ![Around the Clock](docs/screenshots/game_atc.png) | ![Game summary](docs/screenshots/game_summary.png) |

### Mobile

<p>
  <img src="docs/screenshots/m_dashboard.png" width="200" alt="Overview">
  <img src="docs/screenshots/m_games.png" width="200" alt="New game">
  <img src="docs/screenshots/m_tournament.png" width="200" alt="Tournament">
  <img src="docs/screenshots/m_players.png" width="200" alt="Players">
  <img src="docs/screenshots/m_player_detail.png" width="200" alt="Player details">
</p>

<p>
  <img src="docs/screenshots/m_connect.png" width="200" alt="Connect devices">
  <img src="docs/screenshots/m_game_x01.png" width="200" alt="X01 game">
  <img src="docs/screenshots/m_game_cricket.png" width="200" alt="Cricket">
  <img src="docs/screenshots/m_game_allfives.png" width="200" alt="All Fives">
  <img src="docs/screenshots/m_game_atc.png" width="200" alt="Around the Clock">
</p>

---

## ✨ Features

- **🎯 Diverse Dart Game Modes:**
  - **X01:** 301, 501, 701 and custom start scores – configurable with Double In / Double Out and multiple legs
  - **Cricket:** Standard & Cut-Throat modes; open numbers of the active player are highlighted on the dartboard
  - **Around the Clock:** 5 variants (Standard, Double, Triple, Quickly, Shanghai), optionally in reverse order (20 → 1) and with the bull as an extra target
  - **All Fives:** Round- and leg-based point game
  - **Tournament Engine:** Single elimination, double elimination, round robin and group stage + knockout, interactive tournament brackets, and an integrated local web server with QR code for sharing live tournament standings across the local network
  - **Statistics & History:** 180s/140s/100s tracking, First 9 Average, checkout percentage, best checkout, and graphical dart heatmaps

- **📷 Computer Vision & Camera Tracking:**
  - **On-Device Detection:** RTMDet model via ONNX Runtime on Desktop (CPU) and NCNN with Vulkan GPU acceleration on Android
  - **Fully Automatic Dartboard Calibration:** No manual setup – the model detects the spider wire intersections and the 20-segment marker, fits the board ellipse, determines the board rotation and computes the perspective homography from four control points (D20, D6, D3, D11)
  - **Platform-Independent Homography:** Pure Kotlin projective math for perspective correction without external OpenCV dependencies
  - **Distance Feedback:** Visual guidance for suboptimal camera distance (*"TOO CLOSE"* / *"TOO FAR"*)
  - **Camera Integration:** Live camera feed and calibrated segment overlay are embedded directly onto the dartboard surface (widescreen layout: dartboard on the left, scoreboard & stats on the right)

- **📱 Companion Mode (Multi-Device over Local Wi-Fi):**
  - **Device roles:** One device hosts the game; smartphones and tablets join as **Camera**, **Input** or **Display**
  - **Camera:** Smartphone detects darts on-device and sends hits plus a live preview to the host via WebSocket
  - **Input:** Manual score entry from a second device, including undo, a dedicated *Miss (0)* button and automatic player change after 3 darts
  - **Display (Live Scoreboard):** Any phone or tablet becomes a live scoreboard showing the active player, remaining score, checkout suggestion and darts of the current round
  - **Real-time sync:** Game state (throws, player changes, bust and winner status) is pushed instantly to all connected devices, including initial sync on join
  - **Energy saving:** Companion devices can dim their screen to the minimum hardware brightness while staying awake during long sessions
  - **Pairing:** Automatic mDNS/UDP discovery on the local network; trusted devices are remembered and can be revoked

- **🔊 British Mastercaller Voice & Audio Engine:**
  - Pre-generated voice caller announcements (24 kHz MP3, bundled offline) powered by **[Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M)** (`bm_lewis` British male voice, **Apache 2.0** licensed)
  - Supports individual dart calls, turn totals, bust and game shot calls, Cricket open/closed announcements, and dramatic `180` calls (`tools/generate_kokoro_audio.py`)

- **🎨 100% Compose Multiplatform:**
  - Shared UI, canvas rendering, scoring, and data models in `commonMain`
  - Consistent user experience across Desktop, Android, and iOS

---

## 🖥️ Supported Platforms

| Platform | Status | Highlights |
|---|---|---|
| **Desktop (Windows)** | ✅ Full | `.msi` installer or standalone `.jar`, companion camera via Wi-Fi & experimental webcam support |
| **Desktop (Linux / macOS)** | ✅ Full | `.deb` / `.dmg` packages or standalone `.jar` via JVM / Compose Multiplatform |
| **Android** | ✅ Full (debug build) | CameraX live stream with real-time canvas overlay; no Play Store release yet |
| **iOS** | 🔄 In Progress | Framework build via Kotlin/Native / AVFoundation integration |

### 📦 Which file do I need?

Prebuilt packages are published on the [Releases](https://github.com/marx861-stack/sDartApp/releases) page (once available):

| System | File | Notes |
|---|---|---|
| Windows | `sDartApp-<version>.msi` | Installer, includes its own Java runtime |
| Linux (Debian/Ubuntu) | `sdartapp_<version>…_amd64.deb` | Installer, includes its own Java runtime |
| macOS | `sDartApp-<version>.dmg` | Installer, includes its own Java runtime |
| Windows / Linux (portable) | `sDartApp-<os>-x64-<version>.jar` | No installation; requires Java 17+ (`java -jar …`) |
| Android | `sDartApp-android-<version>.apk` | Debug-signed APK for sideloading; allow installation from unknown sources |

---

## 🚀 Quick Start & Local Development

### Prerequisites

- **Java Development Kit (JDK):** Version 17 or 21
- **Android SDK (required for all builds, including Desktop):** The Android Gradle plugin is part of the shared module, so Gradle needs an SDK location even for Desktop builds. Install it via Android Studio (or the command-line tools) and either set the `ANDROID_HOME` environment variable or create a `local.properties` file in the project root:
  ```properties
  sdk.dir=/path/to/Android/Sdk
  ```
- **For Android builds additionally:** Android SDK Platform 35, **NDK `27.0.12077973`** and **CMake `3.22.1`** (install via Android Studio → *SDK Manager* → *SDK Tools*, or `sdkmanager "ndk;27.0.12077973" "cmake;3.22.1"`)
- **Android Studio** or **IntelliJ IDEA** (with the *Kotlin Multiplatform* plugin installed) – optional, but recommended

### Clone Repository

```bash
git clone https://github.com/marx861-stack/sDartApp.git
cd sDartApp
```

### Run Desktop App

On Windows:
```cmd
gradlew.bat :composeApp:run
```

On macOS / Linux:
```bash
./gradlew :composeApp:run
```

### Build Standalone Desktop Uber-JAR

```bash
./gradlew :composeApp:packageUberJarForCurrentOS
```
The packaged JAR file will be located in `dist/` or `composeApp/build/compose/jars/`.

### Build Desktop Installer

```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```
Creates an `.msi` (Windows), `.deb` (Linux) or `.dmg` (macOS) under `composeApp/build/compose/binaries/`. Installers can only be built on their respective operating system.

### Build Android App

```bash
./gradlew :composeApp:assembleDebug
```
The generated APK can be found under `composeApp/build/outputs/apk/debug/`.

> **Note:** There is currently no release signing configuration, so only debug-signed APKs are produced. These are fine for sideloading but cannot be published on Google Play.

---

## 🏗️ Project & Module Structure

The project follows a strict multiplatform architecture:

```text
sDartApp_KMP/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/        # 100% shared UI (Compose), state, models & game logic
│   │   │   ├── kotlin/
│   │   │   └── composeResources/
│   │   ├── desktopMain/       # Desktop-specific hardware/camera implementation
│   │   ├── androidMain/       # Android-specific CameraX implementation & MainActivity
│   │   └── iosMain/           # iOS entry points & framework bridge
├── docs/                      # Architecture, networking & interface concepts
├── gradle/
└── tools/                     # Scripts for release builds & installers
```

Detailed architecture and integration concepts can be found in the [`docs/`](docs/) directory:
- [`docs/kmp_migration_architecture.md`](docs/kmp_migration_architecture.md) – Architecture overview and multiplatform setup
- [`docs/tournament_architecture.md`](docs/tournament_architecture.md) – Tournament engine specification
- [`docs/companion_connection_concept.md`](docs/companion_connection_concept.md) – Companion devices, roles and pairing

---

## 🤝 Contributing

Contributions to s'Dart App are always welcome! Please check out our [Contributing Guidelines](CONTRIBUTING.md) before opening a pull request or proposing major changes.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/my-amazing-feature`)
3. Commit your changes (`git commit -m 'feat: Add amazing feature'`)
4. Push to the branch (`git push origin feature/my-amazing-feature`)
5. Open a Pull Request

---

## 📄 License

Copyright (C) 2026 marx861-stack

This project utilizes a clear separation of licenses for application code, machine learning weights, and audio assets:

- **Application & Source Code:** Licensed under the **[GNU General Public License v3.0 or later (GPL-3.0-or-later)](LICENSE)**.
- **Dartboard Detection Weights (`rtmdet_tiny.onnx`, `rtmdet_tiny.bin` & `rtmdet_tiny.param`):** Licensed under **[Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)](LICENSE-MODEL)**.
  - The weights are separate data files and are **not** covered by the GPL. The GPL applies to the source code only.
  - Prebuilt app packages (APK, JAR, MSI, DEB) bundle the weights and may therefore only be redistributed for non-commercial purposes.
  - ✅ **Free** for personal, hobby, educational, and non-commercial research use.
  - ❌ **Commercial use, redistribution in commercial software, or integration into commercial hardware systems is strictly prohibited** without prior written permission.
  - *Base Model Attribution:* Based on [RTMDet (OpenMMLab)](https://github.com/open-mmlab/mmdetection), licensed under Apache 2.0.
  - *Training Data Attribution:* Fine-tuned on public Roboflow Universe datasets: [Dart Pose v6](https://universe.roboflow.com/test-rgjac/dart-pose-v6), [Dart Scoring](https://universe.roboflow.com/dart-qhrs1/dart-scoring-5lanc), [Dart board detection](https://universe.roboflow.com/marcowich/dart-board-detection), [dart-counter](https://universe.roboflow.com/mitrevs-workspace/dart-counter-auuqi) (all CC BY 4.0) and [Dart](https://universe.roboflow.com/object-detection/dart-datnn) (MIT). See [LICENSE-MODEL](LICENSE-MODEL) for details.

- **Voice Caller Audio Assets:** Generated offline using [Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) (`bm_lewis`), licensed under the **Apache License 2.0**.

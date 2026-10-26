# s'Dart App 🎯

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin_Multiplatform-v2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-v1.7+-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: GPL v3](https://img.shields.io/badge/Code_License-GPLv3-blue.svg)](LICENSE)
[![Model Weights: CC BY-NC 4.0](https://img.shields.io/badge/Model_Weights-CC_BY--NC_4.0-orange.svg)](LICENSE-MODEL)
[![Platform](https://img.shields.io/badge/Platform-Desktop_%7C_Android_%7C_iOS-lightgrey.svg)](#-supported-platforms)

**s'Dart App** is a modern, cross-platform dart scoring and tracking application built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**.

The app combines classic scorekeeping (X01, Cricket, tournament modes) with camera-based dartboard detection, automatic calibration, and companion camera support.

---

## ✨ Features

- **🎯 Diverse Dart Game Modes:**
  - **X01:** 301, 501, 701, Custom – configurable with Single/Double/Master Out, Sets & Legs
  - **Cricket:** Standard & Cut-Throat modes
  - **Around the Clock:** 5 variants (Standard, Double, Triple, Quickly, Shanghai)
  - **All Fives:** Round- and leg-based point game
  - **Tournament Engine:** Knockout systems & group stages, interactive tournament brackets, and integrated local web server with QR code for sharing live tournament standings across the local network
  - **Statistics & History:** 180s/140s/100s tracking, First 9 Average, checkout percentage, and graphical dart heatmaps

- **📷 Computer Vision & Camera Tracking:**
  - **Hardware-Accelerated Detection:** ONNX Runtime with RTMDet (`rtmdet_tiny.onnx`) on Desktop and NCNN Vulkan on Android
  - **Automatic Dartboard Calibration:** Detects all segment intersections on the spider wire grid as well as the 20-point marker for automatic homography calculation
  - **Manual Adjustment:** Optional 4-point calibration (D20, D6, D3, D11) with sight brackets and crosshairs
  - **Platform-Independent Homography:** Pure Kotlin projective math for perspective correction without external OpenCV dependencies
  - **Distance Feedback:** Visual guidance for suboptimal camera distance (*"TOO CLOSE"* / *"TOO FAR"*)
  - **Camera Integration:** Live camera feed and sight overlay are embedded directly onto the dartboard surface (widescreen layout: dartboard on the left, scoreboard & stats on the right)

- **📱 Companion Mode (Companion Connection):**
  - Smartphone camera wirelessly streamed as an external video feed to the Desktop via WebSocket (primary workflow)
  - Automatic mDNS/UDP service discovery on the local Wi-Fi network

- **🔊 British Mastercaller Voice & Audio Engine:**
  - Pre-generated 24 kHz / 320 kbps voice caller announcements powered by **[Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M)** (`bm_lewis` British male voice, **Apache 2.0** licensed)
  - Supports individual dart calls, turn totals, Cricket status announcements, and dramatic `180` calls (`tools/generate_kokoro_audio.py`)

- **🎨 100% Compose Multiplatform:**
  - Shared UI, canvas rendering, scoring, and data models in `commonMain`
  - Consistent user experience across Desktop, Android, and iOS

---

## 🖥️ Supported Platforms

| Platform | Status | Highlights |
|---|---|---|
| **Desktop (Windows)** | ✅ Full | Standalone `.exe` installer, companion camera via Wi-Fi & experimental webcam support |
| **Desktop (Linux / macOS)** | ✅ Full | Native desktop execution via JVM / Compose Multiplatform |
| **Android** | ✅ Full | CameraX live stream with real-time canvas overlay |
| **iOS** | 🔄 In Progress | Framework build via Kotlin/Native / AVFoundation integration |

---

## 🚀 Quick Start & Local Development

### Prerequisites

- **Java Development Kit (JDK):** Version 17 or 21
- **Android Studio** or **IntelliJ IDEA** (with the *Kotlin Multiplatform* plugin installed)
- For Android development: Android SDK (API Level 24+)

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

### Build Android App

```bash
./gradlew :composeApp:assembleDebug
```
The generated APK can be found under `composeApp/build/outputs/apk/debug/`.

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
- [`docs/companion_connection_concept.md`](docs/companion_connection_concept.md) – Remote camera pairing

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

This project utilizes a clear separation of licenses for application code, machine learning weights, and audio assets:

- **Application & Source Code:** Licensed under the **[GNU General Public License v3.0 (GPL-3.0)](LICENSE)**.
- **Dartboard Detection Weights (`rtmdet_tiny.onnx` & `rtmdet_tiny.bin`):** Licensed under **[Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)](LICENSE-MODEL)**.
  - ✅ **Free** for personal, hobby, educational, and non-commercial research use.
  - ❌ **Commercial use, redistribution in commercial software, or integration into commercial hardware systems is strictly prohibited** without prior written permission.
  - *Base Model Attribution:* Based on [RTMDet (OpenMMLab)](https://github.com/open-mmlab/mmdetection), licensed under Apache 2.0.

- **Voice Caller Audio Assets:** Generated offline using [Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) (`bm_lewis`), licensed under the **Apache License 2.0**.

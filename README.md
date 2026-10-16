# s'Dart App 🎯

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin_Multiplatform-v2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-v1.7+-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Desktop_%7C_Android_%7C_iOS-lightgrey.svg)](#unterst%C3%BCtzte-plattformen)

**s'Dart App** ist eine moderne, plattformübergreifende Dart-Scoring- und Tracking-Anwendung, entwickelt mit **Kotlin Multiplatform (KMP)** und **Compose Multiplatform**.

Die App kombiniert klassisches Punktezählen (X01, Cricket, Turniermodi) mit kamerabasierter Scheibenerkennung, interaktiver Kalibrierung und Multicam-Split-Screen-Unterstützung.

---

## ✨ Features

- **🎯 Dart-Spielmodi:**
  - **X01** (301, 501, 701, Single/Double/Master Out, Sets & Legs)
  - **Cricket** (Standard, Cut-Throat)
  - **Around the Clock**, Trainingsmodi und Checkout-Vorschläge
  - **Turnier-Engine:** K.O.-Turniere und Gruppenphasen mit automatischem Spielplan

- **📷 Kamera-Tracking & Overlay:**
  - Visier-Brackets & Fadenkreuze zur präzisen Scheibenausrichtung
  - Automatische & interaktive 4-Punkt-Kalibrierung (Doppel-20, Doppel-6, Doppel-3, Doppel-11)
  - Live-Feedback bei falschem Abstand oder Winkel (*"ZU NAH"*, *"ZU WEIT WEG"*)
  - Desktop Dual-Kamera Split-Screen für simultane Überwachung aus zwei Winkeln

- **📱 Begleit-Modus (Companion Connection):**
  - Smartphone-Kamera als externer Video-Feed für den Desktop-Client
  - Automatische Kopplung im lokalen WLAN

- **🎨 Einheitliches UI:**
  - 100 % geteilter UI- und Zeichencode in `commonMain`
  - Konsistentes Look & Feel über Desktop, Android und iOS

---

## 🖥️ Unterstützte Plattformen

| Plattform | Status | Besonderheiten |
|---|---|---|
| **Desktop (Windows)** | ✅ Vollständig | Standalone `.exe` Installer, Webcam-Support (Webcam-Capture / Skia) |
| **Desktop (Linux / macOS)** | ✅ Vollständig | Native Desktop-Ausführung via JVM / Compose Multiplatform |
| **Android** | ✅ Vollständig | CameraX Live-Stream mit Echtzeit-Canvas-Overlay |
| **iOS** | 🔄 In Vorbereitung | Framework-Build via Kotlin Native / AVFoundation-Anbindung |

---

## 🚀 Schnellstart & Lokale Entwicklung

### Voraussetzungen

- **Java Development Kit (JDK):** Version 17 oder 21
- **Android Studio** oder **IntelliJ IDEA** (mit installiertem *Kotlin Multiplatform*-Plugin)
- Für Android-Entwicklung: Android SDK (API Level 24+)

### Repository klonen

```bash
git clone https://github.com/<DEIN-BENUTZERNAME>/sDartApp.git
cd sDartApp
```

### Desktop-App starten

Unter Windows:
```cmd
gradlew.bat :composeApp:run
```

Unter macOS / Linux:
```bash
./gradlew :composeApp:run
```

### Standalone Desktop Uber-JAR bauen

```bash
./gradlew :composeApp:packageUberJarForCurrentOS
```
Die fertige JAR-Datei liegt anschließend in `dist/` bzw. `composeApp/build/compose/jars/`.

### Android-App bauen

```bash
./gradlew :composeApp:assembleDebug
```
Die generierte APK befindet sich unter `composeApp/build/outputs/apk/debug/`.

---

## 🏗️ Projekt- & Modulstruktur

Das Projekt folgt einer strikten Multiplatform-Architektur:

```text
sDartApp_KMP/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/        # 100 % geteilte UI (Compose), State, Modelle & Spiel-Logik
│   │   │   ├── kotlin/
│   │   │   └── composeResources/
│   │   ├── desktopMain/       # Desktop-spezifische Hardware/Kamera-Implementierung
│   │   ├── androidMain/       # Android-spezifische CameraX-Implementierung & MainActivity
│   │   └── iosMain/           # iOS Entry-Points & Framework-Brücke
├── docs/                      # Architektur-, Netzwerk- & Schnittstellen-Konzepte
├── gradle/
└── tools/                     # Skripte für Release-Builds & Installer
```

Ausführliche Architektur- und Integrationskonzepte findest du im Verzeichnis [`docs/`](docs/):
- [`docs/kmp_migration_architecture.md`](docs/kmp_migration_architecture.md) – Architekturübersicht und Multiplatform-Aufbau
- [`docs/tournament_architecture.md`](docs/tournament_architecture.md) – Spezifikation der Turnier-Engine
- [`docs/companion_connection_concept.md`](docs/companion_connection_concept.md) – Remote-Kamera-Kopplung

---

## 🤝 Mitwirken (Contributing)

Beiträge zur App sind herzlich willkommen! Lies dir bitte unsere [Richtlinien für Mitwirkende](CONTRIBUTING.md) durch, bevor du einen Pull Request einreichst oder größere Änderungen planst.

1. Forke das Projekt
2. Erstelle einen Feature-Branch (`git checkout -b feature/mein-tolles-feature`)
3. Committe deine Änderungen (`git commit -m 'feat: Neues Feature hinzugefügt'`)
4. Pushe den Branch (`git push origin feature/mein-tolles-feature`)
5. Eröffne einen Pull Request

---

## 📄 Lizenz

Dieses Projekt ist unter der **GNU General Public License v3.0 (GPL-3.0)** lizenziert – siehe die Datei [LICENSE](LICENSE) für Details.

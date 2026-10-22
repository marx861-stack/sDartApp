# s'Dart App – Kotlin Multiplatform (KMP) Architektur & Migrationsleitfaden

## 1. Überblick & Ziele
Dieses Projekt ist die Multiplattform-Weiterentwicklung der **s'Dart App**.
Ziel ist es, die Spiellogik, Punktezählung, UI und Computer-Vision-Algorithmen über eine geteilte Codebasis für mehrere Plattformen bereitzustellen:
* **Android** (Handy / Tablet)
* **Desktop** (Windows Standalone via DirectX / Skia)
* **iOS** (iPhone / iPad via Metal / Skia)

Das Ursprungsprojekt verbleibt unverändert als Referenz unter `Original Android-App`.

---

## 2. Modul- und Ordnerstruktur

```text
s'Dart App_KMP/
├── composeApp/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/          # Geteilte Plattform-unabhängige Logik & Compose UI
│       │   └── kotlin/
│       │       └── com/example/myapplication/
│       │           ├── App.kt             # Gemeinsamer Einstiegspunkt
│       │           ├── game/              # Spiellogik (501, Cricket, ATC, etc.)
│       │           ├── score/             # ScoreCalculator, Checkout-Logik
│       │           ├── ui/                # Compose Multiplatform Screens & Themes
│       │           └── camera/            # expect-Deklarationen für Kamera-Frames
│       │
│       ├── androidMain/         # Android-spezifische Implementierungen
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/
│       │       ├── MainActivity.kt
│       │       └── camera/                # actual-Implementierung via CameraX
│       │
│       ├── desktopMain/         # Windows Desktop Implementierung
│       │   └── kotlin/
│       │       ├── Main.kt                # Windows Fenster-Einstieg
│       │       └── camera/                # actual-Implementierung via OpenCV Webcam
│       │
│       └── iosMain/             # iOS-spezifische Implementierungen
│           └── kotlin/
│               └── camera/                # actual-Implementierung via AVFoundation
│
├── docs/                        # Projektdokumentation
├── gradle/                      # Version Catalog (libs.versions.toml) & Wrapper
├── build.gradle.kts             # Root Build-Konfiguration
└── settings.gradle.kts          # Projekt- und Repository-Konfiguration
```

---

## 3. Build, Ausführung & Installationsdateien (`dist/`)

Alle fertigen Installations- und Release-Dateien werden zentral im gemeinsamen Stammverzeichnis **`dist/`** gesammelt:
* **Android:** Kompilieren und Installieren über `./gradlew :composeApp:assembleDebug`. Das fertige APK wird automatisch nach `dist/sDartApp-android-1.0.0.apk` kopiert.
* **Windows Desktop:** 
  - **Setup-Installer (.exe):** Erstellen via `tools/build_windows_setup.bat` bzw. `tools/build_windows_setup.ps1` (mittels Inno Setup). Das autarke Installationspaket landet in `dist/sDartApp-windows-x64-1.0.0-Setup.exe` (enthält eingebettete Java-Runtime, Startmenü- & Desktop-Verknüpfung und Uninstaller; dank `PrivilegesRequired=lowest` komplett **ohne Administratorrechte** installierbar; benötigt keinerlei Vorinstallation am Zielsystem).
  - **Standalone JAR:** Packen via `./gradlew :composeApp:packageUberJarForCurrentOS`. Das JAR wird automatisch nach `dist/sDartApp-windows-x64-1.0.0.jar` kopiert.
* **Linux Desktop:** Standalone JAR packen via `./gradlew :composeApp:packageLinuxJar`. Das JAR landet in `dist/sDartApp-linux-x64-1.0.0.jar`.
* **iOS:** Zielort für das App-Paket ist `dist/ios/sDartApp.ipa` bzw. `dist/ios/sDartApp.framework`. Die SwiftUI-App (`iosApp/iosApp/iOSApp.swift`), der Compose-Einstiegspunkt (`MainViewController.kt`) sowie Berechtigungen für Kamera & Bonjour (`Info.plist`) sind vollständig vorbereitet. Eine detaillierte Anleitung für Sideloading (Sideloadly / AltStore / TrollStore) und macOS-Builds liegt unter `dist/ios/README.md`.
* **Manueller Export:** `./gradlew :composeApp:copyDistributables` kopiert alle aktuellen Installationsdateien gebündelt nach `dist/`.
* **Google Drive Upload:** `./gradlew :composeApp:uploadToGoogleDrive` oder `tools/upload_to_drive.bat` synchronisiert alle Binaries (.apk, .jar, .exe) direkt in den Google Drive Ordner `Projekte/ Dart app` und aktualisiert vorhandene Dateien in-place.
* **App-Icons:** Das offizielle Logo (`dart_app_logo.png` / `icon.png`) wird für alle Plattformen einheitlich verwendet:
  - **Android:** Adaptive Icons (`ic_launcher.xml` mit Safe-Zone Inset & dunklem Hintergrund `#2B2B2A`) sowie automatisch gerenderte Mipmaps (`mdpi` bis `xxxhdpi`, eckig und rund).
  - **Desktop (Windows/Linux):** Multi-Resolution ICO (`icon.ico`), PNG für Linux und Compose-Fenster-Icon (`Window(icon = ...)`). Der Inno Setup Installer (`sDartApp-windows-x64-Setup.exe`) und die Starter-EXE nutzen das Icon nativ.
  - **iOS:** `Assets.xcassets/AppIcon.appiconset` mit 1024x1024 Universal-Asset.
  - **Generator-Skript:** `tools/generate_app_icons.py` aktualisiert bei Bedarf alle Plattform-Icons aus dem Master-PNG.

---

## 4. Geplante Migrationsschritte
1. [X] **Grundgerüst & Build-Pipeline:** KMP & Compose Multiplatform für Android und Desktop erfolgreich aufgesetzt und verifiziert.
2. [X] **Spiellogik & Theme überführen:**
   * `Game.kt` (Spielmodi 301/501/701, Cricket, All Fives, Around the Clock, Shanghai, Cut Throat) nach `commonMain` überführt.
   * `ScoreCalculator.kt` (Punkte- & Segment-Berechnung, Bullseye, Double/Triple) nach `commonMain` überführt.
   * `PlayerStatistics.kt` (Averages, Rundenberechnung, Highscores) nach `commonMain` überführt.
   * `ui/theme/` (Color.kt, Type.kt, Theme.kt - Glassmorphism & Dark Mode) nach `commonMain` überführt.
   * Plattformunabhängiger Zeitstempel (`PlatformUtils.kt` via `expect/actual`) implementiert.
   * Kompilierung für **Android** und **Desktop (Windows)** erfolgreich getestet.
3. [X] **UI-Komponenten & Navigation überführen:**
   * `DartViewModel.kt` (komplette Spielsteuerung, Scoring, Runden, Undo, Zustände) erfolgreich in `commonMain` eingebunden.
   * `NavigationManager.kt` (plattformunabhängige Screen-Navigation mit Stack) implementiert.
   * `StartScreen.kt` (Hauptmenü mit Spielmodi, Logos, Glassmorphism Cards) nach `commonMain` überführt.
   * `SettingsScreen.kt` (Sound, Vibration, Voice Caller, Entwickler-Modus & Modell-Dialog) nach `commonMain` überführt.
   * Multiplatform-Ressourcen (`composeResources/drawable`) für alle Vektor-Icons und Logos eingerichtet.
   * **Windows Standalone Desktop JAR** (`sDartApp-windows-x64-1.0.0.jar`) und **Android APK** erfolgreich gebaut.
4. [X] **Spieler-Setup, Management & Auswertung überführen:**
   * `PlayerSetupScreen.kt` (Spieler-Eingabe, Leg-Auswahl, Double-In/Out, All Fives Ziele, Cricket- und ATC-Regeln) nach `commonMain` überführt.
   * `PlayerManagementScreen.kt` und `PlayerDetailScreen.kt` (Verwaltung, Averages, 180s, Heatmaps) nach `commonMain` portiert.
   * `PlayerLoadDialog.kt` (Multi-Player-Auswahl) nach `commonMain` portiert.
   * `GameSummaryScreen.kt` (Match-Abschluss, Siegerehrung, Pokale) nach `commonMain` portiert.
   * UI-Komponenten (`ui/`): `SetupComponents.kt`, `AllFivesComponents.kt`, `ATCComponents.kt`, `ChartComponents.kt`, `StatisticsComponents.kt` vollständig im Shared Module aktiv.
   * Kompilierung für **Android** und **Windows Desktop** erfolgreich verifiziert.
5. [X] **GameScreen & Kamera-Abstraktion:**
   * `GameScreen.kt` (interaktives Dartboard mit Segment-Berechnung, Lupe/Magnifier mit Live-Targeting, Checkout-Pfad-Anzeige, Cricket-/AllFives-Score-Tabellen, Runden-Animationen, Spielabschluss-Dialoge und Spieler-Statistiken-Dialoge) vollständig nach `commonMain` portiert.
   * `expect/actual fun CameraBackground` als saubere Plattform-Abstraktionsschicht implementiert:
     - `commonMain`: `expect @Composable fun CameraBackground(...)`
     - `desktopMain`: Live-HUD mit Kalibrierungs-Visier, Eckklammern, Fadenkreuz und Inferenz-Status (für RTSP / USB-Webcam / Companion Streaming).
     - `androidMain`: Entkoppelte Composable-Integration (vorbereitet für CameraX & NCNN in Phase 6).
     - `iosMain`: Entkoppelte Composable-Integration (vorbereitet für AVFoundation & Metal in Phase 6).
   * `Screen.Game` in `App.kt` verdrahtet (inklusive Spielabbruch-Bestätigungsdialog).
   * Build-Status:
     - **Android Debug APK** (`composeApp-debug.apk`) baut fehlerfrei (`BUILD SUCCESSFUL`).
     - **Windows Standalone UberJar** (`sDartApp-windows-x64-1.0.0.jar`) baut fehlerfrei und öffnet nativ das Windows-Fenster.
6. [X] **Computer Vision Core & Android CameraX / NCNN Integration:**
   * **Phase 6.1 (Shared CV Core in `commonMain`):**
     - `ObjectDetection.kt`: Reine plattformunabhängige Datenklasse mit normalisierter BoundingBox (`Rect`), Konfidenz und Klassen-ID.
     - `AutoDetectionSystem.kt`: Geteiltes Stabilitäts- und Tracking-System für Dart-Spitzen und -Körper mit Jitter-Toleranz und Empty-Board-Erkennung.
     - `DartboardCalibrationSystem.kt`: 100% reine Kotlin-Implementierung für 2D-Homographie (Direct Linear Transformation / DLT-Gleichungslöser mit partieller Pivotisierung) – **vollständige Eliminierung der OpenCV-Abhängigkeit**, läuft nativ auf Desktop, Android und iOS.
   * **Phase 6.2 (Android CameraX & NCNN Vulkan Pipeline):**
     - NDK/CMake Native Build (`src/androidMain/cpp/` mit `ncnn_detector.cpp`, Prebuilt Vulkan Static Libs für `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`).
     - `NCNNDetector.kt`: JNI-Bridge zur nativen RTMDet-Inferenz.
     - `MLDartDetector.kt`: Android CameraX Pipeline mit 16:9 ResolutionSelector, Zero-Allocation Bitmap Copy, dynamischem ROI-Tracking und NMS.
     - `CameraBackground.android.kt`: Vollständige Composable-Implementierung mit `PreviewView`, interaktiven Eckklammern, Abstands-Visier, Live-Inferenz-HUD und direkter Punktverbuchung (`vm.enterScoreML`).
     - **Build-Status:** Vollständiges Android Debug-APK (`assembleDebug`) erfolgreich kompiliert und gelinkt.
7. [X] **Phase 6.3: Desktop Video & Live-Stream Integration:**
   * `DesktopMjpegClient.kt`: Asynchroner Coroutines-basierter HTTP-MJPEG-Stream Client für Windows Desktop mit Skia `Image.makeFromEncoded` und Compose `ImageBitmap` Dekodierung.
   * `CameraBackground.desktop.kt`: Vollständiges Desktop Camera-HUD mit Live-Stream Rendering, dynamischem Verbindungsdialog für Companion/IP-Streams (z.B. vom Smartphone oder Webcams), Zielvisier, Eckklammern und Debug-HUD.
   * **Build-Status:** Windows Desktop Jar (`desktopJar`) und Android (`compileDebugKotlinAndroid`) bauen fehlerfrei.
8. [X] **Phase 7: Persistenz (Spielerdaten & App-Einstellungen):**
   * **Entscheidung:** Option A (JSON-File Kompatibilität mit `saved_players_v2.json`) wurde gewählt, um 100%ige Datenkompatibilität mit der Original-Android-App ohne komplexe Schema-Migrationen zu garantieren.
   * `PlayerRepository.kt` (`commonMain`): `expect fun createPersistentPlayerRepository(): PlayerRepository`
   * **Implementierungen:**
     - **Desktop (JVM):** `DesktopJsonPlayerRepository` (`PlayerRepository.desktop.kt`) speichert in `%APPDATA%\sDartApp\saved_players_v2.json` (bzw. `~/.sDartApp`).
     - **Android:** `AndroidJsonPlayerRepository` (`PlayerRepository.android.kt`) speichert in `context.filesDir/saved_players_v2.json`.
     - **iOS:** Fallback-Implementierung (`PlayerRepository.ios.kt`) via `InMemoryPlayerRepository`.
   * **App-Einstellungen:** `AppPreferences.kt` persistiert Einstellungen, UI-Zustände sowie Kamerazoom (`cameraZoomLevel`, `cameraZoomRatio`) spiel- und sitzungsübergreifend über `expect/actual fun saveAppPreference` / `loadAppPreference`:
     - Desktop: Java Properties (`preferences.properties` im AppData-Verzeichnis).
     - Android: `SharedPreferences` (`sdart_app_prefs`).
     - iOS: `NSUserDefaults`.
   * **Kamera-Zoom Parität & Persistenz:**
     - Beim Umschalten zwischen Kamera und Board im `GameScreen` und spielübergreifend bleibt der Zoom erhalten.
     - In `MLDartDetector.kt` wird der gespeicherte Zoom-Faktor (`currentZoomRatio` / `currentZoom`) beim Reconnect direkt wiederhergestellt und initiale Standard-Meldungen (1.0x / 0.0) von CameraX gefiltert, sodass der gespeicherte Zustand nicht überschrieben wird.
     - Auf Desktop (`CameraVideoFeed.desktop.kt`) skaliert der Video-Feed via `graphicsLayer` digital synchron mit dem gespeicherten Zoom-Level.
   * **Verdrahtung:** Vollständig integriert in `App.kt`, `PlayerSetupScreen.kt`, `PlayerLoadDialog.kt` und `PlayerManagementScreen.kt`.
   * **Build-Status:** Verifiziert für Desktop (`desktopJar` BUILD SUCCESSFUL) und Android (`compileDebugKotlinAndroid` BUILD SUCCESSFUL).




9. [X] **Phase 8: Audio & Voice Caller Engine (Multiplattform):**
   * `AudioPlayer.kt` (`commonMain`): `interface AudioPlayer` mit `playSound(text, excitement)`, `vibrate()`, `release()` und `@Composable expect fun rememberAudioPlayer(): AudioPlayer`.
   * **Asset-Management:** Die 376 MP3-Audiodateien (~8,2 MB) und `manifest.json` werden ohne Daten-Duplikation geteilt:
     - Quelle verbleibt in `composeApp/src/androidMain/assets/audio/`.
     - Desktop bindet die Assets direkt über `desktopMain.resources.srcDirs("src/androidMain/assets")` in das JAR ein.
   * **Implementierungen:**
     - **Android (`AudioPlayer.android.kt`):** `AndroidAudioPlayer` nutzt nativen `SoundPool` mit AudioAttributes (Musik/Medien), 5 parallelen Audio-Streams, asynchronem Vorladen, Debounce-Schutz (150ms) und sequenziellen Ansagen (`...`-Delimiter). Haptisches Feedback via `Vibrator` / `VibratorManager` mit Waveform-Vibration.
     - **Desktop JVM (`AudioPlayer.desktop.kt`):** `DesktopAudioPlayer` nutzt `javazoom:jlayer:1.0.1` zur MP3-Dekodierung via Java Sound API (`Dispatchers.IO`), In-Memory Byte-Caching gegen Disk-I/O-Verzögerungen, Debounce (150ms) und sequenzielle Caller-Ansagen.
     - **iOS (`AudioPlayer.ios.kt`):** `IosAudioPlayer` als sauberes Stub-Interface (vorbereitet für spätere AVFoundation-Anbindung).
   * **Verdrahtung in `App.kt`:** `rememberAudioPlayer()` koppelt `vm.onPlaySound` und `vm.onVibrate` direkt an den Compose-Lifecycle.
   * **Build-Status:**
     - Desktop: Standalone UberJar (`packageUberJarForCurrentOS`) erfolgreich gebaut inklusive verifizierter Audio-Assets.
     - Android: Vollständiges Debug APK (`assembleDebug`) erfolgreich kompiliert und gelinkt.

10. [X] **Phase 9: Desktop UI & Adaptive Responsive Layouts:**
    * **Fundament:** AdaptiveLayout.kt mit Google Material 3 WindowSizeClasses (COMPACT, MEDIUM, EXPANDED) und isWidescreen-Erkennung in commonMain etabliert.
    * **Fenster-Setup:** Main.kt auf Standardgröße 1280x720 (16:9 HD) mit Mindestgröße 800x600 (window.minimumSize = Dimension(800, 600)) und zentrierter Platzierung aktualisiert.
    * **StartScreen Dashboard-Raster:**
      - *Desktop/Widescreen:* 2-Spalten-Raster mit harmonischer Maximalbreite (1000dp) und automatischer Zeilen-Höhenangleichung (IntrinsicSize.Max):
        - Zeile 1: *Standard (501)* & *Cricket*
        - Zeile 2: *Durch 5 teilbar (All Fives)* & *Around the Clock*
        - Zeile 3: *Spieler-Verwaltung* & *PC/Companion-Verbindung*
      - *Mobile:* 1-Spalten-Liste bleibt unverändert erhalten.
    * **PlayerSetupScreen 2-Spalten-Layout:**
      - *Desktop/Widescreen:* 2-Spalten Split mit zentrierter Maximalbreite (1100dp):
        - *Linke Spalte:* Spieler-Verwaltung in Card (Namensfelder, Löschen, Neu, Laden, Zufall).
        - *Rechte Spalte:* Spiel-Einstellungen in Card (Startscores, Double In/Out, Legs, Varianten) direkt über dem prominenten "SPIEL STARTEN"-Button.
      - *Mobile:* Vertikaler Fluss bleibt für Handys 100% erhalten.
    * **GameScreen Split-View & Scoreboard:** Vollständiges 2-Spalten-Layout bei Widescreen/Landscape (exakt 50/50 Aufteilung, je `weight(1f)` für Dartboard-Kachel und Scoreboard-Sidebar):
      - *In-Tile Zurück-Button:* Direkt in der Dartboard-Kachel oben links schwebend (`CircleShape`, 46dp, Glassmorphism-Border) integriert mit Bestätigungsdialog vor dem Verlassen.
      - *Monumentale Aktive Spieler-Kopfzeile (`DesktopCurrentPlayerCard`):*
        - Containerlos auf dem Hintergrund, ohne Kachel-Rahmen.
        - Links: Riesiger Spielername (50sp Black, ohne störenden Punkt), prominente Checkout-Empfehlung (18sp Bold) & 3-Dart-Average (16sp SemiBold).
        - Rechts: Monumentale Restpunkte (96sp Black Typography, letterSpacing -2sp) mit orangefarbenem `REST`-Label.
      - *Desktop Player Scoreboard Table (`DesktopPlayerScoreboardTable`):*
        - Containerlose tabellarische Übersicht aller Mitspieler untereinander mit 5 gleichmäßig verteilten Spalten (je `weight(1f)`):
          1. **SPIELER (1f):** Name (ohne Punkt).
          2. **DURCHSCHNITT (1f):** 3-Dart-Average (`Ø X Pkt.`) und Checkout zentriert.
          3. **LEGS (1f):** Gewonnene Legs zentriert (`$wonLegs`).
          4. **WÜRFE (1f):** Unboxed ohne Kacheln zentriert in reiner weißer Typografie (für aktiven Spieler: aktuelle Runde inkl. Live-Indikator `···`, für andere Spieler: letzte Runde) ohne Summenanzeige.
          5. **REST (1f):** Großer Restscore (bis 34sp Black) rechtsbündig in klarer weißer Typografie.
        - Aktiver Spieler hebt sich mit dezentem Orange-Glow und Border ab.
        - Klick auf einen Spieler öffnet weiterhin die detaillierten Spielerstatistiken.
      - *Mobile:* Behält die kompakte Original-Einzelkachel (`GameHeader`: 24sp Name, 36sp Score in 120x72dp Card, 8dp Padding, 12sp/16sp/10sp LazyRow) für maximale Spielfläche im Hochformat exakt unverändert bei.
    * **Build-Status:** Windows Standalone UberJar (sDartApp-windows-x64-1.0.0.jar) und Android Debug (compileDebugKotlinAndroid) fehlerfrei kompiliert und verifiziert.

11. [X] **Phase 10: Companion-Verbindung (Host & Rollen):**
    * Automatische Geräteerkennung ohne QR-Code; nur der Host ist (nach Opt-in) sichtbar, Teilnehmer wählen Kamera / Anzeige / Eingabe.
    * Vollständiges Konzept und Umsetzungsschritte: [companion_connection_concept.md](companion_connection_concept.md)
    * `CameraBackground` ist direkt angebunden an `SessionManager` und `ParticipantManager`.
    * Im Kamera-Modus wird der Live-Feed in `CompanionConnectionScreen.kt` gerendert.

12. [X] **Phase 11: Multiplatform Dart-Erkennung (Desktop ONNX Runtime, Companion Stream & Common Pipeline):**
    * **Gemeinsame Pipeline:** [DartDetectionPipeline.kt](../composeApp/src/commonMain/kotlin/com/example/myapplication/DartDetectionPipeline.kt) in `commonMain` für Bounding-Box-Dekodierung, Schwellenwerte, NMS und zeitliche Mehr-Frame-Stabilität.
    * **Desktop-Inferenz:** [OnnxDartDetector.kt](../composeApp/src/desktopMain/kotlin/com/example/myapplication/OnnxDartDetector.kt) nutzt Microsoft ONNX Runtime mit dem praxiserprobten Modell `rtmdet_tiny.onnx` aus `Dart Game Pro`.
    * **Hybrid-Architektur (Standalone vs. Companion-Stream):**
      - **Standalone-Betrieb (Handy oder PC alleine):** Lokale Inferenz (Android: NCNN Vulkan, Desktop: ONNX Runtime).
      - **Companion-Stream-Betrieb:** Wenn das Smartphone als Kamera mit dem Host gekoppelt ist, schaltet Android die lokale Inferenz ab (`inferenceEnabled = false`), komprimiert Frames als JPEG (~24 FPS) und sendet sie über WebSocket-Binärframes an den Host.
      - **Host-seitige Verarbeitung:** [CameraBackground.desktop.kt](../composeApp/src/desktopMain/kotlin/com/example/myapplication/CameraBackground.desktop.kt) empfängt die Preview-Frames (`CameraSourceType.COMPANION_STREAM`), decodiert sie via Skia und führt die ONNX-Erkennung sowie das Auto-Scoring zentral auf dem Host aus.
    * **Desktop-Kamerafeed:** Unterstützt USB-Webcam (`BufferedImage`), HTTP-MJPEG-Stream und Companion-WebSocket-Stream (`COMPANION PHONE (HOST ML)`), inklusive Bounding-Box-Overlay und Auto-Scoring via [DartboardCalibrationSystem.kt](../composeApp/src/commonMain/kotlin/com/example/myapplication/DartboardCalibrationSystem.kt) und [AutoDetectionSystem.kt](../composeApp/src/commonMain/kotlin/com/example/myapplication/AutoDetectionSystem.kt).
    * **Linux-Unterstützung (.deb & Standalone-JAR):**
      - `nativeDistributions.linux` mit App-Icon (`icon.png`), Menü-Kategorie `Game` und `debPackageVersion = "1.0.0"` konfiguriert; natives `.deb`-Packaging via `./gradlew packageDeb` auf Linux/Ubuntu.
      - **Nativer C++-Symbolkonflikt (Skiko vs. ONNX Runtime & LD_PRELOAD-Lösung):**
        * *Ursache:* Skiko (`libskiko-linux-x64.so`) bringt eine eigene C++-Runtime mit und exportiert globale Standard-C++-Symbole (`std::locale`, `std::ostringstream`). Da Skiko beim UI-Start vor ONNX Runtime geladen wird, griffen ONNX-C++-Aufrufe fälschlicherweise auf die Skiko-Symbole zu, was beim Erstellen der ONNX-Session (`std::ostream::_M_insert`) zu einem Speicherzugriffsfehler (`SIGSEGV 0xb`) führte.
        * *Lösung:* Durch Vorab-Laden der System-`libstdc++.so.6` via `LD_PRELOAD` wird sichergestellt, dass ONNX Runtime garantiert die sauberen C++-System-Symbole bindet. Bereitgestellt über das Linux-Startskript [`run_app.sh`](../run_app.sh) bzw. `~/.local/bin/sdartapp`:
          ```bash
          if [ -f "/usr/lib/x86_64-linux-gnu/libstdc++.so.6" ]; then
              export LD_PRELOAD="/usr/lib/x86_64-linux-gnu/libstdc++.so.6${LD_PRELOAD:+:$LD_PRELOAD}"
          fi
          ```
    * **Stabilitäts- & Streaming-Fixes:**
      - **Asynchrones Webcam-Beenden (Desktop):** `DesktopWebcamManager.stopWebcam()` blockiert den AWT-UI-Thread nicht mehr. Das Herunterfahren des Webcam-Treibers (`activeWebcam.close()`) und das Warten auf native `ioctl`-Leseaufrufe erfolgt geschützt über einen `Mutex` vollständig im Hintergrund (`Dispatchers.IO`), wodurch UI-Freezes und verwaiste Lock-Dateien (`/tmp/.webcam-lock-*`) vermieden werden.
      - **Thread-sichere ONNX-Session & Lifecycle:**
        * `OnnxDartDetector.detect()` und `close()` sind über ein dediziertes `sessionLock` synchronisiert. Ein atomarer `isClosed`-Flag verhindert neue Frame-Starts, während ein separater Daemon-Thread wartet, bis ein laufender `session.run()`-Aufruf beendet ist, bevor `session.close()` aufgerufen wird (verhindert SIGSEGV Use-After-Free).
        * `OrtEnvironment.getEnvironment()` (JVM-weites Singleton) wird beim Schließen des Detectors **nicht mehr geschlossen**, sodass erneutes Öffnen der Kamera in derselben Sitzung sofort wieder funktioniert.
      - Automatisches Umschalten und Blockieren von USB-Webcam-Hardwarezugriffen, wenn ein aktiver Companion-Stream vorliegt (verhindert Konflikte/Abstürze beim Öffnen im GameScreen).
      - Thread-sichere ONNX-Inferenz und Frame-Dropping via atomarem Statusindikator (`isInferencing`), um CPU-Überlastung durch auflaufende Inferenz-Tasks bei 24 FPS zu unterbinden.
      - Null-sichere Skia-/Bitmap-Dekodierung und globales Uncaught-Exception-Handling.
      - Host-Unterstützung für Companion-Streams auch unter Android (verhindert fälschliche lokale CameraX-Aktivierung auf dem Host).
    * **UI-Parität & Visier-Kalibrierung (Single Source of Truth):**
      - Beseitigung von Geometriefehlern bei Visier-Brackets (`pathBL` / `pathBR`) auf Desktop.
      - Aktivierung automatischer Kalibrierung (`autoCalibrateFromDetections`) und Distanz-Warnungen („ZU NAH / ZU WEIT WEG“) für Desktop-Kamerastreams.
      - Festhalten der KMP-Projektregel in `GEMINI.md`: Trennung von nativem Kamera-Hardwarefeed (`expect/actual`) und plattformunabhängigem UI-Overlay (`commonMain`), um UI-Divergenzen zwischen Android, Desktop und iOS dauerhaft auszuschließen.
    * **Build-Status:** Beide Targets (`compileKotlinDesktop` und `compileDebugKotlinAndroid`) bauen erfolgreich.

13. [X] **Phase 12: Vollständige Parität der Dart-Erkennung & des Auto-Scorings (Android & PC):**
    * **Entkopplung Feed vs. UI (`CameraVideoFeed` vs. `CameraBackground`):**
      - `CameraVideoFeed` als minimale `expect/actual`-Schnittstelle kapselt ausschließlich die rohe Bildbeschaffung (Android: CameraX, Desktop: Webcam/MJPEG/Companion, iOS: Stub).
      - `CameraBackground` liegt zu 100% in `commonMain`: Enthält sämtliche Visier-Brackets, Canvas-Zeichenroutinen, TextMeasurer-Abstandswarnungen, Box-Overlays sowie die Auto-Scoring-Pipeline.
    * **ML- und NMS-Gleichstand mit der Original-Android-App:**
      - Korrektur von `centerDistanceThreshold` auf `0.008f` (~5px statt 32px), wodurch eng gruppierte Treffer (z.B. T20/T20) nicht mehr fälschlicherweise als Duplikate gefiltert werden.
      - Korrektur von `maxDartboardDetections` auf `20` (vorher `1`), sodass alle Spider-Schnittpunkte durchgelassen werden.
      - Klassen 2 (Spider/Board) und 3 (20er-Marker) passieren die zeitliche Stabilitätsprüfung ohne Frame-Verzögerung für sofortige Kalibrierung.
    * **Aufwertung der Board-Kalibrierung (`autoCalibrateFromDetections`):**
      - Multi-Spider-Detektionen (>= 5 Punkte) nutzen Kovarianz-Ellipsen-Fitting, 20er-Marker-Winkel-Ausrichtung, 18°-Spider-Gitter-Optimierung und 4 Kontrollpunkte (D20, D6, D3, D11).
      - Temporäre Glättung (`smoothingFactor = 0.3f`) und Fallback auf Einzel-Bounding-Box.
    * **Zuverlässige Auto-Scoring-Schleife (Coroutines statt DrawScope):**
      - Punkteverbuchung (`findUnbookedDarts`, `enterScoreML`) und Statusaktualisierung (`updateGameState`, `resetTrackingIfNeeded`) laufen in einem `LaunchedEffect`-Block auf dem Main-Dispatcher statt im Canvas-Rendering-Pass.
      - Board-Empty-Erkennung (`checkBoardEmptyStatus`) fragt das Board per Coroutinen-Intervall ab, sodass nach Entfernen der Darts das 1000ms-Timeout auch bei leeren Detektionslisten zuverlässig erreicht wird und die Runde mit automatischen Fehlwürfen/Rundenwechsel abschließt.
    * **Build-Status:** Beide Targets (`compileKotlinDesktop` und `compileDebugKotlinAndroid`) bauen erfolgreich ohne Fehler.

14. [X] **Phase 13: Dauerhafte Hintergrund-Darterkennung im Host-Modus (Desktop):**
    * **Anforderung:** In der Desktop-Version soll im Host-Modus immer die automatische Darterkennung laufen, unabhängig davon, ob die Kameraansicht (`showCamera`) im Game-Screen aktiviert ist oder nicht.
    * **Entkopplung von UI-Darstellung und Erkennungs-Pipeline:**
      - In [`GameScreen.kt`](../composeApp/src/commonMain/kotlin/com/example/myapplication/GameScreen.kt) wird [`CameraBackground`](../composeApp/src/commonMain/kotlin/com/example/myapplication/CameraBackground.kt) im Desktop-Split-Screen aktiv gehalten, sobald `showCamera || isHostActive` zutrifft (`isHostActive = hostState is HostState.Active`).
      - Ist `showCamera == false`, wird an `CameraBackground` `showOverlay = false` und `showCamera = false` übergeben. Dadurch wird weder der Video-Feed noch das Visier über das digitale Board gezeichnet.
      - [`ScoreInputSection`](../composeApp/src/commonMain/kotlin/com/example/myapplication/GameScreen.kt) liegt über dem Hintergrund und bleibt für manuelle Eingaben vollständig sichtbar und bedienbar.
    * **Capture- und Inferenz-Loop (`CameraVideoFeed.desktop.kt`):**
      - In [`CameraVideoFeed.desktop.kt`](../composeApp/src/desktopMain/kotlin/com/example/myapplication/CameraVideoFeed.desktop.kt) steuert die Bedingung `val shouldCapture = showCamera || isHostActive` sowohl den Start der USB-Webcam / MJPEG-Stream als auch die Coroutinen-Inferenzschleife.
      - Eingehende Frames (USB-Webcam, MJPEG-Stream oder Companion-Handy-Stream) werden kontinuierlich mit ONNX ausgewertet und an die Auto-Scoring-Pipeline in `commonMain` übergeben.
      - Status-Banner und Debug-HUD werden nur gerendert, wenn die Kamera-Ansicht explizit eingeschaltet ist (`if (showCamera)`), um das digitale Dartboard im normalen Modus nicht zu überlagern.
    * **Build & Distribution:** Desktop-UberJar (`sDartApp-windows-x64-1.0.0.jar`) und Android APK erfolgreich kompiliert und im Verzeichnis `dist/` bereitgestellt.

15. [X] **Phase 14: Mobile System-Zurücktasten & Gesten-Navigation:**
    * **Anforderung:** Auf mobilen Geräten sollen die klassischen Zurücktasten (Hardware/Software 3-Tasten-Navigation) sowie Wischgesten vom Bildschirmrand (Gesten-Navigation) genutzt werden, um innerhalb der App intuitiv zurückzugehen.
    * **Plattformunabhängige Abstraktion (`PlatformBackHandler` & `edgeSwipeBack`):**
      - `PlatformBackHandler`: `expect/actual`-Schnittstelle in `commonMain` eingebunden in `App.kt`.
        * **Android (`PlatformBackHandler.android.kt`):** Nutzt `androidx.activity.compose.BackHandler(enabled = canGoBack, onBack = handleBack)`, wodurch sowohl die Dreiecks-Zurücktaste (3-Tasten-Navigation) als auch die systemweiten Wischgesten vom Bildschirmrand (Gesten-Navigation inkl. Predictive Back) automatisch und nativ abgefangen werden.
        * **Desktop (`PlatformBackHandler.desktop.kt`):** No-op (Desktop nutzt On-Screen-Schaltflächen).
        * **iOS (`PlatformBackHandler.ios.kt`):** No-op auf Systemebene; Gesten werden via Compose abgedeckt.
      - `Modifier.edgeSwipeBack` in `commonMain`: Fängt Wischgesten (Touch) vom linken Bildschirmrand (innerhalb 28dp) nach rechts (> 60dp) ab, um plattformübergreifend auf mobilen Touchscreens (iOS & Android) flüssige Zurück-Gesten zu gewährleisten. Filtert Desktop-Mausinteraktionen zuverlässig heraus (`PointerType.Touch`).
    * **Intelligente Navigations- & Abbruch-Logik (`NavigationManager`):**
      - `canGoBack()`: Liefert `false` ausschließlich auf dem Startbildschirm ohne aktive Dialoge, sodass das System die App ordnungsgemäß minimiert/beendet. Liefert `true` auf allen Unterbildschirmen (`PlayerSetup`, `Settings`, `PlayerManagement`, `PlayerDetail`, `Game`, `GameSummary`, `PcIntegration`) oder bei geöffneten Dialogen.
      - `goBack()`:
        * Ist der Spielabbruch-Dialog aktiv (`showGameExitDialog`), schließt ein Zurück-Event den Dialog und hält das Spiel aktiv.
        * Befindet sich der Nutzer im aktiven Spiel (`Screen.Game`), öffnet das Zurück-Event sicherheitsabfragend den Abbruch-Dialog (`showGameExitDialog = true`), um versehentlichen Spielverlust zu verhindern.
        * Befindet sich der Nutzer in der Spielzusammenfassung (`Screen.GameSummary`), führt das Zurück-Event sauber zum Hauptmenü (`Screen.Start`).
        * Auf allen weiteren Screens wird der vorherige Bildschirm aus dem `navigationStack` wiederhergestellt.
      - Global geöffnete Dialoge wie die Host-Übersicht (`showHostOverviewFromStart`) werden vorrangig geschlossen.
    * **Build- & Test-Status:** `allTests`, `compileDebugKotlinAndroid` und `compileKotlinDesktop` erfolgreich verifiziert.


16. [X] **Phase 15: Kachel-Restrukturierung (Games, Turnier, Spieler, Geräte verbinden):**
    * **Hauptmenü (`StartScreen.kt`):**
      - Gliederung des Startbildschirms in vier zentrale Hauptkacheln:
        1. **Games:** Verzweigt zur neuen Spielmodus-Auswahl (`GameModesScreen`).
        2. **Turnier:** K.O.-System & Liga bzw. laufendes Turnier fortsetzen.
        3. **Spieler:** Spieler-Verwaltung & Statistiken.
        4. **Geräte verbinden:** Companion-Verbindungen & Netzwerk-Rollen.
      - Perfekte 2x2-Grid-Symmetrie auf Widescreen/Desktop und übersichtliche 4-Kachel-Liste auf mobilen Endgeräten.
    * **Spielmodus-Auswahl (`GameModesScreen.kt` & `Screen.GameModes`):**
      - Eigener Bildschirm mit einheitlichem `ScreenHeader(title = "Games", onBack = ...)` und den 4 Spielmodi:
        * **Standard (501)**
        * **Cricket**
        * **Durch 5 teilbar (All Fives)**
        * **Around the Clock**
      - Nahtlose Rückkehr zum Hauptmenü via Back-Button / System-Gesten und Weiterleitung zu `PlayerSetup` bei Auswahl.
    * **Build- & Test-Status:** `compileKotlinDesktop` und `compileDebugKotlinAndroid` erfolgreich verifiziert.

17. [X] **Phase 16: Internationalisierung & Compose Multiplatform String-Ressourcen (i18n):**
    * **Ressourcenstruktur:**
      - `composeResources/values/strings.xml` als Standard-Sprache (Englisch).
      - `composeResources/values-de/strings.xml` als lokalisierte deutsche Fassung.
    * **Integration in Menüs:**
      - Vollständige Umstellung von `StartScreen.kt` und `GameModesScreen.kt` auf `stringResource(Res.string.<id>)`.
      - Automatische Sprachanpassung basierend auf der Betriebssystem-Sprache (Android, Windows Desktop, iOS).
    * **Build-Status:** `compileKotlinDesktop` und `compileDebugKotlinAndroid` erfolgreich verifiziert.

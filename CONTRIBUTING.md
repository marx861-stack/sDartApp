# Richtlinien für Mitwirkende (Contributing Guidelines)

Vielen Dank für dein Interesse, zur **s'Dart App** beizutragen! Open Source lebt vom Mitmachen – sei es durch Fehlerberichte, Verbesserungsvorschläge oder eigene Code-Beiträge (Pull Requests).

---

## 1. Fehler melden (Bug Reports)

Bevor du einen Fehler meldest:
1. Prüfe bitte in den bestehenden [GitHub Issues](https://github.com), ob das Problem bereits gemeldet wurde.
2. Beschreibe das Verhalten möglichst detailliert:
   - **Plattform & Betriebssystem:** (z. B. Windows 11, macOS Sonoma, Android 14)
   - **Kamera-Hardware / Auflösung:** (z. B. USB-Webcam 1080p, Smartphone-Kamera)
   - **Schritte zur Reproduktion:** Was hast du genau getan, als der Fehler auftrat?
   - **Erwartetes vs. tatsächliches Verhalten**
   - Falls möglich: Screenshots oder Konsolen-Logs anhängen.

---

## 2. Architekturgrundsätze für Code-Beiträge

Damit die App über alle Zielplattformen (Desktop, Android, iOS) hinweg stabil, wartbar und einheitlich bleibt, befolge bitte strikt diese Richtlinien:

### A. Single Source of Truth in `commonMain`
- **Sämtliche UI-Komponenten, Compose Canvas-Routinen, Overlays, Visier-Brackets, Textausgaben und Auswertungen gehören in `commonMain`.**
- Es darf keine doppelte UI-Implementierung in plattformspezifischen Modulen geben.

### B. Minimale `expect/actual`-Schnittstellen
- `expect`/`actual` darf **ausschließlich** für unvermeidbare native Hardware- und System-APIs verwendet werden (z. B. CameraX auf Android, WebcamCapture/Skia auf Desktop, AVFoundation auf iOS).
- Native Feeds beschränken sich rein auf die Bereitstellung des rohen Kamera-Frames. Sämtliche Overlays und Berechnungen liegen in `commonMain`.

### C. Keine festen Bildschirm- oder Kameraauflösungen
- Berechnungen und Normalisierungen von Bounding-Boxes oder Kalibrierungspunkten dürfen keine festen Auflösungen (wie 1280x720) hardcoden, sondern müssen dynamisch die tatsächlichen Frame-Dimensionen nutzen.

### D. Dokumentation & Code-Qualität
- Halte Code sauber, gut lesbar und verzichte auf unnötige Verschachtelungen oder unlesbare One-Liner.
- Wenn neue Konzepte oder Schnittstellen hinzukommen, aktualisiere die Dokumentation im Ordner `docs/`.

---

## 3. Pull-Request-Workflow

1. **Branch anlegen:** Erstelle einen beschreibenden Branch-Namen (z. B. `fix/camera-aspect-ratio` oder `feat/cricket-cut-throat`).
2. **Kompilierbarkeit prüfen:** Stelle sicher, dass das Projekt fehlerfrei baut:
   - Desktop: `./gradlew :composeApp:compileKotlinDesktop`
   - Android: `./gradlew :composeApp:compileDebugKotlin`
3. **Commit-Nachrichten:** Schreibe klare und prägnante Commit-Nachrichten (vorzugsweise nach Conventional Commits wie `fix: ...`, `feat: ...`, `docs: ...`).
4. **Keine Secrets oder lokalen Pfade:** Achte darauf, dass keine privaten Daten oder Pfade committet werden.
5. **Pull Request öffnen:** Beschreibe kurz, was geändert wurde und wie man die Änderung am besten testen kann.

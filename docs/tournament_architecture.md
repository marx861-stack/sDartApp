# Turnier-Architektur (Paket 1): s'Dart App (KMP)

Dieses Dokument beschreibt die Architektur, Datenmodelle, Algorithmen und die Integration der **Turnier-Funktion** in der Kotlin Multiplatform (KMP) s'Dart App.

---

## 1. Übersicht & KMP-Architekturprinzipien

Gemäß den KMP-Projektregeln liegt die gesamte Turnier-Logik, Datenhaltung und UI zu **100 % in `commonMain`**:
- **Keine nativen Plattform-Abhängigkeiten:** Läuft identisch auf Desktop (JVM), Android und iOS.
- **Single Source of Truth:** `TournamentManager` verwaltet den Zustand und synchronisiert ihn mit `AppPreferences`.
- **Match-Lifecycle:** Startet echte KMP-Spiele (`GameScreen`), fängt Spielabbrüche ab und übernimmt nach Spielende automatisch Sieger, Legs und Averages.

```
composeApp/src/commonMain/kotlin/com/example/myapplication/
└── tournament/
    ├── TournamentModels.kt        // Datenklassen, Enums, Serializable State
    ├── TournamentScheduler.kt     // KO-Baum & Round-Robin Algorithmen
    ├── TournamentManager.kt       // State-Management, Tie-Breaker, Persistenz
    └── ui/
        ├── TournamentSetupScreen.kt    // Konfiguration, Regeln & Spielerauswahl
        ├── TournamentViewScreen.kt     // Hauptschirm & Routing
        ├── TournamentBracketView.kt    // Widescreen-Turnierbaum & Mobile-Tabs
        ├── TournamentLeagueView.kt     // Liga-Tabelle & Begegnungen
        └── TournamentCelebration.kt    // Siegerehrung & Champion-Banner
```

---

## 2. Datenmodelle (`TournamentModels.kt`)

Alle Kernmodelle sind mit `kotlinx.serialization` annotiert und damit vollständig serialisierbar:

### `TournamentPlayer`
- `id: String`: Eindeutige ID (verhindert Namenskollisionen bei gleichnamigen Spielern).
- `name: String`: Anzeigename.
- `savedPlayerName: String?`: Optionaler Verweis auf ein gespeichertes Profil im `PlayerRepository`.

### `TournamentMatch`
- `id: String`: Eindeutige Match-ID (z. B. `tourn_123_r1_m0`).
- `round: Int`: Rundennummer (1 = Runde 1 / Viertelfinale, etc.).
- `matchIndexInRound: Int`: Position innerhalb der Runde.
- `player1: TournamentPlayer?` & `player2: TournamentPlayer?`: Zugeordnete Teilnehmer.
- `winner: TournamentPlayer?`: Match-Gewinner.
- `isFinished: Boolean`: Match abgeschlossen.
- `isBye: Boolean`: Automatisches Freilos.
- `nextMatchId: String?` & `nextMatchSlot: Int`: Feste Slot-Zuordnung (1 oder 2) im Folge-Match.
- `player1Result` & `player2Result`: Einzelergebnisse (`legsWon`, `average`, `dartsThrown`, `isForfeit`).

### `TournamentState`
- `id: String`, `name: String`, `type: TournamentType`, `config: TournamentGameConfig`, `players: List<TournamentPlayer>`, `matches: List<TournamentMatch>`, `isFinished: Boolean`, `winner: TournamentPlayer?`.

---

## 3. Algorithmen (`TournamentScheduler.kt`)

### Single Elimination (K.-o.-System)
- **Rundenzahl & Slots:** $R = \lceil \log_2(N) \rceil$, Slots $S = 2^R$.
- **Baum-Konstruktion:** Rekursiv rückwärts vom Finale zu Runde 1 mit festen `nextMatchSlot` (1 für oberen Ast, 2 für unteren Ast).
- **Faires Freilos-Seeding (Byes):**
  - Verwendet Standard-Turnier-Seeding ($S=8 \rightarrow [1, 8, 4, 5, 2, 7, 3, 6]$).
  - Freilose treffen **niemals** aufeinander.
  - Matches mit Freilos werden direkt bei Erstellung als `isFinished = true, isBye = true` gewertet und der reale Teilnehmer rückt sofort in Runde 2 vor.

### Round Robin (Jeder gegen Jeden)
- Verwendet die klassische **Circle-Methode**:
  - Bei ungerader Teilnehmerzahl wird ein temporärer Bye-Platzhalter ergänzt.
  - Fixiert das erste Element; die restlichen rotieren im Uhrzeigersinn.
  - Garantiert, dass jeder Teilnehmer genau einmal gegen jeden anderen spielt.

---

## 4. Tie-Breaker in der Liga (`TournamentManager.kt`)

Bei Punktgleichheit im Jeder-gegen-Jeden-Modus greift folgende Standard-Priorisierung:
1. **Anzahl Siege:** Meiste gewonnene Matches.
2. **Direkter Vergleich (Head-to-Head):** Wer hat das direkte Duell gewonnen?
3. **Leg-Differenz:** `gewonnene Legs - verlorene Legs`.
4. **3-Dart-Average:** Höchster Gesamtdurchschnitt aller gespielten Legs.

---

## 5. UI & Responsive Design (Apple-Sports & Clean Glass Ästhetik)

Das UI des Turnier-Bereichs folgt den Gestaltungsprinzipien von **Apple Sports, iOS und macOS**:
- **Translucent Materials & Hairlines:** Sanfte, translucente Oberflächen (`Color(0x0CFFFFFF)`) mit hauchdünnen `0.5dp` bis `1dp` Haarlinien (`Color(0x1FFFFFFF)`). Verzicht auf klobige Kacheln oder grellen Glow.
- **Typografie-first Hierarchie:** Ruhige Schriftgewichte (Medium, SemiBold), keine durchgehenden All-Caps. Ausreichend Weiß- bzw. Dark-Space für optimale Lesbarkeit.

### Widescreen / Desktop
- **Turnierbaum mit echten 1dp-Verbindungslinien:**
  - Jede Vorrunden-Paarung ist über eine präzise gezeichnete Canvas-Vektorlinie (`1dp`, `Color(0x26FFFFFF)`) mit dem Folge-Match verbunden.
  - Sobald ein Match beendet ist, färbt sich der Pfad des Siegers leuchtend in `PrimaryOrange`.
- **Zweizeilige Match-Karten (Apple Sports Look):**
  - Schlanke Kachel mit 14dp Eckenradius und `0.5dp` Trennlinie zwischen den Spielern.
  - Initialen-Badges (`•`, Initialen), Sieger in SemiBold Weiß, Verlierer dezent abgedunkelt.
  - Dedizierte Score-Kapseln (`[ 3 ] : [ 1 ]`).
  - Dezenter Status-Indikator: Feiner, pulsierender Orange-Dot für „Bereit“ statt lauter Banner.
- **Setup-Screen:**
  - Schnellauswahl für Standardgrößen (`4`, `8`, `16` Spieler).
  - Übersichtliche Spielerauswahl mit nummerierten Seed-Badges (`#1`, `#2` ...).
  - 2-Spalten-Aufteilung mit `AdaptiveContainer`.

### Mobile / Smartphones
- **Cupertino Segmented Control:**
  - Kapsel-Slider für Runden (`[ Runde 1 ] [ Halbfinale ] [ Finale ]`) mit weichem Inset-Hintergrund und Aktivitäts-Dots.
  - Optionaler Umschalter auf den vollständigen, horizontal scrollbaren Turnierbaum.

### Liga-Modus (Round Robin)
- **Apple Inset-Grouped Standings:**
  - Feine Tabelle mit dezenten Podium-Badges für Rang 1 (Gold/Orange-Ring 🥇), Rang 2 (Silber-Ring 🥈), Rang 3 (Bronze-Ring 🥉).
  - Formatierte Leg-Differenzen (`+4` / `-2`) und 3-Dart-Averages.
  - Wiederverwendung der einheitlichen Match-Karten für den Spielplan.

### UI-Styling & Icon-Richtlinie
- **Keine Material Icons / Emojis:** Sämtliche Symbole nutzen projekteigene SVG-Vektorgrafiken (`composeResources/drawable`):
  - `ic_trophy.xml` (Pokal / Champion-Badge / Siegerehrung)
  - `ic_plus.xml` (Spieler hinzufügen)
  - `ic_trash.xml` (Spieler / Turnier löschen)
  - `ic_folder.xml` (Gespeicherte Profile laden)
  - `ic_refresh.xml` (Reihenfolge mischen)
  - `ic_arrow_back.xml` (Zurück-Navigation via `ScreenHeader`)
  - `ic_alert_triangle.xml` (Validierungs- & Fehlermeldungen)
- **Farbgebung:** Dark Theme Gradient (`DarkBgGradient`), Glasmorphismus-Rahmen (`GlassBorder`) und edle Orange-Akzente (`PrimaryOrange`, `Color(0x26FF8800)`).

---

## 6. Persistenz & Crash-Schutz
- Bei jeder Statusänderung (Match gestartet, Match beendet, Freilos gewertet) serialisiert der `TournamentManager` den aktuellen Stand als JSON und speichert ihn in `AppPreferences` unter `active_tournament_v1`.
- Beim Neustart der App stellt `loadFromPreferences()` das Turnier nahtlos wieder her.

---

## 7. Web-Live-Ticker & Dezentrale Ergebniseingabe (Zero-Install)

Ermöglicht es Zuschauern und Spielern an Neben-Boards, ohne vorherige App-Installation über jeden Smartphone-Browser (Safari, Chrome) am Turnier teilzunehmen:

```
composeApp/src/commonMain/kotlin/com/example/myapplication/tournament/web/
├── QrCodeGenerator.kt          // Reiner Kotlin ISO-18004 QR-Code Generator & Canvas View
├── TournamentWebHtml.kt        // Autarkes Mobile-Web-Portal (Apple-Sports Look, Dark Mode)
├── TournamentWebModels.kt      // MatchSubmissionRequest, PendingMatchSubmission, Overview
└── TournamentWebServer.kt      // Plattformunabhängige Schnittstelle & expect/actual
```

### Funktionsweise
1. **Zentraler QR-Code auf dem Host:**
   - Ein Klick auf das Smartphone-Icon (`ic_mobile_connect`) in der `TournamentViewScreen` öffnet den `TournamentWebShareDialog`.
   - Zeigt den scannbaren QR-Code (generiert via `SimpleQrCode`) und die lokale IP-URL (z. B. `http://192.168.1.50:8080`).
2. **Web-Portal (Zero-Install):**
   - Beim Scannen mit der Handykamera öffnet sich die mobile Web-App.
   - Der Spieler wählt seinen Namen („Ich bin Marc“) $\rightarrow$ die Web-App hebt sein aktives Match prominent hervor.
   - **Weg A (Endstand melden):** Der Spieler tippt direkt im Browser auf „Endstand melden“ und trägt das Ergebnis (z. B. `3 : 1`) ein.
   - **Weg B (In s'Dart App spielen):** Ein Tipp auf „In s'Dart App spielen“ startet per Deep-Link (`sdart://tournament/play`) den normalen `GameScreen` der nativen App mit automatischer KI-Kamerazählung oder manuellem Touch-Board.
3. **Host-Freigabe & Schutz vor Fehleingaben:**
   - Im Standardmodus werden externe Meldungen in `TournamentManager.pendingSubmissions` eingereiht.
   - Der Host zeigt ein animiertes Banner (`PendingSubmissionsBanner`): *„Meldung aus Web: Marc 3:1 Peter – [Übernehmen] [Ablehnen]“*.
   - Nach Übernahme rückt der Sieger automatisch im Turnierbaum vor und alle verbundenen Browser aktualisieren sich latenzarm per Server-Sent Events (SSE: `GET /api/events`).


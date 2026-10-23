package com.sdart.app.tournament.web

/**
 * Liefert das vollständige, eigenständige Mobile-Web-Portal für S'Dart Turniere.
 * Gestaltet im modernen Apple-Sports Look (Dark Mode, Glasmorphismus, responsive für alle Smartphones).
 * Vollständig offline-fähig (keine externen CDNs oder Fonts, ausschließlich projekteigene SVG-Elemente).
 */
object TournamentWebHtml {

    fun buildHtml(serverPort: Int): String {
        return """
<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>S'Dart Turnier Live-Ticker</title>
    <style>
        :root {
            --bg-color: #0c0c0e;
            --surface-color: rgba(255, 255, 255, 0.06);
            --surface-border: rgba(255, 255, 255, 0.12);
            --primary-orange: #ff8800;
            --primary-orange-glow: rgba(255, 136, 0, 0.25);
            --primary-orange-subtle: rgba(255, 136, 0, 0.15);
            --text-main: #ffffff;
            --text-secondary: rgba(255, 255, 255, 0.65);
            --text-muted: rgba(255, 255, 255, 0.38);
            --success-color: #30d158;
            --danger-color: #ff453a;
            --gold-color: #ffaa00;
            --silver-color: #d1d1d6;
            --bronze-color: #cd7f32;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            -webkit-tap-highlight-color: transparent;
        }

        body {
            font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "SF Pro Display", "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: var(--bg-color);
            color: var(--text-main);
            min-height: 100vh;
            padding-bottom: 40px;
        }

        /* SVG Icons Basis */
        .svg-icon {
            display: inline-block;
            width: 16px;
            height: 16px;
            vertical-align: middle;
            fill: currentColor;
            flex-shrink: 0;
        }

        .svg-stroke {
            fill: none !important;
            stroke: currentColor;
            stroke-width: 2;
            stroke-linecap: round;
            stroke-linejoin: round;
        }

        header {
            position: sticky;
            top: 0;
            z-index: 100;
            background: rgba(12, 12, 14, 0.85);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border-bottom: 0.5px solid var(--surface-border);
            padding: 12px 18px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .header-title-wrap {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .header-title {
            font-size: 17px;
            font-weight: 700;
            letter-spacing: -0.4px;
        }

        .header-sub {
            font-size: 11px;
            color: var(--text-secondary);
            font-weight: 500;
        }

        .status-pill {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: 600;
            background: rgba(48, 209, 88, 0.15);
            color: var(--success-color);
            border: 0.5px solid rgba(48, 209, 88, 0.3);
        }

        .status-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background-color: var(--success-color);
            box-shadow: 0 0 6px var(--success-color);
        }

        .container {
            max-width: 640px;
            margin: 0 auto;
            padding: 14px 16px;
        }

        /* Champion Banner */
        .champion-card {
            background: linear-gradient(180deg, rgba(255, 136, 0, 0.16) 0%, rgba(28, 28, 30, 0.95) 100%);
            border: 0.5px solid rgba(255, 136, 0, 0.45);
            border-radius: 20px;
            padding: 22px 18px;
            text-align: center;
            margin-bottom: 16px;
            box-shadow: 0 8px 30px rgba(0, 0, 0, 0.5);
        }

        .champion-trophy-circle {
            width: 58px;
            height: 58px;
            border-radius: 50%;
            background: rgba(255, 136, 0, 0.2);
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: var(--primary-orange);
            margin-bottom: 12px;
            box-shadow: 0 0 20px rgba(255, 136, 0, 0.3);
        }

        .champion-trophy-circle .svg-icon {
            width: 32px;
            height: 32px;
        }

        .champion-label {
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 1.8px;
            color: var(--primary-orange);
            text-transform: uppercase;
            margin-bottom: 4px;
        }

        .champion-name {
            font-size: 24px;
            font-weight: 800;
            color: #ffffff;
            margin-bottom: 4px;
        }

        .champion-sub {
            font-size: 12px;
            color: var(--text-secondary);
        }

        /* Identity Bar */
        .identity-bar {
            background: var(--surface-color);
            border: 0.5px solid var(--surface-border);
            border-radius: 14px;
            padding: 10px 14px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 14px;
        }

        .identity-label {
            font-size: 13px;
            color: var(--text-secondary);
            display: inline-flex;
            align-items: center;
            gap: 7px;
        }

        .identity-select {
            background: rgba(255, 255, 255, 0.1);
            color: #fff;
            border: 0.5px solid rgba(255, 255, 255, 0.2);
            padding: 6px 12px;
            border-radius: 8px;
            font-size: 13px;
            font-weight: 600;
            outline: none;
        }

        /* Hero Match Card */
        .hero-match-card {
            background: linear-gradient(135deg, rgba(255, 136, 0, 0.15), rgba(255, 255, 255, 0.04));
            border: 1px solid var(--primary-orange);
            border-radius: 18px;
            padding: 16px;
            margin-bottom: 18px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
        }

        .hero-badge {
            display: inline-block;
            font-size: 10px;
            text-transform: uppercase;
            font-weight: 700;
            letter-spacing: 0.8px;
            background: var(--primary-orange);
            color: #000;
            padding: 3px 8px;
            border-radius: 6px;
            margin-bottom: 8px;
        }

        .hero-title {
            font-size: 17px;
            font-weight: 700;
            margin-bottom: 12px;
        }

        .hero-players {
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: rgba(0, 0, 0, 0.25);
            padding: 10px 14px;
            border-radius: 12px;
            margin-bottom: 14px;
        }

        .hero-player-name {
            font-size: 15px;
            font-weight: 600;
        }

        .hero-vs {
            font-size: 11px;
            color: var(--text-muted);
            font-weight: 700;
            letter-spacing: 0.5px;
        }

        .hero-pending-box {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            background: rgba(255, 136, 0, 0.15);
            border: 0.5px solid rgba(255, 136, 0, 0.4);
            border-radius: 10px;
            padding: 10px 14px;
            font-size: 13px;
            font-weight: 600;
            color: var(--primary-orange);
            margin-bottom: 10px;
            text-align: center;
        }

        .hero-actions {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
        }

        .btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 7px;
            padding: 12px;
            border-radius: 12px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            border: none;
            transition: transform 0.1s ease, opacity 0.1s ease;
            text-decoration: none;
            text-align: center;
        }

        .btn:active {
            transform: scale(0.97);
            opacity: 0.85;
        }

        .btn-primary {
            background: var(--primary-orange);
            color: #000;
            font-weight: 700;
        }

        .btn-secondary {
            background: rgba(255, 255, 255, 0.12);
            color: #fff;
            border: 0.5px solid var(--surface-border);
        }

        /* Segmented Tabs (Cupertino Style) */
        .tabs {
            display: flex;
            background: rgba(255, 255, 255, 0.08);
            padding: 3px;
            border-radius: 12px;
            margin-bottom: 16px;
        }

        .tab-btn {
            flex: 1;
            padding: 8px;
            font-size: 13px;
            font-weight: 600;
            color: var(--text-secondary);
            background: transparent;
            border: none;
            border-radius: 10px;
            cursor: pointer;
            text-align: center;
            transition: background-color 0.15s ease, color 0.15s ease;
        }

        .tab-btn.active {
            background: rgba(255, 255, 255, 0.18);
            color: #fff;
        }

        /* Apple Sports Match Cards */
        .match-card {
            background: var(--surface-color);
            border: 0.5px solid var(--surface-border);
            border-radius: 14px;
            padding: 11px 14px;
            margin-bottom: 10px;
            transition: border-color 0.15s ease;
        }

        .match-card.ready {
            border-color: rgba(255, 136, 0, 0.5);
            background: rgba(255, 136, 0, 0.07);
        }

        .match-card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 11px;
            color: var(--text-muted);
            font-weight: 600;
            margin-bottom: 8px;
        }

        .match-status-badge {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            font-size: 11px;
            font-weight: 600;
        }

        .status-dot-ready {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background-color: var(--primary-orange);
            box-shadow: 0 0 5px var(--primary-orange);
        }

        .player-slot-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 2px 0;
        }

        .player-slot-info {
            display: flex;
            align-items: center;
            gap: 8px;
            min-width: 0;
            flex: 1;
        }

        .player-badge {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.1);
            color: rgba(255, 255, 255, 0.75);
            font-size: 10px;
            font-weight: 600;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
        }

        .player-badge.winner {
            background: rgba(255, 136, 0, 0.25);
            color: var(--primary-orange);
            border: 0.5px solid rgba(255, 136, 0, 0.45);
        }

        .player-name-text {
            font-size: 14px;
            color: rgba(255, 255, 255, 0.85);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .player-name-text.winner {
            font-weight: 600;
            color: #ffffff;
        }

        .trophy-inline-icon {
            width: 14px;
            height: 14px;
            color: var(--primary-orange);
            display: inline-block;
            vertical-align: middle;
        }

        .player-score-capsule {
            border-radius: 6px;
            padding: 3px 8px;
            font-size: 12px;
            font-weight: 600;
            background: rgba(255, 255, 255, 0.08);
            color: rgba(255, 255, 255, 0.75);
            min-width: 26px;
            text-align: center;
        }

        .player-score-capsule.winner {
            background: rgba(255, 136, 0, 0.22);
            color: var(--primary-orange);
            font-weight: 700;
            border: 0.5px solid rgba(255, 136, 0, 0.35);
        }

        .match-hairline-divider {
            height: 0.5px;
            background: rgba(255, 255, 255, 0.08);
            margin: 6px 0;
        }

        /* League Table (Apple Inset-Grouped) */
        .table-wrap {
            overflow-x: auto;
            border-radius: 14px;
            border: 0.5px solid var(--surface-border);
            background: var(--surface-color);
        }

        table {
            width: 100%;
            border-collapse: collapse;
            font-size: 13px;
            text-align: left;
        }

        th {
            background: rgba(255, 255, 255, 0.04);
            color: var(--text-secondary);
            font-size: 11px;
            text-transform: uppercase;
            padding: 10px 12px;
            font-weight: 600;
            border-bottom: 0.5px solid var(--surface-border);
        }

        td {
            padding: 10px 12px;
            border-bottom: 0.5px solid rgba(255, 255, 255, 0.06);
            vertical-align: middle;
        }

        tr:last-child td {
            border-bottom: none;
        }

        /* Podium Badges */
        .podium-badge {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 10px;
            font-weight: 700;
        }

        .podium-1 {
            background: rgba(255, 136, 0, 0.22);
            color: var(--primary-orange);
            border: 0.5px solid rgba(255, 136, 0, 0.45);
        }

        .podium-2 {
            background: rgba(255, 255, 255, 0.16);
            color: #ffffff;
            border: 0.5px solid rgba(255, 255, 255, 0.3);
        }

        .podium-3 {
            background: rgba(205, 127, 50, 0.2);
            color: #cd7f32;
            border: 0.5px solid rgba(205, 127, 50, 0.4);
        }

        .podium-other {
            color: var(--text-muted);
            font-size: 11px;
            font-weight: 500;
            padding-left: 5px;
        }

        /* Modal Dialog */
        .modal-overlay {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.7);
            backdrop-filter: blur(10px);
            -webkit-backdrop-filter: blur(10px);
            z-index: 200;
            display: none;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }

        .modal-overlay.active {
            display: flex;
        }

        .modal-card {
            background: #1c1c1e;
            border: 0.5px solid var(--surface-border);
            border-radius: 20px;
            width: 100%;
            max-width: 400px;
            padding: 20px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.6);
        }

        .modal-title {
            font-size: 18px;
            font-weight: 700;
            margin-bottom: 4px;
        }

        .modal-sub {
            font-size: 12px;
            color: var(--text-secondary);
            margin-bottom: 16px;
        }

        .stepper-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: rgba(255, 255, 255, 0.05);
            padding: 10px 14px;
            border-radius: 12px;
            margin-bottom: 10px;
        }

        .stepper-name {
            font-size: 14px;
            font-weight: 600;
            max-width: 150px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .stepper-controls {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .stepper-btn {
            width: 34px;
            height: 34px;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.15);
            color: #fff;
            border: none;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: background 0.1s ease;
        }

        .stepper-btn:active {
            background: rgba(255, 255, 255, 0.28);
        }

        .stepper-value {
            font-size: 18px;
            font-weight: 800;
            min-width: 24px;
            text-align: center;
        }

        .modal-btns {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
            margin-top: 18px;
        }

        /* Toast Notifications */
        .toast {
            position: fixed;
            bottom: 24px;
            left: 50%;
            transform: translateX(-50%) translateY(100px);
            background: rgba(30, 30, 34, 0.95);
            backdrop-filter: blur(15px);
            -webkit-backdrop-filter: blur(15px);
            color: #fff;
            padding: 12px 20px;
            border-radius: 12px;
            box-shadow: 0 8px 30px rgba(0, 0, 0, 0.5);
            border: 0.5px solid var(--surface-border);
            font-size: 14px;
            font-weight: 600;
            z-index: 1000;
            transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275), opacity 0.3s;
            opacity: 0;
            pointer-events: none;
            display: flex;
            align-items: center;
            gap: 8px;
            max-width: 90%;
        }

        .toast.show {
            transform: translateX(-50%) translateY(0);
            opacity: 1;
        }

        .toast.success {
            border-color: rgba(48, 209, 88, 0.6);
            color: #fff;
        }

        .toast.error {
            border-color: rgba(255, 69, 58, 0.6);
            color: #fff;
        }
    </style>
</head>
<body>
    <div id="toast" class="toast">
        <span id="toast-icon"></span>
        <span id="toast-msg"></span>
    </div>

    <header>
        <div>
            <div class="header-title-wrap">
                <div class="header-title" id="tourn-title">S'Dart Turnier</div>
            </div>
            <div class="header-sub" id="tourn-subtitle">Verbinde zum Server...</div>
        </div>
        <div class="status-pill" id="conn-pill">
            <span class="status-dot"></span>
            <span id="conn-text">Live</span>
        </div>
    </header>

    <div class="container">
        <!-- Champion Celebration Banner -->
        <div class="champion-card" id="champion-card" style="display: none;">
            <div class="champion-trophy-circle" id="champion-trophy-box"></div>
            <div class="champion-label">Turniersieger</div>
            <div class="champion-name" id="champion-winner-name">Sieger</div>
            <div class="champion-sub" id="champion-sub-text">Herzlichen Glückwunsch zum Turniersieg!</div>
        </div>

        <!-- Identity Selector -->
        <div class="identity-bar">
            <span class="identity-label" id="identity-label-wrap">
                <span id="identity-icon"></span>
                <span>Ich bin:</span>
            </span>
            <select class="identity-select" id="player-selector" onchange="onPlayerSelected()">
                <option value="">-- Spieler wählen --</option>
            </select>
        </div>

        <!-- Hero Match Card -->
        <div class="hero-match-card" id="hero-card" style="display: none;">
            <span class="hero-badge" id="hero-round">Runde 1</span>
            <div class="hero-title">Dein anstehendes Match</div>
            <div class="hero-players">
                <span class="hero-player-name" id="hero-p1">Spieler 1</span>
                <span class="hero-vs">VS</span>
                <span class="hero-player-name" id="hero-p2">Spieler 2</span>
            </div>
            <div class="hero-pending-box" id="hero-pending-box" style="display: none;">
                <span id="hero-pending-icon"></span>
                <span>Ergebnis übermittelt – wartet auf Freigabe durch den Turnierleiter</span>
            </div>
            <div class="hero-actions" id="hero-actions">
                <button class="btn btn-primary" onclick="openScoreModal()">
                    <span id="btn-lightning-icon"></span>
                    <span>Endstand melden</span>
                </button>
                <a class="btn btn-secondary" id="hero-app-link" href="#">
                    <span id="btn-target-icon"></span>
                    <span>In App spielen</span>
                </a>
            </div>
        </div>

        <!-- Segmented Tabs -->
        <div class="tabs">
            <button class="tab-btn active" id="tab-matches-btn" onclick="showTab('matches')">Matches</button>
            <button class="tab-btn" id="tab-table-btn" onclick="showTab('table')">Tabelle</button>
        </div>

        <!-- Matches Tab -->
        <div id="tab-matches">
            <div id="matches-list"></div>
        </div>

        <!-- Table Tab -->
        <div id="tab-table" style="display: none;">
            <div class="table-wrap">
                <table id="standings-table">
                    <thead>
                        <tr>
                            <th style="width: 36px;">#</th>
                            <th>Spieler</th>
                            <th style="text-align: center;">Sp</th>
                            <th style="text-align: center;">S</th>
                            <th style="text-align: center;">N</th>
                            <th style="text-align: center;">Legs</th>
                            <th style="text-align: center;">Diff</th>
                        </tr>
                    </thead>
                    <tbody id="standings-tbody"></tbody>
                </table>
            </div>
        </div>
    </div>

    <!-- Score Modal -->
    <div class="modal-overlay" id="score-modal">
        <div class="modal-card">
            <div class="modal-title">Endstand melden</div>
            <div class="modal-sub" id="modal-match-sub">Best of 3 Legs</div>

            <div class="stepper-row">
                <span class="stepper-name" id="modal-p1-name">Spieler 1</span>
                <div class="stepper-controls">
                    <button class="stepper-btn" onclick="changeLegs('p1', -1)" id="btn-p1-minus"></button>
                    <span class="stepper-value" id="modal-p1-legs">0</span>
                    <button class="stepper-btn" onclick="changeLegs('p1', 1)" id="btn-p1-plus"></button>
                </div>
            </div>

            <div class="stepper-row">
                <span class="stepper-name" id="modal-p2-name">Spieler 2</span>
                <div class="stepper-controls">
                    <button class="stepper-btn" onclick="changeLegs('p2', -1)" id="btn-p2-minus"></button>
                    <span class="stepper-value" id="modal-p2-legs">0</span>
                    <button class="stepper-btn" onclick="changeLegs('p2', 1)" id="btn-p2-plus"></button>
                </div>
            </div>

            <div class="modal-btns">
                <button class="btn btn-secondary" id="modal-cancel-btn" onclick="closeScoreModal()">Abbrechen</button>
                <button class="btn btn-primary" id="modal-submit-btn" onclick="submitScore()">Absenden</button>
            </div>
        </div>
    </div>

    <script>
        // --- Exakte, projekteigene SVG-Vektoren aus composeResources/drawable ---
        const SVGS = {
            trophy: '<svg class="svg-icon" viewBox="0 0 100 100"><path fill-rule="evenodd" d="M18,12 Q4,12 4,28 Q4,40 18,44 L18,12 Z M18,19 Q11,19 11,28 Q11,37 18,37 L18,19 Z"/><path fill-rule="evenodd" d="M82,12 Q96,12 96,28 Q96,40 82,44 L82,12 Z M82,19 Q89,19 89,28 Q89,37 82,37 L82,19 Z"/><path d="M18,8 L82,8 L82,44 Q82,68 50,72 Q18,68 18,44 Z"/><path d="M42,72 L42,82 L58,82 L58,72 Z"/><path d="M32,82 L68,82 L68,88 L32,88 Z"/><path d="M24,88 L76,88 L76,94 L24,94 Z"/></svg>',
            player: '<svg class="svg-icon svg-stroke" viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
            lightning: '<svg class="svg-icon" viewBox="0 0 24 24"><path d="M7 2v11h3v9l7-12h-4l4-8H7z"/></svg>',
            target: '<svg class="svg-icon" viewBox="0 0 24 24"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2z M12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8s8,3.58 8,8S16.42 20 12 20z"/><circle cx="12" cy="12" r="3"/><circle cx="12" cy="12" r="5" fill="none" stroke="currentColor" stroke-width="1"/></svg>',
            checkCircle: '<svg class="svg-icon svg-stroke" viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="M22 4L12 14.01l-3-3"/></svg>',
            alertTriangle: '<svg class="svg-icon svg-stroke" viewBox="0 0 24 24"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><path d="M12 9v4"/><path d="M12 17h.01"/></svg>',
            clock: '<svg class="svg-icon svg-stroke" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><polyline points="12 6 12 12 16 14"/></svg>',
            plus: '<svg class="svg-icon svg-stroke" viewBox="0 0 24 24" style="stroke-width: 2.5;"><path d="M12 5v14M5 12h14"/></svg>',
            minus: '<svg class="svg-icon svg-stroke" viewBox="0 0 24 24" style="stroke-width: 2.5;"><path d="M5 12h14"/></svg>'
        };

        // Statische Icons im DOM platzieren
        document.getElementById('identity-icon').innerHTML = SVGS.player;
        document.getElementById('champion-trophy-box').innerHTML = SVGS.trophy;
        document.getElementById('hero-pending-icon').innerHTML = SVGS.clock;
        document.getElementById('btn-lightning-icon').innerHTML = SVGS.lightning;
        document.getElementById('btn-target-icon').innerHTML = SVGS.target;
        document.getElementById('btn-p1-minus').innerHTML = SVGS.minus;
        document.getElementById('btn-p1-plus').innerHTML = SVGS.plus;
        document.getElementById('btn-p2-minus').innerHTML = SVGS.minus;
        document.getElementById('btn-p2-plus').innerHTML = SVGS.plus;

        let tournamentData = null;
        let selectedPlayerId = localStorage.getItem('sdart_player_id') || "";
        let activeMatch = null;
        let p1Legs = 0;
        let p2Legs = 0;

        function showToast(msg, type) {
            const toast = document.getElementById('toast');
            const iconWrap = document.getElementById('toast-icon');
            const msgWrap = document.getElementById('toast-msg');

            msgWrap.textContent = msg;
            iconWrap.innerHTML = (type === 'error') ? SVGS.alertTriangle : SVGS.checkCircle;
            toast.className = 'toast show ' + (type || 'info');
            setTimeout(() => {
                toast.classList.remove('show');
            }, 3500);
        }

        async function fetchTournament() {
            try {
                const res = await fetch('/api/tournament');
                if (!res.ok) throw new Error('Fehler beim Laden');
                tournamentData = await res.json();
                renderUI();
            } catch (e) {
                console.error(e);
                document.getElementById('conn-pill').style.background = 'rgba(255, 69, 58, 0.2)';
                document.getElementById('conn-pill').style.color = 'var(--danger-color)';
                document.getElementById('conn-text').textContent = 'Getrennt';
            }
        }

        function initSSE() {
            const evtSource = new EventSource('/api/events');
            evtSource.onopen = () => {
                document.getElementById('conn-pill').style.background = 'rgba(48, 209, 88, 0.15)';
                document.getElementById('conn-pill').style.color = 'var(--success-color)';
                document.getElementById('conn-text').textContent = 'Live';
            };
            evtSource.onmessage = () => {
                fetchTournament();
            };
            evtSource.onerror = () => {
                document.getElementById('conn-pill').style.background = 'rgba(255, 69, 58, 0.2)';
                document.getElementById('conn-pill').style.color = 'var(--danger-color)';
                document.getElementById('conn-text').textContent = 'Wiederverbinden...';
            };
        }

        function renderUI() {
            if (!tournamentData || !tournamentData.tournament) {
                document.getElementById('tourn-title').textContent = "Kein aktives Turnier";
                document.getElementById('tourn-subtitle').textContent = "Warte auf Turnierstart...";
                document.getElementById('champion-card').style.display = 'none';
                return;
            }

            const t = tournamentData.tournament;
            const pendingMatchIds = tournamentData.pendingMatchIds || [];
            document.getElementById('tourn-title').textContent = t.name || "S'Dart Turnier";
            const modeName = t.type === 'SINGLE_ELIMINATION' ? "K.O.-System" : "Liga";
            document.getElementById('tourn-subtitle').textContent = modeName + " • " + (t.config?.mode || "501");

            // Turniersieger-Celebration Banner
            const championCard = document.getElementById('champion-card');
            if (t.isFinished && t.winner) {
                championCard.style.display = 'block';
                document.getElementById('champion-winner-name').textContent = t.winner.name;
                document.getElementById('champion-sub-text').textContent = "Herzlichen Glückwunsch zum Sieg im " + (t.name || "Turnier") + "!";
            } else {
                championCard.style.display = 'none';
            }

            // Spieler Dropdown
            const selector = document.getElementById('player-selector');
            const prevSelected = selector.value || selectedPlayerId;
            selector.innerHTML = '<option value="">-- Spieler wählen --</option>';
            t.players.forEach(p => {
                const opt = document.createElement('option');
                opt.value = p.id;
                opt.textContent = p.name;
                if (p.id === prevSelected) opt.selected = true;
                selector.appendChild(opt);
            });

            // Finde aktives Match des ausgewählten Spielers
            activeMatch = null;
            if (prevSelected) {
                activeMatch = t.matches.find(m => !m.isFinished && !m.isBye && m.player1 && m.player2 && (m.player1.id === prevSelected || m.player2.id === prevSelected));
            }

            const heroCard = document.getElementById('hero-card');
            if (activeMatch) {
                heroCard.style.display = 'block';
                document.getElementById('hero-round').textContent = 'Runde ' + activeMatch.round;
                document.getElementById('hero-p1').textContent = activeMatch.player1.name;
                document.getElementById('hero-p2').textContent = activeMatch.player2.name;

                const isPending = pendingMatchIds.includes(activeMatch.id);
                document.getElementById('hero-pending-box').style.display = isPending ? 'flex' : 'none';
                document.getElementById('hero-actions').style.display = isPending ? 'none' : 'grid';

                // Deep-Link zur S'Dart App
                const appLink = "sdart://tournament/play?matchId=" + activeMatch.id + "&host=" + window.location.hostname + "&port=" + (window.location.port || "8080");
                document.getElementById('hero-app-link').href = appLink;
            } else {
                heroCard.style.display = 'none';
            }

            // Matches Liste (Apple Sports Stil mit Initialen, Divider & Score-Capsules)
            const list = document.getElementById('matches-list');
            list.innerHTML = '';
            t.matches.filter(m => !m.isBye).forEach(m => {
                const card = document.createElement('div');
                const isReady = !m.isFinished && m.player1 && m.player2 && !pendingMatchIds.includes(m.id);
                card.className = 'match-card' + (isReady ? ' ready' : '');

                const p1Name = m.player1 ? m.player1.name : "TBD";
                const p2Name = m.player2 ? m.player2.name : "TBD";
                const isFinished = m.isFinished;
                const isPending = pendingMatchIds.includes(m.id);
                const p1Winner = isFinished && m.winner && m.player1 && m.winner.id === m.player1.id;
                const p2Winner = isFinished && m.winner && m.player2 && m.winner.id === m.player2.id;

                const p1Initial = m.player1 ? (m.player1.name.trim().charAt(0).toUpperCase() || '•') : '•';
                const p2Initial = m.player2 ? (m.player2.name.trim().charAt(0).toUpperCase() || '•') : '•';

                let statusBadge = '';
                if (isFinished) {
                    statusBadge = '<span class="match-status-badge" style="color: var(--text-secondary);">' + SVGS.checkCircle + ' Beendet</span>';
                } else if (isPending) {
                    statusBadge = '<span class="match-status-badge" style="color: var(--primary-orange);">' + SVGS.clock + ' Freigabe offen</span>';
                } else if (isReady) {
                    statusBadge = '<span class="match-status-badge" style="color: var(--primary-orange);"><span class="status-dot-ready"></span> Bereit</span>';
                } else {
                    statusBadge = '<span style="color: var(--text-muted);">Wartet</span>';
                }

                const p1Trophy = p1Winner ? '<span class="trophy-inline-icon">' + SVGS.trophy + '</span>' : '';
                const p2Trophy = p2Winner ? '<span class="trophy-inline-icon">' + SVGS.trophy + '</span>' : '';

                card.innerHTML = `
                    <div class="match-card-header">
                        <span>Runde ${'$'}{m.round} • Spiel ${'$'}{m.matchIndexInRound + 1}</span>
                        ${'$'}{statusBadge}
                    </div>

                    <div class="player-slot-row">
                        <div class="player-slot-info">
                            <div class="player-badge ${'$'}{p1Winner ? 'winner' : ''}">${'$'}{p1Initial}</div>
                            <span class="player-name-text ${'$'}{p1Winner ? 'winner' : ''}">
                                ${'$'}{p1Name}
                                ${'$'}{p1Trophy}
                            </span>
                        </div>
                        <div class="player-score-capsule ${'$'}{p1Winner ? 'winner' : ''}">${'$'}{m.player1Result ? m.player1Result.legsWon : (isFinished ? 0 : '-')}</div>
                    </div>

                    <div class="match-hairline-divider"></div>

                    <div class="player-slot-row">
                        <div class="player-slot-info">
                            <div class="player-badge ${'$'}{p2Winner ? 'winner' : ''}">${'$'}{p2Initial}</div>
                            <span class="player-name-text ${'$'}{p2Winner ? 'winner' : ''}">
                                ${'$'}{p2Name}
                                ${'$'}{p2Trophy}
                            </span>
                        </div>
                        <div class="player-score-capsule ${'$'}{p2Winner ? 'winner' : ''}">${'$'}{m.player2Result ? m.player2Result.legsWon : (isFinished ? 0 : '-')}</div>
                    </div>
                `;
                list.appendChild(card);
            });

            // Standings Tabelle (Apple Inset-Grouped mit Podium-Badges)
            const tbody = document.getElementById('standings-tbody');
            tbody.innerHTML = '';
            if (tournamentData.standings && tournamentData.standings.length > 0) {
                document.getElementById('tab-table-btn').style.display = 'block';
                tournamentData.standings.forEach(row => {
                    const tr = document.createElement('tr');
                    const diffSign = row.legDifference > 0 ? '+' : '';

                    let rankContent = '';
                    if (row.rank === 1) {
                        rankContent = '<span class="podium-badge podium-1">1</span>';
                    } else if (row.rank === 2) {
                        rankContent = '<span class="podium-badge podium-2">2</span>';
                    } else if (row.rank === 3) {
                        rankContent = '<span class="podium-badge podium-3">3</span>';
                    } else {
                        rankContent = '<span class="podium-other">' + row.rank + '.</span>';
                    }

                    tr.innerHTML = `
                        <td>${'$'}{rankContent}</td>
                        <td style="font-weight: 600;">${'$'}{row.player.name}</td>
                        <td style="text-align: center;">${'$'}{row.played}</td>
                        <td style="text-align: center;">${'$'}{row.won}</td>
                        <td style="text-align: center;">${'$'}{row.lost}</td>
                        <td style="text-align: center; color: var(--text-secondary);">${'$'}{row.legsWon}:${'$'}{row.legsLost}</td>
                        <td style="text-align: center; color: ${'$'}{row.legDifference > 0 ? 'var(--success-color)' : (row.legDifference < 0 ? 'var(--danger-color)' : 'inherit')}; font-weight: 600;">${'$'}{diffSign}${'$'}{row.legDifference}</td>
                    `;
                    tbody.appendChild(tr);
                });
            } else {
                if (t.type === 'SINGLE_ELIMINATION') {
                    document.getElementById('tab-table-btn').style.display = 'none';
                }
            }
        }

        function onPlayerSelected() {
            const selector = document.getElementById('player-selector');
            selectedPlayerId = selector.value;
            localStorage.setItem('sdart_player_id', selectedPlayerId);
            renderUI();
        }

        function showTab(tab) {
            document.getElementById('tab-matches').style.display = (tab === 'matches') ? 'block' : 'none';
            document.getElementById('tab-table').style.display = (tab === 'table') ? 'block' : 'none';
            document.getElementById('tab-matches-btn').className = (tab === 'matches') ? 'tab-btn active' : 'tab-btn';
            document.getElementById('tab-table-btn').className = (tab === 'table') ? 'tab-btn active' : 'tab-btn';
        }

        function openScoreModal() {
            if (!activeMatch) return;
            document.getElementById('modal-match-sub').textContent = 'Match: ' + activeMatch.player1.name + ' vs. ' + activeMatch.player2.name;
            document.getElementById('modal-p1-name').textContent = activeMatch.player1.name;
            document.getElementById('modal-p2-name').textContent = activeMatch.player2.name;
            p1Legs = 0;
            p2Legs = 0;
            updateStepperUI();
            document.getElementById('score-modal').classList.add('active');
        }

        function closeScoreModal() {
            document.getElementById('score-modal').classList.remove('active');
        }

        function changeLegs(player, delta) {
            if (player === 'p1') {
                p1Legs = Math.max(0, p1Legs + delta);
            } else {
                p2Legs = Math.max(0, p2Legs + delta);
            }
            updateStepperUI();
        }

        function updateStepperUI() {
            document.getElementById('modal-p1-legs').textContent = p1Legs;
            document.getElementById('modal-p2-legs').textContent = p2Legs;
        }

        async function submitScore() {
            if (!activeMatch) return;
            if (p1Legs === p2Legs) {
                showToast('Ein Unentschieden ist im Darts nicht möglich. Bitte Sieger-Legs angeben.', 'error');
                return;
            }

            const submitBtn = document.getElementById('modal-submit-btn');
            const cancelBtn = document.getElementById('modal-cancel-btn');
            submitBtn.disabled = true;
            cancelBtn.disabled = true;
            submitBtn.textContent = 'Wird gesendet...';

            const winnerId = (p1Legs > p2Legs) ? activeMatch.player1.id : activeMatch.player2.id;
            const submitterName = (selectedPlayerId === activeMatch.player1.id) 
                ? activeMatch.player1.name 
                : (selectedPlayerId === activeMatch.player2.id ? activeMatch.player2.name : (activeMatch.player1.name || "Spieler"));

            const payload = {
                matchId: activeMatch.id,
                winnerPlayerId: winnerId,
                player1Legs: p1Legs,
                player2Legs: p2Legs,
                submittedByPlayerName: submitterName
            };

            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 7000);

            try {
                const res = await fetch('/api/matches/' + activeMatch.id + '/submit', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload),
                    signal: controller.signal
                });
                clearTimeout(timeoutId);

                const data = await res.json();
                if (data.success) {
                    closeScoreModal();
                    showToast(data.message || 'Ergebnis erfolgreich übermittelt!', 'success');
                    fetchTournament();
                } else {
                    showToast('Fehler: ' + (data.message || 'Konnte nicht gespeichert werden'), 'error');
                }
            } catch (e) {
                clearTimeout(timeoutId);
                console.error(e);
                showToast('Netzwerkfehler: Bitte Verbindung prüfen.', 'error');
            } finally {
                submitBtn.disabled = false;
                cancelBtn.disabled = false;
                submitBtn.textContent = 'Absenden';
            }
        }

        // Start
        fetchTournament();
        initSSE();
    </script>
</body>
</html>
        """.trimIndent()
    }
}

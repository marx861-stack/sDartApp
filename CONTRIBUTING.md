# Contributing Guidelines

Thank you for your interest in contributing to **s'Dart App**! Open source thrives on community participation – whether through bug reports, feature suggestions, or pull requests.

---

## 1. Reporting Bugs

Before submitting a bug report:
1. Please search existing [GitHub Issues](https://github.com/marx861-stack/sDartApp/issues) to verify the problem has not already been reported.
2. Provide as much detail as possible:
   - **Platform & Operating System:** (e.g. Windows 11, macOS Sonoma, Android 14)
   - **Camera Hardware / Resolution:** (e.g. USB Webcam 1080p, smartphone camera)
   - **Steps to Reproduce:** What exact steps did you take when the issue occurred?
   - **Expected vs. Actual Behavior**
   - If applicable: Attach screenshots or console logs.

---

## 2. Architectural Principles for Code Contributions

To keep the application stable, maintainable, and consistent across all target platforms (Desktop, Android, iOS), please strictly follow these guidelines:

### A. Single Source of Truth in `commonMain`
- **All UI components, Compose Canvas routines, overlays, sight brackets, text rendering, and evaluations belong in `commonMain`.**
- Never duplicate UI implementations across platform-specific modules.

### B. Minimal `expect/actual` Interfaces
- `expect`/`actual` must be used **exclusively** for unavoidable native hardware and platform APIs (e.g. CameraX on Android, WebcamCapture/Skia on Desktop, AVFoundation on iOS).
- Native video feeds are strictly limited to delivering raw camera frames. All overlays, sight brackets, and math calculations reside in `commonMain`.

### C. No Fixed Screen or Camera Resolutions
- Calculations and normalizations of bounding boxes or calibration points must not hardcode static dimensions (such as 1280x720); they must dynamically adapt to actual frame dimensions.

### D. Documentation & Code Quality
- Keep code clean, readable, and avoid unnecessary complexity or obscure one-liners.
- When introducing new concepts or interfaces, update the documentation in the `docs/` folder.

---

## 3. Pull Request Workflow

1. **Create a branch:** Choose a descriptive branch name (e.g. `fix/camera-aspect-ratio` or `feat/cricket-cut-throat`).
2. **Verify builds:** Ensure the project compiles without errors:
   - Desktop: `./gradlew :composeApp:compileKotlinDesktop`
   - Android: `./gradlew :composeApp:compileDebugKotlin`
3. **Commit messages:** Write clear, concise commit messages (preferably following Conventional Commits, such as `fix: ...`, `feat: ...`, `docs: ...`).
4. **No secrets or local paths:** Ensure no private credentials, secrets, or machine-specific paths are committed.
5. **Open a Pull Request:** Provide a brief summary of the changes and how to test them.

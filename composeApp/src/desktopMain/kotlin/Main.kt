import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.jetbrains.JBR
import com.jetbrains.WindowDecorations
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sdart.app.App
import com.sdart.app.ui.LocalWindowControlsInset
import org.jetbrains.compose.resources.painterResource
import sdartapp.composeapp.generated.resources.Res
import sdartapp.composeapp.generated.resources.dart_app_logo

fun main(args: Array<String>) {
    val screenshotType = System.getProperty("screenshot.type") ?: args.firstOrNull { it.startsWith("--screenshot=") }?.substringAfter("--screenshot=")
    val screenshotOutput = System.getProperty("screenshot.output") ?: args.firstOrNull { it.startsWith("--output=") }?.substringAfter("--output=")

    if (screenshotType != null || screenshotOutput != null || args.contains("--screenshot")) {
        com.sdart.app.runScreenshotCapture(
            screenType = screenshotType ?: "bracket",
            outputFilePath = screenshotOutput ?: "tournament_preview.png"
        )
        return
    }

    // Native Hardware-Beschleunigung & Skiko Rendering je nach Betriebssystem optimieren
    try {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        when {
            os.contains("win") -> {
                System.setProperty("skiko.renderApi", "DIRECT3D")
                System.setProperty("sun.java2d.d3d", "true")
                System.setProperty("sun.java2d.noddraw", "true")
            }
            os.contains("mac") -> {
                System.setProperty("skiko.renderApi", "METAL")
            }
            os.contains("linux") -> {
                System.setProperty("skiko.renderApi", "OPENGL")
                System.setProperty("sun.java2d.opengl", "true")
            }
        }
    } catch (_: Throwable) {}

    // WICHTIG FÜR LINUX: ONNX Runtime VOR Compose/Skiko initialisieren
    try {
        ai.onnxruntime.OrtEnvironment.getEnvironment()
    } catch (e: Throwable) {
        e.printStackTrace()
    }

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        System.err.println("FATAL UNCAUGHT EXCEPTION on thread ${thread.name}: ${throwable.message}")
        throwable.printStackTrace()
        try {
            java.io.File("crash.log").printWriter().use { out ->
                out.println("FATAL UNCAUGHT EXCEPTION on thread ${thread.name}: ${throwable.message}")
                throwable.printStackTrace(out)
            }
        } catch (_: Exception) {}
    }

    application {
    val windowState = rememberWindowState(
        width = 1280.dp,
        height = 720.dp,
        position = WindowPosition(Alignment.Center)
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "S'Dart App",
        state = windowState,
        icon = painterResource(Res.drawable.dart_app_logo)
    ) {
        window.minimumSize = java.awt.Dimension(800, 600)
        val isMac = remember { System.getProperty("os.name")?.lowercase()?.contains("mac") == true }
        val titleBar = remember { if (isMac) null else installCustomTitleBar(window) }
        if (isMac) {
            // macOS: Inhalt bis in die Titelleiste, nur die Ampel-Knöpfe bleiben sichtbar.
            // Die Menüleiste und Screens ohne Leiste halten oben Abstand zu den Knöpfen.
            remember { installMacTransparentTitleBar(window) }
            CompositionLocalProvider(LocalWindowControlsInset provides MAC_TITLE_BAR_HEIGHT.dp) {
                Box(Modifier.fillMaxSize().macTitleBarDrag(window)) {
                    App()
                }
            }
        } else if (titleBar == null) {
            SideEffect { applyDarkTitleBar(window) }
            App()
        } else {
            // Keine sichtbare Titelleiste: die App reicht bis zum oberen Rand, Windows legt nur die
            // Fenster-Buttons darüber. Freie Flächen oben ziehen das Fenster, Bedienelemente bleiben klickbar.
            Box(Modifier.fillMaxSize().titleBarHitTest(titleBar)) {
                App()
            }
        }
    }
    }
}

private const val TITLE_BAR_HEIGHT = 32

/**
 * JetBrains Runtime: Fensterinhalt reicht bis in die Titelleiste, Windows zeichnet nur noch
 * Minimieren / Maximieren / Schließen (inkl. Snap-Layouts). Andere Laufzeiten → null.
 */
private fun installCustomTitleBar(window: java.awt.Window): WindowDecorations.CustomTitleBar? {
    if (System.getProperty("os.name")?.lowercase()?.contains("win") != true) return null
    return try {
        if (!JBR.isWindowDecorationsSupported()) return null
        val decorations = JBR.getWindowDecorations()
        decorations.createCustomTitleBar().also { titleBar ->
            titleBar.height = TITLE_BAR_HEIGHT.toFloat()
            titleBar.putProperty("controls.dark", true)
            decorations.setCustomTitleBar(window as java.awt.Frame, titleBar)
        }
    } catch (_: Throwable) {
        null
    }
}

/** Ereignisse, die kein Bedienelement verbraucht hat, gehören der Titelleiste (Ziehen, Doppelklick = Maximieren). */
private fun Modifier.titleBarHitTest(titleBar: WindowDecorations.CustomTitleBar): Modifier = pointerInput(titleBar) {
    awaitPointerEventScope {
        var inControl = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val consumed = event.changes.any { it.isConsumed }
            if (consumed && event.type == PointerEventType.Press) inControl = true
            titleBar.forceHitTest(consumed || inControl)
            if (event.type == PointerEventType.Release) inControl = false
        }
    }
}

private const val MAC_TITLE_BAR_HEIGHT = 28

private fun installMacTransparentTitleBar(window: javax.swing.JFrame) {
    window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
    window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
    window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
}

/** Ziehen an freien Stellen im oberen Streifen verschiebt das Fenster (Bedienelemente verbrauchen ihre Klicks selbst). */
private fun Modifier.macTitleBarDrag(window: java.awt.Window): Modifier = pointerInput(window) {
    val barHeight = MAC_TITLE_BAR_HEIGHT.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown()
        if (down.position.y > barHeight) return@awaitEachGesture
        val start = java.awt.MouseInfo.getPointerInfo()?.location ?: return@awaitEachGesture
        val origin = window.location
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val now = java.awt.MouseInfo.getPointerInfo()?.location ?: break
            // Absolut aus dem Startpunkt gerechnet: zieht macOS selbst mit, landet das Fenster an derselben Stelle
            window.setLocation(origin.x + now.x - start.x, origin.y + now.y - start.y)
        }
    }
}

private interface DwmApi : com.sun.jna.Library {
    fun DwmSetWindowAttribute(hwnd: com.sun.jna.Pointer, attr: Int, value: com.sun.jna.ptr.IntByReference, size: Int): Int
}

/** Windows 10/11: Titelleiste im dunklen App-Design (schwarz-grau, weiße Schrift) – bleibt die native Leiste. */
private fun applyDarkTitleBar(window: java.awt.Window) {
    if (System.getProperty("os.name")?.lowercase()?.contains("win") != true) return
    try {
        val hwnd = com.sun.jna.Native.getComponentPointer(window) ?: return
        val dwm = com.sun.jna.Native.load("dwmapi", DwmApi::class.java)
        fun set(attr: Int, v: Int) = dwm.DwmSetWindowAttribute(hwnd, attr, com.sun.jna.ptr.IntByReference(v), 4)
        set(20, 1)            // DWMWA_USE_IMMERSIVE_DARK_MODE
        set(35, 0x001A1A1A)   // DWMWA_CAPTION_COLOR (COLORREF 0x00BBGGRR), Windows 11
        set(36, 0x00FFFFFF)   // DWMWA_TEXT_COLOR
    } catch (_: Throwable) {}
}

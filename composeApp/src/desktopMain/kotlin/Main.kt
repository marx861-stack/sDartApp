import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sdart.app.App
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
        App()
    }
    }
}

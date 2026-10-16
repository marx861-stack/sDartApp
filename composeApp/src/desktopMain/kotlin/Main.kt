import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.myapplication.App

fun main(args: Array<String>) {
    val screenshotType = System.getProperty("screenshot.type") ?: args.firstOrNull { it.startsWith("--screenshot=") }?.substringAfter("--screenshot=")
    val screenshotOutput = System.getProperty("screenshot.output") ?: args.firstOrNull { it.startsWith("--output=") }?.substringAfter("--output=")

    if (screenshotType != null || screenshotOutput != null || args.contains("--screenshot")) {
        com.example.myapplication.runScreenshotCapture(
            screenType = screenshotType ?: "bracket",
            outputFilePath = screenshotOutput ?: "tournament_preview.png"
        )
        return
    }

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
        title = "s'Dart App",
        state = windowState
    ) {
        window.minimumSize = java.awt.Dimension(800, 600)
        App()
    }
    }
}

package com.example.myapplication

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.InputStream
import java.nio.FloatBuffer

/**
 * Native Desktop ONNX-Inferenz für Dart-Erkennung (RTMDet / YOLOX).
 * Verwendet standardisiertes BGR-Preprocessing mit Mittelwert/Std-Normalisierung identisch zur Python/Android-Pipeline.
 */
class OnnxDartDetector(
    private val modelResourceName: String = "rtmdet_tiny.onnx",
    val pipeline: DartDetectionPipeline = DartDetectionPipeline()
) {
    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var isInitialized = false

    private val inputSize = 640

    // RTMDet BGR Mean: [103.53, 116.28, 123.675]
    // RTMDet BGR Std:  [57.375, 57.12, 58.395]
    private val meanB = 103.53f
    private val meanG = 116.28f
    private val meanR = 123.675f
    private val normB = 1.0f / 57.375f
    private val normG = 1.0f / 57.12f
    private val normR = 1.0f / 58.395f

    private val sessionLock = Any()
    @Volatile
    private var isClosed = false

    @Synchronized
    fun initialize(): Boolean {
        if (isClosed) return false
        if (isInitialized && session != null) return true

        return try {
            val envInstance = OrtEnvironment.getEnvironment()
            this.env = envInstance

            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }

            val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream(modelResourceName)
                ?: javaClass.getResourceAsStream("/$modelResourceName")

            if (inputStream == null) {
                System.err.println("[OnnxDartDetector] Modell-Ressource nicht gefunden: $modelResourceName")
                return false
            }

            val modelBytes = inputStream.use { it.readBytes() }
            this.session = envInstance.createSession(modelBytes, sessionOptions)
            isInitialized = true
            println("[OnnxDartDetector] ONNX Modell erfolgreich geladen ($modelResourceName, ${modelBytes.size} Bytes)")
            true
        } catch (e: Exception) {
            System.err.println("[OnnxDartDetector] Fehler bei der Modell-Initialisierung: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Führt die Erkennung auf einem AWT BufferedImage aus (aus SARXOS Webcam).
     */
    fun detect(bufferedImage: BufferedImage): List<ObjectDetection> {
        if (isClosed) return emptyList()
        synchronized(sessionLock) {
            if (isClosed) return emptyList()
            if (!isInitialized && !initialize()) return emptyList()

            return try {
                val resized = BufferedImage(inputSize, inputSize, BufferedImage.TYPE_INT_RGB)
                val g: Graphics2D = resized.createGraphics()
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g.drawImage(bufferedImage, 0, 0, inputSize, inputSize, null)
                g.dispose()

                runInference(resized)
            } catch (e: Exception) {
                System.err.println("[OnnxDartDetector] Fehler bei der Inferenz: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Führt die Erkennung auf rohen JPEG-Bytes aus (z.B. aus MJPEG Stream).
     */
    fun detect(jpegBytes: ByteArray): List<ObjectDetection> {
        if (jpegBytes.isEmpty() || isClosed) return emptyList()
        return try {
            val bais = java.io.ByteArrayInputStream(jpegBytes)
            val readImg = javax.imageio.ImageIO.read(bais)
            if (readImg != null) {
                detect(readImg)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            System.err.println("[OnnxDartDetector] Fehler bei JPEG-Inferenz: ${e.message}")
            emptyList()
        }
    }

    private fun runInference(inputImage: BufferedImage): List<ObjectDetection> {
        val session = this.session ?: return emptyList()
        val env = this.env ?: return emptyList()

        val floatBuffer = FloatBuffer.allocate(1 * 3 * inputSize * inputSize)
        val channelSize = inputSize * inputSize

        // BGR Planar Layout mit Normalisierung
        val rgbPixels = IntArray(inputSize * inputSize)
        inputImage.getRGB(0, 0, inputSize, inputSize, rgbPixels, 0, inputSize)

        val bOffset = 0
        val gOffset = channelSize
        val rOffset = channelSize * 2

        for (i in 0 until channelSize) {
            val pixel = rgbPixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            floatBuffer.put(bOffset + i, (b - meanB) * normB)
            floatBuffer.put(gOffset + i, (g - meanG) * normG)
            floatBuffer.put(rOffset + i, (r - meanR) * normR)
        }
        floatBuffer.rewind()

        var inputTensor: OnnxTensor? = null
        var results: OrtSession.Result? = null
        return try {
            val tensorShape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
            inputTensor = OnnxTensor.createTensor(env, floatBuffer, tensorShape)

            val inputName = session.inputNames.iterator().next()
            val inputs = mapOf(inputName to inputTensor)

            results = session.run(inputs)
            val rawOutput = extractRawOutput(results)

            val parsed = pipeline.parseRawOutput(rawOutput, inputSize)
            val nms = pipeline.applyNMS(parsed)
            pipeline.filterStableDetections(nms)
        } catch (e: Exception) {
            System.err.println("[OnnxDartDetector] Fehler im Inferenz-Ablauf: ${e.message}")
            emptyList()
        } finally {
            try { inputTensor?.close() } catch (_: Exception) {}
            try { results?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Extrahiert die Inferenz-Ergebnisse und bringt sie in das Format:
     * [x1, y1, x2, y2, score, classId, ...]
     * Unterstützt MMDeploy RTMDet Format mit ('boxes', 'scores')
     */
    private fun extractRawOutput(results: OrtSession.Result): FloatArray {
        val outputList = mutableListOf<Float>()

        val boxesTensor = results.get("boxes").orElse(null) as? OnnxTensor
        val scoresTensor = results.get("scores").orElse(null) as? OnnxTensor

        if (boxesTensor != null && scoresTensor != null) {
            val boxesBuffer = boxesTensor.floatBuffer
            val scoresBuffer = scoresTensor.floatBuffer

            val totalBoxes = boxesBuffer.capacity() / 4
            val numClasses = scoresBuffer.capacity() / totalBoxes

            for (i in 0 until totalBoxes) {
                val boxOffset = i * 4
                val scoreOffset = i * numClasses

                var maxScore = -1.0f
                var maxClass = -1
                for (c in 0 until numClasses) {
                    val s = scoresBuffer.get(scoreOffset + c)
                    if (s > maxScore) {
                        maxScore = s
                        maxClass = c
                    }
                }

                if (maxScore > 0.1f) {
                    val x1 = boxesBuffer.get(boxOffset)
                    val y1 = boxesBuffer.get(boxOffset + 1)
                    val x2 = boxesBuffer.get(boxOffset + 2)
                    val y2 = boxesBuffer.get(boxOffset + 3)

                    outputList.add(x1)
                    outputList.add(y1)
                    outputList.add(x2)
                    outputList.add(y2)
                    outputList.add(maxScore)
                    outputList.add(maxClass.toFloat())
                }
            }
        }

        val resultArr = FloatArray(outputList.size)
        for (i in outputList.indices) {
            resultArr[i] = outputList[i]
        }
        return resultArr
    }

    /**
     * Schließt die ONNX-Session threadsicher und asynchron.
     * Blockiert nicht den UI-Thread und wartet sauber, bis laufende Inferenz-Calls abgeschlossen sind.
     * Schließt OrtEnvironment NICHT, da dies ein prozessweites Singleton ist.
     */
    fun close() {
        isClosed = true
        Thread {
            synchronized(sessionLock) {
                try {
                    isInitialized = false
                    val sess = session
                    session = null
                    sess?.close()
                    env = null
                    println("[OnnxDartDetector] ONNX-Session sauber geschlossen.")
                } catch (e: Exception) {
                    System.err.println("[OnnxDartDetector] Fehler beim Schließen der ONNX-Session: ${e.message}")
                }
            }
        }.apply {
            isDaemon = true
            name = "onnx-detector-closer"
            start()
        }
    }
}

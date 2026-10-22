package com.sdart.app

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import androidx.compose.ui.geometry.Rect
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.InputStream
import java.nio.FloatBuffer

/**
 * Native Desktop ONNX-Inferenz für Dart-Erkennung (RTMDet / YOLOX).
 * Verwendet standardisiertes Top-Left Letterbox (Grau 114,114,114), Dynamic Auto-Tracking ROI (30% Padding)
 * und BGR-Preprocessing mit Mittelwert/Std-Normalisierung 1:1 identisch zur Android/Original-Pipeline.
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

    @Volatile
    var lastInferenceTimeMs: Long = 0L
        private set

    val dynamicRoi: Rect?
        get() = pipeline.normalizedDynamicRoi

    // Wiederverwendbarer 640x640 Puffer zur Vermeidung von Allokationen pro Frame
    private val scaledImage = BufferedImage(inputSize, inputSize, BufferedImage.TYPE_INT_RGB)
    private val rgbPixels = IntArray(inputSize * inputSize)
    private val floatBuffer = FloatBuffer.allocate(1 * 3 * inputSize * inputSize)

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
     * Führt die Erkennung auf einem AWT BufferedImage aus (aus SARXOS Webcam oder MJPEG-Stream).
     * Verwendet Top-Left Letterbox (114, 114, 114) + Dynamic ROI Zoom identisch zur Android-App.
     */
    fun detect(bufferedImage: BufferedImage): List<ObjectDetection> {
        if (isClosed) return emptyList()
        synchronized(sessionLock) {
            if (isClosed) return emptyList()
            if (!isInitialized && !initialize()) return emptyList()

            return try {
                val startTime = System.currentTimeMillis()
                val imgW = bufferedImage.width
                val imgH = bufferedImage.height
                if (imgW <= 0 || imgH <= 0) return emptyList()

                val cropParams = pipeline.computeCropParams(imgW, imgH, inputSize)

                val g: Graphics2D = scaledImage.createGraphics()
                g.color = Color(114, 114, 114)
                g.fillRect(0, 0, inputSize, inputSize)
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

                val scaleTotal = (cropParams.baseRatio * cropParams.roiScale).toDouble()
                val tx = (-cropParams.frameSafeRoi.left * cropParams.roiScale + cropParams.roiOffsetX).toDouble()
                val ty = (-cropParams.frameSafeRoi.top * cropParams.roiScale + cropParams.roiOffsetY).toDouble()
                val transform = AffineTransform(scaleTotal, 0.0, 0.0, scaleTotal, tx, ty)

                g.drawImage(bufferedImage, transform, null)
                g.dispose()

                val rawDetections = runInference(scaledImage)
                val afterNms = pipeline.applyNMS(rawDetections)
                // Erst in den Vollbild-Raum zurückrechnen, dann zeitlich filtern (ROI wandert zwischen Frames)
                val mapped = pipeline.mapDetectionsAndUpdateDynamicRoi(afterNms, cropParams, inputSize)
                val stableDetections = pipeline.filterStableDetections(mapped)

                lastInferenceTimeMs = System.currentTimeMillis() - startTime
                stableDetections
            } catch (e: Exception) {
                System.err.println("[OnnxDartDetector] Fehler bei der Inferenz: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Führt die Erkennung auf rohen JPEG-Bytes aus (z.B. aus MJPEG Stream oder Companion-Stream).
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

        val channelSize = inputSize * inputSize
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

            pipeline.parseRawOutput(rawOutput, inputSize)
        } catch (e: Exception) {
            System.err.println("[OnnxDartDetector] Fehler im Inferenz-Ablauf: ${e.message}")
            emptyList()
        } finally {
            try { inputTensor?.close() } catch (_: Exception) {}
            try { results?.close() } catch (_: Exception) {}
        }
    }

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

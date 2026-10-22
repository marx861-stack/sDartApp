package com.sdart.app

import com.sdart.app.core.detection.*

import android.content.Context
import android.graphics.Bitmap
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.ui.geometry.Rect
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

/**
 * CameraX and NCNN Vulkan RTMDet ML Pipeline for Android.
 * Integrates directly with the shared DartDetectionPipeline in commonMain.
 */
class MLDartDetector(
    private val context: Context,
    val pipeline: DartDetectionPipeline = DartDetectionPipeline()
) {
    companion object {
        private const val TAG = "MLDartDetector"
        private const val INPUT_SIZE = 640
        private const val MAX_ZOOM_RESTORE_ATTEMPTS = 15
        private const val ZOOM_RESTORE_RETRY_MS = 200L
        
        // Single thread executor to prevent OpenMP crashes in C++
        private val sharedCameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    // Camera components
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null
    private val cameraExecutor = sharedCameraExecutor
    
    // State flows
    private val _previewView = MutableStateFlow<androidx.camera.view.PreviewView?>(null)
    val previewView: StateFlow<androidx.camera.view.PreviewView?> = _previewView
    
    private val _detections = MutableStateFlow<List<ObjectDetection>>(emptyList())
    val detections: StateFlow<List<ObjectDetection>> = _detections
    
    private val _inferenceTimeMs = MutableStateFlow<Long>(0L)
    val inferenceTimeMs: StateFlow<Long> = _inferenceTimeMs
    
    private val _framesPerSecond = MutableStateFlow<Int>(0)
    val framesPerSecond: StateFlow<Int> = _framesPerSecond
    
    private val _gpuActive = MutableStateFlow<Boolean>(false)
    val gpuActive: StateFlow<Boolean> = _gpuActive
    
    private val _gpuInitFailed = MutableStateFlow<Boolean>(false)
    val gpuInitFailed: StateFlow<Boolean> = _gpuInitFailed
    
    private val _dynamicRoiState = MutableStateFlow<Rect?>(null)
    val dynamicRoiState: StateFlow<Rect?> = _dynamicRoiState

    private val _frameSize = MutableStateFlow(Pair(0, 0))
    val frameSize: StateFlow<Pair<Int, Int>> = _frameSize
    
    private val _zoomState = MutableStateFlow<ZoomState?>(null)
    val zoomState: StateFlow<ZoomState?> = _zoomState
    
    private var processedFramesCount = 0
    
    // Model components
    private var isModelLoaded = false
    private var ncnnDetector: NCNNDetector? = null
    
    // Image processing pipeline
    private var inputFrameBitmap: Bitmap? = null
    private var streamingBitmap: Bitmap? = null
    private var streamingRotatedBitmap: Bitmap? = null
    private var streamingRotatedCanvas: android.graphics.Canvas? = null
    private var scaledBitmap: Bitmap? = null
    private var scaledCanvas: android.graphics.Canvas? = null
    private val scaledPaint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    private val frameMatrix = android.graphics.Matrix()
    
    var isCameraActive = false
        private set

    private var currentLifecycleOwner: LifecycleOwner? = null
    var inferenceEnabled: Boolean = true
    private var currentZoom = 0f
    private var currentZoomRatio = 1.0f
    private var isRestoringZoom = false
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var lastProcessedFrameTime = 0L
    private var frameListener: ((Bitmap) -> Unit)? = null

    fun setFrameListener(listener: ((Bitmap) -> Unit)?) {
        frameListener = listener
    }

    private fun initializeModel() {
        try {
            processedFramesCount = 0
            ncnnDetector?.destroy()
            ncnnDetector = NCNNDetector()
            
            val initSuccess = ncnnDetector!!.init(context.assets, true)
            if (initSuccess) {
                isModelLoaded = true
                _gpuActive.value = true
                Log.d(TAG, "NCNN Vulkan GPU successfully initialized")
                
                scaledBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
                scaledCanvas = android.graphics.Canvas(scaledBitmap!!)
            } else {
                Log.e(TAG, "NCNN initialization failed")
                _gpuInitFailed.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing NCNN", e)
            _gpuInitFailed.value = true
        }
    }
    
    fun startCamera(lifecycleOwner: LifecycleOwner) {
        if (isCameraActive) {
            stopCamera()
        }
        
        currentLifecycleOwner = lifecycleOwner
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProvider = cameraProviderFuture.get()
                
                if (cameraProvider != null) {
                    bindCameraUseCases(lifecycleOwner)
                    isCameraActive = true
                    reconnectPreviewView()
                    keepScreenOn(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start camera", e)
                isCameraActive = false
                keepScreenOn(false)
            }
        }
    }
    
    fun setZoomRatio(zoomRatio: Float) {
        currentZoomRatio = zoomRatio
        isRestoringZoom = false
        if (!isCameraActive || camera == null) return
        try {
            camera?.cameraControl?.setZoomRatio(zoomRatio)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set zoom ratio", e)
        }
    }
    
    fun setZoom(linearZoom: Float) {
        currentZoom = linearZoom
        // Linearer Zoom hat Vorrang; eine alte Ratio darf beim Kamerastart nicht darüber gewinnen
        currentZoomRatio = 1.0f
        isRestoringZoom = false
        if (!isCameraActive || camera == null) return
        
        try {
            camera?.cameraControl?.setLinearZoom(linearZoom)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set linear zoom", e)
        }
    }

    fun resetZoom() {
        currentZoom = 0f
        currentZoomRatio = 1.0f
        isRestoringZoom = false
        if (!isCameraActive || camera == null) return
        try {
            val state = camera?.cameraInfo?.zoomState?.value
            val targetRatio = if (state != null) {
                1.0f.coerceIn(state.minZoomRatio, state.maxZoomRatio)
            } else {
                1.0f
            }
            camera?.cameraControl?.setZoomRatio(targetRatio)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset zoom ratio", e)
        }
    }

    /**
     * Wendet den gespeicherten Zoom an, sobald die Kamera ihn akzeptiert. Vor dem Öffnen der Kamera
     * schlagen Zoom-Requests fehl, daher wird bis zu MAX_ZOOM_RESTORE_ATTEMPTS mal erneut versucht.
     */
    private fun applyRestoredZoom(cam: Camera, attempt: Int) {
        if (!isRestoringZoom || camera !== cam) return
        val future = try {
            if (currentZoomRatio > 1.01f) {
                cam.cameraControl.setZoomRatio(currentZoomRatio)
            } else {
                cam.cameraControl.setLinearZoom(currentZoom)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore zoom", e)
            null
        }

        val onDone = Runnable {
            if (!isRestoringZoom || camera !== cam) return@Runnable
            val applied = future != null && try {
                future.get()
                true
            } catch (e: Exception) {
                false
            }
            if (applied || attempt >= MAX_ZOOM_RESTORE_ATTEMPTS) {
                if (!applied) Log.w(TAG, "Zoom restore gave up after $attempt retries")
                isRestoringZoom = false
                cam.cameraInfo.zoomState.value?.let { publishZoomState(it) }
            } else {
                mainHandler.postDelayed({ applyRestoredZoom(cam, attempt + 1) }, ZOOM_RESTORE_RETRY_MS)
            }
        }
        if (future != null) {
            future.addListener(onDone, ContextCompat.getMainExecutor(context))
        } else {
            mainHandler.post(onDone)
        }
    }

    private fun publishZoomState(state: ZoomState) {
        _zoomState.value = state
        currentZoom = state.linearZoom
        currentZoomRatio = state.zoomRatio
    }

    fun stopCamera() {
        if (!isCameraActive) return
        
        try {
            currentLifecycleOwner?.let { owner ->
                camera?.cameraInfo?.zoomState?.removeObservers(owner)
            }
            cameraProvider?.unbindAll()
            camera = null
            preview = null
            imageAnalysis = null
            cameraProvider = null
            currentLifecycleOwner = null
            isCameraActive = false
            isRestoringZoom = false
            _zoomState.value = null
            keepScreenOn(false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop camera", e)
        }
    }

    private fun findActivity(context: Context): android.app.Activity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }

    private fun keepScreenOn(enabled: Boolean) {
        val activity = findActivity(context) ?: return
        activity.runOnUiThread {
            try {
                if (enabled) {
                    activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set keepScreenOn to $enabled", e)
            }
        }
    }
    
    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner) {
        val cameraProvider = cameraProvider ?: return
        
        try {
            cameraProvider.unbindAll()
            
            val resolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                )
                .build()

            Log.d(TAG, "Binding camera use cases with ResolutionSelector (Target 1280x720, 16:9)")
            
            val previewBuilder = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .setTargetRotation(Surface.ROTATION_0)
                
            Camera2Interop.Extender(previewBuilder)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(15, 15))
                
            preview = previewBuilder.build()
        
            val analysisBuilder = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                
            Camera2Interop.Extender(analysisBuilder)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(15, 15))
                
            imageAnalysis = analysisBuilder.build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }
        
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
            
            isRestoringZoom = currentZoomRatio > 1.01f || currentZoom > 0.001f
            if (isRestoringZoom) {
                camera?.let { applyRestoredZoom(it, attempt = 0) }
            }

            camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { state ->
                // Solange der gespeicherte Zoom noch nicht bestätigt angewendet wurde, keine Zwischenzustände
                // (z.B. 1.0x beim Öffnen) übernehmen – sie würden den gemerkten Zoom überschreiben.
                if (isRestoringZoom) return@observe
                publishZoomState(state)
            }
            
            val currentPreviewView = _previewView.value
            if (currentPreviewView != null && preview != null) {
                preview?.setSurfaceProvider(currentPreviewView.surfaceProvider)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind camera use cases", e)
        }
    }
    
    private fun processImage(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastProcessedFrameTime < 100) { // Limit to ~10 FPS for battery efficiency
            imageProxy.close()
            return
        }
        val delta = currentTime - lastProcessedFrameTime
        if (lastProcessedFrameTime > 0L && delta > 0) {
            _framesPerSecond.value = (1000 / delta).toInt()
        }
        lastProcessedFrameTime = currentTime
        
        try {
            if (frameListener != null) {
                val bitmap = imageProxyToBitmap(imageProxy)
                if (bitmap != null) {
                    frameListener?.invoke(bitmap)
                }
            }
            
            if (inferenceEnabled) {
                if (!isModelLoaded) {
                    synchronized(this) {
                        if (!isModelLoaded) {
                            initializeModel()
                        }
                    }
                }
                
                if (isModelLoaded && ncnnDetector != null) {
                    val detections = performInferenceZeroAlloc(imageProxy)
                    _detections.value = detections
                } else {
                    _detections.value = emptyList()
                }
            } else {
                _detections.value = emptyList()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error in analyzeImage", e)
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Führt die NCNN-Inferenz auf einem beliebigen Android Bitmap aus (z.B. aus einem Netzwerk-Stream).
     * Verwendet exakt dieselbe Top-Left Letterbox + Dynamic ROI Pipeline wie die lokale Kamera.
     */
    fun detectBitmap(bitmap: Bitmap): List<ObjectDetection> {
        if (!inferenceEnabled) {
            _detections.value = emptyList()
            return emptyList()
        }

        if (!isModelLoaded) {
            synchronized(this) {
                if (!isModelLoaded) {
                    initializeModel()
                }
            }
        }

        if (!isModelLoaded || ncnnDetector == null) {
            _detections.value = emptyList()
            return emptyList()
        }

        return try {
            val startTime = System.currentTimeMillis()
            val delta = startTime - lastProcessedFrameTime
            if (lastProcessedFrameTime > 0L && delta > 0) {
                _framesPerSecond.value = (1000 / delta).toInt()
            }
            lastProcessedFrameTime = startTime

            val rotatedWidth = bitmap.width
            val rotatedHeight = bitmap.height
            if (rotatedWidth <= 0 || rotatedHeight <= 0) return emptyList()

            if (_frameSize.value.first != rotatedWidth || _frameSize.value.second != rotatedHeight) {
                _frameSize.value = Pair(rotatedWidth, rotatedHeight)
            }

            val mappedDetections = synchronized(this) {
                val cropParams = pipeline.computeCropParams(rotatedWidth, rotatedHeight, INPUT_SIZE)

                if (scaledBitmap == null) {
                    scaledBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
                    scaledCanvas = android.graphics.Canvas(scaledBitmap!!)
                }

                scaledCanvas?.drawColor(android.graphics.Color.rgb(114, 114, 114))
                frameMatrix.reset()
                frameMatrix.postScale(cropParams.baseRatio, cropParams.baseRatio)
                frameMatrix.postTranslate(-cropParams.frameSafeRoi.left, -cropParams.frameSafeRoi.top)
                frameMatrix.postScale(cropParams.roiScale, cropParams.roiScale)
                frameMatrix.postTranslate(cropParams.roiOffsetX, cropParams.roiOffsetY)

                scaledCanvas?.drawBitmap(bitmap, frameMatrix, scaledPaint)

                val rawOutput = scaledBitmap?.let { bmp ->
                    ncnnDetector?.detect(bmp)
                } ?: FloatArray(0)

                val rawDetections = pipeline.parseRawOutput(rawOutput, INPUT_SIZE)
                val afterNms = pipeline.applyNMS(rawDetections)
                // Erst in den Vollbild-Raum zurückrechnen, dann zeitlich filtern: die Dynamic ROI wandert,
                // daher sind Tensor-Koordinaten verschiedener Frames nicht vergleichbar.
                val mapped = pipeline.mapDetectionsAndUpdateDynamicRoi(afterNms, cropParams, INPUT_SIZE)
                pipeline.filterStableDetections(mapped)
            }

            _dynamicRoiState.value = pipeline.normalizedDynamicRoi
            _detections.value = mappedDetections
            _inferenceTimeMs.value = System.currentTimeMillis() - startTime
            mappedDetections
        } catch (e: Exception) {
            Log.e(TAG, "Error in detectBitmap", e)
            _detections.value = emptyList()
            emptyList()
        }
    }

    private fun performInferenceZeroAlloc(imageProxy: ImageProxy): List<ObjectDetection> {
        val startTime = System.currentTimeMillis()
        return try {
            val t0 = System.currentTimeMillis()
            
            // 1. Zero-Allocation Pixel Copy
            val buffer = imageProxy.planes[0].buffer
            val pixelStride = imageProxy.planes[0].pixelStride
            val rowStride = imageProxy.planes[0].rowStride
            val rowPadding = rowStride - pixelStride * imageProxy.width
            val requiredWidth = imageProxy.width + rowPadding / pixelStride
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val rotatedWidth = if (rotationDegrees % 180 != 0) imageProxy.height else imageProxy.width
            val rotatedHeight = if (rotationDegrees % 180 != 0) imageProxy.width else imageProxy.height

            if (_frameSize.value.first != rotatedWidth || _frameSize.value.second != rotatedHeight) {
                _frameSize.value = Pair(rotatedWidth, rotatedHeight)
            }

            var t1 = t0
            var t2 = t0
            val mappedDetections = synchronized(this) {
                if (inputFrameBitmap == null || inputFrameBitmap!!.width != requiredWidth || inputFrameBitmap!!.height != imageProxy.height) {
                    inputFrameBitmap = Bitmap.createBitmap(requiredWidth, imageProxy.height, Bitmap.Config.ARGB_8888)
                }
                inputFrameBitmap!!.copyPixelsFromBuffer(buffer)
                
                // 2. Hardwarenahe Matrix-Transformation (Crop, Scale, Rotate)
                scaledCanvas?.drawColor(android.graphics.Color.rgb(114, 114, 114))
                frameMatrix.reset()

                val cropParams = pipeline.computeCropParams(rotatedWidth, rotatedHeight, INPUT_SIZE)
                
                frameMatrix.postTranslate(-imageProxy.width / 2f, -imageProxy.height / 2f)
                frameMatrix.postRotate(rotationDegrees.toFloat())
                frameMatrix.postTranslate(rotatedWidth / 2f, rotatedHeight / 2f)
                frameMatrix.postScale(cropParams.baseRatio, cropParams.baseRatio)
                frameMatrix.postTranslate(-cropParams.frameSafeRoi.left, -cropParams.frameSafeRoi.top)
                frameMatrix.postScale(cropParams.roiScale, cropParams.roiScale)
                frameMatrix.postTranslate(cropParams.roiOffsetX, cropParams.roiOffsetY)
                
                scaledCanvas?.drawBitmap(inputFrameBitmap!!, frameMatrix, scaledPaint)
                
                t1 = System.currentTimeMillis()
                val rawOutput = scaledBitmap?.let { bmp ->
                    ncnnDetector?.detect(bmp)
                } ?: FloatArray(0)
                t2 = System.currentTimeMillis()
                
                val rawDetections = pipeline.parseRawOutput(rawOutput, INPUT_SIZE)
                val afterNms = pipeline.applyNMS(rawDetections)
                // Erst in den Vollbild-Raum zurückrechnen, dann zeitlich filtern: die Dynamic ROI wandert,
                // daher sind Tensor-Koordinaten verschiedener Frames nicht vergleichbar.
                val mapped = pipeline.mapDetectionsAndUpdateDynamicRoi(afterNms, cropParams, INPUT_SIZE)
                pipeline.filterStableDetections(mapped)
            }
            
            _dynamicRoiState.value = pipeline.normalizedDynamicRoi
            
            val t3 = System.currentTimeMillis()
            val totalTime = t3 - startTime
            val cropTime = t1 - t0
            val inferenceTime = t2 - t1
            val postTime = t3 - t2
            
            _inferenceTimeMs.value = totalTime
            if (totalTime > 0 && processedFramesCount % 30 == 0) {
                Log.d(TAG, "Timing - Prep: ${cropTime}ms | Inf: ${inferenceTime}ms | Post: ${postTime}ms | Total: ${totalTime}ms (GPU: ${_gpuActive.value})")
            }
            
            processedFramesCount++
            mappedDetections
        } catch (e: Exception) {
            Log.e(TAG, "Error during zero-alloc inference", e)
            emptyList()
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val buffer = imageProxy.planes[0].buffer
            val pixelStride = imageProxy.planes[0].pixelStride
            val rowStride = imageProxy.planes[0].rowStride
            val rowPadding = rowStride - pixelStride * imageProxy.width
            val requiredWidth = imageProxy.width + rowPadding / pixelStride
            
            if (streamingBitmap == null || streamingBitmap!!.width != requiredWidth || streamingBitmap!!.height != imageProxy.height) {
                streamingBitmap = Bitmap.createBitmap(
                    requiredWidth,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            }
            streamingBitmap!!.copyPixelsFromBuffer(buffer)
            
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            // Auch ohne Rotation umkopieren, wenn der Puffer Row-Padding hat: streamingBitmap ist dann
            // breiter als das eigentliche Bild, das Zeichnen in die Zielgröße schneidet das Padding ab.
            if (rotationDegrees != 0 || rowPadding != 0) {
                val targetW = if (rotationDegrees % 180 != 0) imageProxy.height else imageProxy.width
                val targetH = if (rotationDegrees % 180 != 0) imageProxy.width else imageProxy.height
                
                if (streamingRotatedBitmap == null || streamingRotatedBitmap!!.width != targetW || streamingRotatedBitmap!!.height != targetH) {
                    streamingRotatedBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    streamingRotatedCanvas = android.graphics.Canvas(streamingRotatedBitmap!!)
                }
                
                val matrix = android.graphics.Matrix().apply {
                    postTranslate(-imageProxy.width / 2f, -imageProxy.height / 2f)
                    postRotate(rotationDegrees.toFloat())
                    postTranslate(targetW / 2f, targetH / 2f)
                }
                streamingRotatedCanvas?.drawBitmap(streamingBitmap!!, matrix, null)
                streamingRotatedBitmap
            } else {
                streamingBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e)
            null
        }
    }
    
    fun reconnectPreviewView() {
        val currentPreviewView = _previewView.value
        val currentPreview = preview
        
        if (currentPreviewView != null && currentPreview != null && isCameraActive) {
            try {
                currentPreview.setSurfaceProvider(currentPreviewView.surfaceProvider)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reconnect PreviewView", e)
            }
        }
    }
    
    fun setPreviewView(previewView: androidx.camera.view.PreviewView) {
        _previewView.value = previewView
        if (isCameraActive && preview != null) {
            preview?.setSurfaceProvider(previewView.surfaceProvider)
        }
    }
    
    fun shutdown() {
        stopCamera()
        synchronized(this) {
            try {
                pipeline.reset()
                ncnnDetector?.destroy()
                ncnnDetector = null
                isModelLoaded = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close NCNN detector", e)
            }
        }
        _previewView.value = null
        _detections.value = emptyList()
    }
}

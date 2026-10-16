package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.os.BatteryManager
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
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

/**
 * CameraX and NCNN Vulkan RTMDet ML Pipeline for Android.
 * Integrates directly with the multiplatform ObjectDetection and AutoDetectionSystem.
 */
class MLDartDetector(private val context: Context) {
    companion object {
        private const val TAG = "MLDartDetector"
        private const val BOARD_CONFIDENCE_THRESHOLD = 0.50f
        private const val DART_CONFIDENCE_THRESHOLD = 0.50f
        private const val TIP_CONFIDENCE_THRESHOLD = 0.50f
        private const val MARKER_CONFIDENCE_THRESHOLD = 0.50f
        private const val NMS_THRESHOLD = 0.65f
        private const val CENTER_DISTANCE_THRESHOLD = 0.008f
        private const val INPUT_SIZE = 640
        private const val MAX_DARTBOARD_DETECTIONS = 20
        private const val MAX_DART_DETECTIONS = 20
        private const val MIN_BOX_SIZE = 0.005f
        
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
    
    private val _zoomState = MutableStateFlow<ZoomState?>(null)
    val zoomState: StateFlow<ZoomState?> = _zoomState
    
    private var processedFramesCount = 0
    
    // Model components
    private var isModelLoaded = false
    private var ncnnDetector: NCNNDetector? = null
    
    // Image processing pipeline
    private var inputFrameBitmap: Bitmap? = null
    private var scaledBitmap: Bitmap? = null
    private var scaledCanvas: android.graphics.Canvas? = null
    private val scaledPaint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    private val frameMatrix = android.graphics.Matrix()
    
    var isCameraActive = false
        private set

    private var currentLifecycleOwner: LifecycleOwner? = null
    var inferenceEnabled: Boolean = true
    private var currentZoom = 0f
    
    // Target ROI for cropping
    private var targetCropRect: Rect? = null
    
    // Dynamic Auto-Tracking ROI
    private var currentDynamicRoi: Rect? = null
    private var framesWithoutBoard = 0
    private val ROI_PADDING = 0.30f
    
    private val batteryManager by lazy { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }

    fun setTargetCropRegion(rectIn640Space: Rect?) {
        targetCropRect = rectIn640Space
    }

    // Temporal filtering for stability
    private val detectionHistory = mutableListOf<List<ObjectDetection>>()
    private val HISTORY_SIZE = 5
    private val CONFIRMATION_THRESHOLD_FRAMES = 3
    
    private var lastProcessedFrameTime = 0L
    private var frameListener: ((Bitmap) -> Unit)? = null
    private var lastKnownDartboardBox: Rect? = null

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
        if (!isCameraActive || camera == null) return
        try {
            camera?.cameraControl?.setZoomRatio(zoomRatio)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set zoom ratio", e)
        }
    }
    
    fun setZoom(linearZoom: Float) {
        currentZoom = linearZoom
        if (!isCameraActive || camera == null) return
        
        try {
            camera?.cameraControl?.setLinearZoom(linearZoom)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set linear zoom", e)
        }
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
            
            if (currentZoom > 0f) {
                try {
                    camera?.cameraControl?.setLinearZoom(currentZoom)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore zoom level", e)
                }
            }
            
            var isRestoringZoom = currentZoom > 0f
            
            camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { state ->
                if (isRestoringZoom && state.linearZoom == 0f) {
                    return@observe
                }
                isRestoringZoom = false
                _zoomState.value = state
                currentZoom = state.linearZoom
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
     * Führt die NCNN-Inferenz auf einem beliebigen Android Bitmap aus (z.B. aus einem Netzwerk-Stream vom PC).
     * Mappt Detektionen auf normalisierte Koordinaten [0..1] unter Beibehaltung des Seitenverhältnisses (Letterbox).
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
            val targetWidth = bitmap.width.toFloat()
            val targetHeight = bitmap.height.toFloat()
            val baseRatio = min(INPUT_SIZE.toFloat() / targetWidth, INPUT_SIZE.toFloat() / targetHeight)
            val scaledW = targetWidth * baseRatio
            val scaledH = targetHeight * baseRatio
            val dx = (INPUT_SIZE - scaledW) / 2f
            val dy = (INPUT_SIZE - scaledH) / 2f

            val rawOutput = synchronized(this) {
                if (scaledBitmap == null) {
                    scaledBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
                    scaledCanvas = android.graphics.Canvas(scaledBitmap!!)
                }

                scaledCanvas?.drawColor(android.graphics.Color.rgb(114, 114, 114))
                frameMatrix.reset()
                frameMatrix.postScale(baseRatio, baseRatio)
                frameMatrix.postTranslate(dx, dy)

                scaledCanvas?.drawBitmap(bitmap, frameMatrix, scaledPaint)

                scaledBitmap?.let { bmp ->
                    ncnnDetector?.detect(bmp)
                } ?: FloatArray(0)
            }

            val rawDetections = processOutput(rawOutput)
            val afterNms = applyNMS(rawDetections)

            // Koordinaten zurückrechnen von Letterbox [0..640] auf Original [0..1]
            val mappedDetections = afterNms.map { det ->
                val box = det.boundingBox
                val pxLeft = box.left * INPUT_SIZE
                val pxTop = box.top * INPUT_SIZE
                val pxRight = box.right * INPUT_SIZE
                val pxBottom = box.bottom * INPUT_SIZE

                val origLeft = ((pxLeft - dx) / baseRatio / targetWidth).coerceIn(0f, 1f)
                val origTop = ((pxTop - dy) / baseRatio / targetHeight).coerceIn(0f, 1f)
                val origRight = ((pxRight - dx) / baseRatio / targetWidth).coerceIn(0f, 1f)
                val origBottom = ((pxBottom - dy) / baseRatio / targetHeight).coerceIn(0f, 1f)

                det.copy(boundingBox = Rect(origLeft, origTop, origRight, origBottom))
            }

            val stableDetections = filterStableDetections(mappedDetections)
            _detections.value = stableDetections
            _inferenceTimeMs.value = System.currentTimeMillis() - startTime
            stableDetections
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
            
            if (inputFrameBitmap == null || inputFrameBitmap!!.width != requiredWidth || inputFrameBitmap!!.height != imageProxy.height) {
                inputFrameBitmap = Bitmap.createBitmap(requiredWidth, imageProxy.height, Bitmap.Config.ARGB_8888)
            }
            inputFrameBitmap!!.copyPixelsFromBuffer(buffer)
            
            // 2. Hardwarenahe Matrix-Transformation (Crop, Scale, Rotate)
            scaledCanvas?.drawColor(android.graphics.Color.rgb(114, 114, 114))
            frameMatrix.reset()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            
            val rotatedWidth = if (rotationDegrees % 180 != 0) imageProxy.height else imageProxy.width
            val rotatedHeight = if (rotationDegrees % 180 != 0) imageProxy.width else imageProxy.height
            val baseRatio = min(INPUT_SIZE.toFloat() / rotatedWidth, INPUT_SIZE.toFloat() / rotatedHeight)
            
            frameMatrix.postTranslate(-imageProxy.width / 2f, -imageProxy.height / 2f)
            frameMatrix.postRotate(rotationDegrees.toFloat())
            frameMatrix.postTranslate(rotatedWidth / 2f, rotatedHeight / 2f)
            frameMatrix.postScale(baseRatio, baseRatio)
            
            // ROI mapping
            val baseRoi = Rect(0f, 0f, INPUT_SIZE.toFloat(), INPUT_SIZE.toFloat())
            val roi = currentDynamicRoi ?: baseRoi
            val safeLeft = max(0f, roi.left)
            val safeTop = max(0f, roi.top)
            val safeRight = min(INPUT_SIZE.toFloat(), roi.right)
            val safeBottom = min(INPUT_SIZE.toFloat(), roi.bottom)
            
            val frameSafeRoi = if (safeRight - safeLeft < 10f || safeBottom - safeTop < 10f) {
                Rect(0f, 0f, INPUT_SIZE.toFloat(), INPUT_SIZE.toFloat())
            } else {
                Rect(safeLeft, safeTop, safeRight, safeBottom)
            }
            
            frameMatrix.postTranslate(-frameSafeRoi.left, -frameSafeRoi.top)
            val roiScaleX = INPUT_SIZE.toFloat() / frameSafeRoi.width
            val roiScaleY = INPUT_SIZE.toFloat() / frameSafeRoi.height
            val roiScale = min(roiScaleX, roiScaleY)
            frameMatrix.postScale(roiScale, roiScale)
            
            val roiOffsetX = 0f
            val roiOffsetY = 0f
            frameMatrix.postTranslate(roiOffsetX, roiOffsetY)
            
            scaledCanvas?.drawBitmap(inputFrameBitmap!!, frameMatrix, scaledPaint)
            
            val t1 = System.currentTimeMillis()
            val rawOutput = synchronized(this) {
                scaledBitmap?.let { bmp ->
                    ncnnDetector?.detect(bmp)
                } ?: FloatArray(0)
            }
            val t2 = System.currentTimeMillis()
            
            val rawDetections = processOutput(rawOutput)
            val afterNms = applyNMS(rawDetections)
            val stableDetections = filterStableDetections(afterNms)
            
            // Map detections back to normalized full-frame coordinates
            val mappedDetections = stableDetections.map { det ->
                val box = det.boundingBox
                val leftZoomPx = box.left * INPUT_SIZE
                val topZoomPx = box.top * INPUT_SIZE
                val rightZoomPx = box.right * INPUT_SIZE
                val bottomZoomPx = box.bottom * INPUT_SIZE
                
                val leftOrigPx = (leftZoomPx - roiOffsetX) / roiScale + frameSafeRoi.left
                val topOrigPx = (topZoomPx - roiOffsetY) / roiScale + frameSafeRoi.top
                val rightOrigPx = (rightZoomPx - roiOffsetX) / roiScale + frameSafeRoi.left
                val bottomOrigPx = (bottomZoomPx - roiOffsetY) / roiScale + frameSafeRoi.top
                
                val finalLeft = ((leftOrigPx / baseRatio) / rotatedWidth).coerceIn(0f, 1f)
                val finalTop = ((topOrigPx / baseRatio) / rotatedHeight).coerceIn(0f, 1f)
                val finalRight = ((rightOrigPx / baseRatio) / rotatedWidth).coerceIn(0f, 1f)
                val finalBottom = ((bottomOrigPx / baseRatio) / rotatedHeight).coerceIn(0f, 1f)
                
                det.copy(boundingBox = Rect(finalLeft, finalTop, finalRight, finalBottom))
            }
            
            // Update dynamic ROI
            val boardDets = mappedDetections.filter { it.classId == 2 }
            val shouldUpdateRoi = (currentDynamicRoi == null && boardDets.size >= 20) || 
                                  (currentDynamicRoi != null && boardDets.isNotEmpty())
            
            if (shouldUpdateRoi) {
                framesWithoutBoard = 0
                var uLeft = boardDets[0].boundingBox.left
                var uTop = boardDets[0].boundingBox.top
                var uRight = boardDets[0].boundingBox.right
                var uBottom = boardDets[0].boundingBox.bottom
                for (det in boardDets) {
                    if (det.boundingBox.left < uLeft) uLeft = det.boundingBox.left
                    if (det.boundingBox.top < uTop) uTop = det.boundingBox.top
                    if (det.boundingBox.right > uRight) uRight = det.boundingBox.right
                    if (det.boundingBox.bottom > uBottom) uBottom = det.boundingBox.bottom
                }
                
                val cx = (uLeft + uRight) / 2f
                val cy = (uTop + uBottom) / 2f
                val scaledWidth = rotatedWidth * baseRatio
                val scaledHeight = rotatedHeight * baseRatio
                val w = (uRight - uLeft) * scaledWidth
                val h = (uBottom - uTop) * scaledHeight
                
                var paddedW = min(w * (1f + ROI_PADDING), scaledWidth)
                var paddedH = min(h * (1f + ROI_PADDING), scaledHeight)
                
                val targetLeft = cx * scaledWidth - paddedW / 2f
                val targetTop = cy * scaledHeight - paddedH / 2f
                val targetRight = cx * scaledWidth + paddedW / 2f
                val targetBottom = cy * scaledHeight + paddedH / 2f
                
                if (currentDynamicRoi == null) {
                    currentDynamicRoi = Rect(targetLeft, targetTop, targetRight, targetBottom)
                } else {
                    val alpha = 0.15f
                    val old = currentDynamicRoi!!
                    currentDynamicRoi = Rect(
                        old.left * (1 - alpha) + targetLeft * alpha,
                        old.top * (1 - alpha) + targetTop * alpha,
                        old.right * (1 - alpha) + targetRight * alpha,
                        old.bottom * (1 - alpha) + targetBottom * alpha
                    )
                }
            } else {
                framesWithoutBoard++
                if (framesWithoutBoard > 5) {
                    val targetLeft = 0f
                    val targetTop = 0f
                    val targetRight = rotatedWidth * baseRatio
                    val targetBottom = rotatedHeight * baseRatio
                    if (currentDynamicRoi != null) {
                        val alpha = 0.1f
                        val old = currentDynamicRoi!!
                        val updated = Rect(
                            old.left * (1 - alpha) + targetLeft * alpha,
                            old.top * (1 - alpha) + targetTop * alpha,
                            old.right * (1 - alpha) + targetRight * alpha,
                            old.bottom * (1 - alpha) + targetBottom * alpha
                        )
                        currentDynamicRoi = if (updated.width > targetRight * 0.95f && updated.height > targetBottom * 0.95f) {
                            null
                        } else {
                            updated
                        }
                    }
                }
            }
            
            if (currentDynamicRoi != null) {
                val scaledWidth = rotatedWidth * baseRatio
                val scaledHeight = rotatedHeight * baseRatio
                _dynamicRoiState.value = Rect(
                    currentDynamicRoi!!.left / scaledWidth,
                    currentDynamicRoi!!.top / scaledHeight,
                    currentDynamicRoi!!.right / scaledWidth,
                    currentDynamicRoi!!.bottom / scaledHeight
                )
            } else {
                _dynamicRoiState.value = null
            }
            
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
    
    private fun filterStableDetections(currentFrame: List<ObjectDetection>): List<ObjectDetection> {
        val boardDetections = currentFrame.filter { it.classId == 2 }
        if (boardDetections.isNotEmpty()) {
            var uLeft = boardDetections[0].boundingBox.left
            var uTop = boardDetections[0].boundingBox.top
            var uRight = boardDetections[0].boundingBox.right
            var uBottom = boardDetections[0].boundingBox.bottom
            for (det in boardDetections) {
                if (det.boundingBox.left < uLeft) uLeft = det.boundingBox.left
                if (det.boundingBox.top < uTop) uTop = det.boundingBox.top
                if (det.boundingBox.right > uRight) uRight = det.boundingBox.right
                if (det.boundingBox.bottom > uBottom) uBottom = det.boundingBox.bottom
            }
            lastKnownDartboardBox = Rect(uLeft, uTop, uRight, uBottom)
        }

        detectionHistory.add(currentFrame)
        if (detectionHistory.size > HISTORY_SIZE) {
            detectionHistory.removeAt(0)
        }
        
        return currentFrame.filter { current ->
            if (current.classId == 2) return@filter true 
            
            lastKnownDartboardBox?.let { boardBox ->
                val marginX = boardBox.width * 0.15f
                val marginY = boardBox.height * 0.15f
                val expandedBoard = Rect(
                    boardBox.left - marginX,
                    boardBox.top - marginY,
                    boardBox.right + marginX,
                    boardBox.bottom + marginY
                )
                val centerX = (current.boundingBox.left + current.boundingBox.right) / 2f
                val centerY = (current.boundingBox.top + current.boundingBox.bottom) / 2f
                if (!expandedBoard.contains(androidx.compose.ui.geometry.Offset(centerX, centerY))) {
                    return@filter false
                }
            }
            
            var counts = 0
            for (historyFrame in detectionHistory) {
                val match = historyFrame.any { prev ->
                    prev.classId == current.classId && 
                    calculateIoU(prev.boundingBox, current.boundingBox) > 0.5f
                }
                if (match) counts++
            }
            
            counts >= CONFIRMATION_THRESHOLD_FRAMES
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val buffer = imageProxy.planes[0].buffer
            val pixelStride = imageProxy.planes[0].pixelStride
            val rowStride = imageProxy.planes[0].rowStride
            val rowPadding = rowStride - pixelStride * imageProxy.width
            
            val bitmap = Bitmap.createBitmap(
                imageProxy.width + rowPadding / pixelStride,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            if (rotationDegrees != 0) {
                val matrix = android.graphics.Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                Bitmap.createBitmap(
                    bitmap, 0, 0, imageProxy.width, imageProxy.height, matrix, true
                )
            } else {
                if (rowPadding == 0) bitmap else Bitmap.createBitmap(bitmap, 0, 0, imageProxy.width, imageProxy.height)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e)
            null
        }
    }
    
    private fun processOutput(rawOutput: FloatArray): List<ObjectDetection> {
        val detections = mutableListOf<ObjectDetection>()
        if (rawOutput.isEmpty()) return emptyList()
        
        val numDetections = rawOutput.size / 6
        var isPixelCoordinates = false
        for (i in 0 until min(numDetections, 100)) {
            val idx = i * 6
            if (idx + 3 < rawOutput.size) {
                if (rawOutput[idx] > 2f || rawOutput[idx+1] > 2f || rawOutput[idx+2] > 2f || rawOutput[idx+3] > 2f) {
                    isPixelCoordinates = true
                    break
                }
            }
        }
        
        for (detectionIdx in 0 until numDetections) {
            val baseIndex = detectionIdx * 6
            if (baseIndex + 5 >= rawOutput.size) continue
            val confidence = rawOutput[baseIndex + 4]
            val bestClassId = rawOutput[baseIndex + 5].toInt()
            val threshold = when (bestClassId) {
                3 -> MARKER_CONFIDENCE_THRESHOLD
                2 -> BOARD_CONFIDENCE_THRESHOLD
                1 -> TIP_CONFIDENCE_THRESHOLD
                else -> DART_CONFIDENCE_THRESHOLD
            }
            
            if (confidence < threshold) continue
            
            val scale = if (isPixelCoordinates) INPUT_SIZE.toFloat() else 1f
            val xMin = (rawOutput[baseIndex]     / scale).coerceIn(0f, 1f)
            val yMin = (rawOutput[baseIndex + 1] / scale).coerceIn(0f, 1f)
            val xMax = (rawOutput[baseIndex + 2] / scale).coerceIn(0f, 1f)
            val yMax = (rawOutput[baseIndex + 3] / scale).coerceIn(0f, 1f)
            
            if (xMax - xMin < MIN_BOX_SIZE || yMax - yMin < MIN_BOX_SIZE) continue
            
            detections.add(ObjectDetection(
                boundingBox = Rect(xMin, yMin, xMax, yMax),
                confidence = confidence,
                classId = bestClassId,
                className = getDartClassName(bestClassId)
            ))
        }
        return detections
    }
    
    private fun getDartClassName(classId: Int): String {
        val dartClasses = arrayOf("Dart", "Spitze", "Dartboard", "Marker")
        return if (classId in dartClasses.indices) dartClasses[classId] else "unknown_$classId"
    }
    
    private fun applyNMS(detections: List<ObjectDetection>): List<ObjectDetection> {
        if (detections.size <= 1) return detections

        val groupedByClass = detections.groupBy { it.classId }
        val finalSelected = mutableListOf<ObjectDetection>()

        for ((classId, classDetections) in groupedByClass) {
            val maxKeep = if (classId == 2) MAX_DARTBOARD_DETECTIONS else MAX_DART_DETECTIONS
            val sorted = classDetections.sortedByDescending { it.confidence }
            val selected = mutableListOf<ObjectDetection>()

            for (detection in sorted) {
                var shouldAdd = true
                for (sel in selected) {
                    val isDuplicate = if (classId == 1) {
                        calculateCenterDistance(detection.boundingBox, sel.boundingBox) < CENTER_DISTANCE_THRESHOLD
                    } else {
                        calculateIoU(detection.boundingBox, sel.boundingBox) > NMS_THRESHOLD
                    }
                    if (isDuplicate) {
                        shouldAdd = false
                        break
                    }
                }
                if (shouldAdd) {
                    selected.add(detection)
                    if (selected.size >= maxKeep) break
                }
            }
            finalSelected.addAll(selected)
        }
        return finalSelected
    }

    private fun calculateCenterDistance(box1: Rect, box2: Rect): Float {
        val cx1 = (box1.left + box1.right) / 2f
        val cy1 = (box1.top + box1.bottom) / 2f
        val cx2 = (box2.left + box2.right) / 2f
        val cy2 = (box2.top + box2.bottom) / 2f
        val dx = cx1 - cx2
        val dy = cy1 - cy2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    private fun calculateIoU(box1: Rect, box2: Rect): Float {
        val intersectionLeft = max(box1.left, box2.left)
        val intersectionTop = max(box1.top, box2.top)
        val intersectionRight = min(box1.right, box2.right)
        val intersectionBottom = min(box1.bottom, box2.bottom)
        
        if (intersectionRight <= intersectionLeft || intersectionBottom <= intersectionTop) {
            return 0f
        }
        
        val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = box1Area + box2Area - intersectionArea
        
        return if (unionArea > 0) intersectionArea / unionArea else 0f
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
        try {
            ncnnDetector?.destroy()
            ncnnDetector = null
            isModelLoaded = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close NCNN detector", e)
        }
        _previewView.value = null
        _detections.value = emptyList()
    }
}

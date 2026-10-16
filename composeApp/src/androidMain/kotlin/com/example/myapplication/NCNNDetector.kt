package com.example.myapplication

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log

class NCNNDetector {
    companion object {
        private const val TAG = "NCNNDetector"
        init {
            try {
                System.loadLibrary("ml_ncnn")
                Log.d(TAG, "Successfully loaded ml_ncnn JNI library")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ml_ncnn library", e)
            }
        }
    }

    external fun init(assetManager: AssetManager, useGpu: Boolean): Boolean
    external fun detect(bitmap: Bitmap): FloatArray
    external fun destroy()
}

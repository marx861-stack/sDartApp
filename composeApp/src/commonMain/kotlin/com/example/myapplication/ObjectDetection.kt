package com.example.myapplication

import androidx.compose.ui.geometry.Rect

/**
 * Standard YOLO/RTMDet Objekterkennung (plattformunabhängig).
 * @param boundingBox Bounding Box (normalisiert 0..1 oder Pixel)
 * @param confidence Erkennungswahrscheinlichkeit (0.0..1.0)
 * @param classId Klassen-ID (0=Dart, 1=Spitze, 2=Dartboard, 3=20er-Feld)
 * @param className Name der erkannten Klasse ("dart", "tip", "dartboard", "20")
 */
data class ObjectDetection(
    val boundingBox: Rect,
    val confidence: Float,
    val classId: Int,
    val className: String
)

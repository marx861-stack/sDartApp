package com.sdart.app.domain

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Standard-Segmentfolge eines Dartboards im Uhrzeigersinn, beginnend bei 12 Uhr (Segment 20).
 */
val STANDARD_DARTBOARD_SEGMENTS: List<Int> = listOf(
    20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5
)

/**
 * Normalisierte Radien-Verhältnisse eines Standard-Dartboards bezogen auf den Gesamtradius bis zum äußeren Doppelring (1.0).
 */
object DartboardDimensions {
    const val BULLSEYE_RATIO = 0.037f
    const val OUTER_BULL_RATIO = 0.093f
    const val TRIPLE_RING_INNER_RATIO = 0.571f
    const val TRIPLE_RING_OUTER_RATIO = 0.629f
    const val DOUBLE_RING_INNER_RATIO = 0.941f
    const val DOUBLE_RING_OUTER_RATIO = 1.000f
}

/**
 * Repräsentiert das Ergebnis eines Dart-Treffers auf dem Board.
 */
data class DartHit(
    val segment: Int,
    val multiplier: Int
) {
    val points: Int get() = segment * multiplier
    val isMiss: Boolean get() = multiplier == 0 || segment == 0
    val isBullseye: Boolean get() = segment == 25 && multiplier == 2
    val isOuterBull: Boolean get() = segment == 25 && multiplier == 1

    /**
     * Formatiert den Treffer für UI- und Ansage-Zwecke (z. B. "T20", "D16", "Bull", "DBull", "Miss").
     */
    val displayText: String get() = when {
        isMiss -> "Miss"
        isBullseye -> "DBull"
        isOuterBull -> "Bull"
        multiplier == 3 -> "T$segment"
        multiplier == 2 -> "D$segment"
        multiplier == 1 -> "$segment"
        else -> "Miss"
    }

    fun toPair(): Pair<Int, Int> = Pair(segment, multiplier)
}

/**
 * Berechnet das getroffene Segment und den Multiplikator anhand relativer Pixel-/Offset-Koordinaten.
 */
fun calculateDartHit(
    tapX: Float,
    tapY: Float,
    centerX: Float,
    centerY: Float,
    radius: Float,
    scores: List<Int> = STANDARD_DARTBOARD_SEGMENTS
): DartHit {
    val bullseyeRadius = radius * DartboardDimensions.BULLSEYE_RATIO
    val outerBullRadius = radius * DartboardDimensions.OUTER_BULL_RATIO
    val tripleRingInnerRadius = radius * DartboardDimensions.TRIPLE_RING_INNER_RATIO
    val tripleRingOuterRadius = radius * DartboardDimensions.TRIPLE_RING_OUTER_RATIO
    val doubleRingInnerRadius = radius * DartboardDimensions.DOUBLE_RING_INNER_RATIO
    val doubleRingOuterRadius = radius * DartboardDimensions.DOUBLE_RING_OUTER_RATIO

    val dx = tapX - centerX
    val dy = tapY - centerY
    val distance = sqrt(dx * dx + dy * dy)

    if (distance <= bullseyeRadius) {
        return DartHit(segment = 25, multiplier = 2)
    } else if (distance <= outerBullRadius) {
        return DartHit(segment = 25, multiplier = 1)
    }

    val angle = atan2(dy.toDouble(), dx.toDouble())
    var degrees = ((angle * 180.0 / PI) + 90.0).toFloat()
    if (degrees < 0f) degrees += 360f

    val segmentIndex = ((degrees + 9f) / 18f).toInt() % 20
    val baseScore = scores[segmentIndex]

    val multiplier = when {
        distance <= bullseyeRadius -> 2
        distance <= outerBullRadius -> 1
        distance <= tripleRingInnerRadius -> 1
        distance <= tripleRingOuterRadius -> 3
        distance <= doubleRingInnerRadius -> 1
        distance <= doubleRingOuterRadius -> 2
        else -> 0
    }

    if (multiplier == 0) {
        return DartHit(segment = 0, multiplier = 0)
    }

    return DartHit(segment = baseScore, multiplier = multiplier)
}

/**
 * Überladung für Compose Offset für nahtlose Abwärtskompatibilität.
 */
fun calculateDartHit(
    tapOffset: Offset,
    center: Offset,
    radius: Float,
    scores: List<Int> = STANDARD_DARTBOARD_SEGMENTS
): DartHit = calculateDartHit(
    tapX = tapOffset.x,
    tapY = tapOffset.y,
    centerX = center.x,
    centerY = center.y,
    radius = radius,
    scores = scores
)

/**
 * Berechnet Segment und Multiplikator als Pair (Rückwärtskompatibilität zu bestehendem Code).
 */
fun calculateDartScore(
    tapOffset: Offset,
    center: Offset,
    radius: Float,
    scores: List<Int> = STANDARD_DARTBOARD_SEGMENTS
): Pair<Int, Int> = calculateDartHit(tapOffset, center, radius, scores).toPair()


// --- DART-CHECKOUT-KONSTANTEN ---
val BOGEY_NUMBERS = setOf(169, 168, 166, 165, 163, 162, 159)

val CHECKOUT_PATHS = mapOf(
    170 to listOf("T20 T20 Bull"),
    167 to listOf("T20 T19 Bull"),
    164 to listOf("T20 T18 Bull"),
    161 to listOf("T20 T17 Bull"),
    160 to listOf("T20 T20 D20"),
    158 to listOf("T20 T18 D22"),
    157 to listOf("T20 T19 D20"),
    155 to listOf("T20 T19 D19"),
    154 to listOf("T20 T18 D20"),
    152 to listOf("T20 T16 D22"),
    151 to listOf("T20 T17 D20"),
    149 to listOf("T20 T19 D16"),
    148 to listOf("T20 T16 D20"),
    146 to listOf("T20 T18 D16"),
    145 to listOf("T20 T15 D20"),
    143 to listOf("T20 T17 D16"),
    142 to listOf("T20 T14 D20"),
    140 to listOf("T20 T20 D20"),
    139 to listOf("T19 T14 D20"),
    137 to listOf("T20 T19 D10"),
    136 to listOf("T20 T20 D8"),
    134 to listOf("T20 T14 D20"),
    133 to listOf("T20 T19 D8"),
    131 to listOf("T20 T17 D10"),
    130 to listOf("T20 T20 D5"),
    128 to listOf("T18 T14 D16"),
    127 to listOf("T20 T17 D8"),
    125 to listOf("T20 T19 D4"),
    124 to listOf("T20 T16 D8"),
    122 to listOf("T18 T18 D7"),
    121 to listOf("T20 T11 D14"),
    120 to listOf("T20 20 D20"),
    118 to listOf("T20 18 D20"),
    116 to listOf("T20 16 D20"),
    115 to listOf("T20 15 D20"),
    113 to listOf("T20 13 D20"),
    112 to listOf("T20 12 D20"),
    110 to listOf("T20 10 D20"),
    109 to listOf("T20 9 D20"),
    107 to listOf("T19 10 D20"),
    106 to listOf("T20 6 D20"),
    104 to listOf("T18 10 D20"),
    103 to listOf("T20 3 D20"),
    101 to listOf("T20 1 D20"),
    100 to listOf("T20 D20"),
    98 to listOf("T20 D19"),
    96 to listOf("T20 D18"),
    94 to listOf("T20 D17"),
    92 to listOf("T20 D16"),
    90 to listOf("T20 D15"),
    88 to listOf("T20 D14"),
    86 to listOf("T20 D13"),
    84 to listOf("T20 D12"),
    82 to listOf("T20 D11"),
    80 to listOf("T20 D10"),
    78 to listOf("T20 D9"),
    76 to listOf("T20 D8"),
    74 to listOf("T20 D7"),
    72 to listOf("T20 D6"),
    70 to listOf("T20 D5"),
    68 to listOf("T20 D4"),
    66 to listOf("T20 D3"),
    64 to listOf("T20 D2"),
    62 to listOf("T20 D1"),
    60 to listOf("T20"),
    58 to listOf("T18 D2"),
    56 to listOf("T16 D4"),
    54 to listOf("T18 D0"),
    52 to listOf("T12 D8"),
    50 to listOf("Bull"),
    48 to listOf("T16"),
    46 to listOf("T14 D2"),
    44 to listOf("T12 D4"),
    42 to listOf("T10 D6"),
    40 to listOf("D20"),
    38 to listOf("D19"),
    36 to listOf("D18"),
    34 to listOf("D17"),
    32 to listOf("D16"),
    30 to listOf("D15"),
    28 to listOf("D14"),
    26 to listOf("D13"),
    24 to listOf("D12"),
    22 to listOf("D11"),
    20 to listOf("D10"),
    18 to listOf("D9"),
    16 to listOf("D8"),
    14 to listOf("D7"),
    12 to listOf("D6"),
    10 to listOf("D5"),
    8 to listOf("D4"),
    6 to listOf("D3"),
    4 to listOf("D2"),
    2 to listOf("D1")
)

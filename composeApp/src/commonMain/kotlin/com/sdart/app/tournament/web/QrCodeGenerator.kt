package com.sdart.app.tournament.web

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Zeichnet einen scannbaren QR-Code für URLs oder Deep-Links auf einem Compose Canvas.
 */
@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    foregroundColor: Color = Color.Black
) {
    val matrix = remember(content) {
        SimpleQrCode.encode(content)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val moduleCount = matrix.size
            if (moduleCount == 0) return@Canvas

            val moduleSize = size.width / moduleCount
            for (y in 0 until moduleCount) {
                for (x in 0 until moduleCount) {
                    if (matrix[y][x]) {
                        drawRect(
                            color = foregroundColor,
                            topLeft = Offset(x * moduleSize, y * moduleSize),
                            size = Size(moduleSize * 1.02f, moduleSize * 1.02f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kompakter, reiner Kotlin-Generator für QR-Codes (ISO/IEC 18004, Byte-Modus, ECC Level L / M).
 * Funktioniert 100 % plattformunabhängig in commonMain ohne externe Abhängigkeiten.
 */
object SimpleQrCode {

    fun encode(text: String): Array<BooleanArray> {
        val data = text.encodeToByteArray()
        // Wähle minimale Version (1 bis 6)
        val version = when {
            data.size <= 17 -> 1 // 21x21
            data.size <= 32 -> 2 // 25x25
            data.size <= 53 -> 3 // 29x29
            data.size <= 78 -> 4 // 33x33
            data.size <= 106 -> 5 // 37x37
            else -> 6 // 41x41
        }
        val size = 17 + 4 * version
        val modules = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        // Finder Patterns (oben links, oben rechts, unten links)
        drawFinderPattern(modules, isFunction, 0, 0)
        drawFinderPattern(modules, isFunction, size - 7, 0)
        drawFinderPattern(modules, isFunction, 0, size - 7)

        // Alignment Patterns (ab Version 2)
        if (version >= 2) {
            val alignPos = getAlignmentPatternPositions(version)
            for (r in alignPos) {
                for (c in alignPos) {
                    if (!isFunction[r][c]) {
                        drawAlignmentPattern(modules, isFunction, c - 2, r - 2)
                    }
                }
            }
        }

        // Timing Patterns
        for (i in 8 until size - 8) {
            val bit = (i % 2 == 0)
            if (!isFunction[6][i]) {
                modules[6][i] = bit
                isFunction[6][i] = true
            }
            if (!isFunction[i][6]) {
                modules[i][6] = bit
                isFunction[i][6] = true
            }
        }

        // Dark Module
        modules[4 * version + 9][8] = true
        isFunction[4 * version + 9][8] = true

        // Reserviere Format-Info Bereiche
        for (i in 0..8) {
            isFunction[8][i] = true
            isFunction[i][8] = true
        }
        for (i in (size - 8) until size) {
            isFunction[8][i] = true
            isFunction[i][8] = true
        }

        // Codierung der Daten (Byte Mode)
        val codewords = encodeData(data, version)

        // Datenbits in Modulmatrix eintragen
        var bitIndex = 0
        val totalBits = codewords.size * 8
        var right = size - 1
        var goingUp = true

        while (right > 0) {
            if (right == 6) right-- // Timing-Spalte überspringen
            val rowRange = if (goingUp) (size - 1 downTo 0) else (0 until size)
            for (row in rowRange) {
                for (colOffset in 0..1) {
                    val col = right - colOffset
                    if (!isFunction[row][col]) {
                        var bit = false
                        if (bitIndex < totalBits) {
                            val byteVal = codewords[bitIndex / 8].toInt() and 0xFF
                            val bitPos = 7 - (bitIndex % 8)
                            bit = ((byteVal shr bitPos) and 1) == 1
                            bitIndex++
                        }
                        // Mask Pattern 0: (row + col) % 2 == 0
                        val mask = (row + col) % 2 == 0
                        modules[row][col] = bit xor mask
                    }
                }
            }
            right -= 2
            goingUp = !goingUp
        }

        // Format-Information eintragen (ECC Level L = 01, Mask Pattern 0 = 000 -> 0b01000)
        drawFormatInfo(modules, size, 0b01000)

        return modules
    }

    private fun drawFinderPattern(m: Array<BooleanArray>, f: Array<BooleanArray>, x: Int, y: Int) {
        for (r in 0..6) {
            for (c in 0..6) {
                val isBlack = (r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4))
                m[y + r][x + c] = isBlack
                f[y + r][x + c] = true
            }
        }
        // Separator
        for (r in -1..7) {
            for (c in -1..7) {
                val py = y + r
                val px = x + c
                if (py in m.indices && px in m.indices && !f[py][px]) {
                    m[py][px] = false
                    f[py][px] = true
                }
            }
        }
    }

    private fun drawAlignmentPattern(m: Array<BooleanArray>, f: Array<BooleanArray>, x: Int, y: Int) {
        for (r in 0..4) {
            for (c in 0..4) {
                val isBlack = (r == 0 || r == 4 || c == 0 || c == 4 || (r == 2 && c == 2))
                m[y + r][x + c] = isBlack
                f[y + r][x + c] = true
            }
        }
    }

    private fun getAlignmentPatternPositions(version: Int): IntArray {
        return when (version) {
            2 -> intArrayOf(6, 18)
            3 -> intArrayOf(6, 22)
            4 -> intArrayOf(6, 26)
            5 -> intArrayOf(6, 30)
            6 -> intArrayOf(6, 34)
            else -> intArrayOf(6, 17 + 4 * version - 7)
        }
    }

    private fun drawFormatInfo(m: Array<BooleanArray>, size: Int, formatBits: Int) {
        // Berechne 15-Bit BCH Code für Format-Info
        var d = formatBits shl 10
        val g = 0x537
        for (i in 4 downTo 0) {
            if ((d and (1 shl (10 + i))) != 0) {
                d = d xor (g shl i)
            }
        }
        val rawCode = (formatBits shl 10) or d
        val maskedCode = rawCode xor 0x5412 // Maskierung gemäß Spezifikation

        // Platziere Format-Bits
        for (i in 0..14) {
            val bit = ((maskedCode shr i) and 1) == 1
            // Um den oberen linken Finder
            when (i) {
                in 0..5 -> m[i][8] = bit
                6 -> m[7][8] = bit
                7 -> m[8][8] = bit
                8 -> m[8][7] = bit
                in 9..14 -> m[8][14 - i] = bit
            }
            // Um die anderen Finder
            if (i < 7) {
                m[8][size - 1 - i] = bit
            } else {
                m[size - 15 + i][8] = bit
            }
        }
    }

    private fun encodeData(data: ByteArray, version: Int): ByteArray {
        val totalCodewords = when (version) {
            1 -> 26
            2 -> 44
            3 -> 70
            4 -> 100
            5 -> 134
            else -> 172
        }
        val dataCodewords = when (version) {
            1 -> 19
            2 -> 34
            3 -> 55
            4 -> 80
            5 -> 108
            else -> 136
        }
        val ecCodewords = totalCodewords - dataCodewords

        val buffer = mutableListOf<Int>()
        var bitBuf = 0
        var bitLen = 0

        fun putBits(value: Int, count: Int) {
            bitBuf = (bitBuf shl count) or (value and ((1 shl count) - 1))
            bitLen += count
            while (bitLen >= 8) {
                buffer.add((bitBuf shr (bitLen - 8)) and 0xFF)
                bitLen -= 8
            }
        }

        // Mode Indicator: Byte Mode (0100)
        putBits(0b0100, 4)
        // Character Count Indicator (8 Bits für V1-V9)
        putBits(data.size, 8)
        // Datenbytes
        for (b in data) {
            putBits(b.toInt() and 0xFF, 8)
        }
        // Terminator (bis zu 4 Nullbits)
        val termLen = minOf(4, (dataCodewords * 8) - (buffer.size * 8 + bitLen))
        if (termLen > 0) putBits(0, termLen)

        // Auffüllen auf Bytegrenze
        if (bitLen > 0) {
            putBits(0, 8 - bitLen)
        }

        // Pad-Bytes (0xEC, 0x11) bis dataCodewords voll sind
        var padToggle = true
        while (buffer.size < dataCodewords) {
            buffer.add(if (padToggle) 0xEC else 0x11)
            padToggle = !padToggle
        }

        // Reed-Solomon Fehlerkorrektur berechnen
        val dataBytes = buffer.map { it.toByte() }.toByteArray()
        val ecBytes = calculateReedSolomon(dataBytes, ecCodewords)

        return dataBytes + ecBytes
    }

    private fun calculateReedSolomon(data: ByteArray, ecCount: Int): ByteArray {
        val gen = generateRsPoly(ecCount)
        val res = IntArray(ecCount)

        for (b in data) {
            val factor = (b.toInt() and 0xFF) xor res[0]
            for (j in 0 until ecCount - 1) {
                res[j] = res[j + 1] xor gfMul(gen[j], factor)
            }
            res[ecCount - 1] = gfMul(gen[ecCount - 1], factor)
        }
        return res.map { it.toByte() }.toByteArray()
    }

    private fun generateRsPoly(ecCount: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until ecCount) {
            val next = IntArray(poly.size + 1)
            val alphaI = gfExp[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], alphaI)
            }
            poly = next
        }
        return poly.copyOfRange(1, poly.size)
    }

    // Galois Field GF(256) mit Primitivpolynom 0x11D
    private val gfExp = IntArray(512)
    private val gfLog = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            gfExp[i] = x
            gfExp[i + 255] = x
            gfLog[x] = i
            x = x shl 1
            if (x >= 256) x = x xor 0x11D
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return gfExp[gfLog[a] + gfLog[b]]
    }
}

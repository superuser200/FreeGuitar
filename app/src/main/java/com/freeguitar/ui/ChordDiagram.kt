package com.freeguitar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.freeguitar.data.GuitarChord

/**
 * Draws a standard guitar chord box.
 */
@Composable
fun ChordDiagram(chord: GuitarChord, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.onSurface
    val dotColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.error

    Box(
        modifier
            .fillMaxWidth()
            .padding(8.dp)
            .aspectRatio(1.15f)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Canvas(Modifier.fillMaxWidth()) {
            val w = size.width
            val h = size.height
            val nStrings = 6
            val nFrets = 5
            val strumX = w * 0.06f
            val gridW = w - strumX
            val gridH = h * 0.82f

            val stringGap = gridW / (nStrings - 1)
            val fretGap = gridH / nFrets

            // strings
            for (i in 0 until nStrings) {
                val x = strumX + i * stringGap
                val isThick = i == 0 || i == 1
                drawLine(
                    color = lineColor.copy(alpha = 0.8f),
                    start = Offset(x, gridH * 0.10f),
                    end = Offset(x, gridH * 0.95f),
                    strokeWidth = if (isThick) 3.5f else 1.5f
                )
            }

            // frets
            for (f in 0..nFrets) {
                val y = gridH * 0.10f + f * fretGap
                drawLine(
                    color = lineColor.copy(alpha = 0.7f),
                    start = Offset(strumX, y),
                    end = Offset(w, y),
                    strokeWidth = if (f == 0) 5f else 2f
                )
            }

            // nut marker (fret number)
            if (chord.barreFret > 1) {
                val y = gridH * 0.12f
                drawCircle(color = lineColor.copy(alpha = 0.6f), radius = w * 0.02f, center = Offset(strumX + stringGap / 2, y))
            } else {
                val y = gridH * 0.06f
                drawLine(color = lineColor, start = Offset(strumX, y), end = Offset(w, y), strokeWidth = 5f)
            }

            // per-string markers
            val dotsPerFret = IntArray(6) { 0 }
            for (f in chord.frets) if (f > 0) dotsPerFret[f]++
            for (i in 0 until nStrings) {
                val fret = chord.frets[i]
                val x = strumX + i * stringGap
                when {
                    fret == 0 -> {
                        drawCircle(
                            color = lineColor.copy(alpha = 0.7f),
                            radius = w * 0.022f,
                            center = Offset(x, gridH * 0.05f)
                        )
                    }
                    fret < 0 -> {
                        drawCircle(
                            color = mutedColor,
                            radius = w * 0.024f,
                            center = Offset(x, gridH * 0.05f)
                        )
                    }
                    else -> {
                        // shift multiple dots on same fret
                        val groupIdx = chord.frets.take(i).count { it == fret }
                        val count = dotsPerFret[fret]
                        val offsetX = (groupIdx - (count - 1) / 2f) * stringGap * 0.7f
                        val y = gridH * 0.10f + (fret - (chord.barreFret - 1)) * fretGap - fretGap / 2
                        drawCircle(
                            color = dotColor,
                            radius = w * 0.045f,
                            center = Offset(x + offsetX, y)
                        )
                        chord.fingers?.get(i)?.let { finger ->
                            if (finger > 0) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    finger.toString(),
                                    x + offsetX,
                                    y + w * 0.015f,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = w * 0.04f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isFakeBoldText = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // barre indicator
            if (chord.isBarre && chord.barreFret > 0) {
                val y = gridH * 0.10f + (chord.barreFret - 1) * fretGap - fretGap / 2
                val fretIdx = chord.frets.filter { it == chord.barreFret }
                if (fretIdx.isNotEmpty()) {
                    drawCircle(color = dotColor, radius = w * 0.045f, center = Offset(strumX + (nStrings - 1) * stringGap * 0.8f, y))
                }
            }
        }
    }
}

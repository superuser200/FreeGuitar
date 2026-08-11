package com.freeguitar.audio

import kotlin.math.sqrt

/**
 * Detects the fundamental frequency of a short[] audio buffer using
 * autocorrelation. Returns null when the signal is too quiet or the
 * pitch is unreliable.
 */
object PitchDetector {

    private const val MIN_FREQ = 60.0
    private const val MAX_FREQ = 1100.0

    data class Result(val freq: Double, val rms: Double)

    fun detect(buffer: ShortArray, sampleRate: Int, offset: Int = 0, length: Int = buffer.size): Result? {
        var sum = 0.0
        for (i in offset until (offset + length)) {
            val v = buffer[i].toDouble()
            sum += v * v
        }
        val rms = sqrt(sum / length)
        if (rms < 400.0) return null // too quiet to trust

        val minLag = (sampleRate / MAX_FREQ).toInt()
        val maxLag = (sampleRate / MIN_FREQ).toInt()
        if (maxLag >= length) return null

        val n = length
        var bestLag = -1
        var bestVal = -1.0

        for (lag in minLag..maxLag) {
            var corr = 0.0
            var energy = 0.0
            for (i in 0 until (n - lag)) {
                val a = buffer[offset + i].toDouble()
                val b = buffer[offset + i + lag].toDouble()
                corr += a * b
                energy += a * a
            }
            if (energy <= 0.0) continue
            val norm = corr / energy
            if (norm > bestVal) {
                bestVal = norm
                bestLag = lag
            }
        }

        if (bestLag < 0 || bestVal < 0.30) return null

        // Parabolic interpolation around the peak for sub-sample accuracy
        val p0 = if (bestLag - 1 >= minLag) normalizedCorr(buffer, sampleRate, bestLag - 1, length) else bestVal
        val p2 = if (bestLag + 1 <= maxLag) normalizedCorr(buffer, sampleRate, bestLag + 1, length) else bestVal
        val denom = p0 - 2 * bestVal + p2
        val shift = if (denom != 0.0) 0.5 * (p0 - p2) / denom else 0.0
        val refinedLag = bestLag + shift

        val freq = sampleRate / refinedLag
        if (freq < MIN_FREQ || freq > MAX_FREQ) return null
        return Result(freq, rms)
    }

    private fun normalizedCorr(buffer: ShortArray, sampleRate: Int, lag: Int, length: Int): Double {
        var corr = 0.0
        var energy = 0.0
        for (i in 0 until (length - lag)) {
            val a = buffer[i].toDouble()
            val b = buffer[i + lag].toDouble()
            corr += a * b
            energy += a * a
        }
        return if (energy > 0.0) corr / energy else 0.0
    }
}

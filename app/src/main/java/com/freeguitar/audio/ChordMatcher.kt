package com.freeguitar.audio

import kotlin.math.roundToInt

/**
 * Detects whether the guitar is actually playing a target chord.
 *
 * Instead of trusting a single pitch sample, it accumulates detected
 * frequencies over a short rolling window and builds a histogram of
 * pitch classes (0..11). A chord "matches" when the expected notes of
 * the chord are present strongly enough — this is how real guitar
 * chords are recognized through the microphone.
 */
class ChordMatcher {

    // Rolling window of recent detections (time, midi note)
    private data class Sample(val time: Long, val midi: Int, val rms: Double)
    private val samples = ArrayDeque<Sample>()
    private var lastTime = 0L
    private val windowMs = 700L

    fun clear() {
        samples.clear()
    }

    /**
     * Feed a detection. Returns a match score 0..1 for the given target
     * chord midi notes. Score near 1.0 means the chord is clearly played.
     */
    fun feed(result: PitchDetector.Result, chordMidi: IntArray): Double {
        val now = System.currentTimeMillis()
        if (result.rms > 300) {
            samples.addLast(Sample(now, NoteUtils.nearestNoteMidi(result.freq), result.rms))
        }
        // drop old samples
        while (samples.isNotEmpty() && now - samples.first().time > windowMs) {
            samples.removeFirst()
        }
        lastTime = now

        if (samples.isEmpty()) return 0.0

        // histogram of pitch classes from loudest contributions
        val pc = IntArray(12)
        var total = 0
        for (s in samples) {
            val pcIdx = ((s.midi % 12) + 12) % 12
            pc[pcIdx]++
            total++
        }
        if (total == 0) return 0.0

        // count how many target chord notes are present, weighted by strength
        val targetPc = chordMidi.map { ((it % 12) + 12) % 12 }.distinct()
        var found = 0
        var need = targetPc.size
        for (pcIdx in targetPc) {
            if (pc[pcIdx] >= 2) found++
        }
        // root note matters most
        val rootPc = ((chordMidi[0] % 12) + 12) % 12
        val rootPresent = pc[rootPc] >= 2

        val coverage = found.toDouble() / need.toDouble()
        return if (rootPresent) 0.4 + 0.6 * coverage else 0.0
    }
}

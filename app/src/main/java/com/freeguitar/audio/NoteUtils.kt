package com.freeguitar.audio

object NoteUtils {
    val NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    // Standard tuning, low E to high e (MIDI numbers)
    val OPEN_STRINGS_MIDI = intArrayOf(40, 45, 50, 55, 59, 64)
    val OPEN_STRINGS_NAMES = arrayOf("E", "A", "D", "G", "B", "e")

    fun midiToFreq(midi: Int): Double = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)

    fun freqToMidi(freq: Double): Double = 69.0 + 12.0 * (Math.log(freq / 440.0) / Math.log(2.0))

    fun midiToName(midi: Int): String {
        val m = ((midi % 12) + 12) % 12
        return NAMES[m]
    }

    fun midiToOctave(midi: Int): Int = (midi / 12) - 1

    fun midiToNameOctave(midi: Int): String = midiToName(midi) + midiToOctave(midi)

    fun freqToName(freq: Double): String {
        val midi = Math.round(freqToMidi(freq)).toInt()
        return midiToName(midi)
    }

    /** Cents offset (how many cents sharp/flat) relative to nearest semitone. Range -50..+50 */
    fun freqToCents(freq: Double): Int {
        val midi = freqToMidi(freq)
        val nearest = Math.round(midi).toInt()
        val diff = midi - nearest
        return Math.round(diff * 100).toInt()
    }

    fun nearestNoteMidi(freq: Double): Int = Math.round(freqToMidi(freq)).toInt()
}

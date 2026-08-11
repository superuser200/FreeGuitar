package com.freeguitar.data

import com.freeguitar.audio.NoteUtils

/**
 * A chord shape. frets: index 0 = low E string ... index 5 = high e string.
 * -1 = not played, 0 = open, positive = fret number.
 */
data class GuitarChord(
    val name: String,
    val fullName: String,
    val frets: IntArray,
    val fingers: IntArray? = null,
    val isBarre: Boolean = false,
    val barreFret: Int = 0
) {
    val playedMidiNotes: IntArray by lazy {
        val notes = mutableListOf<Int>()
        for (i in 0..5) {
            if (frets[i] >= 0) {
                notes.add(NoteUtils.OPEN_STRINGS_MIDI[i] + frets[i])
            }
        }
        notes.toIntArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GuitarChord) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}

object Chords {

    // Common open chords (standard tuning), low E -> high e
    val all = listOf(
        GuitarChord("G", "G Major", intArrayOf(3, 2, 0, 0, 0, 3), intArrayOf(2, 1, 0, 0, 0, 3)),
        GuitarChord("G7", "G Seven", intArrayOf(3, 2, 0, 0, 0, 1), intArrayOf(3, 2, 0, 0, 0, 1)),
        GuitarChord("C", "C Major", intArrayOf(-1, 3, 2, 0, 1, 0), intArrayOf(0, 3, 2, 0, 1, 0)),
        GuitarChord("C7", "C Seven", intArrayOf(-1, 3, 2, 3, 1, 0), intArrayOf(0, 3, 2, 4, 1, 0)),
        GuitarChord("D", "D Major", intArrayOf(-1, -1, 0, 2, 3, 2), intArrayOf(0, 0, 0, 1, 3, 2)),
        GuitarChord("Dm", "D Minor", intArrayOf(-1, -1, 0, 2, 3, 1), intArrayOf(0, 0, 0, 1, 3, 2)),
        GuitarChord("D7", "D Seven", intArrayOf(-1, -1, 0, 2, 1, 2), intArrayOf(0, 0, 0, 2, 1, 3)),
        GuitarChord("A", "A Major", intArrayOf(-1, 0, 2, 2, 2, 0), intArrayOf(0, 0, 2, 3, 4, 0)),
        GuitarChord("Am", "A Minor", intArrayOf(-1, 0, 2, 2, 1, 0), intArrayOf(0, 0, 2, 3, 1, 0)),
        GuitarChord("A7", "A Seven", intArrayOf(-1, 0, 2, 0, 2, 0), intArrayOf(0, 0, 2, 0, 3, 0)),
        GuitarChord("E", "E Major", intArrayOf(0, 2, 2, 1, 0, 0), intArrayOf(0, 2, 3, 1, 0, 0)),
        GuitarChord("Em", "E Minor", intArrayOf(0, 2, 2, 0, 0, 0), intArrayOf(0, 2, 3, 0, 0, 0)),
        GuitarChord("E7", "E Seven", intArrayOf(0, 2, 0, 1, 0, 0), intArrayOf(0, 2, 0, 1, 0, 0)),
        GuitarChord("F", "F Major", intArrayOf(1, 3, 3, 2, 1, 1), intArrayOf(1, 3, 4, 2, 1, 1), true, 1),
        GuitarChord("Fmaj7", "F Major Seven", intArrayOf(-1, 3, 2, 1, 0, 0), intArrayOf(0, 3, 2, 1, 0, 0)),
        GuitarChord("F#m", "F# Minor", intArrayOf(2, 4, 4, 2, 2, 2), intArrayOf(1, 3, 4, 1, 1, 1), true, 2),
        GuitarChord("Gm", "G Minor", intArrayOf(3, 5, 5, 3, 3, 3), intArrayOf(1, 3, 4, 1, 1, 1), true, 3),
        GuitarChord("B7", "B Seven", intArrayOf(-1, 2, 1, 2, 0, 2), intArrayOf(0, 1, 2, 3, 0, 4)),
        GuitarChord("Bm", "B Minor", intArrayOf(-1, 2, 4, 4, 3, 2), intArrayOf(0, 1, 3, 4, 2, 1), true, 2),
        GuitarChord("C#m", "C# Minor", intArrayOf(-1, 4, 6, 6, 5, 4), intArrayOf(0, 1, 3, 4, 2, 1), true, 4),
        GuitarChord("G/B", "G over B", intArrayOf(-1, 2, 0, 0, 3, 3), intArrayOf(0, 1, 0, 0, 3, 4)),
        GuitarChord("D/F#", "D over F#", intArrayOf(-1, 2, 0, 2, 3, 2), intArrayOf(0, 2, 0, 1, 3, 2))
    )

    fun byName(name: String): GuitarChord? = all.find { it.name == name }
}

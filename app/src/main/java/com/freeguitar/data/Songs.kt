package com.freeguitar.data

/**
 * A simplified play-along song. Chords are timed in beats; tap the
 * downbeat with your strum. The app listens and tells you if you're
 * hitting the right note.
 */
data class Song(
    val title: String,
    val artist: String,
    val bpm: Int,
    val timeSignature: Int = 4, // beats per bar
    val chordProgression: List<Pair<String, Int>>, // chord name -> beats
    val strum: String = "↓ ↓ ↑ ↑ ↓ ↑"
) {
    val totalBeats: Int by lazy { chordProgression.sumOf { it.second } }

    fun chordAtBeat(beat: Int): String {
        if (beat < 0) return chordProgression.first().first
        var cursor = 0
        for ((chord, beats) in chordProgression) {
            if (beat < cursor + beats) return chord
            cursor += beats
        }
        return chordProgression.last().first
    }

    /** Chord index at a given beat (for looping the progression). */
    fun indexAtBeat(beat: Int): Int {
        if (beat < 0) return 0
        var cursor = 0
        for (i in chordProgression.indices) {
            val beats = chordProgression[i].second
            if (beat < cursor + beats) return i
            cursor += beats
        }
        return chordProgression.size - 1
    }

    fun toJson(): org.json.JSONObject {
        val prog = org.json.JSONArray()
        for ((chord, beats) in chordProgression) {
            prog.put(org.json.JSONObject().put("chord", chord).put("beats", beats))
        }
        return org.json.JSONObject()
            .put("title", title)
            .put("artist", artist)
            .put("bpm", bpm)
            .put("timeSignature", timeSignature)
            .put("strum", strum)
            .put("progression", prog)
    }

    companion object {
        fun fromJson(o: org.json.JSONObject): Song? {
            return try {
                val arr = o.getJSONArray("progression")
                val progression = mutableListOf<Pair<String, Int>>()
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    progression.add(p.getString("chord") to p.getInt("beats"))
                }
                Song(
                    title = o.getString("title"),
                    artist = o.optString("artist", "Unknown"),
                    bpm = o.optInt("bpm", 90),
                    timeSignature = o.optInt("timeSignature", 4),
                    chordProgression = progression,
                    strum = o.optString("strum", "↓ ↓ ↑ ↓ ↑")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

object Songs {

    // 4 beats per bar; progressions repeat until you stop.
    val all = listOf(
        Song(
            "Knockin' On Heaven's Door",
            "Bob Dylan",
            84,
            chordProgression = listOf(
                "G" to 4, "D" to 4, "Am" to 8,
                "G" to 4, "D" to 4, "C" to 8
            ),
            strum = "↓ ↓ ↓ ↓"
        ),
        Song(
            "Brown Eyed Girl",
            "Van Morrison",
            96,
            chordProgression = listOf(
                "G" to 4, "C" to 4, "G" to 4, "D" to 4
            ),
            strum = "↓ ↓ ↑ ↓ ↑"
        ),
        Song(
            "Wonderwall",
            "Oasis",
            90,
            chordProgression = listOf(
                "Em" to 4, "G" to 4, "D" to 4, "A" to 4
            ),
            strum = "↓ ↓ ↑ ↑ ↓ ↑"
        ),
        Song(
            "Three Little Birds",
            "Bob Marley",
            100,
            chordProgression = listOf(
                "A" to 4, "D" to 4, "E" to 4, "A" to 4
            ),
            strum = "↓ ↑ ↓ ↑"
        ),
        Song(
            "Horse With No Name",
            "America",
            88,
            chordProgression = listOf(
                "Em" to 8, "B7" to 8
            ),
            strum = "↓ ↓ ↓ ↓"
        ),
        Song(
            "Riptide",
            "Vance Joy",
            102,
            chordProgression = listOf(
                "Am" to 4, "G" to 4, "C" to 4, "C" to 4
            ),
            strum = "↓ ↑ ↓ ↑"
        ),
        Song(
            "Let It Be",
            "The Beatles",
            75,
            chordProgression = listOf(
                "C" to 4, "G" to 4, "Am" to 4, "F" to 4,
                "C" to 4, "G" to 4, "F" to 4, "C" to 4
            ),
            strum = "↓ ↓ ↓ ↓"
        ),
        Song(
            "Stand By Me",
            "Ben E. King",
            96,
            chordProgression = listOf(
                "G" to 4, "Em" to 4, "C" to 4, "D" to 4
            ),
            strum = "↓ ↓ ↑ ↓ ↑"
        ),
        Song(
            "House of the Rising Sun",
            "The Animals",
            76,
            chordProgression = listOf(
                "Am" to 4, "C" to 4, "D" to 4, "F" to 4,
                "Am" to 4, "C" to 4, "E" to 4, "E" to 4
            ),
            strum = "↓ ↓ ↓ ↓"
        ),
        Song(
            "Sweet Home Alabama",
            "Lynyrd Skynyrd",
            98,
            chordProgression = listOf(
                "D" to 4, "C" to 4, "G" to 4, "D" to 4
            ),
            strum = "↓ ↓ ↑ ↓ ↑"
        ),
        Song(
            "Hey Jude",
            "The Beatles",
            72,
            chordProgression = listOf(
                "F" to 4, "C" to 4, "C" to 4, "F" to 4,
                "F" to 4, "C" to 4, "G" to 4, "G" to 4
            ),
            strum = "↓ ↓ ↑ ↓ ↑"
        ),
        Song(
            "Canon in D (easy)",
            "Pachelbel",
            80,
            chordProgression = listOf(
                "D" to 4, "A" to 4, "Bm" to 4, "F#m" to 4,
                "G" to 4, "D" to 4, "G" to 4, "A" to 4
            ),
            strum = "↓ ↓ ↓ ↓"
        ),
        Song(
            "Zombie",
            "The Cranberries",
            96,
            chordProgression = listOf(
                "Em" to 4, "C" to 4, "G" to 4, "D" to 4
            ),
            strum = "↓ ↓ ↓ ↓"
        ),
        Song(
            "Wish You Were Here",
            "Pink Floyd",
            92,
            chordProgression = listOf(
                "G" to 4, "C" to 4, "D" to 4, "G" to 4
            ),
            strum = "↓ ↓ ↑ ↓ ↑"
        ),
        Song(
            "Hallelujah",
            "Leonard Cohen",
            66,
            chordProgression = listOf(
                "C" to 4, "Am" to 4, "F" to 4, "G" to 4,
                "C" to 4, "G" to 4, "Am" to 4, "G" to 4
            ),
            strum = "↓ ↓ ↓ ↓"
        ),
        Song(
            "Ain't No Sunshine",
            "Bill Withers",
            90,
            chordProgression = listOf(
                "Am" to 4, "Em" to 4, "G" to 4, "Am" to 4,
                "Am" to 4, "Em" to 4, "G" to 4, "A" to 4
            ),
            strum = "↓ ↑ ↓ ↑"
        )
    )
}

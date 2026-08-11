package com.freeguitar.data

import android.content.Context
import org.json.JSONArray

/**
 * Persists songs downloaded from the update manifest so they appear in
 * the app's song list even after the app is restarted.
 */
object SongStore {

    private const val PREFS = "song_store"
    private const val KEY_SONGS = "downloaded_songs"

    fun addSongs(context: Context, songs: List<Song>) {
        val existing = load(context).toMutableList()
        for (s in songs) {
            if (existing.none { it.title == s.title && it.artist == s.artist }) existing.add(s)
        }
        save(context, existing)
    }

    fun save(context: Context, songs: List<Song>) {
        val arr = JSONArray()
        for (s in songs) {
            arr.put(s.toJson())
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SONGS, arr.toString()).apply()
    }

    fun load(context: Context): List<Song> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SONGS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<Song>()
            for (i in 0 until arr.length()) {
                Song.fromJson(arr.getJSONObject(i))?.let { out.add(it) }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }
}

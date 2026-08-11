package com.freeguitar.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks a remote JSON manifest (host it anywhere for free, e.g. GitHub
 * Pages) for new songs and new app builds. The app never costs anything —
 * this just lets you push updates to the installed app for free.
 *
 * Manifest format:
 * {
 *   "app":   { "versionCode": 2, "versionName": "1.1", "apkUrl": "https://.../free-guitar.apk", "notes": "..." },
 *   "songs": [ { "title": "...", "artist": "...", "bpm": 100, "strum": "...", "progression": [ {"chord":"G","beats":4} ] } ]
 * }
 */
object UpdateService {

    data class AppUpdate(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val notes: String
    )

    data class Manifest(
        val app: AppUpdate?,
        val songs: List<Song>
    )

    fun fetchManifest(url: String): Manifest? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"
            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            parseManifest(text)
        } catch (e: Exception) {
            null
        }
    }

    fun parseManifest(text: String): Manifest {
        val root = JSONObject(text)

        var app: AppUpdate? = null
        if (root.has("app")) {
            val a = root.getJSONObject("app")
            app = AppUpdate(
                versionCode = a.optInt("versionCode", 1),
                versionName = a.optString("versionName", "1.0"),
                apkUrl = a.optString("apkUrl", ""),
                notes = a.optString("notes", "")
            )
        }

        val songs = mutableListOf<Song>()
        if (root.has("songs")) {
            val arr = root.getJSONArray("songs")
            for (i in 0 until arr.length()) {
                parseSong(arr.getJSONObject(i))?.let { songs.add(it) }
            }
        }
        return Manifest(app, songs)
    }

    private fun parseSong(o: JSONObject): Song? {
        return try {
            val progressionJson = o.getJSONArray("progression")
            val progression = mutableListOf<Pair<String, Int>>()
            for (i in 0 until progressionJson.length()) {
                val p = progressionJson.getJSONObject(i)
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

    /** Downloads the APK to cache and returns the file, or null on failure. */
    fun downloadApk(url: String, cacheDir: File): File? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.requestMethod = "GET"
            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }
            val out = File(cacheDir, "free-guitar-update.apk")
            conn.inputStream.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            }
            conn.disconnect()
            if (out.length() > 1000) out else null
        } catch (e: Exception) {
            null
        }
    }
}

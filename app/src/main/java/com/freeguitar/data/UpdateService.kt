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

    private const val FILE_NAME = "free-guitar-update.apk"
    private const val PART_FILE_NAME = "free-guitar-update.apk.part"
    private const val META_FILE_NAME = "free-guitar-update.apk.meta"

    sealed class DownloadResult {
        data class Progress(val doneBytes: Long, val totalBytes: Long) : DownloadResult()
        data class Paused(val doneBytes: Long, val totalBytes: Long) : DownloadResult()
        object Success : DownloadResult()
        object Failed : DownloadResult()
    }

    /** Mutable pause flag. Set [paused] to true from any thread to stop the download. */
    class PauseFlags {
        @Volatile var paused: Boolean = false
    }

    /** State of a partially-downloaded update file. */
    data class PartialState(val doneBytes: Long, val totalBytes: Long)

    /** Returns info about an in-progress partial download, or null if none exists. */
    fun partialState(cacheDir: File): PartialState? {
        val part = File(cacheDir, PART_FILE_NAME)
        if (!part.exists() || part.length() <= 0) return null
        val total = try { File(cacheDir, META_FILE_NAME).readText().trim().toLong() } catch (e: Exception) { 0L }
        return PartialState(part.length(), total)
    }

    /**
     * Downloads (or resumes) the APK. Existing partial data is continued using an
     * HTTP Range request. Returns [DownloadResult.Paused] when cancelled via
     * [flags] so the partial file can be resumed later.
     */
    fun resumeDownload(url: String, cacheDir: File, flags: PauseFlags): DownloadResult {
        return try {
            val part = File(cacheDir, PART_FILE_NAME)
            val meta = File(cacheDir, META_FILE_NAME)
            val startBytes = if (part.exists()) part.length() else 0L

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.requestMethod = "GET"
            if (startBytes > 0) conn.setRequestProperty("Range", "bytes=$startBytes-")
            if (conn.responseCode != 200 && conn.responseCode != 206) {
                conn.disconnect()
                return DownloadResult.Failed
            }

            val totalBytes = if (conn.responseCode == 206 && conn.contentLength > 0) {
                startBytes + conn.contentLength
            } else {
                conn.contentLength.toLong()
            }
            if (totalBytes <= 0) {
                conn.disconnect()
                return DownloadResult.Failed
            }

            if (totalBytes > 0) meta.writeText(totalBytes.toString())

            var doneBytes = startBytes
            val buffer = ByteArray(64 * 1024)
            conn.inputStream.use { input ->
                FileOutputStream(part, true).use { output ->
                    var n: Int
                    while (input.read(buffer).also { n = it } != -1) {
                        if (n > 0) {
                            output.write(buffer, 0, n)
                            doneBytes += n
                        }
                        if (flags.paused) {
                            output.flush()
                            conn.disconnect()
                            return DownloadResult.Paused(doneBytes, totalBytes)
                        }
                    }
                }
            }
            conn.disconnect()

            if (doneBytes >= totalBytes && doneBytes > 1000) {
                val final = File(cacheDir, FILE_NAME)
                part.renameTo(final)
                meta.delete()
                DownloadResult.Success
            } else {
                DownloadResult.Failed
            }
        } catch (e: Exception) {
            DownloadResult.Failed
        }
    }
}

package com.freeguitar.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Continuously reads from the microphone and reports the detected
 * pitch + volume via a listener on the main thread.
 */
class AudioEngine(private val context: Context) {

    fun interface Listener {
        fun onAudioResult(pitch: PitchDetector.Result?)
    }

    private val running = AtomicBoolean(false)
    private var record: AudioRecord? = null
    private var thread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    fun start(listener: Listener): Boolean {
        if (!hasPermission || running.get()) return false

        val sampleRate = 44100
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) return false

        val bufferSize = Math.max(minBuf, 8192)
        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        } catch (e: Exception) {
            Log.e("AudioEngine", "Failed to create AudioRecord", e)
            return false
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return false
        }

        record = rec
        running.set(true)
        rec.startRecording()

        thread = Thread {
            val buf = ShortArray(bufferSize)
            while (running.get()) {
                val read = rec.read(buf, 0, buf.size, AudioRecord.READ_BLOCKING)
                if (read > 0) {
                    val pitch = PitchDetector.detect(buf, sampleRate, 0, read)
                    mainHandler.post { listener.onAudioResult(pitch) }
                }
            }
        }
        thread?.name = "pitch-detector"
        thread?.start()
        return true
    }

    fun stop() {
        running.set(false)
        thread?.join(500)
        thread = null
        try {
            record?.stop()
        } catch (_: Exception) {
        }
        record?.release()
        record = null
    }

    fun release() {
        stop()
    }
}

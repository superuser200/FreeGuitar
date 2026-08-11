package com.freeguitar.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.PI

/**
 * Synthesizes a quick "strummed" chord from MIDI notes using a single
 * AudioTrack, so the app can play chord sounds without any audio files.
 */
object ChordPlayer {

    private const val SAMPLE_RATE = 44100

    /** Play a chord (list of MIDI notes) with a light strum effect. */
    fun play(midiNotes: IntArray, strumMs: Int = 18, lengthMs: Int = 1600) {
        if (midiNotes.isEmpty()) return
        val nSamples = SAMPLE_RATE * lengthMs / 1000
        val samples = FloatArray(nSamples)

        var stringIndex = 0
        for (midi in midiNotes) {
            val freq = NoteUtils.midiToFreq(midi)
            val startOffset = (SAMPLE_RATE * strumMs * stringIndex / 1000)
            val startSample = (SAMPLE_RATE * 0.02) + startOffset
            val dur = nSamples - startSample.toInt()
            if (dur <= 0) continue

            for (i in 0 until dur) {
                val t = i.toFloat() / SAMPLE_RATE
                val decay = exp(-t * 3.2)
                var v = sin(2.0 * PI * freq * t).toFloat()
                v += (0.5 * sin(2.0 * PI * freq * 2.0 * t)).toFloat()
                v += (0.25 * sin(2.0 * PI * freq * 3.0 * t)).toFloat()
                v *= decay.toFloat() * 0.45f
                val idx = startSample.toInt() + i
                if (idx < nSamples) samples[idx] += v
            }
            stringIndex++
        }

        // normalize
        var peak = 0f
        for (s in samples) if (Math.abs(s) > peak) peak = Math.abs(s)
        val gain = if (peak > 1f) 1f / peak else 1f

        val pcm = ShortArray(nSamples)
        for (i in samples.indices) {
            var v = samples[i] * gain * 32767
            if (v > 32767) v = 32767f
            if (v < -32768) v = -32768f
            pcm[i] = v.toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            track.write(pcm, 0, pcm.size)
            track.play()
        } catch (e: Exception) {
            // ignore
        }
    }
}

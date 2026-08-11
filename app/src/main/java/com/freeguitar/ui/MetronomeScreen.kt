package com.freeguitar.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun MetronomeScreen() {
    var bpm by remember { mutableIntStateOf(90) }
    var running by remember { mutableStateOf(false) }
    var beat by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose { running = false }
    }

    LaunchedEffect(running, bpm) {
        if (running) {
            while (true) {
                playClick(beat == 0)
                beat = (beat + 1) % 4
                delay((60000 / bpm).toLong())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Metronome", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Practice your strumming timing.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(32.dp))

        Text("$bpm", fontSize = 96.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("beats per minute", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(16.dp))

        Slider(
            value = bpm.toFloat(),
            onValueChange = { bpm = it.toInt() },
            valueRange = 40f..240f,
            modifier = Modifier.fillMaxWidth()
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("40", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("240", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            for (i in 0 until 4) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                running && beat == i -> if (i == 0) Color(0xFFFFB74D) else MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { running = !running },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(if (running) "Stop" else "Start", fontSize = 20.sp)
        }
    }
}

private fun playClick(accent: Boolean) {
    try {
        val sampleRate = 44100
        val freq = if (accent) 1568f else 1175f
        val dur = 0.06
        val n = (sampleRate * dur).toInt()
        val pcm = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toFloat() / sampleRate
            val env = (1.0 - i.toDouble() / n).toFloat()
            val v = (sin(2.0 * PI * freq * t) * 0.6 * env).toFloat()
            pcm[i] = (v * 32767).toInt().toShort()
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
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
    } catch (e: Exception) {
        // ignore
    }
}

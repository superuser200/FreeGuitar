package com.freeguitar.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.freeguitar.audio.AudioEngine
import com.freeguitar.audio.ChordMatcher
import com.freeguitar.audio.ChordPlayer
import com.freeguitar.audio.NoteUtils
import com.freeguitar.audio.PitchDetector
import com.freeguitar.data.Chords
import com.freeguitar.data.GuitarChord
import com.freeguitar.data.Song
import com.freeguitar.data.SongStore
import com.freeguitar.data.Songs
import com.freeguitar.data.UpdatePrefs
import com.freeguitar.data.UpdateService
import kotlinx.coroutines.delay

/** Requests mic permission and returns whether it was granted. */
@Composable
fun rememberMicPermission(): Boolean {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            android.content.pm.PackageManager.PERMISSION_GRANTED ==
                    context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }
    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
    return granted
}

// ---------- CHORD LIBRARY ----------

@Composable
fun ChordLibraryScreen(onChordClick: (GuitarChord) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(Chords.all) { chord ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChordClick(chord) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        chord.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        chord.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    ChordDiagram(chord, Modifier.height(130.dp))
                }
            }
        }
    }
}

// ---------- CHORD DETAIL ----------

@Composable
fun ChordDetailScreen(chord: GuitarChord, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Back") }
            Spacer(Modifier.weight(1f))
            Text(chord.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
        ChordDiagram(chord, Modifier.fillMaxWidth())

        Spacer(Modifier.height(16.dp))
        Text(
            "Notes: ${chord.playedMidiNotes.joinToString(" ") { NoteUtils.midiToName(it) }}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "Frets: ${chord.frets.joinToString(" ") { if (it < 0) "x" else if (it == 0) "o" else it.toString() }}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { ChordPlayer.play(chord.playedMidiNotes) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Play chord", fontSize = 18.sp)
        }
    }
}

// ---------- SONG LIST ----------

@Composable
fun SongListScreen(onSongClick: (Song) -> Unit) {
    val context = LocalContext.current
    val allSongs = remember { Songs.all + SongStore.load(context) }
    Column(Modifier.padding(12.dp)) {
        Text(
            "Play-along songs",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
        Text(
            "Tune your guitar and hold each chord while you strum. The app listens and tells you when you hit the right note.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(12.dp))
        for (song in allSongs) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onSongClick(song) },
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Chords: ${song.chordProgression.joinToString(" ") { it.first }}   ${song.bpm} bpm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("▶", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ---------- SONG PLAYER ----------

@Composable
fun SongPlayerScreen(song: Song, onBack: () -> Unit) {
    val context = LocalContext.current
    val audioEngine = remember { AudioEngine(context) }
    val matcher = remember { ChordMatcher() }
    var detected by remember { mutableStateOf<PitchDetector.Result?>(null) }
    var matchScore by remember { mutableStateOf(0.0) }
    var playing by remember { mutableStateOf(false) }
    var beat by remember { mutableStateOf(0) }
    var chordIndex by remember { mutableStateOf(0) }
    var lastGood by remember { mutableStateOf(false) }
    var goodCount by remember { mutableStateOf(0) }

    val beatMs = (60000.0 / song.bpm).toLong()

    DisposableEffect(Unit) {
        onDispose { audioEngine.stop() }
    }

    LaunchedEffect(audioEngine) {
        audioEngine.start { result ->
            detected = result
            val currentChord = Chords.byName(song.chordAtBeat(beat)) ?: return@start
            if (result != null) {
                val score = matcher.feed(result, currentChord.playedMidiNotes)
                matchScore = score
                lastGood = score >= 0.62
                if (lastGood) goodCount++
            } else {
                matchScore = 0.0
                lastGood = false
            }
        }
    }

    LaunchedEffect(playing, beatMs) {
        if (playing) {
            while (true) {
                chordIndex = song.indexAtBeat(beat)
                delay(beatMs)
                beat++
                if (beat >= song.totalBeats) beat = 0
            }
        }
    }

    val chord = Chords.byName(song.chordAtBeat(beat))
    val barBeat = beat % song.timeSignature

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack) { Text("Back") }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(20.dp))

        if (chord != null) {
            ChordDiagram(chord, Modifier.fillMaxWidth())
            Text(
                chord.name,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = if (lastGood) Color(0xFF2ECC71) else MaterialTheme.colorScheme.primary
            )
            Text(chord.fullName, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(12.dp))

        // Beat indicator
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (i in 0 until song.timeSignature) {
                val active = playing && barBeat == i
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(30.dp)
                        .clip(CircleShape)
                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
        Text(
            "Bar ${beat / song.timeSignature + 1}   Strum: ${song.strum}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        // Mic status
        Surface(
            color = if (lastGood) Color(0xFF1F5A38) else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (lastGood) "✓ CHORD MATCH!"
                    else if (detected != null) "Keep strumming the chord..."
                    else "Listening for your guitar...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (lastGood) Color(0xFF7CFC98) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                detected?.let { d ->
                    Text(
                        "Detected: ${NoteUtils.midiToName(NoteUtils.nearestNoteMidi(d.freq))}   Match: ${(matchScore * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (lastGood) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Good hits: $goodCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7CFC98)
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { playing = !playing },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(if (playing) "Pause" else "Start", fontSize = 20.sp)
        }
    }
}

// ---------- TUNER ----------

@Composable
fun TunerScreen() {
    val context = LocalContext.current
    val audioEngine = remember { AudioEngine(context) }
    var detected by remember { mutableStateOf<PitchDetector.Result?>(null) }
    var listening by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { audioEngine.stop() }
    }

    val start = {
        val ok = audioEngine.start { result ->
            detected = result
        }
        listening = ok
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Tuner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Pluck one string at a time. Aim for the green zone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Big note display
        val midi = detected?.let { NoteUtils.nearestNoteMidi(it.freq) }
        val cents = detected?.let { NoteUtils.freqToCents(it.freq) }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (midi != null) {
                    Text(
                        NoteUtils.midiToName(midi),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        color = if (cents != null && Math.abs(cents) <= 5) Color(0xFF2ECC71) else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "octave ${NoteUtils.midiToOctave(midi)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    val abs = Math.abs(cents ?: 0)
                    val status = when {
                        abs <= 5 -> "IN TUNE ✓"
                        (cents ?: 0) < 0 -> "SHARP — tune down"
                        else -> "FLAT — tune up"
                    }
                    Text(status, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    // cents bar
                    CentsBar(cents ?: 0)
                } else {
                    Text(
                        "—",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(if (listening) "Listening..." else "Start the tuner", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (listening) {
                    audioEngine.stop()
                    listening = false
                    detected = null
                } else start()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(if (listening) "Stop" else "Start Tuner", fontSize = 20.sp)
        }
    }
}

@Composable
fun CentsBar(cents: Int) {
    val width = 260.dp
    Box(
        Modifier
            .width(width)
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val center = width / 2
        val clamp = cents.coerceIn(-50, 50)
        val pos = center + androidx.compose.ui.unit.Dp(clamp.toFloat() * (width / 2).value / 50f) - (10.dp)
        Box(
            Modifier
                .width(20.dp)
                .height(14.dp)
                .offset(x = pos)
                .clip(RoundedCornerShape(4.dp))
                .background(if (Math.abs(cents) <= 5) Color(0xFF2ECC71) else Color(0xFFE67E22))
        )
        Box(
            Modifier
                .width(1.dp)
                .height(14.dp)
                .offset(x = center - 1.dp)
                .background(Color.White)
        )
    }
}

// ---------- UPDATES (auto-update from free database) ----------

@Composable
fun UpdatesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf(UpdatePrefs.getUrl(context)) }
    var autoCheck by remember { mutableStateOf(UpdatePrefs.autoCheckEnabled(context)) }
    var checking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var manifest by remember { mutableStateOf<UpdateService.Manifest?>(null) }
    var installedSongs by remember { mutableStateOf(SongStore.load(context)) }
    var downloadingApk by remember { mutableStateOf(false) }

    val appVersion = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionCode }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Auto-Updates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "This app is 100% free, forever. Point it at any free URL (like a GitHub Pages JSON file) and it will fetch new songs and app updates automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Update database URL (JSON)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    UpdatePrefs.setUrl(context, url)
                    checking = true
                    message = ""
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { UpdateService.fetchManifest(url) }
                        manifest = result
                        checking = false
                        message = when {
                            result == null -> "Could not reach that URL."
                            result.app != null && result.app.versionCode > appVersion ->
                                "App update available (v${result.app.versionName})"
                            result.songs.isNotEmpty() -> "${result.songs.size} new song(s) available!"
                            else -> "You're up to date!"
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Check for Updates")
            }

            if (manifest?.songs?.isNotEmpty() == true) {
                Button(
                    onClick = {
                        SongStore.addSongs(context, manifest!!.songs)
                        installedSongs = SongStore.load(context)
                        manifest = manifest!!.copy(songs = emptyList())
                        message = "Songs added to your library!"
                    }
                ) {
                    Text("Add ${manifest!!.songs.size} Songs")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Switch(
                checked = autoCheck,
                onCheckedChange = {
                    autoCheck = it
                    UpdatePrefs.setAutoCheck(context, it)
                }
            )
            Spacer(Modifier.width(10.dp))
            Text("Check for new songs when the app opens")
        }

        if (checking) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.width(24.dp).height(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Checking...")
            }
        }

        if (message.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(16.dp))

        manifest?.app?.let { app ->
            if (app.versionCode > appVersion) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("New app version available", fontWeight = FontWeight.Bold)
                        Text("v${app.versionName}", style = MaterialTheme.typography.headlineSmall)
                        if (app.notes.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(app.notes, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                downloadingApk = true
                                scope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        UpdateService.downloadApk(app.apkUrl, context.cacheDir)
                                    }
                                    downloadingApk = false
                                    if (file != null) {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                setDataAndType(
                                                    androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file
                                                    ),
                                                    "application/vnd.android.package-archive"
                                                )
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            message = "Install failed. Enable 'Install unknown apps' for this app in Settings."
                                        }
                                    } else {
                                        message = "Download failed."
                                    }
                                }
                            },
                            enabled = !downloadingApk,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (downloadingApk) "Downloading..." else "Download & Install Update")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Your downloaded songs", fontWeight = FontWeight.Bold)
        if (installedSongs.isEmpty()) {
            Text("None yet. Check for updates to grab new ones.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            for (s in installedSongs) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("🎵 ${s.title} — ${s.artist}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            "Installed app version: $appVersion",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

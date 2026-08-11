package com.freeguitar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.freeguitar.data.GuitarChord
import com.freeguitar.data.Song
import com.freeguitar.data.SongStore
import com.freeguitar.data.UpdatePrefs
import com.freeguitar.data.UpdateService
import com.freeguitar.ui.ChordDetailScreen
import com.freeguitar.ui.ChordLibraryScreen
import com.freeguitar.ui.MetronomeScreen
import com.freeguitar.ui.SongListScreen
import com.freeguitar.ui.SongPlayerScreen
import com.freeguitar.ui.TunerScreen
import com.freeguitar.ui.UpdatesScreen
import com.freeguitar.ui.rememberMicPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColors()) {
                Surface(Modifier.fillMaxSize()) {
                    FreeGuitarApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeGuitarApp() {
    rememberMicPermission()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(0) }
    var selectedChord by remember { mutableStateOf<GuitarChord?>(null) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }

    // Auto-check for new songs from the update database on every app open
    LaunchedEffect(Unit) {
        if (UpdatePrefs.autoCheckEnabled(context)) {
            val url = UpdatePrefs.getUrl(context)
            if (url.isNotBlank()) {
                val manifest = withContext(Dispatchers.IO) { UpdateService.fetchManifest(url) }
                manifest?.songs?.takeIf { it.isNotEmpty() }?.let { newSongs ->
                    val before = SongStore.load(context).size
                    SongStore.addSongs(context, newSongs)
                    val after = SongStore.load(context).size
                    if (after > before) {
                        snackbarHostState.showSnackbar("${after - before} new song(s) downloaded!")
                    }
                }
            }
        }
    }

    val titles = listOf("Chords", "Songs", "Tuner", "Metronome", "Updates")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            selectedChord != null -> "Chord"
                            selectedSong != null -> "Play Along"
                            else -> titles[tab]
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF14141F),
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (selectedChord == null && selectedSong == null) {
                NavigationBar(containerColor = Color(0xFF14141F)) {
                    for (i in 0..4) {
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = {
                                Text(
                                    when (i) {
                                        0 -> "🎸"
                                        1 -> "🎵"
                                        2 -> "🎯"
                                        3 -> "⏱️"
                                        else -> "🔄"
                                    }
                                )
                            },
                            label = { Text(titles[i]) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            when {
                selectedChord != null -> ChordDetailScreen(selectedChord!!) { selectedChord = null }
                selectedSong != null -> SongPlayerScreen(selectedSong!!) { selectedSong = null }
                tab == 0 -> ChordLibraryScreen { selectedChord = it }
                tab == 1 -> SongListScreen { selectedSong = it }
                tab == 2 -> TunerScreen()
                tab == 3 -> MetronomeScreen()
                else -> UpdatesScreen()
            }
        }
    }
}

@Composable
fun darkColors() = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFF3A2500),
    background = Color(0xFF0F0F17),
    surface = Color(0xFF1A1A26),
    surfaceVariant = Color(0xFF23232F),
    onSurface = Color(0xFFEAEAEA),
    onSurfaceVariant = Color(0xFF9A9AAB),
    onBackground = Color(0xFFEAEAEA),
    error = Color(0xFFE74C3C)
)

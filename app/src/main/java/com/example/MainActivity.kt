package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DownloadItem
import com.example.model.Song
import com.example.model.ThemePreset
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppTab
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyatoPlayerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = MyatoApplication.instance
        val musicRepo = app.musicRepository
        val settingsRepo = app.settingsRepository
        val downloadRepo = app.downloadRepository
        val player = app.playerManager

        setContent {
            val settings by settingsRepo.settings.collectAsStateWithLifecycle()
            val allSongs by musicRepo.allSongs.collectAsStateWithLifecycle(emptyList())
            val favoriteSongs by musicRepo.favoriteSongs.collectAsStateWithLifecycle(emptyList())
            val recentlyAdded by musicRepo.recentlyAdded.collectAsStateWithLifecycle(emptyList())
            val recentlyPlayed by musicRepo.recentlyPlayed.collectAsStateWithLifecycle(emptyList())
            val allPlaylists by musicRepo.allPlaylists.collectAsStateWithLifecycle(emptyList())
            val allDownloads by downloadRepo.allDownloads.collectAsStateWithLifecycle(emptyList())

            val playbackState by player.playbackState.collectAsStateWithLifecycle()
            val equalizerState by player.equalizerState.collectAsStateWithLifecycle()
            val visualizerBars by player.visualizerBars.collectAsStateWithLifecycle()

            var currentTab by remember { mutableStateOf(AppTab.HOME) }
            var isFullPlayerVisible by remember { mutableStateOf(false) }
            var isQueueVisible by remember { mutableStateOf(false) }
            var isSearchVisible by remember { mutableStateOf(false) }

            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            // Ensure sample songs are available on first run
            LaunchedEffect(Unit) {
                musicRepo.ensureSampleSongsIfEmpty()
            }

            // Restore last playback queue and position
            LaunchedEffect(allSongs) {
                if (allSongs.isNotEmpty()) {
                    player.restoreLastQueueIfAvailable(allSongs)
                }
            }

            // BackHandler for sub-screens and modals
            BackHandler(enabled = isQueueVisible || isFullPlayerVisible || isSearchVisible || currentTab != AppTab.HOME) {
                when {
                    isQueueVisible -> isQueueVisible = false
                    isFullPlayerVisible -> isFullPlayerVisible = false
                    isSearchVisible -> isSearchVisible = false
                    currentTab != AppTab.HOME -> currentTab = AppTab.HOME
                }
            }

            MyatoPlayerTheme(settings = settings) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        Column(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            // Docked MiniPlayer
                            val progress = if (playbackState.durationMs > 0) {
                                playbackState.currentPositionMs.toFloat() / playbackState.durationMs
                            } else 0f

                            MiniPlayer(
                                song = playbackState.currentSong,
                                isPlaying = playbackState.isPlaying,
                                progress = progress,
                                onPlayPause = { player.togglePlayPause() },
                                onNext = { player.next() },
                                onClick = { isFullPlayerVisible = true },
                                onOpenQueue = { isQueueVisible = true }
                            )

                            // 5-tab Navigation Bar
                            AppBottomNav(
                                currentTab = currentTab,
                                onTabSelected = { currentTab = it }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        // Current Tab Screen
                        when (currentTab) {
                            AppTab.HOME -> {
                                HomeScreen(
                                    recentlyPlayed = recentlyPlayed,
                                    recentlyAdded = recentlyAdded,
                                    allSongs = allSongs,
                                    onSongSelected = { song, list ->
                                        player.playSong(song, list)
                                    },
                                    onNavigateTab = { currentTab = it },
                                    onOpenSearch = { isSearchVisible = true }
                                )
                            }
                            AppTab.LIBRARY -> {
                                LibraryScreen(
                                    songs = allSongs,
                                    favoriteSongs = favoriteSongs,
                                    playlists = allPlaylists,
                                    onSongSelected = { song, list ->
                                        player.playSong(song, list)
                                    },
                                    onToggleFavorite = { song ->
                                        scope.launch { musicRepo.toggleFavorite(song) }
                                    },
                                    onImportUris = { uris ->
                                        scope.launch {
                                            var imported = 0
                                            uris.forEach { uri ->
                                                if (musicRepo.importAudioUri(uri) != null) imported++
                                            }
                                            Toast.makeText(this@MainActivity, "$imported música(s) importada(s)!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onScanDevice = {
                                        scope.launch {
                                            val count = musicRepo.scanDeviceAudio()
                                            Toast.makeText(this@MainActivity, "$count música(s) encontradas no celular", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onCreatePlaylist = { name ->
                                        scope.launch { musicRepo.createPlaylist(name) }
                                    },
                                    onRenamePlaylist = { pId, newName ->
                                        scope.launch {
                                            musicRepo.renamePlaylist(pId, newName)
                                            Toast.makeText(this@MainActivity, "Playlist renomeada", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDuplicatePlaylist = { pId, copyName ->
                                        scope.launch {
                                            musicRepo.duplicatePlaylist(pId, copyName)
                                            Toast.makeText(this@MainActivity, "Playlist duplicada com sucesso", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDeletePlaylist = { id ->
                                        scope.launch { musicRepo.deletePlaylist(id) }
                                    },
                                    onAddSongToPlaylist = { pId, sId ->
                                        scope.launch {
                                            musicRepo.addSongToPlaylist(pId, sId)
                                            Toast.makeText(this@MainActivity, "Música adicionada à playlist!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onAddSongsToPlaylistBatch = { pId, sIds ->
                                        scope.launch {
                                            musicRepo.addSongsToPlaylist(pId, sIds)
                                        }
                                    },
                                    onAddToFavoritesBatch = { ids ->
                                        scope.launch {
                                            musicRepo.addSongsToFavorites(ids)
                                        }
                                    },
                                    onDeleteSong = { song ->
                                        scope.launch {
                                            player.onSongDeleted(song.id)
                                            musicRepo.deleteSong(song)
                                            Toast.makeText(this@MainActivity, "\"${song.title}\" excluída com sucesso", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDeleteSongsBatch = { songsToDelete ->
                                        scope.launch {
                                            player.removeSongsFromQueue(songsToDelete.map { it.id }.toSet())
                                            musicRepo.deleteSongsBatch(songsToDelete)
                                        }
                                    },
                                    onPlaySelectedSongs = { selected ->
                                        player.playQueue(selected, startIndex = 0, title = "Músicas Selecionadas")
                                        isFullPlayerVisible = true
                                    },
                                    onAddSongsToQueue = { songsToAdd ->
                                        player.addListToQueue(songsToAdd)
                                    },
                                    onPlayNextSong = { song ->
                                        player.playNext(song)
                                    },
                                    onImportM3u = { uri ->
                                        scope.launch {
                                            val result = musicRepo.importM3uPlaylist(uri)
                                            Toast.makeText(this@MainActivity, "Playlist \"${result.first}\" importada (${result.second} músicas)!", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    onViewArtist = { artist ->
                                        isSearchVisible = true
                                    }
                                )
                            }
                            AppTab.DOWNLOADS -> {
                                DownloadsScreen(
                                    downloads = allDownloads,
                                    onValidateUrl = { downloadRepo.validateUrl(it) },
                                    onStartDownload = { url, title ->
                                        scope.launch {
                                            downloadRepo.startDownload(url, title)
                                            Toast.makeText(this@MainActivity, "Download iniciado...", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onCancelDownload = { downloadRepo.cancelDownload(it) },
                                    onDeleteDownload = { id ->
                                        scope.launch { downloadRepo.deleteDownload(id) }
                                    },
                                    onClearHistory = {
                                        scope.launch { downloadRepo.clearHistory() }
                                    },
                                    onPlayDownloadedSong = { item ->
                                        val song = allSongs.find { it.title == item.title || it.path == item.filePath }
                                        if (song != null) {
                                            player.playSong(song)
                                        } else {
                                            Toast.makeText(this@MainActivity, "Arquivo disponível na biblioteca", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onNavigateToLibrary = { currentTab = AppTab.LIBRARY }
                                )
                            }
                            AppTab.EQUALIZER -> {
                                EqualizerScreen(
                                    equalizerState = equalizerState,
                                    onToggleEnabled = { player.setEqualizerEnabled(it) },
                                    onSelectPreset = { player.applyPreset(it) },
                                    onBandLevelChanged = { band, level -> player.setBandLevel(band, level) },
                                    onBassBoostChanged = { player.setBassBoost(it) },
                                    onVirtualizerChanged = { player.setVirtualizer(it) },
                                    onReset = { player.resetEqualizer() }
                                )
                            }
                            AppTab.SETTINGS -> {
                                SettingsScreen(
                                    settings = settings,
                                    totalSongs = allSongs.size,
                                    onUpdateTheme = { settingsRepo.updateTheme(it) },
                                    onUpdateCustomColors = { prim, sec -> settingsRepo.updateCustomColors(prim, sec) },
                                    onUpdateGlowIntensity = { settingsRepo.updateGlowIntensity(it) },
                                    onUpdateEnableAnimations = { settingsRepo.updateEnableAnimations(it) },
                                    onUpdateBackground = { uri, blur, dim -> settingsRepo.updateBackground(uri, blur, dim) },
                                    onUpdatePlaybackSpeed = { settingsRepo.updatePlaybackSpeed(it) },
                                    onUpdateGaplessPlayback = { settingsRepo.updateGaplessPlayback(it) },
                                    onUpdateRememberPosition = { settingsRepo.updateRememberPosition(it) },
                                    onClearPlaybackHistory = {
                                        scope.launch {
                                            musicRepo.clearHistory()
                                            Toast.makeText(this@MainActivity, "Histórico de reprodução limpo!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onClearAllSongs = {
                                        scope.launch {
                                            musicRepo.clearAllSongs()
                                            Toast.makeText(this@MainActivity, "Catálogo limpo. Seus arquivos originais permanecem intactos.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onScanLibrary = {
                                        if (!com.example.permission.AudioPermissionService.hasAudioPermission(this@MainActivity)) {
                                            currentTab = AppTab.LIBRARY
                                            Toast.makeText(this@MainActivity, "Autorize o acesso para carregar suas músicas", Toast.LENGTH_SHORT).show()
                                        } else {
                                            scope.launch {
                                                val count = musicRepo.scanDeviceAudio()
                                                Toast.makeText(this@MainActivity, "$count músicas re-escaneadas!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onRestoreDefaults = {
                                        settingsRepo.restoreDefaults()
                                        player.resetEqualizer()
                                        Toast.makeText(this@MainActivity, "Configurações restauradas com sucesso!", Toast.LENGTH_SHORT).show()
                                    },
                                    onNavigateTab = { currentTab = it }
                                )
                            }
                        }

                        // Search screen overlay
                        if (isSearchVisible) {
                            SearchScreen(
                                allSongs = allSongs,
                                onSongSelected = { song, list ->
                                    player.playSong(song, list)
                                    isSearchVisible = false
                                },
                                onClose = { isSearchVisible = false }
                            )
                        }

                        // Full player modal overlay with smooth slide-up animation
                        AnimatedVisibility(
                            visible = isFullPlayerVisible && playbackState.currentSong != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            PlayerScreen(
                                playbackState = playbackState,
                                visualizerBars = visualizerBars,
                                onPlayPause = { player.togglePlayPause() },
                                onNext = { player.next() },
                                onPrevious = { player.previous() },
                                onSeekTo = { player.seekTo(it) },
                                onToggleShuffle = { player.toggleShuffle() },
                                onCycleRepeat = { player.cycleRepeatMode() },
                                onVolumeChanged = { player.setVolume(it) },
                                onToggleFavorite = { song -> scope.launch { musicRepo.toggleFavorite(song) } },
                                onOpenEqualizer = {
                                    isFullPlayerVisible = false
                                    currentTab = AppTab.EQUALIZER
                                },
                                onOpenQueue = {
                                    isQueueVisible = true
                                },
                                onAddToQueue = { song ->
                                    player.addToQueue(song)
                                },
                                onAddToPlaylist = { song ->
                                    scope.launch {
                                        if (allPlaylists.isNotEmpty()) {
                                            musicRepo.addSongToPlaylist(allPlaylists.first().id, song.id)
                                            Toast.makeText(this@MainActivity, "Adicionada à playlist \"${allPlaylists.first().name}\"", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val newId = musicRepo.createPlaylist("Minhas Músicas")
                                            musicRepo.addSongToPlaylist(newId, song.id)
                                            Toast.makeText(this@MainActivity, "Playlist criada e música adicionada!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onUpdateBackground = { uri ->
                                    settingsRepo.updateBackground(uri, settings.backgroundBlur, settings.backgroundDimAlpha)
                                },
                                onClose = { isFullPlayerVisible = false },
                                onDeleteSong = { song ->
                                    scope.launch {
                                        player.onSongDeleted(song.id)
                                        musicRepo.deleteSong(song)
                                        Toast.makeText(this@MainActivity, "\"${song.title}\" excluída", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        // Queue Screen modal overlay with smooth slide-up animation
                        AnimatedVisibility(
                            visible = isQueueVisible,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            QueueScreen(
                                playbackState = playbackState,
                                playerManager = player,
                                playlists = allPlaylists,
                                onSaveQueueAsPlaylist = { name, ids ->
                                    scope.launch {
                                        val pId = musicRepo.createPlaylist(name)
                                        musicRepo.addSongsToPlaylist(pId, ids)
                                    }
                                },
                                onAddToPlaylistBatch = { pId, ids ->
                                    scope.launch {
                                        musicRepo.addSongsToPlaylist(pId, ids)
                                    }
                                },
                                onAddToFavoritesBatch = { ids ->
                                    scope.launch {
                                        musicRepo.addSongsToFavorites(ids)
                                    }
                                },
                                onDeleteSongsBatch = { songsToDelete ->
                                    scope.launch {
                                        player.removeSongsFromQueue(songsToDelete.map { it.id }.toSet())
                                        musicRepo.deleteSongsBatch(songsToDelete)
                                    }
                                },
                                onViewArtist = { artist ->
                                    isQueueVisible = false
                                    isFullPlayerVisible = false
                                    currentTab = AppTab.LIBRARY
                                },
                                onClose = { isQueueVisible = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

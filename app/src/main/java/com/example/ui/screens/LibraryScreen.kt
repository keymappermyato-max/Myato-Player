package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.MyatoApplication
import com.example.R
import com.example.model.Playlist
import com.example.model.Song
import com.example.permission.AudioPermissionService
import com.example.ui.components.AudioCoverImage
import com.example.ui.components.AudioPermissionPromptDialog
import com.example.ui.components.MultiSelectBottomActionBar
import com.example.ui.components.MultiSelectTopBar
import com.example.ui.components.PlaylistDetailDialog
import com.example.ui.components.SongActionBottomSheet
import com.example.util.M3uUtils
import com.example.util.ShareUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class LibraryTab(val title: String) {
    ALL("Todas"),
    ARTISTS("Artistas"),
    ALBUMS("Álbuns"),
    FOLDERS("Pastas"),
    FAVORITES("Favoritos"),
    PLAYLISTS("Playlists")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    favoriteSongs: List<Song>,
    playlists: List<Playlist>,
    onSongSelected: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onImportUris: (List<Uri>) -> Unit,
    onScanDevice: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDuplicatePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddSongToPlaylist: (Long, Long) -> Unit,
    onAddSongsToPlaylistBatch: (Long, List<Long>) -> Unit,
    onAddToFavoritesBatch: (List<Long>) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onDeleteSongsBatch: (List<Song>) -> Unit,
    onPlaySelectedSongs: (List<Song>) -> Unit,
    onAddSongsToQueue: (List<Song>) -> Unit,
    onPlayNextSong: (Song) -> Unit,
    onImportM3u: (Uri) -> Unit,
    onViewArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Multi-Selection state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    // Dialogs
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showBatchPlaylistDialog by remember { mutableStateOf(false) }
    var batchSongsToAdd by remember { mutableStateOf<List<Long>>(emptyList()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    // Individual Song Options BottomSheet
    var activeSongForOptions by remember { mutableStateOf<Song?>(null) }

    // Playlist Details Modal
    var selectedPlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }
    var detailPlaylistSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    var showPermissionPromptDialog by remember { mutableStateOf(false) }
    var isPermissionPermanentlyDenied by remember { mutableStateOf(false) }

    val hasAudioPermission = AudioPermissionService.hasAudioPermission(context)

    // SAF file picker launcher for audio files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) onImportUris(uris)
    }

    // SAF M3U playlist file picker
    val m3uPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportM3u(it) }
    }

    // Media permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isPermissionPermanentlyDenied = false
            onScanDevice()
        } else {
            val activity = context as? android.app.Activity
            isPermissionPermanentlyDenied = activity != null && !AudioPermissionService.shouldShowRationale(activity)
        }
    }

    val requestScanWithPermission = {
        if (AudioPermissionService.hasAudioPermission(context)) {
            onScanDevice()
        } else {
            showPermissionPromptDialog = true
        }
    }

    // Filter songs by search query
    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else songs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.fillMaxSize().testTag("library_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isMultiSelectMode) {
                MultiSelectTopBar(
                    selectedCount = selectedIds.size,
                    totalCount = filteredSongs.size,
                    onSelectAll = {
                        selectedIds.clear()
                        selectedIds.addAll(filteredSongs.map { it.id })
                    },
                    onDeselectAll = { selectedIds.clear() },
                    onClose = {
                        isMultiSelectMode = false
                        selectedIds.clear()
                    }
                )
            } else {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Biblioteca",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Multi-select toggle button
                        IconButton(
                            onClick = { isMultiSelectMode = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("library_multi_select_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selecionar Várias",
                                tint = primaryColor
                            )
                        }

                        // Import M3U playlist file button
                        IconButton(
                            onClick = {
                                m3uPickerLauncher.launch(
                                    arrayOf(
                                        "audio/x-mpegurl",
                                        "application/x-mpegurl",
                                        "audio/mpegurl",
                                        "text/plain",
                                        "*/*"
                                    )
                                )
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("library_import_m3u_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Importar Playlist M3U",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Import file button
                        IconButton(
                            onClick = { filePickerLauncher.launch(arrayOf("audio/*", "application/ogg")) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("library_import_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Importar Músicas do Celular",
                                tint = primaryColor
                            )
                        }

                        // Re-scan device
                        IconButton(
                            onClick = requestScanWithPermission,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("library_rescan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Escanear Dispositivo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .testTag("library_search_input"),
                placeholder = { Text("Filtrar na biblioteca...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = primaryColor
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Permission Prompt Banner
            if (!hasAudioPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clickable { showPermissionPromptDialog = true }
                        .testTag("library_permission_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Carregar Músicas do Celular",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Autorize o acesso para escanear sua biblioteca automaticamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showPermissionPromptDialog = true },
                            modifier = Modifier.testTag("library_permission_prompt_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("Escanear", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Category Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 20.dp,
                containerColor = Color.Transparent,
                contentColor = primaryColor,
                divider = {}
            ) {
                LibraryTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Content
            when (LibraryTab.entries[selectedTabIndex]) {
                LibraryTab.ALL -> {
                    AllSongsTab(
                        songs = filteredSongs,
                        isMultiSelectMode = isMultiSelectMode,
                        selectedIds = selectedIds,
                        onSongSelected = { song ->
                            if (isMultiSelectMode) {
                                if (selectedIds.contains(song.id)) selectedIds.remove(song.id) else selectedIds.add(song.id)
                            } else {
                                onSongSelected(song, filteredSongs)
                            }
                        },
                        onSongLongClick = { song ->
                            if (!isMultiSelectMode) {
                                isMultiSelectMode = true
                                selectedIds.add(song.id)
                            }
                        },
                        onToggleFavorite = onToggleFavorite,
                        onOptionsClick = { activeSongForOptions = it },
                        onImportClicked = { filePickerLauncher.launch(arrayOf("audio/*", "application/ogg")) }
                    )
                }
                LibraryTab.ARTISTS -> {
                    ArtistsTab(
                        songs = filteredSongs,
                        onSongSelected = { song, list -> onSongSelected(song, list) }
                    )
                }
                LibraryTab.ALBUMS -> {
                    AlbumsTab(
                        songs = filteredSongs,
                        onSongSelected = { song, list -> onSongSelected(song, list) }
                    )
                }
                LibraryTab.FOLDERS -> {
                    FoldersTab(
                        songs = filteredSongs,
                        onSongSelected = { song, list -> onSongSelected(song, list) }
                    )
                }
                LibraryTab.FAVORITES -> {
                    FavoritesTab(
                        favorites = favoriteSongs,
                        isMultiSelectMode = isMultiSelectMode,
                        selectedIds = selectedIds,
                        onSongSelected = { song ->
                            if (isMultiSelectMode) {
                                if (selectedIds.contains(song.id)) selectedIds.remove(song.id) else selectedIds.add(song.id)
                            } else {
                                onSongSelected(song, favoriteSongs)
                            }
                        },
                        onSongLongClick = { song ->
                            if (!isMultiSelectMode) {
                                isMultiSelectMode = true
                                selectedIds.add(song.id)
                            }
                        },
                        onToggleFavorite = onToggleFavorite,
                        onOptionsClick = { activeSongForOptions = it }
                    )
                }
                LibraryTab.PLAYLISTS -> {
                    PlaylistsTab(
                        playlists = playlists,
                        allSongs = songs,
                        onCreatePlaylist = { showCreatePlaylistDialog = true },
                        onDeletePlaylist = onDeletePlaylist,
                        onOpenPlaylistDetail = { playlist ->
                            scope.launch {
                                detailPlaylistSongs = MyatoApplication.instance.musicRepository.getSongsForPlaylist(playlist.id).first()
                                selectedPlaylistForDetail = playlist
                            }
                        },
                        onPlayPlaylist = { playlist ->
                            scope.launch {
                                val pSongs = MyatoApplication.instance.musicRepository.getSongsForPlaylist(playlist.id).first()
                                if (pSongs.isNotEmpty()) {
                                    onSongSelected(pSongs.first(), pSongs)
                                } else {
                                    Toast.makeText(context, "Playlist vazia", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        // Floating Bottom Multi-Select Action Bar
        AnimatedVisibility(
            visible = isMultiSelectMode && selectedIds.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            val selectedSongs = remember(selectedIds.size, songs) {
                songs.filter { it.id in selectedIds }
            }

            MultiSelectBottomActionBar(
                selectedCount = selectedIds.size,
                isInQueue = false,
                onPlaySelected = {
                    if (selectedSongs.isNotEmpty()) {
                        onPlaySelectedSongs(selectedSongs)
                        isMultiSelectMode = false
                        selectedIds.clear()
                    }
                },
                onAddToQueue = {
                    onAddSongsToQueue(selectedSongs)
                    Toast.makeText(context, "${selectedSongs.size} músicas adicionadas à fila!", Toast.LENGTH_SHORT).show()
                    isMultiSelectMode = false
                    selectedIds.clear()
                },
                onAddToPlaylist = {
                    batchSongsToAdd = selectedIds.toList()
                    showBatchPlaylistDialog = true
                },
                onShare = { ShareUtils.shareMultipleSongs(context, selectedSongs) },
                onRemoveFromQueue = null,
                onDeleteSelected = { showBatchDeleteConfirm = true },
                onAddToFavorites = {
                    onAddToFavoritesBatch(selectedIds.toList())
                    Toast.makeText(context, "${selectedIds.size} adicionada(s) aos favoritos!", Toast.LENGTH_SHORT).show()
                    isMultiSelectMode = false
                    selectedIds.clear()
                },
                onMoveToPlaylist = {
                    batchSongsToAdd = selectedIds.toList()
                    showBatchPlaylistDialog = true
                }
            )
        }

        // Dialog for creating new playlist
        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                title = { Text("Nova Playlist") },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Nome da Playlist") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreatePlaylist(newPlaylistName.trim())
                                newPlaylistName = ""
                                showCreatePlaylistDialog = false
                            }
                        }
                    ) {
                        Text("Criar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Dialog for batch adding songs to a playlist
        if (showBatchPlaylistDialog) {
            AlertDialog(
                onDismissRequest = { showBatchPlaylistDialog = false },
                title = { Text("Adicionar à Playlist") },
                text = {
                    if (playlists.isEmpty()) {
                        Text("Nenhuma playlist criada ainda. Crie uma nova primeiro!")
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(playlists) { playlist ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddSongsToPlaylistBatch(playlist.id, batchSongsToAdd)
                                            Toast.makeText(
                                                context,
                                                "${batchSongsToAdd.size} adicionada(s) a \"${playlist.name}\"!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showBatchPlaylistDialog = false
                                            isMultiSelectMode = false
                                            selectedIds.clear()
                                        }
                                        .padding(vertical = 10.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.QueueMusic,
                                            contentDescription = null,
                                            tint = primaryColor
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(text = playlist.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                            Text(text = "${playlist.songCount} faixas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBatchPlaylistDialog = false }) {
                        Text("Fechar")
                    }
                }
            )
        }

        // Batch Delete Confirmation
        if (showBatchDeleteConfirm) {
            val selectedSongs = remember(selectedIds.size, songs) {
                songs.filter { it.id in selectedIds }
            }
            AlertDialog(
                onDismissRequest = { showBatchDeleteConfirm = false },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Excluir ${selectedSongs.size} Música(s)?") },
                text = { Text("As músicas serão removidas da sua biblioteca e os arquivos apagados definitivamente.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showBatchDeleteConfirm = false
                            onDeleteSongsBatch(selectedSongs)
                            Toast.makeText(context, "${selectedSongs.size} músicas excluídas", Toast.LENGTH_SHORT).show()
                            isMultiSelectMode = false
                            selectedIds.clear()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Excluir Todas")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteConfirm = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Single Song Options Sheet
        activeSongForOptions?.let { song ->
            SongActionBottomSheet(
                song = song,
                isInQueue = false,
                onDismiss = { activeSongForOptions = null },
                onPlayNow = { onSongSelected(song, filteredSongs) },
                onPlayNext = {
                    onPlayNextSong(song)
                    Toast.makeText(context, "\"${song.title}\" tocará a seguir!", Toast.LENGTH_SHORT).show()
                },
                onAddToQueue = {
                    onAddSongsToQueue(listOf(song))
                    Toast.makeText(context, "\"${song.title}\" adicionada à fila", Toast.LENGTH_SHORT).show()
                },
                onToggleFavorite = { onToggleFavorite(song) },
                onAddToPlaylist = {
                    batchSongsToAdd = listOf(song.id)
                    showBatchPlaylistDialog = true
                },
                onShare = { ShareUtils.shareSong(context, song) },
                onViewArtist = onViewArtist,
                onRemoveFromQueue = null,
                onDeleteFile = {
                    onDeleteSong(song)
                }
            )
        }

        // Playlist Detail Full Dialog
        selectedPlaylistForDetail?.let { playlist ->
            PlaylistDetailDialog(
                playlist = playlist,
                playlistSongs = detailPlaylistSongs,
                allSongs = songs,
                onDismiss = { selectedPlaylistForDetail = null },
                onPlayPlaylist = { songsList, startIndex ->
                    if (songsList.isNotEmpty()) {
                        val first = songsList.getOrNull(startIndex) ?: songsList.first()
                        onSongSelected(first, songsList)
                    }
                },
                onAddSongsToPlaylist = { pId, songIds ->
                    onAddSongsToPlaylistBatch(pId, songIds)
                    scope.launch {
                        detailPlaylistSongs = MyatoApplication.instance.musicRepository.getSongsForPlaylist(pId).first()
                    }
                },
                onRemoveSongFromPlaylist = { pId, songId ->
                    scope.launch {
                        MyatoApplication.instance.musicRepository.removeSongFromPlaylist(pId, songId)
                        detailPlaylistSongs = MyatoApplication.instance.musicRepository.getSongsForPlaylist(pId).first()
                    }
                },
                onRemoveSongsBatch = { pId, songIds ->
                    scope.launch {
                        MyatoApplication.instance.musicRepository.removeSongsFromPlaylist(pId, songIds)
                        detailPlaylistSongs = MyatoApplication.instance.musicRepository.getSongsForPlaylist(pId).first()
                    }
                },
                onReorderSongs = { pId, orderedIds ->
                    scope.launch {
                        MyatoApplication.instance.musicRepository.reorderPlaylistSongs(pId, orderedIds)
                        detailPlaylistSongs = MyatoApplication.instance.musicRepository.getSongsForPlaylist(pId).first()
                    }
                },
                onRenamePlaylist = { pId, newName ->
                    onRenamePlaylist(pId, newName)
                    selectedPlaylistForDetail = playlist.copy(name = newName)
                },
                onDuplicatePlaylist = { pId, newName ->
                    onDuplicatePlaylist(pId, newName)
                },
                onDeletePlaylist = { pId ->
                    onDeletePlaylist(pId)
                    selectedPlaylistForDetail = null
                },
                onDeleteSongsBatch = onDeleteSongsBatch,
                onAddToFavoritesBatch = onAddToFavoritesBatch,
                onAddSongsToQueue = onAddSongsToQueue,
                onViewArtist = onViewArtist
            )
        }

        // Permission Prompt Dialog
        if (showPermissionPromptDialog) {
            AudioPermissionPromptDialog(
                isPermanentlyDenied = isPermissionPermanentlyDenied,
                onConfirmGrant = {
                    showPermissionPromptDialog = false
                    permissionLauncher.launch(AudioPermissionService.requiredAudioPermission)
                },
                onOpenSettings = {
                    showPermissionPromptDialog = false
                    AudioPermissionService.openAppSettings(context)
                },
                onDismiss = { showPermissionPromptDialog = false },
                onImportManually = {
                    showPermissionPromptDialog = false
                    filePickerLauncher.launch(arrayOf("audio/*", "application/ogg"))
                }
            )
        }
    }
}

@Composable
fun AllSongsTab(
    songs: List<Song>,
    isMultiSelectMode: Boolean,
    selectedIds: List<Long>,
    onSongSelected: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOptionsClick: (Song) -> Unit,
    onImportClicked: () -> Unit
) {
    if (songs.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nenhuma música encontrada",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Importe arquivos MP3, FLAC, WAV ou M4A do seu aparelho.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onImportClicked,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Importar Arquivos de Áudio")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                SongLibraryRow(
                    song = song,
                    isMultiSelectMode = isMultiSelectMode,
                    isSelected = selectedIds.contains(song.id),
                    onClick = { onSongSelected(song) },
                    onLongClick = { onSongLongClick(song) },
                    onToggleFavorite = { onToggleFavorite(song) },
                    onOptionsClick = { onOptionsClick(song) }
                )
            }
        }
    }
}

@Composable
fun SongLibraryRow(
    song: Song,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = if (isSelected) primaryColor.copy(alpha = 0.25f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("library_song_${song.id}"),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AudioCoverImage(
                    model = song.albumArtUri,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.durationFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isMultiSelectMode) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favoritar",
                        tint = if (song.isFavorite) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onOptionsClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Mais opções",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistsTab(
    songs: List<Song>,
    onSongSelected: (Song, List<Song>) -> Unit
) {
    val grouped = remember(songs) {
        songs.groupBy { it.artist }.toSortedMap()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        grouped.forEach { (artist, artistSongs) ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${artistSongs.size} faixas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        artistSongs.take(3).forEach { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSongSelected(song, artistSongs) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumsTab(
    songs: List<Song>,
    onSongSelected: (Song, List<Song>) -> Unit
) {
    val grouped = remember(songs) {
        songs.groupBy { it.album }.toSortedMap()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        grouped.forEach { (album, albumSongs) ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { albumSongs.firstOrNull()?.let { onSongSelected(it, albumSongs) } }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_default_art),
                                contentDescription = album,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${albumSongs.firstOrNull()?.artist ?: ""} • ${albumSongs.size} músicas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoldersTab(
    songs: List<Song>,
    onSongSelected: (Song, List<Song>) -> Unit
) {
    val grouped = remember(songs) {
        songs.groupBy { it.folderName }.toSortedMap()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        grouped.forEach { (folder, folderSongs) ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { folderSongs.firstOrNull()?.let { onSongSelected(it, folderSongs) } }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${folderSongs.size} arquivos de áudio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTab(
    favorites: List<Song>,
    isMultiSelectMode: Boolean,
    selectedIds: List<Long>,
    onSongSelected: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOptionsClick: (Song) -> Unit
) {
    if (favorites.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Nenhuma música favorita ainda",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Toque no ícone de coração para salvar suas faixas favoritas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(favorites, key = { it.id }) { song ->
                SongLibraryRow(
                    song = song,
                    isMultiSelectMode = isMultiSelectMode,
                    isSelected = selectedIds.contains(song.id),
                    onClick = { onSongSelected(song) },
                    onLongClick = { onSongLongClick(song) },
                    onToggleFavorite = { onToggleFavorite(song) },
                    onOptionsClick = { onOptionsClick(song) }
                )
            }
        }
    }
}

@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    allSongs: List<Song>,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onOpenPlaylistDetail: (Playlist) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${playlists.size} Playlists",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Button(
                onClick = onCreatePlaylist,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nova Playlist")
            }
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma playlist criada. Toque em 'Nova Playlist' para organizar suas músicas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPlaylistDetail(playlist) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${playlist.songCount} músicas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { onPlayPlaylist(playlist) }) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Reproduzir Playlist",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir Playlist",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

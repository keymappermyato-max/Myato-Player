package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Playlist
import com.example.model.Song
import com.example.player.AudioPlayerManager
import com.example.player.PlaybackState
import com.example.player.QueueSortType
import com.example.ui.components.AudioCoverImage
import com.example.ui.components.MultiSelectBottomActionBar
import com.example.ui.components.MultiSelectTopBar
import com.example.ui.components.SongActionBottomSheet
import com.example.util.M3uUtils
import com.example.util.ShareUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    playbackState: PlaybackState,
    playerManager: AudioPlayerManager,
    playlists: List<Playlist>,
    onSaveQueueAsPlaylist: (String, List<Long>) -> Unit,
    onAddToPlaylistBatch: (Long, List<Long>) -> Unit,
    onAddToFavoritesBatch: (List<Long>) -> Unit,
    onDeleteSongsBatch: (List<Song>) -> Unit,
    onViewArtist: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val queue = playbackState.queue
    val currentSong = playbackState.currentSong

    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    var showHeaderMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSaveQueueDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showClearQueueConfirm by remember { mutableStateOf(false) }

    // Dialog for batch adding to playlist
    var showPlaylistChooserDialog by remember { mutableStateOf(false) }
    var songsForPlaylistBatch by remember { mutableStateOf<List<Long>>(emptyList()) }

    // Individual Song Options sheet
    var activeSongForMenu by remember { mutableStateOf<Song?>(null) }
    var showDeleteConfirmSingle by remember { mutableStateOf<Song?>(null) }
    var showDeleteConfirmBatch by remember { mutableStateOf(false) }

    // Queue statistics
    val totalDurationMs = remember(queue) { queue.sumOf { it.durationMs } }
    val totalFormatted = remember(totalDurationMs) {
        val totalSec = totalDurationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val hr = min / 60
        if (hr > 0) "%dh %02dmin".format(hr, min % 60) else "%dmin %02ds".format(min, sec)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("queue_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isMultiSelectMode) {
                MultiSelectTopBar(
                    selectedCount = selectedIds.size,
                    totalCount = queue.size,
                    onSelectAll = {
                        selectedIds.clear()
                        selectedIds.addAll(queue.map { it.id })
                    },
                    onDeselectAll = { selectedIds.clear() },
                    onClose = {
                        isMultiSelectMode = false
                        selectedIds.clear()
                    }
                )
            } else {
                // Main Queue Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onClose, modifier = Modifier.testTag("queue_close_button")) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar fila",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playbackState.queueTitle.ifBlank { "Fila de Reprodução" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${queue.size} músicas • $totalFormatted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row {
                                // Sort button
                                Box {
                                    IconButton(onClick = { showSortMenu = true }, modifier = Modifier.testTag("queue_sort_button")) {
                                        Icon(
                                            imageVector = Icons.Default.Sort,
                                            contentDescription = "Ordenar fila",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Nome (A-Z)") },
                                            onClick = {
                                                showSortMenu = false
                                                playerManager.sortQueue(QueueSortType.TITLE)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Artista (A-Z)") },
                                            onClick = {
                                                showSortMenu = false
                                                playerManager.sortQueue(QueueSortType.ARTIST)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Álbum (A-Z)") },
                                            onClick = {
                                                showSortMenu = false
                                                playerManager.sortQueue(QueueSortType.ALBUM)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Duração") },
                                            onClick = {
                                                showSortMenu = false
                                                playerManager.sortQueue(QueueSortType.DURATION)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Inverter ordem") },
                                            onClick = {
                                                showSortMenu = false
                                                playerManager.sortQueue(QueueSortType.REVERSE)
                                            }
                                        )
                                    }
                                }

                                // 3-dot overflow menu
                                Box {
                                    IconButton(onClick = { showHeaderMenu = true }, modifier = Modifier.testTag("queue_menu_button")) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Mais opções da fila",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showHeaderMenu,
                                        onDismissRequest = { showHeaderMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Selecionar várias") },
                                            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                            onClick = {
                                                showHeaderMenu = false
                                                isMultiSelectMode = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Salvar fila atual") },
                                            leadingIcon = { Icon(Icons.Default.BookmarkAdd, contentDescription = null) },
                                            onClick = {
                                                showHeaderMenu = false
                                                showSaveQueueDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Exportar como arquivo .M3U") },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                            onClick = {
                                                showHeaderMenu = false
                                                scope.launch {
                                                    val file = M3uUtils.exportM3uFile(
                                                        context,
                                                        playbackState.queueTitle.ifBlank { "Fila_Myato" },
                                                        queue
                                                    )
                                                    ShareUtils.shareM3uFile(context, file, playbackState.queueTitle)
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Compartilhar músicas") },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                            onClick = {
                                                showHeaderMenu = false
                                                ShareUtils.shareMultipleSongs(context, queue)
                                            }
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Limpar fila", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                showHeaderMenu = false
                                                showClearQueueConfirm = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Header action buttons row: Play Queue, Save, Clear
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (queue.isNotEmpty()) {
                                        if (playbackState.isPlaying) {
                                            playerManager.pause()
                                        } else {
                                            playerManager.resume()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("queue_play_pause_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (playbackState.isPlaying) "Pausar Fila" else "Iniciar Fila")
                            }

                            Button(
                                onClick = { showSaveQueueDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("queue_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salvar Fila", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // Queue Tracklist
            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "A fila de reprodução está vazia",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Selecione músicas da sua biblioteca ou toque em uma playlist para iniciar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = if (isMultiSelectMode) 90.dp else 24.dp)
                ) {
                    itemsIndexed(queue, key = { index, item -> "${item.id}_$index" }) { index, song ->
                        val isCurrent = song.id == currentSong?.id
                        val isSelected = selectedIds.contains(song.id)

                        QueueSongRow(
                            song = song,
                            index = index,
                            totalCount = queue.size,
                            isCurrentPlaying = isCurrent,
                            isPlaying = playbackState.isPlaying,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = isSelected,
                            onSelect = {
                                if (isMultiSelectMode) {
                                    if (isSelected) selectedIds.remove(song.id) else selectedIds.add(song.id)
                                } else {
                                    playerManager.playSong(song, queue)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectMode) {
                                    isMultiSelectMode = true
                                    selectedIds.add(song.id)
                                }
                            },
                            onMoveUp = {
                                if (index > 0) playerManager.reorderQueue(index, index - 1)
                            },
                            onMoveDown = {
                                if (index < queue.size - 1) playerManager.reorderQueue(index, index + 1)
                            },
                            onOptionsClick = {
                                activeSongForMenu = song
                            }
                        )
                    }
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
            val selectedSongs = remember(selectedIds.size, queue) {
                queue.filter { it.id in selectedIds }
            }

            MultiSelectBottomActionBar(
                selectedCount = selectedIds.size,
                isInQueue = true,
                onPlaySelected = {
                    if (selectedSongs.isNotEmpty()) {
                        playerManager.playQueue(selectedSongs, startIndex = 0, title = "Músicas Selecionadas")
                        isMultiSelectMode = false
                        selectedIds.clear()
                    }
                },
                onAddToQueue = {
                    playerManager.addListToQueue(selectedSongs)
                    Toast.makeText(context, "${selectedSongs.size} músicas adicionadas à fila!", Toast.LENGTH_SHORT).show()
                    isMultiSelectMode = false
                    selectedIds.clear()
                },
                onAddToPlaylist = {
                    songsForPlaylistBatch = selectedIds.toList()
                    showPlaylistChooserDialog = true
                },
                onShare = {
                    ShareUtils.shareMultipleSongs(context, selectedSongs)
                },
                onRemoveFromQueue = {
                    playerManager.removeSongsFromQueue(selectedIds.toSet())
                    Toast.makeText(context, "${selectedIds.size} removida(s) da fila", Toast.LENGTH_SHORT).show()
                    isMultiSelectMode = false
                    selectedIds.clear()
                },
                onDeleteSelected = {
                    showDeleteConfirmBatch = true
                },
                onAddToFavorites = {
                    onAddToFavoritesBatch(selectedIds.toList())
                    Toast.makeText(context, "${selectedIds.size} adicionada(s) aos favoritos!", Toast.LENGTH_SHORT).show()
                    isMultiSelectMode = false
                    selectedIds.clear()
                },
                onMoveToPlaylist = {
                    songsForPlaylistBatch = selectedIds.toList()
                    showPlaylistChooserDialog = true
                }
            )
        }

        // Save Queue as Playlist Dialog
        if (showSaveQueueDialog) {
            AlertDialog(
                onDismissRequest = { showSaveQueueDialog = false },
                title = { Text("Salvar Fila como Playlist") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Crie uma nova playlist com todas as ${queue.size} faixas da fila atual:")
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Nome da Playlist") },
                            placeholder = { Text("Minha Fila Favorita") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank() && queue.isNotEmpty()) {
                                onSaveQueueAsPlaylist(newPlaylistName.trim(), queue.map { it.id })
                                Toast.makeText(context, "Playlist \"$newPlaylistName\" salva com sucesso!", Toast.LENGTH_SHORT).show()
                                newPlaylistName = ""
                                showSaveQueueDialog = false
                            }
                        }
                    ) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveQueueDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Clear Queue Confirmation Dialog
        if (showClearQueueConfirm) {
            AlertDialog(
                onDismissRequest = { showClearQueueConfirm = false },
                title = { Text("Limpar Fila?") },
                text = { Text("Deseja realmente limpar todas as músicas da fila de reprodução?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearQueueConfirm = false
                            playerManager.clearQueue()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Limpar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearQueueConfirm = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Playlist Chooser Dialog for Multi-Selection
        if (showPlaylistChooserDialog) {
            AlertDialog(
                onDismissRequest = { showPlaylistChooserDialog = false },
                title = { Text("Adicionar à Playlist") },
                text = {
                    if (playlists.isEmpty()) {
                        Text("Nenhuma playlist criada. Crie uma na aba Biblioteca > Playlists.")
                    } else {
                        LazyColumn(modifier = Modifier.height(220.dp)) {
                            itemsIndexed(playlists) { _, playlist ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddToPlaylistBatch(playlist.id, songsForPlaylistBatch)
                                            Toast.makeText(
                                                context,
                                                "${songsForPlaylistBatch.size} músicas adicionadas a \"${playlist.name}\"!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showPlaylistChooserDialog = false
                                            isMultiSelectMode = false
                                            selectedIds.clear()
                                        }
                                        .padding(vertical = 10.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistAdd,
                                            contentDescription = null,
                                            tint = primaryColor
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(playlist.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                            Text("${playlist.songCount} músicas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlaylistChooserDialog = false }) {
                        Text("Fechar")
                    }
                }
            )
        }

        // Batch Delete Confirmation
        if (showDeleteConfirmBatch) {
            val selectedSongs = remember(selectedIds.size, queue) {
                queue.filter { it.id in selectedIds }
            }
            AlertDialog(
                onDismissRequest = { showDeleteConfirmBatch = false },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Excluir ${selectedSongs.size} Música(s)?") },
                text = { Text("As músicas selecionadas serão removidas da biblioteca e excluídas definitivamente.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmBatch = false
                            onDeleteSongsBatch(selectedSongs)
                            playerManager.removeSongsFromQueue(selectedIds.toSet())
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
                    TextButton(onClick = { showDeleteConfirmBatch = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Single Song Options Sheet
        activeSongForMenu?.let { song ->
            SongActionBottomSheet(
                song = song,
                isInQueue = true,
                onDismiss = { activeSongForMenu = null },
                onPlayNow = { playerManager.playSong(song, queue) },
                onPlayNext = {
                    playerManager.playNext(song)
                    Toast.makeText(context, "\"${song.title}\" tocará a seguir!", Toast.LENGTH_SHORT).show()
                },
                onAddToQueue = {
                    playerManager.addToQueue(song)
                    Toast.makeText(context, "\"${song.title}\" adicionada à fila", Toast.LENGTH_SHORT).show()
                },
                onToggleFavorite = {
                    scope.launch {
                        com.example.MyatoApplication.instance.musicRepository.toggleFavorite(song)
                    }
                },
                onAddToPlaylist = {
                    songsForPlaylistBatch = listOf(song.id)
                    showPlaylistChooserDialog = true
                },
                onShare = { ShareUtils.shareSong(context, song) },
                onViewArtist = onViewArtist,
                onRemoveFromQueue = {
                    val idx = queue.indexOfFirst { it.id == song.id }
                    if (idx != -1) playerManager.removeFromQueue(idx)
                },
                onDeleteFile = {
                    onDeleteSongsBatch(listOf(song))
                    playerManager.removeSongsFromQueue(setOf(song.id))
                    Toast.makeText(context, "\"${song.title}\" excluída", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun QueueSongRow(
    song: Song,
    index: Int,
    totalCount: Int,
    isCurrentPlaying: Boolean,
    isPlaying: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onLongClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Pulsing indicator for active song
    val infiniteTransition = rememberInfiniteTransition(label = "row_active_pulse")
    val pulseAlpha by if (isCurrentPlaying && isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
            label = "pulse"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val backgroundColor = when {
        isSelected -> primaryColor.copy(alpha = 0.25f)
        isCurrentPlaying -> primaryColor.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = if (isSelected) primaryColor else if (isCurrentPlaying) primaryColor.copy(alpha = 0.5f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .testTag("queue_song_$index"),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (isSelected || isCurrentPlaying) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(borderColor, borderColor))) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for Multi-Select
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Album cover with playing badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                AudioCoverImage(
                    model = song.albumArtUri,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (isCurrentPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Tocando agora",
                            tint = primaryColor.copy(alpha = pulseAlpha),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isCurrentPlaying) primaryColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.durationFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrentPlaying) primaryColor else MaterialTheme.colorScheme.outline
                )
            }

            // Reorder Quick Arrows / Drag Indicator
            if (!isMultiSelectMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Mover para cima",
                            tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Mover para baixo",
                            tint = if (index < totalCount - 1) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onOptionsClick,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("queue_song_options_$index")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Mais opções",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

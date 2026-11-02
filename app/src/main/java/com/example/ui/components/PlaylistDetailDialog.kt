package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistRemove
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Playlist
import com.example.model.Song
import com.example.util.M3uUtils
import com.example.util.ShareUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailDialog(
    playlist: Playlist,
    playlistSongs: List<Song>,
    allSongs: List<Song>,
    onDismiss: () -> Unit,
    onPlayPlaylist: (List<Song>, Int) -> Unit,
    onAddSongsToPlaylist: (Long, List<Long>) -> Unit,
    onRemoveSongFromPlaylist: (Long, Long) -> Unit,
    onRemoveSongsBatch: (Long, List<Long>) -> Unit,
    onReorderSongs: (Long, List<Long>) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDuplicatePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onDeleteSongsBatch: (List<Song>) -> Unit,
    onAddToFavoritesBatch: (List<Long>) -> Unit,
    onAddSongsToQueue: (List<Song>) -> Unit,
    onViewArtist: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    var showHeaderMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renamedName by remember { mutableStateOf(playlist.name) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var duplicateName by remember { mutableStateOf("${playlist.name} (Cópia)") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddSongsPicker by remember { mutableStateOf(false) }
    var activeSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showDeleteBatchConfirm by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                if (isMultiSelectMode) {
                    MultiSelectTopBar(
                        selectedCount = selectedIds.size,
                        totalCount = playlistSongs.size,
                        onSelectAll = {
                            selectedIds.clear()
                            selectedIds.addAll(playlistSongs.map { it.id })
                        },
                        onDeselectAll = { selectedIds.clear() },
                        onClose = {
                            isMultiSelectMode = false
                            selectedIds.clear()
                        }
                    )
                } else {
                    // Header Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fechar playlist",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${playlistSongs.size} músicas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = primaryColor
                                    )
                                }

                                Box {
                                    IconButton(onClick = { showHeaderMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Opções da playlist",
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
                                            text = { Text("Adicionar músicas") },
                                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                            onClick = {
                                                showHeaderMenu = false
                                                showAddSongsPicker = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Renomear playlist") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                showHeaderMenu = false
                                                renamedName = playlist.name
                                                showRenameDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Duplicar playlist") },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                            onClick = {
                                                showHeaderMenu = false
                                                duplicateName = "${playlist.name} (Cópia)"
                                                showDuplicateDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Exportar como .M3U") },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                            onClick = {
                                                showHeaderMenu = false
                                                scope.launch {
                                                    val file = M3uUtils.exportM3uFile(context, playlist.name, playlistSongs)
                                                    ShareUtils.shareM3uFile(context, file, playlist.name)
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Compartilhar playlist") },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                            onClick = {
                                                showHeaderMenu = false
                                                ShareUtils.shareMultipleSongs(context, playlistSongs)
                                            }
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Excluir playlist", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                showHeaderMenu = false
                                                showDeleteConfirm = true
                                            }
                                        )
                                    }
                                }
                            }

                            // Action buttons row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (playlistSongs.isNotEmpty()) {
                                            onPlayPlaylist(playlistSongs, 0)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reproduzir Tudo")
                                }

                                Button(
                                    onClick = { showAddSongsPicker = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Adicionar", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // Songs List
                if (playlistSongs.isEmpty()) {
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
                                modifier = Modifier.size(56.dp),
                                tint = primaryColor.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Esta playlist não possui músicas",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showAddSongsPicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Adicionar Faixas")
                            }
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
                        itemsIndexed(playlistSongs, key = { idx, item -> "${item.id}_$idx" }) { index, song ->
                            val isSelected = selectedIds.contains(song.id)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isMultiSelectMode) {
                                            if (isSelected) selectedIds.remove(song.id) else selectedIds.add(song.id)
                                        } else {
                                            onPlayPlaylist(playlistSongs, index)
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) primaryColor.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isMultiSelectMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                if (isSelected) selectedIds.remove(song.id) else selectedIds.add(song.id)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = primaryColor),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
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
                                        // Reorder arrows
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val reordered = playlistSongs.toMutableList()
                                                    val item = reordered.removeAt(index)
                                                    reordered.add(index - 1, item)
                                                    onReorderSongs(playlist.id, reordered.map { it.id })
                                                }
                                            },
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
                                            onClick = {
                                                if (index < playlistSongs.size - 1) {
                                                    val reordered = playlistSongs.toMutableList()
                                                    val item = reordered.removeAt(index)
                                                    reordered.add(index + 1, item)
                                                    onReorderSongs(playlist.id, reordered.map { it.id })
                                                }
                                            },
                                            enabled = index < playlistSongs.size - 1,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "Mover para baixo",
                                                tint = if (index < playlistSongs.size - 1) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { activeSongForOptions = song },
                                            modifier = Modifier.size(34.dp)
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
                }
            }

            // Floating Multi-Select Bottom Bar
            AnimatedVisibility(
                visible = isMultiSelectMode && selectedIds.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                val selectedSongs = remember(selectedIds.size, playlistSongs) {
                    playlistSongs.filter { it.id in selectedIds }
                }

                MultiSelectBottomActionBar(
                    selectedCount = selectedIds.size,
                    isInQueue = false,
                    onPlaySelected = {
                        onPlayPlaylist(selectedSongs, 0)
                        isMultiSelectMode = false
                        selectedIds.clear()
                    },
                    onAddToQueue = {
                        onAddSongsToQueue(selectedSongs)
                        Toast.makeText(context, "${selectedSongs.size} músicas adicionadas à fila!", Toast.LENGTH_SHORT).show()
                        isMultiSelectMode = false
                        selectedIds.clear()
                    },
                    onAddToPlaylist = {
                        // Open add songs to another playlist
                        isMultiSelectMode = false
                        selectedIds.clear()
                    },
                    onShare = { ShareUtils.shareMultipleSongs(context, selectedSongs) },
                    onRemoveFromQueue = {
                        onRemoveSongsBatch(playlist.id, selectedIds.toList())
                        Toast.makeText(context, "${selectedIds.size} removida(s) da playlist", Toast.LENGTH_SHORT).show()
                        isMultiSelectMode = false
                        selectedIds.clear()
                    },
                    onDeleteSelected = { showDeleteBatchConfirm = true },
                    onAddToFavorites = {
                        onAddToFavoritesBatch(selectedIds.toList())
                        Toast.makeText(context, "${selectedIds.size} adicionada(s) aos favoritos!", Toast.LENGTH_SHORT).show()
                        isMultiSelectMode = false
                        selectedIds.clear()
                    },
                    onMoveToPlaylist = {
                        isMultiSelectMode = false
                        selectedIds.clear()
                    }
                )
            }

            // Rename Dialog
            if (showRenameDialog) {
                AlertDialog(
                    onDismissRequest = { showRenameDialog = false },
                    title = { Text("Renomear Playlist") },
                    text = {
                        OutlinedTextField(
                            value = renamedName,
                            onValueChange = { renamedName = it },
                            label = { Text("Novo Nome") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (renamedName.isNotBlank()) {
                                    onRenamePlaylist(playlist.id, renamedName.trim())
                                    showRenameDialog = false
                                }
                            }
                        ) {
                            Text("Salvar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Duplicate Dialog
            if (showDuplicateDialog) {
                AlertDialog(
                    onDismissRequest = { showDuplicateDialog = false },
                    title = { Text("Duplicar Playlist") },
                    text = {
                        OutlinedTextField(
                            value = duplicateName,
                            onValueChange = { duplicateName = it },
                            label = { Text("Nome da Cópia") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (duplicateName.isNotBlank()) {
                                    onDuplicatePlaylist(playlist.id, duplicateName.trim())
                                    Toast.makeText(context, "Playlist duplicada com sucesso!", Toast.LENGTH_SHORT).show()
                                    showDuplicateDialog = false
                                }
                            }
                        ) {
                            Text("Duplicar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDuplicateDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Delete Playlist Confirm
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Excluir Playlist?") },
                    text = { Text("Deseja realmente excluir \"${playlist.name}\"? Suas faixas originais não serão afetadas.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirm = false
                                onDeletePlaylist(playlist.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Excluir")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Add Songs Picker Dialog
            if (showAddSongsPicker) {
                val availableSongs = remember(allSongs, playlistSongs) {
                    val existingIds = playlistSongs.map { it.id }.toSet()
                    allSongs.filter { it.id !in existingIds }
                }
                val newlySelectedIds = remember { mutableStateListOf<Long>() }

                AlertDialog(
                    onDismissRequest = { showAddSongsPicker = false },
                    title = { Text("Adicionar Músicas à Playlist") },
                    text = {
                        if (availableSongs.isEmpty()) {
                            Text("Todas as músicas da sua biblioteca já estão nesta playlist.")
                        } else {
                            LazyColumn(modifier = Modifier.height(260.dp)) {
                                itemsIndexed(availableSongs) { _, song ->
                                    val isChecked = newlySelectedIds.contains(song.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) newlySelectedIds.remove(song.id) else newlySelectedIds.add(song.id)
                                            }
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                if (isChecked) newlySelectedIds.remove(song.id) else newlySelectedIds.add(song.id)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(song.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newlySelectedIds.isNotEmpty()) {
                                    onAddSongsToPlaylist(playlist.id, newlySelectedIds.toList())
                                    Toast.makeText(context, "${newlySelectedIds.size} músicas adicionadas!", Toast.LENGTH_SHORT).show()
                                }
                                showAddSongsPicker = false
                            },
                            enabled = newlySelectedIds.isNotEmpty()
                        ) {
                            Text("Adicionar (${newlySelectedIds.size})")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddSongsPicker = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Song Options Sheet
            activeSongForOptions?.let { song ->
                SongActionBottomSheet(
                    song = song,
                    isInQueue = false,
                    onDismiss = { activeSongForOptions = null },
                    onPlayNow = { onPlayPlaylist(listOf(song), 0) },
                    onPlayNext = {
                        onAddSongsToQueue(listOf(song))
                        Toast.makeText(context, "\"${song.title}\" adicionada à fila", Toast.LENGTH_SHORT).show()
                    },
                    onAddToQueue = {
                        onAddSongsToQueue(listOf(song))
                        Toast.makeText(context, "\"${song.title}\" adicionada à fila", Toast.LENGTH_SHORT).show()
                    },
                    onToggleFavorite = {
                        scope.launch {
                            com.example.MyatoApplication.instance.musicRepository.toggleFavorite(song)
                        }
                    },
                    onAddToPlaylist = {},
                    onShare = { ShareUtils.shareSong(context, song) },
                    onViewArtist = onViewArtist,
                    onRemoveFromQueue = {
                        onRemoveSongFromPlaylist(playlist.id, song.id)
                        Toast.makeText(context, "Removida da playlist", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteFile = {
                        onDeleteSongsBatch(listOf(song))
                        onRemoveSongFromPlaylist(playlist.id, song.id)
                    }
                )
            }

            // Batch Delete Confirm
            if (showDeleteBatchConfirm) {
                val selectedSongs = remember(selectedIds.size, playlistSongs) {
                    playlistSongs.filter { it.id in selectedIds }
                }
                AlertDialog(
                    onDismissRequest = { showDeleteBatchConfirm = false },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Excluir ${selectedSongs.size} Música(s)?") },
                    text = { Text("As músicas serão removidas da biblioteca e excluídas definitivamente.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteBatchConfirm = false
                                onDeleteSongsBatch(selectedSongs)
                                onRemoveSongsBatch(playlist.id, selectedIds.toList())
                                isMultiSelectMode = false
                                selectedIds.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Excluir Todas")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteBatchConfirm = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}
}

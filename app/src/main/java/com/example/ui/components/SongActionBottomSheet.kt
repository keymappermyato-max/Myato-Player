package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionBottomSheet(
    song: Song,
    isInQueue: Boolean = false,
    onDismiss: () -> Unit,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onViewArtist: (String) -> Unit,
    onRemoveFromQueue: (() -> Unit)? = null,
    onDeleteFile: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Track Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AudioCoverImage(
                        model = song.albumArtUri,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
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
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Actions
            SongActionItem(
                icon = Icons.Default.PlayArrow,
                title = "Reproduzir agora",
                onClick = {
                    onDismiss()
                    onPlayNow()
                }
            )

            SongActionItem(
                icon = Icons.Default.QueuePlayNext,
                title = "Reproduzir a seguir",
                onClick = {
                    onDismiss()
                    onPlayNext()
                }
            )

            SongActionItem(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                title = "Adicionar à fila",
                onClick = {
                    onDismiss()
                    onAddToQueue()
                }
            )

            SongActionItem(
                icon = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                title = if (song.isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                iconTint = if (song.isFavorite) MaterialTheme.colorScheme.primary else null,
                onClick = {
                    onDismiss()
                    onToggleFavorite()
                }
            )

            SongActionItem(
                icon = Icons.Default.PlaylistAdd,
                title = "Adicionar a uma playlist",
                onClick = {
                    onDismiss()
                    onAddToPlaylist()
                }
            )

            SongActionItem(
                icon = Icons.Default.Share,
                title = "Compartilhar",
                onClick = {
                    onDismiss()
                    onShare()
                }
            )

            SongActionItem(
                icon = Icons.Default.Info,
                title = "Ver informações da música",
                onClick = {
                    showInfoDialog = true
                }
            )

            SongActionItem(
                icon = Icons.Default.Person,
                title = "Ver músicas de \"${song.artist}\"",
                onClick = {
                    onDismiss()
                    onViewArtist(song.artist)
                }
            )

            if (isInQueue && onRemoveFromQueue != null) {
                SongActionItem(
                    icon = Icons.Default.PlaylistRemove,
                    title = "Remover da fila",
                    onClick = {
                        onDismiss()
                        onRemoveFromQueue()
                    }
                )
            }

            SongActionItem(
                icon = Icons.Default.Delete,
                title = "Excluir arquivo",
                textColor = MaterialTheme.colorScheme.error,
                iconTint = MaterialTheme.colorScheme.error,
                onClick = {
                    showDeleteConfirmDialog = true
                }
            )
        }
    }

    // Technical Info Dialog
    if (showInfoDialog) {
        SongInfoDialog(
            song = song,
            onDismiss = { showInfoDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Excluir Música?") },
            text = {
                Text("Deseja realmente remover \"${song.title}\" permanentemente? A ação não pode ser desfeita.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDismiss()
                        onDeleteFile()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun SongActionItem(
    icon: ImageVector,
    title: String,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    iconTint: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongInfoDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(song.dateAdded) {
        if (song.dateAdded > 0) dateFormat.format(Date(song.dateAdded)) else "Desconhecido"
    }

    val formattedSize = remember(song.fileSize) {
        when {
            song.fileSize <= 0 -> "Não especificado"
            song.fileSize < 1024 * 1024 -> "%.1f KB".format(song.fileSize / 1024.0)
            else -> "%.2f MB".format(song.fileSize / (1024.0 * 1024.0))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Informações da Música")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoField(label = "Título", value = song.title)
                InfoField(label = "Artista", value = song.artist)
                InfoField(label = "Álbum", value = song.album)
                InfoField(label = "Duração", value = song.durationFormatted)
                InfoField(label = "Tamanho", value = formattedSize)
                InfoField(label = "Pasta", value = song.folderName)
                InfoField(label = "Data de Adição", value = formattedDate)
                InfoField(
                    label = "Caminho / Origem",
                    value = if (song.path.isNotBlank()) song.path else song.uriString
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

@Composable
private fun InfoField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value.ifBlank { "Desconhecido" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

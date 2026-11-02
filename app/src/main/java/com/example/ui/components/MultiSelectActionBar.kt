package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MultiSelectTopBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("multi_select_close")) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar seleção múltipla",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "$selectedCount selecionada(s)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "de $totalCount faixas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                if (selectedCount < totalCount) {
                    TextButton(onClick = onSelectAll, modifier = Modifier.testTag("multi_select_all_btn")) {
                        Text("Todas", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                if (selectedCount > 0) {
                    TextButton(onClick = onDeselectAll, modifier = Modifier.testTag("multi_deselect_all_btn")) {
                        Text("Limpar", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun MultiSelectBottomActionBar(
    selectedCount: Int,
    isInQueue: Boolean = false,
    onPlaySelected: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onRemoveFromQueue: (() -> Unit)? = null,
    onDeleteSelected: () -> Unit,
    onAddToFavorites: () -> Unit,
    onMoveToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 6. Reproduzir músicas selecionadas
            MultiActionButton(
                icon = Icons.Default.PlayArrow,
                label = "Reproduzir",
                isPrimary = true,
                onClick = onPlaySelected
            )

            // 1. Adicionar à fila
            MultiActionButton(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = "À Fila",
                onClick = onAddToQueue
            )

            // 2. Adicionar a uma playlist
            MultiActionButton(
                icon = Icons.Default.PlaylistAdd,
                label = "Playlist",
                onClick = onAddToPlaylist
            )

            // 7. Adicionar aos favoritos
            MultiActionButton(
                icon = Icons.Default.Favorite,
                label = "Favoritos",
                onClick = onAddToFavorites
            )

            // 3. Compartilhar músicas
            MultiActionButton(
                icon = Icons.Default.Share,
                label = "Compartilhar",
                onClick = onShare
            )

            // 8. Mover para outra playlist
            MultiActionButton(
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                label = "Mover",
                onClick = onMoveToPlaylist
            )

            // 4. Remover da fila (quando aplicável)
            if (isInQueue && onRemoveFromQueue != null) {
                MultiActionButton(
                    icon = Icons.Default.PlaylistRemove,
                    label = "Remover",
                    onClick = onRemoveFromQueue
                )
            }

            // 5. Excluir arquivos selecionados
            MultiActionButton(
                icon = Icons.Default.Delete,
                label = "Excluir",
                tint = MaterialTheme.colorScheme.error,
                onClick = onDeleteSelected
            )
        }
    }
}

@Composable
private fun MultiActionButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean = false,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = if (isPrimary) primaryColor.copy(alpha = 0.2f) else Color.Transparent

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint ?: if (isPrimary) primaryColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = tint ?: if (isPrimary) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

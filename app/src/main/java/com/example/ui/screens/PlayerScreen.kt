package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.player.PlaybackState
import com.example.player.RepeatMode as PlayerRepeatMode
import com.example.ui.components.AudioCoverImage
import com.example.ui.components.AudioVisualizer
import com.example.ui.components.SongInfoDialog
import com.example.ui.theme.LocalAppSettings
import com.example.util.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playbackState: PlaybackState,
    visualizerBars: FloatArray,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onUpdateBackground: (String?) -> Unit,
    onClose: () -> Unit,
    onDeleteSong: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return
    val settings = LocalAppSettings.current
    val context = LocalContext.current

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPosition by remember { mutableFloatStateOf(0f) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Vinyl rotation angle
    val rotationAnim = remember { Animatable(0f) }

    LaunchedEffect(playbackState.isPlaying, settings.enableAnimations) {
        if (playbackState.isPlaying && settings.enableAnimations) {
            while (true) {
                rotationAnim.animateTo(
                    targetValue = rotationAnim.value + 360f,
                    animationSpec = tween(12000, easing = LinearEasing)
                )
            }
        }
    }

    // Glow pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "player_aura")
    val auraAlpha by if (settings.enableAnimations && playbackState.isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f * settings.glowIntensity,
            targetValue = 0.75f * settings.glowIntensity,
            animationSpec = infiniteRepeatable(
                animation = tween(1800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aura"
        )
    } else {
        remember { mutableFloatStateOf(0.4f * settings.glowIntensity) }
    }

    // Gallery background picker
    val bgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onUpdateBackground(it.toString()) }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("full_player_screen")
    ) {
        // Custom background layer
        if (!settings.backgroundUri.isNullOrBlank()) {
            AudioCoverImage(
                model = settings.backgroundUri,
                contentDescription = "Plano de fundo personalizado",
                modifier = Modifier
                    .fillMaxSize()
                    .blur(settings.backgroundBlur.dp),
                contentScale = ContentScale.Crop
            )
            // Overlay tint
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.backgroundDimAlpha))
            )
        } else {
            // Default dark neon gradient backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("player_collapse_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Recolher player",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "REPRODUZINDO AGORA",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = primaryColor
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Open Queue Button with badge indicator
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onOpenQueue() }
                            .padding(6.dp)
                            .testTag("player_open_queue_button"),
                        color = primaryColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Fila",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${playbackState.queue.size}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = primaryColor
                            )
                        }
                    }

                    // 3-dot overflow menu
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opções do player",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Fila de reprodução") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenQueue()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Equalizador de som") },
                                leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenEqualizer()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Adicionar a uma playlist") },
                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onAddToPlaylist(song)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compartilhar música") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    ShareUtils.shareSong(context, song)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Informações da faixa") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showInfoDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Plano de fundo personalizado") },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showBackgroundDialog = true
                                }
                            )
                            if (onDeleteSong != null) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Excluir música", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Large Animated Album Art with Vinyl Frame and Neon Aura
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = auraAlpha),
                                    Color.Transparent
                                ),
                                radius = size.width * 0.7f
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Vinyl outer groove
                Box(
                    modifier = Modifier
                        .size(270.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF14121A))
                        .rotate(rotationAnim.value % 360f),
                    contentAlignment = Alignment.Center
                ) {
                    // Center album cover with smooth crossfade
                    Crossfade(
                        targetState = song.albumArtUri,
                        label = "cover_crossfade",
                        modifier = Modifier
                            .size(190.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) { artUri ->
                        AudioCoverImage(
                            model = artUri,
                            contentDescription = song.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Vinyl center spindle hole
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic Audio Visualizer
            AudioVisualizer(
                levels = visualizerBars,
                height = 44.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title, Artist, Album
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Quick Actions: Favorite, Info, Add to Queue, Equalizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onToggleFavorite(song) },
                    modifier = Modifier.testTag("player_favorite_toggle")
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favoritar",
                        tint = if (song.isFavorite) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.testTag("player_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Informações da música",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        onAddToQueue(song)
                        Toast.makeText(context, "\"${song.title}\" adicionada à fila", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("player_add_to_queue_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.QueuePlayNext,
                        contentDescription = "Adicionar à fila",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier.testTag("player_equalizer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Equalizador",
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Slider
            val currentPos = if (isDraggingSlider) {
                (sliderDragPosition * song.durationMs).toLong()
            } else {
                playbackState.currentPositionMs
            }
            val progressFraction = if (song.durationMs > 0) {
                (currentPos.toFloat() / song.durationMs).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progressFraction,
                onValueChange = {
                    isDraggingSlider = true
                    sliderDragPosition = it
                },
                onValueChangeFinished = {
                    isDraggingSlider = false
                    onSeekTo((sliderDragPosition * song.durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = primaryColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Current Time & Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val curSec = currentPos / 1000
                val durSec = song.durationMs / 1000
                Text(
                    text = "%d:%02d".format(curSec / 60, curSec % 60),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "%d:%02d".format(durSec / 60, durSec % 60),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main playback controls: Shuffle, Previous, Play/Pause, Next, Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.testTag("player_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Aleatório",
                        tint = if (playbackState.isShuffle) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(52.dp).testTag("player_previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Play / Pause FAB
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(primaryColor, secondaryColor)
                            )
                        )
                        .clickable { onPlayPause() }
                        .testTag("player_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproduzir",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(52.dp).testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Próxima",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.testTag("player_repeat_button")
                ) {
                    val icon = when (playbackState.repeatMode) {
                        PlayerRepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    val tint = when (playbackState.repeatMode) {
                        PlayerRepeatMode.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> primaryColor
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repetir",
                        tint = tint
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Volume slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Slider(
                    value = playbackState.volume,
                    onValueChange = onVolumeChanged,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = secondaryColor,
                        activeTrackColor = secondaryColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Song Info Dialog
        if (showInfoDialog) {
            SongInfoDialog(
                song = song,
                onDismiss = { showInfoDialog = false }
            )
        }

        // Background personalization dialog
        if (showBackgroundDialog) {
            AlertDialog(
                onDismissRequest = { showBackgroundDialog = false },
                title = { Text("Plano de Fundo do Player") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Personalize o visual da tela de reprodução com imagens da sua galeria ou remova para usar o tema neon padrão.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        TextButton(
                            onClick = {
                                showBackgroundDialog = false
                                bgPickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Escolher Imagem da Galeria")
                        }

                        if (settings.backgroundUri != null) {
                            TextButton(
                                onClick = {
                                    onUpdateBackground(null)
                                    showBackgroundDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Remover Fundo Personalizado", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBackgroundDialog = false }) {
                        Text("Fechar")
                    }
                }
            )
        }

        // Delete confirmation dialog
        if (showDeleteDialog && onDeleteSong != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Excluir Música?") },
                text = {
                    Text("Deseja realmente excluir a faixa \"${song.title}\"? Ela será removida da biblioteca e a reprodução atual será encerrada.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteSong(song)
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Excluir", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.EqualizerBand
import com.example.player.EqualizerState

@Composable
fun EqualizerScreen(
    equalizerState: EqualizerState,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onBandLevelChanged: (Short, Short) -> Unit,
    onBassBoostChanged: (Int) -> Unit,
    onVirtualizerChanged: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("equalizer_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with switch and reset
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Equalizador",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (equalizerState.isEnabled) "Efeitos de áudio ativos" else "Equalizador desativado",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (equalizerState.isEnabled) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.testTag("equalizer_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Restaurar Padrão",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = equalizerState.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primaryColor,
                            checkedTrackColor = primaryColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("equalizer_toggle_switch")
                    )
                }
            }
        }

        // Presets Chips Carousel
        item {
            Column {
                Text(
                    text = "Predefinições de Áudio",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(equalizerState.presets) { preset ->
                        val isSelected = equalizerState.currentPreset.equals(preset, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectPreset(preset) },
                            label = {
                                Text(
                                    text = preset,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor,
                                selectedLabelColor = Color.White
                            ),
                            enabled = equalizerState.isEnabled
                        )
                    }
                }
            }
        }

        // Frequency Bands Sliders Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Frequências (dB)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (equalizerState.bands.isEmpty()) {
                        Text(
                            text = "Ajustando bandas de áudio com base no hardware...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        equalizerState.bands.forEach { band ->
                            BandSliderRow(
                                band = band,
                                isEnabled = equalizerState.isEnabled,
                                onLevelChange = { newLevel ->
                                    onBandLevelChanged(band.bandIndex, newLevel)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Bass Boost and Virtualizer Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Efeitos Sonoros Avançados",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bass Boost Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reforço de Graves (Bass Boost)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Text(
                            text = "${(equalizerState.bassBoost / 10)}%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Slider(
                        value = equalizerState.bassBoost.toFloat(),
                        onValueChange = { onBassBoostChanged(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = equalizerState.isEnabled && equalizerState.isBassBoostSupported,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor
                        ),
                        modifier = Modifier.testTag("bass_boost_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Virtualizer Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SurroundSound,
                                contentDescription = null,
                                tint = secondaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Espacialização 3D (Virtualizer)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Text(
                            text = "${(equalizerState.virtualizer / 10)}%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = secondaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Slider(
                        value = equalizerState.virtualizer.toFloat(),
                        onValueChange = { onVirtualizerChanged(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = equalizerState.isEnabled && equalizerState.isVirtualizerSupported,
                        colors = SliderDefaults.colors(
                            thumbColor = secondaryColor,
                            activeTrackColor = secondaryColor
                        ),
                        modifier = Modifier.testTag("virtualizer_slider")
                    )
                }
            }
        }
    }
}

@Composable
fun BandSliderRow(
    band: EqualizerBand,
    isEnabled: Boolean,
    onLevelChange: (Short) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = band.displayFrequency,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = band.displayDb,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = band.currentLevelMdB.toFloat(),
            onValueChange = { onLevelChange(it.toInt().toShort()) },
            valueRange = band.minLevelMdB.toFloat()..band.maxLevelMdB.toFloat(),
            enabled = isEnabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

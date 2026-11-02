package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.ThemePreset
import com.example.ui.components.AppTab
import com.example.ui.components.MyatoLogo

@Composable
fun SettingsScreen(
    settings: AppSettings,
    totalSongs: Int,
    onUpdateTheme: (ThemePreset) -> Unit,
    onUpdateCustomColors: (Long, Long) -> Unit,
    onUpdateGlowIntensity: (Float) -> Unit,
    onUpdateEnableAnimations: (Boolean) -> Unit,
    onUpdateBackground: (String?, Float, Float) -> Unit,
    onUpdatePlaybackSpeed: (Float) -> Unit,
    onUpdateGaplessPlayback: (Boolean) -> Unit,
    onUpdateRememberPosition: (Boolean) -> Unit,
    onClearPlaybackHistory: () -> Unit,
    onClearAllSongs: () -> Unit,
    onScanLibrary: () -> Unit,
    onRestoreDefaults: () -> Unit,
    onNavigateTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearLibraryDialog by remember { mutableStateOf(false) }
    var showRestoreDefaultsDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Background Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onUpdateBackground(it.toString(), settings.backgroundBlur, settings.backgroundDimAlpha) }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Configurações",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Personalize o Myato Player, temas, áudio e preferências",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Theme Selection Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = primaryColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tema & Identidade Visual",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "10 Temas Disponíveis:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(ThemePreset.entries) { preset ->
                            val isSelected = settings.themePreset == preset
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onUpdateTheme(preset) }
                                    .testTag("theme_preset_${preset.id}"),
                                color = Color(preset.surfaceHex)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(preset.primaryHex))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(preset.secondaryHex))
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = preset.title.substringBefore(" ("),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (preset == ThemePreset.MINIMAL_WHITE) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Color palette quick picks for custom theme
                    Text(
                        text = "Cores Neon Customizadas:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val customPalette = listOf(
                        Pair(0xFFA855F7, 0xFF38BDF8),
                        Pair(0xFF38BDF8, 0xFF818CF8),
                        Pair(0xFF22C55E, 0xFF10B981),
                        Pair(0xFFEF4444, 0xFFF97316),
                        Pair(0xFFF97316, 0xFFFBBF24),
                        Pair(0xFFEC4899, 0xFFA855F7)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        customPalette.forEach { (prim, sec) ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(prim))
                                    .clickable { onUpdateCustomColors(prim, sec) }
                                    .border(2.dp, Color(sec), CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // 2. Background Personalization Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = secondaryColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Plano de Fundo da Reprodução",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (settings.backgroundUri != null) "Imagem personalizada ativa" else "Fundo padrão neon ativo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.testTag("settings_bg_choose_button")
                        ) {
                            Text("Escolher da Galeria")
                        }

                        if (settings.backgroundUri != null) {
                            OutlinedButton(
                                onClick = { onUpdateBackground(null, settings.backgroundBlur, settings.backgroundDimAlpha) }
                            ) {
                                Text("Remover", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (settings.backgroundUri != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Intensidade do Desfoque: ${settings.backgroundBlur.toInt()}dp",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = settings.backgroundBlur,
                            onValueChange = { onUpdateBackground(settings.backgroundUri, it, settings.backgroundDimAlpha) },
                            valueRange = 0f..40f
                        )

                        Text(
                            text = "Escurecimento do Fundo: ${(settings.backgroundDimAlpha * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = settings.backgroundDimAlpha,
                            onValueChange = { onUpdateBackground(settings.backgroundUri, settings.backgroundBlur, it) },
                            valueRange = 0.2f..0.95f
                        )
                    }
                }
            }
        }

        // 3. Animations & Visual Effects
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Animation,
                                contentDescription = null,
                                tint = primaryColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Efeitos & Animações",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Switch(
                            checked = settings.enableAnimations,
                            onCheckedChange = onUpdateEnableAnimations,
                            modifier = Modifier.testTag("settings_animations_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Intensidade do Brilho Neon (Glow): ${(settings.glowIntensity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.glowIntensity,
                        onValueChange = onUpdateGlowIntensity,
                        valueRange = 0.1f..1.0f,
                        enabled = settings.enableAnimations,
                        colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                    )
                }
            }
        }

        // 4. Playback Preferences Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = primaryColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Preferências de Reprodução",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Playback speed
                    Text(
                        text = "Velocidade de Reprodução: %.1fx".format(settings.playbackSpeed),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.8f, 1.0f, 1.25f, 1.5f).forEach { spd ->
                            val isSel = settings.playbackSpeed == spd
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onUpdatePlaybackSpeed(spd) }
                                    .padding(vertical = 4.dp, horizontal = 10.dp),
                                color = if (isSel) primaryColor else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = "${spd}x",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reprodução contínua (Gapless)", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = settings.gaplessPlayback,
                            onCheckedChange = onUpdateGaplessPlayback
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lembrar última posição", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = settings.rememberPlaybackPosition,
                            onCheckedChange = onUpdateRememberPosition
                        )
                    }
                }
            }
        }

        // 5. Library Management Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            tint = primaryColor
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gerenciamento da Biblioteca",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Total de faixas indexadas: $totalSongs músicas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onScanLibrary,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            modifier = Modifier.testTag("settings_rescan_library_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-escanear")
                        }

                        OutlinedButton(
                            onClick = { showClearLibraryDialog = true },
                            modifier = Modifier.testTag("settings_clear_library_button")
                        ) {
                            Text("Limpar Catálogo", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // 6. History & Data Management Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Histórico & Dados",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearHistoryDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Limpar histórico de reprodução", style = MaterialTheme.typography.bodyMedium)
                            Text("Remove o registro de músicas tocadas recentemente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRestoreDefaultsDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Restaurar configurações padrão", style = MaterialTheme.typography.bodyMedium)
                            Text("Retorna temas, efeitos e cores ao padrão de fábrica", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // 7. App Info & Privacy Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutDialog = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = primaryColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sobre o Myato Player", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Versão 1.0.0 Pro • Sua música, do seu jeito.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyPolicyDialog = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Policy, contentDescription = null, tint = secondaryColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Política de Privacidade & Termos", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("100% offline, seguro e privado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // Modal: Confirmation to Clear History
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Limpar Histórico de Reprodução?") },
            text = { Text("Deseja realmente apagar o registro de músicas reproduzidas recentemente? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearPlaybackHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmar e Limpar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal: Confirmation to Clear Library
    if (showClearLibraryDialog) {
        AlertDialog(
            onDismissRequest = { showClearLibraryDialog = false },
            title = { Text("Limpar Catálogo da Biblioteca?") },
            text = { Text("Isso removerá a indexação local das músicas no Myato Player. Seus arquivos de áudio originais NÃO serão excluídos do aparelho e você poderá re-escanear a qualquer momento.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllSongs()
                        showClearLibraryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Limpar Catálogo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLibraryDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal: Confirmation to Restore Defaults
    if (showRestoreDefaultsDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDefaultsDialog = false },
            title = { Text("Restaurar Configurações Padrão?") },
            text = { Text("Deseja restaurar todas as configurações visuais, temas, plano de fundo e equalizador para o padrão original Preto e Roxo?") },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreDefaults()
                        showRestoreDefaultsDialog = false
                    }
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDefaultsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal: About App
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                MyatoLogo(size = 32.dp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Myato Player Pro", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Versão 1.0.0 (Release Android)", style = MaterialTheme.typography.bodySmall, color = primaryColor)
                    Text("Sua música, do seu jeito.", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Reprodutor profissional de alta fidelidade desenvolvido com arquitetura moderna, processamento de áudio digital com equalizador integrado de 5 bandas, Bass Boost, renderização em tempo real de visualizadores sonoros e temas com iluminação neon personalizada.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Modal: Privacy Policy
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("Privacidade & Segurança") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Armazenamento 100% Local: Nenhuma informação pessoal ou biblioteca musical é transmitida a servidores remotos.", style = MaterialTheme.typography.bodySmall)
                    Text("• Permissões Mínimas: O aplicativo apenas acessa arquivos de áudio mediante permissão estrita ou seleção direta do usuário.", style = MaterialTheme.typography.bodySmall)
                    Text("• Downloads Autorizados: Respeita os direitos autorais e Termos de Serviço de provedores externos.", style = MaterialTheme.typography.bodySmall)
                    Text("• Sem Rastreamento: Nenhum anúncio intrusivo ou telemetria invasiva.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

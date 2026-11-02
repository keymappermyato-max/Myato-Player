package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.permission.AudioPermissionService

@Composable
fun AudioPermissionPromptDialog(
    isPermanentlyDenied: Boolean = false,
    onConfirmGrant: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    onImportManually: () -> Unit
) {
    val permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        "READ_MEDIA_AUDIO (Android 13+)"
    } else {
        "READ_EXTERNAL_STORAGE (Android 12-)"
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("audio_permission_prompt_dialog"),
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                text = if (isPermanentlyDenied) "Permissão Necessária" else "Acesso à Biblioteca de Músicas",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isPermanentlyDenied) {
                        "A permissão para carregar arquivos de áudio foi desativada. Para que o Myato Player possa escanear e indexar suas músicas automaticamente, habilite a permissão nas configurações do sistema."
                    } else {
                        "Para reproduzir suas músicas e organizar faixas por artista, álbum e pastas, o Myato Player precisa de autorização para ler os arquivos de áudio salvos no seu aparelho."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Permissão: $permissionName",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = primaryColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• 100% Offline: Nenhum arquivo sai do seu celular.\n• Apenas áudios: Não acessamos suas fotos, contatos ou localização.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = if (isPermanentlyDenied) onOpenSettings else onConfirmGrant,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.testTag("audio_permission_confirm_button")
            ) {
                Text(if (isPermanentlyDenied) "Abrir Configurações" else "Permitir Acesso")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onImportManually,
                    modifier = Modifier.testTag("audio_permission_manual_import_button")
                ) {
                    Text("Escolher Arquivos")
                }
                TextButton(onClick = onDismiss) {
                    Text("Agora Não")
                }
            }
        }
    )
}

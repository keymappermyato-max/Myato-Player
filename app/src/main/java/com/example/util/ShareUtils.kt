package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.Song
import java.io.File

object ShareUtils {

    fun shareSong(context: Context, song: Song) {
        val shareIntent = Intent(Intent.ACTION_SEND)

        val file = if (song.path.isNotBlank()) File(song.path) else null
        val canShareFile = file != null && file.exists() && file.isFile

        if (canShareFile && file != null) {
            try {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                shareIntent.type = "audio/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Ouvindo \"${song.title}\" de ${song.artist} no Myato Player 🎵")
            } catch (e: Exception) {
                shareIntent.type = "text/plain"
                shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "🎵 ${song.title}\n👤 Artista: ${song.artist}\n💿 Álbum: ${song.album}\n⏱ Duração: ${song.durationFormatted}\n\nCompartilhado via Myato Player"
                )
            }
        } else {
            shareIntent.type = "text/plain"
            val text = buildString {
                append("🎵 ").append(song.title).append("\n")
                append("👤 Artista: ").append(song.artist).append("\n")
                append("💿 Álbum: ").append(song.album).append("\n")
                append("⏱ Duração: ").append(song.durationFormatted).append("\n")
                if (song.uriString.isNotBlank() && !song.uriString.startsWith("sample://")) {
                    append("🔗 ").append(song.uriString).append("\n")
                }
                append("\nCompartilhado via Myato Player")
            }
            shareIntent.putExtra(Intent.EXTRA_TEXT, text)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar música"))
    }

    fun shareMultipleSongs(context: Context, songs: List<Song>) {
        if (songs.isEmpty()) return

        val fileUris = ArrayList<Uri>()
        for (song in songs) {
            if (song.path.isNotBlank()) {
                val file = File(song.path)
                if (file.exists() && file.isFile) {
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        fileUris.add(uri)
                    } catch (ignored: Exception) {}
                }
            }
        }

        val intent = if (fileUris.isNotEmpty()) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val summary = "${songs.size} músicas selecionadas no Myato Player 🎵"
                putExtra(Intent.EXTRA_TEXT, summary)
            }
        } else {
            val trackListText = buildString {
                appendLine("🎵 Lista de Músicas (${songs.size} faixas) - Myato Player:")
                appendLine()
                songs.forEachIndexed { index, s ->
                    appendLine("${index + 1}. ${s.title} - ${s.artist} (${s.durationFormatted})")
                }
                appendLine()
                appendLine("Compartilhado via Myato Player")
            }
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, trackListText)
            }
        }

        context.startActivity(Intent.createChooser(intent, "Compartilhar ${songs.size} músicas"))
    }

    fun shareM3uFile(context: Context, m3uFile: File, playlistTitle: String) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                m3uFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/x-mpegurl"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Playlist: $playlistTitle")
                putExtra(Intent.EXTRA_TEXT, "Playlist \"$playlistTitle\" exportada do Myato Player 🎶")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar arquivo de Playlist (.m3u)"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

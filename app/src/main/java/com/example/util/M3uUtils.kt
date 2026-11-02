package com.example.util

import android.content.Context
import android.net.Uri
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object M3uUtils {

    /**
     * Generates a valid .m3u / .m3u8 playlist file containing EXTM3U metadata and references.
     */
    suspend fun exportM3uFile(
        context: Context,
        playlistName: String,
        songs: List<Song>
    ): File = withContext(Dispatchers.IO) {
        val sanitizedName = playlistName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "playlist" }
        val exportDir = File(context.cacheDir, "playlists").apply { mkdirs() }
        val m3uFile = File(exportDir, "$sanitizedName.m3u")

        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("#EXTM3U")
        stringBuilder.appendLine("#PLAYLIST:$playlistName")

        for (song in songs) {
            val durationSec = (song.durationMs / 1000).coerceAtLeast(0)
            stringBuilder.appendLine("#EXTINF:$durationSec,${song.artist} - ${song.title}")
            // Prefer actual file path if accessible, else content URI string
            val location = when {
                song.path.isNotBlank() && !song.path.startsWith("sample://") -> song.path
                song.uriString.isNotBlank() -> song.uriString
                else -> song.path
            }
            stringBuilder.appendLine(location)
        }

        m3uFile.writeText(stringBuilder.toString())
        m3uFile
    }

    /**
     * Parses an M3U file from a Uri, extracting track entries.
     * Returns a list of ParsedTrack containing title, artist, and path/uri hint.
     */
    data class ParsedTrack(
        val title: String,
        val artist: String,
        val durationSec: Long,
        val location: String
    )

    suspend fun parseM3u(
        context: Context,
        uri: Uri
    ): List<ParsedTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<ParsedTrack>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    var currentTitle = ""
                    var currentArtist = ""
                    var currentDuration = 0L

                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line?.trim() ?: continue
                        if (trimmed.isEmpty()) continue

                        if (trimmed.startsWith("#EXTINF:")) {
                            // Example format: #EXTINF:214,Artist - Title
                            val info = trimmed.removePrefix("#EXTINF:")
                            val parts = info.split(",", limit = 2)
                            currentDuration = parts.getOrNull(0)?.trim()?.toLongOrNull() ?: 0L
                            val namePart = parts.getOrNull(1)?.trim() ?: ""
                            if (namePart.contains(" - ")) {
                                val split = namePart.split(" - ", limit = 2)
                                currentArtist = split[0].trim()
                                currentTitle = split[1].trim()
                            } else {
                                currentTitle = namePart
                                currentArtist = "Artista Desconhecido"
                            }
                        } else if (!trimmed.startsWith("#")) {
                            // Track location / path / URI line
                            val location = trimmed
                            val title = if (currentTitle.isNotBlank()) currentTitle else File(location).nameWithoutExtension
                            val artist = if (currentArtist.isNotBlank()) currentArtist else "Artista Desconhecido"

                            tracks.add(
                                ParsedTrack(
                                    title = title,
                                    artist = artist,
                                    durationSec = currentDuration,
                                    location = location
                                )
                            )

                            currentTitle = ""
                            currentArtist = ""
                            currentDuration = 0L
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracks
    }
}

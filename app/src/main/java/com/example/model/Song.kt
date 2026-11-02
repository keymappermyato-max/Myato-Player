package com.example.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String = "Artista Desconhecido",
    val album: String = "Álbum Desconhecido",
    val durationMs: Long = 0L,
    val path: String = "",
    val uriString: String = "",
    val albumArtUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val folderName: String = "Músicas",
    val isFavorite: Boolean = false,
    val fileSize: Long = 0L
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

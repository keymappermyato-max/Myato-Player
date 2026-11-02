package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val path: String,
    val uriString: String,
    val albumArtUri: String?,
    val dateAdded: Long,
    val folderName: String,
    val isFavorite: Boolean = false,
    val fileSize: Long = 0L
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        path = path,
        uriString = uriString,
        albumArtUri = albumArtUri,
        dateAdded = dateAdded,
        folderName = folderName,
        isFavorite = isFavorite,
        fileSize = fileSize
    )

    companion object {
        fun fromSong(song: Song): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs,
            path = song.path,
            uriString = song.uriString,
            albumArtUri = song.albumArtUri,
            dateAdded = song.dateAdded,
            folderName = song.folderName,
            isFavorite = song.isFavorite,
            fileSize = song.fileSize
        )
    }
}

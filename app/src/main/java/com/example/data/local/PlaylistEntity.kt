package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toPlaylist(songCount: Int = 0): Playlist = Playlist(
        id = id,
        name = name,
        coverUri = coverUri,
        songCount = songCount,
        createdAt = createdAt
    )
}

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Long = 0,
    val songId: Long,
    val playedAt: Long = System.currentTimeMillis()
)

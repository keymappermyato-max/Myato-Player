package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongCrossRef
import com.example.data.local.SongDao
import com.example.data.local.SongEntity
import com.example.model.Playlist
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MusicRepository(
    private val context: Context,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao
) {

    val allSongs: Flow<List<Song>> = songDao.getAllSongs().map { list -> list.map { it.toSong() } }
    val favoriteSongs: Flow<List<Song>> = favoriteDao.getFavoriteSongs().map { list -> list.map { it.toSong() } }
    val recentlyAdded: Flow<List<Song>> = songDao.getRecentlyAdded().map { list -> list.map { it.toSong() } }
    val recentlyPlayed: Flow<List<Song>> = songDao.getRecentlyPlayed().map { list -> list.map { it.toSong() } }
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { list ->
        list.map { entity ->
            val count = playlistDao.getSongCountForPlaylist(entity.id)
            entity.toPlaylist(count)
        }
    }

    suspend fun getSongById(id: Long): Song? {
        return songDao.getSongById(id)?.toSong()
    }

    fun searchSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongs(query).map { list -> list.map { it.toSong() } }
    }

    suspend fun toggleFavorite(song: Song) {
        val isFav = favoriteDao.isFavoriteSync(song.id)
        if (isFav) {
            favoriteDao.removeFavorite(song.id)
            songDao.setFavorite(song.id, false)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(songId = song.id))
            songDao.setFavorite(song.id, true)
        }
    }

    suspend fun logPlayback(songId: Long) {
        songDao.insertHistory(HistoryEntity(songId = songId))
    }

    suspend fun clearHistory() {
        songDao.clearPlaybackHistory()
    }

    suspend fun clearAllSongs() {
        songDao.clearAllSongs()
    }

    suspend fun deleteSong(song: Song) = withContext(Dispatchers.IO) {
        val songId = song.id
        songDao.deleteSong(songId)
        songDao.deleteHistoryForSong(songId)
        favoriteDao.removeFavorite(songId)
        playlistDao.removeSongFromAllPlaylists(songId)

        if (song.path.isNotBlank()) {
            try {
                val file = File(song.path)
                val appExtDir = context.getExternalFilesDir(null)
                if (file.exists() && file.isFile) {
                    if (appExtDir != null && file.absolutePath.startsWith(appExtDir.absolutePath)) {
                        file.delete()
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    suspend fun deleteSongById(songId: Long) = withContext(Dispatchers.IO) {
        songDao.deleteSong(songId)
        songDao.deleteHistoryForSong(songId)
        favoriteDao.removeFavorite(songId)
        playlistDao.removeSongFromAllPlaylists(songId)
    }

    // Playlists
    suspend fun createPlaylist(name: String, coverUri: String? = null): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name, coverUri = coverUri))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.updatePlaylistName(playlistId, newName)
    }

    suspend fun updatePlaylistCover(playlistId: Long, coverUri: String) {
        playlistDao.updatePlaylistCover(playlistId, coverUri)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.clearPlaylistSongs(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val currentCount = playlistDao.getSongCountForPlaylist(playlistId)
        playlistDao.addSongToPlaylist(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                orderIndex = currentCount
            )
        )
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) = withContext(Dispatchers.IO) {
        var startOrder = playlistDao.getSongCountForPlaylist(playlistId)
        val crossRefs = songIds.map { id ->
            PlaylistSongCrossRef(playlistId = playlistId, songId = id, orderIndex = startOrder++)
        }
        playlistDao.addSongsToPlaylist(crossRefs)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun removeSongsFromPlaylist(playlistId: Long, songIds: List<Long>) {
        playlistDao.removeSongsFromPlaylist(playlistId, songIds)
    }

    suspend fun reorderPlaylistSongs(playlistId: Long, orderedSongIds: List<Long>) = withContext(Dispatchers.IO) {
        orderedSongIds.forEachIndexed { index, songId ->
            playlistDao.updateSongOrder(playlistId, songId, index)
        }
    }

    suspend fun duplicatePlaylist(playlistId: Long, newName: String): Long = withContext(Dispatchers.IO) {
        val existingSongs = playlistDao.getSongsForPlaylist(playlistId).first()
        val newId = playlistDao.insertPlaylist(PlaylistEntity(name = newName))
        val refs = existingSongs.mapIndexed { index, song ->
            PlaylistSongCrossRef(playlistId = newId, songId = song.id, orderIndex = index)
        }
        playlistDao.addSongsToPlaylist(refs)
        newId
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId).map { list -> list.map { it.toSong() } }
    }

    suspend fun addSongsToFavorites(songIds: List<Long>) = withContext(Dispatchers.IO) {
        val entities = songIds.map { FavoriteEntity(songId = it) }
        favoriteDao.addFavorites(entities)
        songDao.setFavoriteBatch(songIds, true)
    }

    suspend fun deleteSongsBatch(songs: List<Song>) = withContext(Dispatchers.IO) {
        val ids = songs.map { it.id }
        songDao.deleteSongs(ids)
        ids.forEach { songDao.deleteHistoryForSong(it) }
        favoriteDao.removeFavorites(ids)
        ids.forEach { playlistDao.removeSongFromAllPlaylists(it) }

        val appExtDir = context.getExternalFilesDir(null)
        songs.forEach { song ->
            if (song.path.isNotBlank()) {
                try {
                    val file = File(song.path)
                    if (file.exists() && file.isFile && appExtDir != null && file.absolutePath.startsWith(appExtDir.absolutePath)) {
                        file.delete()
                    }
                } catch (ignored: Exception) {}
            }
        }
    }

    suspend fun importM3uPlaylist(uri: Uri): Pair<String, Int> = withContext(Dispatchers.IO) {
        val parsedTracks = com.example.util.M3uUtils.parseM3u(context, uri)
        var fileName = "Playlist Importada"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                fileName = cursor.getString(nameIndex)?.substringBeforeLast(".") ?: fileName
            }
        }

        val allCurrent = songDao.getAllSongs().first()
        val matchedIds = mutableListOf<Long>()

        for (track in parsedTracks) {
            val match = allCurrent.find {
                it.title.equals(track.title, ignoreCase = true) ||
                        (it.path.isNotBlank() && it.path == track.location) ||
                        (it.uriString.isNotBlank() && it.uriString == track.location)
            }
            if (match != null) {
                matchedIds.add(match.id)
            } else {
                // Create a song entry from parsed track if location exists
                val newId = System.currentTimeMillis() + (0..999).random()
                val newSong = SongEntity(
                    id = newId,
                    title = track.title,
                    artist = track.artist,
                    album = fileName,
                    durationMs = track.durationSec * 1000,
                    path = track.location,
                    uriString = track.location,
                    albumArtUri = null,
                    dateAdded = System.currentTimeMillis(),
                    folderName = "M3U",
                    isFavorite = false,
                    fileSize = 0L
                )
                songDao.insertSong(newSong)
                matchedIds.add(newId)
            }
        }

        val playlistId = playlistDao.insertPlaylist(PlaylistEntity(name = fileName))
        val refs = matchedIds.mapIndexed { idx, songId ->
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId, orderIndex = idx)
        }
        playlistDao.addSongsToPlaylist(refs)
        Pair(fileName, matchedIds.size)
    }

    // MediaStore Scanner
    suspend fun scanDeviceAudio(): Int = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<SongEntity>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Sem Título"
                    val artist = it.getString(artistCol)?.let { a ->
                        if (a == "<unknown>" || a.isBlank()) "Artista Desconhecido" else a
                    } ?: "Artista Desconhecido"
                    val album = it.getString(albumCol)?.let { al ->
                        if (al == "<unknown>" || al.isBlank()) "Álbum Desconhecido" else al
                    } ?: "Álbum Desconhecido"
                    val duration = it.getLong(durationCol)
                    val path = it.getString(dataCol) ?: ""
                    val dateAdded = it.getLong(dateCol) * 1000
                    val size = it.getLong(sizeCol)
                    val albumId = it.getLong(albumIdCol)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val albumArtUri = "content://media/external/audio/albumart/$albumId"

                    val folderName = if (path.isNotBlank()) {
                        File(path).parentFile?.name ?: "Músicas"
                    } else "Músicas"

                    songsList.add(
                        SongEntity(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            path = path,
                            uriString = contentUri.toString(),
                            albumArtUri = albumArtUri,
                            dateAdded = dateAdded,
                            folderName = folderName,
                            isFavorite = false,
                            fileSize = size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (songsList.isNotEmpty()) {
            songDao.insertSongs(songsList)
        }
        songsList.size
    }

    // Import from SAF Uri (single or multiple files chosen via file picker)
    suspend fun importAudioUri(uri: Uri): Song? = withContext(Dispatchers.IO) {
        try {
            var fileName = "Áudio Importado"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            var title = fileName.substringBeforeLast(".")
            var artist = "Artista Desconhecido"
            var album = "Álbum Importado"
            var durationMs = 0L

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                    if (it.isNotBlank()) title = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                    if (it.isNotBlank()) artist = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                    if (it.isNotBlank()) album = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                    durationMs = it.toLongOrNull() ?: 0L
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { retriever.release() } catch (ignored: Exception) {}
            }

            val id = System.currentTimeMillis() + (0..999).random()
            val song = Song(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                path = "",
                uriString = uri.toString(),
                albumArtUri = null,
                dateAdded = System.currentTimeMillis(),
                folderName = "Importadas",
                isFavorite = false,
                fileSize = fileSize
            )

            songDao.insertSong(SongEntity.fromSong(song))
            song
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Insert downloaded song into library
    suspend fun addDownloadedSong(
        title: String,
        artist: String,
        file: File,
        durationMs: Long
    ): Song = withContext(Dispatchers.IO) {
        val id = System.currentTimeMillis() + (0..999).random()
        val song = Song(
            id = id,
            title = title,
            artist = artist,
            album = "Downloads Myato",
            durationMs = durationMs,
            path = file.absolutePath,
            uriString = Uri.fromFile(file).toString(),
            albumArtUri = null,
            dateAdded = System.currentTimeMillis(),
            folderName = "Downloads",
            isFavorite = false,
            fileSize = file.length()
        )
        songDao.insertSong(SongEntity.fromSong(song))
        song
    }

    // Seed sample songs if the database is empty (ensures the user can immediately play tracks!)
    suspend fun ensureSampleSongsIfEmpty() = withContext(Dispatchers.IO) {
        val existing = songDao.getAllSongs().first()
        if (existing.isEmpty()) {
            val samples = listOf(
                SongEntity(
                    id = 1001L,
                    title = "Neon Horizon (Synthwave)",
                    artist = "Myato Studio",
                    album = "Cyber Dreams Vol. 1",
                    durationMs = 214000L,
                    path = "sample://neon_horizon",
                    uriString = "sample://neon_horizon",
                    albumArtUri = null,
                    dateAdded = System.currentTimeMillis() - 86400000 * 2,
                    folderName = "Myato Exclusive",
                    isFavorite = true,
                    fileSize = 5120000L
                ),
                SongEntity(
                    id = 1002L,
                    title = "Midnight Pulse (Cyberpunk)",
                    artist = "Aura & Pulse",
                    album = "Neon Velocity",
                    durationMs = 185000L,
                    path = "sample://midnight_pulse",
                    uriString = "sample://midnight_pulse",
                    albumArtUri = null,
                    dateAdded = System.currentTimeMillis() - 86400000,
                    folderName = "Myato Exclusive",
                    isFavorite = true,
                    fileSize = 4420000L
                ),
                SongEntity(
                    id = 1003L,
                    title = "Starlight Voyage (Lo-Fi Chill)",
                    artist = "Komorebi Beats",
                    album = "Lofi Astral",
                    durationMs = 162000L,
                    path = "sample://starlight_voyage",
                    uriString = "sample://starlight_voyage",
                    albumArtUri = null,
                    dateAdded = System.currentTimeMillis() - 3600000 * 5,
                    folderName = "Relax & Chill",
                    isFavorite = false,
                    fileSize = 3890000L
                ),
                SongEntity(
                    id = 1004L,
                    title = "Electric Bass Dimension",
                    artist = "SubZero Sound",
                    album = "Deep Frequency",
                    durationMs = 240000L,
                    path = "sample://electric_bass",
                    uriString = "sample://electric_bass",
                    albumArtUri = null,
                    dateAdded = System.currentTimeMillis() - 3600000 * 2,
                    folderName = "Bass Heavy",
                    isFavorite = false,
                    fileSize = 5760000L
                )
            )
            songDao.insertSongs(samples)

            val pId = playlistDao.insertPlaylist(PlaylistEntity(name = "Favoritas da Galera"))
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef(pId, 1001L))
            playlistDao.addSongToPlaylist(PlaylistSongCrossRef(pId, 1002L))

            favoriteDao.addFavorite(FavoriteEntity(1001L))
            favoriteDao.addFavorite(FavoriteEntity(1002L))
        }
    }
}

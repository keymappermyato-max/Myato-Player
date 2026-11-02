package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongCrossRef
import com.example.data.local.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app_name string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Myato Player", appName)
    }

    @Test
    fun `test Room Song, Playlist, Favorite, and History entities`() = runBlocking {
        val songDao = database.songDao()
        val playlistDao = database.playlistDao()
        val favoriteDao = database.favoriteDao()

        // 1. SongEntity insertion and retrieval
        val song = SongEntity(
            id = 1L,
            title = "Neon Drift",
            artist = "Myato Beats",
            album = "Cyber Sessions",
            durationMs = 180000L,
            path = "/storage/music/neon_drift.mp3",
            uriString = "content://media/external/audio/1",
            albumArtUri = null,
            dateAdded = System.currentTimeMillis(),
            folderName = "Synthwave",
            isFavorite = true
        )
        songDao.insertSong(song)

        val retrievedSong = songDao.getSongById(1L)
        assertNotNull(retrievedSong)
        assertEquals("Neon Drift", retrievedSong?.title)

        // 2. PlaylistEntity & CrossRef
        val playlistId = playlistDao.insertPlaylist(PlaylistEntity(name = "Midnight Vibes"))
        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = 1L))

        val playlistSongs = playlistDao.getSongsForPlaylist(playlistId).first()
        assertEquals(1, playlistSongs.size)
        assertEquals("Neon Drift", playlistSongs.first().title)

        // 3. FavoriteEntity
        favoriteDao.addFavorite(FavoriteEntity(songId = 1L))
        val isFav = favoriteDao.isFavoriteSync(1L)
        assertTrue(isFav)

        val favoriteSongs = favoriteDao.getFavoriteSongs().first()
        assertEquals(1, favoriteSongs.size)
        assertEquals(1L, favoriteSongs.first().id)

        // 4. Playback History
        songDao.insertHistory(HistoryEntity(songId = 1L))
        val historySongs = songDao.getRecentlyPlayed(10).first()
        assertEquals(1, historySongs.size)
        assertEquals(1L, historySongs.first().id)
    }
}

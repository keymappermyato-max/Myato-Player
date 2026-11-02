package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.repository.DownloadRepository
import com.example.data.repository.MusicRepository
import com.example.data.repository.SettingsRepository
import com.example.player.AudioPlayerManager

class MyatoApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var musicRepository: MusicRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var downloadRepository: DownloadRepository
        private set
    lateinit var playerManager: AudioPlayerManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannel()

        database = AppDatabase.getInstance(this)
        settingsRepository = SettingsRepository(this)
        musicRepository = MusicRepository(this, database.songDao(), database.playlistDao(), database.favoriteDao())
        downloadRepository = DownloadRepository(this, database.downloadDao(), musicRepository)
        playerManager = AudioPlayerManager(this, musicRepository, settingsRepository)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Myato Player Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reprodução de música do Myato Player"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "myato_playback_channel"
        lateinit var instance: MyatoApplication
            private set
    }
}

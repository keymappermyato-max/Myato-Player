package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.MyatoApplication
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlaybackService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var stateObserverJob: Job? = null
    private var mediaSession: MediaSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        observePlaybackState()
    }

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "MyatoMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    MyatoApplication.instance.playerManager.resume()
                }

                override fun onPause() {
                    MyatoApplication.instance.playerManager.pause()
                }

                override fun onSkipToNext() {
                    MyatoApplication.instance.playerManager.next()
                }

                override fun onSkipToPrevious() {
                    MyatoApplication.instance.playerManager.previous()
                }

                override fun onSeekTo(pos: Long) {
                    MyatoApplication.instance.playerManager.seekTo(pos)
                }

                override fun onStop() {
                    val player = MyatoApplication.instance.playerManager
                    player.pause()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = MyatoApplication.instance
        val player = app.playerManager

        when (intent?.action) {
            ACTION_PLAY -> player.resume()
            ACTION_PAUSE -> player.pause()
            ACTION_TOGGLE -> player.togglePlayPause()
            ACTION_NEXT -> player.next()
            ACTION_PREVIOUS -> player.previous()
            ACTION_TOGGLE_FAVORITE -> {
                val currentSong = player.playbackState.value.currentSong
                if (currentSong != null) {
                    serviceScope.launch(Dispatchers.IO) {
                        app.musicRepository.toggleFavorite(currentSong)
                    }
                }
            }
            ACTION_STOP -> {
                player.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE -> updateNotification()
        }

        return START_NOT_STICKY
    }

    private fun observePlaybackState() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            MyatoApplication.instance.playerManager.playbackState.collectLatest {
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        val player = MyatoApplication.instance.playerManager
        val state = player.playbackState.value
        val song = state.currentSong

        if (song == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }

        // Update MediaSession PlaybackState
        val pbState = if (state.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP

        mediaSession?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(pbState, state.currentPositionMs, 1.0f)
                .build()
        )

        // Try load album art bitmap
        val defaultArtBitmap: Bitmap? = try {
            BitmapFactory.decodeResource(resources, R.drawable.ic_default_art)
        } catch (e: Exception) {
            null
        }

        // Update MediaMetadata
        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, if (state.durationMs > 0) state.durationMs else song.durationMs)

        if (defaultArtBitmap != null) {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, defaultArtBitmap)
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, defaultArtBitmap)
        }
        mediaSession?.setMetadata(metadataBuilder.build())

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREVIOUS }
        val prevPending = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseAction = if (state.isPlaying) {
            val pauseIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PAUSE }
            val pausePending = PendingIntent.getService(this, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            Notification.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pausar",
                pausePending
            ).build()
        } else {
            val playIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PLAY }
            val playPending = PendingIntent.getService(this, 3, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            Notification.Action.Builder(
                android.R.drawable.ic_media_play,
                "Reproduzir",
                playPending
            ).build()
        }

        val nextIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getService(this, 4, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val favIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_TOGGLE_FAVORITE }
        val favPending = PendingIntent.getService(this, 5, favIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 6, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val prevAction = Notification.Action.Builder(
            android.R.drawable.ic_media_previous,
            "Anterior",
            prevPending
        ).build()

        val nextAction = Notification.Action.Builder(
            android.R.drawable.ic_media_next,
            "Próxima",
            nextPending
        ).build()

        val favAction = Notification.Action.Builder(
            if (song.isFavorite) android.R.drawable.star_big_on else android.R.drawable.star_big_off,
            if (song.isFavorite) "Favoritada" else "Favoritar",
            favPending
        ).build()

        val closeAction = Notification.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Fechar",
            stopPending
        ).build()

        val mediaStyle = Notification.MediaStyle()
        mediaSession?.let {
            mediaStyle.setMediaSession(it.sessionToken)
            mediaStyle.setShowActionsInCompactView(0, 1, 2)
        }

        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, MyatoApplication.CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = notificationBuilder
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSubText(song.album)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(defaultArtBitmap)
            .setContentIntent(contentPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(state.isPlaying)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(favAction)
            .addAction(closeAction)
            .setStyle(mediaStyle)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        stateObserverJob?.cancel()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.example.myatoplayer.PLAY"
        const val ACTION_PAUSE = "com.example.myatoplayer.PAUSE"
        const val ACTION_TOGGLE = "com.example.myatoplayer.TOGGLE"
        const val ACTION_NEXT = "com.example.myatoplayer.NEXT"
        const val ACTION_PREVIOUS = "com.example.myatoplayer.PREVIOUS"
        const val ACTION_TOGGLE_FAVORITE = "com.example.myatoplayer.TOGGLE_FAVORITE"
        const val ACTION_STOP = "com.example.myatoplayer.STOP"
        const val ACTION_UPDATE = "com.example.myatoplayer.UPDATE"
    }
}

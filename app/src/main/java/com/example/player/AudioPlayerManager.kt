package com.example.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import com.example.data.repository.MusicRepository
import com.example.data.repository.SettingsRepository
import com.example.model.Song
import com.example.service.MusicPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.sin

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class QueueSortType {
    TITLE, ARTIST, ALBUM, DURATION, REVERSE
}

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val queueTitle: String = "Fila de Reprodução",
    val volume: Float = 1.0f
)

class AudioPlayerManager(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var sampleSynthJob: Job? = null
    private var sampleAudioTrack: AudioTrack? = null

    // Audio effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    // 24 visualizer frequency bars
    private val _visualizerBars = MutableStateFlow(FloatArray(24) { 0.05f })
    val visualizerBars: StateFlow<FloatArray> = _visualizerBars.asStateFlow()

    private var progressTrackingJob: Job? = null

    init {
        initEqualizerDefaults()
        startVisualizerLoop()
    }

    private fun initEqualizerDefaults() {
        val bands = listOf(
            EqualizerBand(0, 60, -1000, 1000, settingsRepository.getBandLevel(0, 200)),
            EqualizerBand(1, 230, -1000, 1000, settingsRepository.getBandLevel(1, 400)),
            EqualizerBand(2, 910, -1000, 1000, settingsRepository.getBandLevel(2, 0)),
            EqualizerBand(3, 3600, -1000, 1000, settingsRepository.getBandLevel(3, 300)),
            EqualizerBand(4, 14000, -1000, 1000, settingsRepository.getBandLevel(4, 500))
        )
        _equalizerState.value = EqualizerState(
            isEnabled = settingsRepository.isEqualizerEnabled(),
            currentPreset = settingsRepository.getEqualizerPreset(),
            bands = bands,
            bassBoost = settingsRepository.getBassBoostStrength(),
            virtualizer = settingsRepository.getVirtualizerStrength()
        )
    }

    fun playSong(song: Song, newQueue: List<Song>? = null, title: String? = null) {
        val queue = newQueue ?: if (_playbackState.value.queue.isEmpty()) listOf(song) else _playbackState.value.queue
        val index = queue.indexOfFirst { it.id == song.id }.let { if (it == -1) 0 else it }

        _playbackState.value = _playbackState.value.copy(
            currentSong = song,
            queue = queue,
            queueIndex = index,
            queueTitle = title ?: _playbackState.value.queueTitle,
            isPlaying = true,
            currentPositionMs = 0L,
            durationMs = song.durationMs
        )

        saveQueueState()

        scope.launch(Dispatchers.IO) {
            musicRepository.logPlayback(song.id)
        }

        startAudioEngine(song)
        startForegroundService()
    }

    fun playQueue(queue: List<Song>, startIndex: Int = 0, title: String = "Fila de Reprodução") {
        if (queue.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, queue.size - 1)
        val song = queue[clampedIndex]
        playSong(song, queue, title)
    }

    fun addToQueue(song: Song) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        val exists = currentQueue.any { it.id == song.id }
        if (!exists) {
            currentQueue.add(song)
        }
        val currentSong = _playbackState.value.currentSong
        val index = if (currentSong != null) currentQueue.indexOfFirst { it.id == currentSong.id } else 0

        _playbackState.value = _playbackState.value.copy(
            queue = currentQueue,
            queueIndex = index
        )
        saveQueueState()
        if (currentSong == null) {
            playSong(song, currentQueue)
        }
    }

    fun addListToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val currentQueue = _playbackState.value.queue.toMutableList()
        val existingIds = currentQueue.map { it.id }.toSet()
        val toAdd = songs.filter { it.id !in existingIds }
        if (toAdd.isEmpty()) return
        currentQueue.addAll(toAdd)

        val currentSong = _playbackState.value.currentSong
        val index = if (currentSong != null) currentQueue.indexOfFirst { it.id == currentSong.id } else 0

        _playbackState.value = _playbackState.value.copy(
            queue = currentQueue,
            queueIndex = index
        )
        saveQueueState()
        if (currentSong == null) {
            playSong(toAdd.first(), currentQueue)
        }
    }

    fun playNext(song: Song) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        val currentSong = _playbackState.value.currentSong
        if (currentSong == null || currentQueue.isEmpty()) {
            playSong(song, listOf(song))
            return
        }

        val existingIndex = currentQueue.indexOfFirst { it.id == song.id }
        if (existingIndex != -1) {
            currentQueue.removeAt(existingIndex)
        }

        val currentIndex = currentQueue.indexOfFirst { it.id == currentSong.id }
        val insertIndex = if (currentIndex != -1) currentIndex + 1 else currentQueue.size
        currentQueue.add(insertIndex.coerceIn(0, currentQueue.size), song)

        val updatedIndex = currentQueue.indexOfFirst { it.id == currentSong.id }
        _playbackState.value = _playbackState.value.copy(
            queue = currentQueue,
            queueIndex = updatedIndex
        )
        saveQueueState()
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        if (index !in currentQueue.indices) return
        val removed = currentQueue.removeAt(index)
        val currentSong = _playbackState.value.currentSong

        if (currentSong?.id == removed.id) {
            if (currentQueue.isNotEmpty()) {
                val nextIndex = index.coerceAtMost(currentQueue.size - 1)
                playSong(currentQueue[nextIndex], currentQueue)
            } else {
                clearQueue()
            }
        } else {
            val newIndex = if (currentSong != null) currentQueue.indexOfFirst { it.id == currentSong.id } else -1
            _playbackState.value = _playbackState.value.copy(
                queue = currentQueue,
                queueIndex = newIndex
            )
            saveQueueState()
        }
    }

    fun removeSongsFromQueue(songIds: Set<Long>) {
        if (songIds.isEmpty()) return
        val currentQueue = _playbackState.value.queue.filter { it.id !in songIds }
        val currentSong = _playbackState.value.currentSong

        if (currentSong != null && currentSong.id in songIds) {
            if (currentQueue.isNotEmpty()) {
                playSong(currentQueue.first(), currentQueue)
            } else {
                clearQueue()
            }
        } else {
            val newIndex = if (currentSong != null) currentQueue.indexOfFirst { it.id == currentSong.id } else -1
            _playbackState.value = _playbackState.value.copy(
                queue = currentQueue,
                queueIndex = newIndex
            )
            saveQueueState()
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) return
        val item = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, item)

        val currentSong = _playbackState.value.currentSong
        val newIndex = if (currentSong != null) currentQueue.indexOfFirst { it.id == currentSong.id } else -1

        _playbackState.value = _playbackState.value.copy(
            queue = currentQueue,
            queueIndex = newIndex
        )
        saveQueueState()
    }

    fun reorderQueueList(newList: List<Song>) {
        val currentSong = _playbackState.value.currentSong
        val newIndex = if (currentSong != null) newList.indexOfFirst { it.id == currentSong.id } else -1
        _playbackState.value = _playbackState.value.copy(
            queue = newList,
            queueIndex = newIndex
        )
        saveQueueState()
    }

    fun clearQueue() {
        stopCurrentPlayback()
        _playbackState.value = _playbackState.value.copy(
            currentSong = null,
            queue = emptyList(),
            queueIndex = -1,
            isPlaying = false,
            currentPositionMs = 0L,
            durationMs = 0L
        )
        saveQueueState()
        updateForegroundService()
    }

    fun sortQueue(sortType: QueueSortType) {
        val currentQueue = _playbackState.value.queue
        if (currentQueue.size <= 1) return

        val sorted = when (sortType) {
            QueueSortType.TITLE -> currentQueue.sortedBy { it.title.lowercase() }
            QueueSortType.ARTIST -> currentQueue.sortedBy { it.artist.lowercase() }
            QueueSortType.ALBUM -> currentQueue.sortedBy { it.album.lowercase() }
            QueueSortType.DURATION -> currentQueue.sortedBy { it.durationMs }
            QueueSortType.REVERSE -> currentQueue.reversed()
        }
        reorderQueueList(sorted)
    }

    private fun saveQueueState() {
        val state = _playbackState.value
        val ids = state.queue.map { it.id }
        settingsRepository.saveLastQueue(ids, state.queueIndex, state.currentPositionMs)
    }

    fun restoreLastQueueIfAvailable(allSongs: List<Song>) {
        if (_playbackState.value.queue.isNotEmpty()) return
        val savedIds = settingsRepository.getLastQueueSongIds()
        if (savedIds.isEmpty()) return

        val map = allSongs.associateBy { it.id }
        val restoredQueue = savedIds.mapNotNull { map[it] }
        if (restoredQueue.isNotEmpty()) {
            val savedIdx = settingsRepository.getLastQueueIndex().coerceIn(0, restoredQueue.size - 1)
            val savedPos = settingsRepository.getLastPositionMs()
            val song = restoredQueue[savedIdx]

            _playbackState.value = _playbackState.value.copy(
                currentSong = song,
                queue = restoredQueue,
                queueIndex = savedIdx,
                isPlaying = false,
                currentPositionMs = savedPos,
                durationMs = song.durationMs
            )
            updateForegroundService()
        }
    }

    private fun startAudioEngine(song: Song) {
        stopCurrentPlayback()

        if (song.path.startsWith("sample://") || song.uriString.startsWith("sample://")) {
            playSyntheticTrack(song)
            return
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (song.path.isNotBlank() && File(song.path).exists()) {
                    setDataSource(song.path)
                } else if (song.uriString.isNotBlank()) {
                    setDataSource(context, Uri.parse(song.uriString))
                } else {
                    playSyntheticTrack(song)
                    return
                }

                prepare()
                start()
            }

            mediaPlayer = player
            attachAudioEffects(player.audioSessionId)

            _playbackState.value = _playbackState.value.copy(
                isPlaying = true,
                durationMs = if (player.duration > 0) player.duration.toLong() else song.durationMs
            )

            player.setOnCompletionListener {
                handleTrackCompletion()
            }

            player.setOnErrorListener { _, _, _ ->
                playSyntheticTrack(song)
                true
            }

            startProgressTracker()
        } catch (e: Exception) {
            e.printStackTrace()
            playSyntheticTrack(song)
        }
    }

    // High quality synth audio generator for sample tracks
    private fun playSyntheticTrack(song: Song) {
        stopCurrentPlayback()
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            sampleAudioTrack = audioTrack
            attachAudioEffects(audioTrack.audioSessionId)
            audioTrack.play()

            _playbackState.value = _playbackState.value.copy(
                isPlaying = true,
                durationMs = song.durationMs
            )

            sampleSynthJob = scope.launch(Dispatchers.Default) {
                val baseFreqs = when (song.id) {
                    1001L -> doubleArrayOf(130.81, 164.81, 196.00, 246.94) // C-E-G-B synth chord
                    1002L -> doubleArrayOf(146.83, 174.61, 220.00, 261.63) // D minor synth
                    1003L -> doubleArrayOf(174.61, 220.00, 261.63, 329.63) // F maj7 chill
                    else -> doubleArrayOf(110.00, 164.81, 220.00, 329.63) // A minor bass
                }
                val buffer = ShortArray(bufferSize)
                var sampleIndex = 0L
                var elapsedMs = 0L

                while (isActive && _playbackState.value.isPlaying) {
                    for (i in buffer.indices step 2) {
                        val t = sampleIndex.toDouble() / sampleRate
                        var sampleVal = 0.0

                        // Multi-harmonic soft synth
                        for (j in baseFreqs.indices) {
                            val f = baseFreqs[j]
                            val lfo = 1.0 + 0.15 * sin(2.0 * Math.PI * 0.5 * t)
                            sampleVal += sin(2.0 * Math.PI * f * lfo * t) * 0.22
                            // Sub-bass
                            sampleVal += sin(2.0 * Math.PI * (f / 2.0) * t) * 0.18
                        }
                        // Soft saturation
                        val clamped = (sampleVal * 16000.0).coerceIn(-30000.0, 30000.0).toInt().toShort()
                        buffer[i] = clamped
                        buffer[i + 1] = clamped
                        sampleIndex++
                    }
                    audioTrack.write(buffer, 0, buffer.size)

                    elapsedMs += (buffer.size / 2 * 1000L) / sampleRate
                    _playbackState.value = _playbackState.value.copy(currentPositionMs = elapsedMs)

                    if (elapsedMs >= song.durationMs) {
                        launch(Dispatchers.Main) { handleTrackCompletion() }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun attachAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()

            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
            }
            equalizer = eq

            // Query bands and center frequencies
            val numBands = eq.numberOfBands
            val (minLevel, maxLevel) = eq.bandLevelRange
            val bandsList = mutableListOf<EqualizerBand>()

            for (i in 0 until numBands) {
                val centerFreq = eq.getCenterFreq(i.toShort()) / 1000 // mHz to Hz
                val savedLevel = settingsRepository.getBandLevel(i, 0)
                try {
                    eq.setBandLevel(i.toShort(), savedLevel)
                } catch (ignored: Exception) {}
                bandsList.add(
                    EqualizerBand(
                        bandIndex = i.toShort(),
                        centerFreqHz = centerFreq,
                        minLevelMdB = minLevel,
                        maxLevelMdB = maxLevel,
                        currentLevelMdB = savedLevel
                    )
                )
            }

            val bb = BassBoost(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
                if (strengthSupported) {
                    setStrength(_equalizerState.value.bassBoost.toShort())
                }
            }
            bassBoost = bb

            val virt = Virtualizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
                if (strengthSupported) {
                    setStrength(_equalizerState.value.virtualizer.toShort())
                }
            }
            virtualizer = virt

            _equalizerState.value = _equalizerState.value.copy(
                bands = bandsList,
                isBassBoostSupported = bb.strengthSupported,
                isVirtualizerSupported = virt.strengthSupported
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        sampleAudioTrack?.pause()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        updateForegroundService()
    }

    fun resume() {
        val currentSong = _playbackState.value.currentSong
        if (currentSong == null) {
            val first = _playbackState.value.queue.firstOrNull()
            if (first != null) playSong(first)
            return
        }

        mediaPlayer?.let {
            it.start()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            startProgressTracker()
            updateForegroundService()
            return
        }

        if (sampleAudioTrack != null) {
            sampleAudioTrack?.play()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
            updateForegroundService()
        } else {
            playSong(currentSong)
        }
    }

    fun next() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        var nextIndex = state.queueIndex + 1
        if (state.isShuffle) {
            nextIndex = (0 until state.queue.size).random()
        } else if (nextIndex >= state.queue.size) {
            if (state.repeatMode == RepeatMode.ALL) {
                nextIndex = 0
            } else {
                pause()
                return
            }
        }
        val nextSong = state.queue.getOrNull(nextIndex) ?: return
        playSong(nextSong)
    }

    fun previous() {
        val state = _playbackState.value
        if (state.currentPositionMs > 3000) {
            seekTo(0)
            return
        }
        if (state.queue.isEmpty()) return

        var prevIndex = state.queueIndex - 1
        if (prevIndex < 0) {
            prevIndex = state.queue.size - 1
        }
        val prevSong = state.queue.getOrNull(prevIndex) ?: return
        playSong(prevSong)
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(clamped, clamped)
        sampleAudioTrack?.setVolume(clamped)
        _playbackState.value = _playbackState.value.copy(volume = clamped)
    }

    fun toggleShuffle() {
        _playbackState.value = _playbackState.value.copy(isShuffle = !_playbackState.value.isShuffle)
    }

    fun cycleRepeatMode() {
        val next = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.value = _playbackState.value.copy(repeatMode = next)
    }

    private fun handleTrackCompletion() {
        when (_playbackState.value.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                resume()
            }
            RepeatMode.ALL -> next()
            RepeatMode.OFF -> {
                if (_playbackState.value.queueIndex < _playbackState.value.queue.size - 1) {
                    next()
                } else {
                    pause()
                    seekTo(0)
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = scope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = it.currentPosition.toLong(),
                            durationMs = if (it.duration > 0) it.duration.toLong() else _playbackState.value.durationMs
                        )
                    }
                }
                delay(250)
            }
        }
    }

    private fun startVisualizerLoop() {
        scope.launch(Dispatchers.Default) {
            var phase = 0.0
            while (isActive) {
                val isPlaying = _playbackState.value.isPlaying
                val currentArray = _visualizerBars.value.clone()

                if (isPlaying) {
                    phase += 0.25
                    val bass = _equalizerState.value.bassBoost / 1000f
                    for (i in currentArray.indices) {
                        val baseWave = sin(phase + i * 0.45).toFloat() * 0.35f + 0.45f
                        val harmonic = sin(phase * 1.8 + i * 0.9).toFloat() * 0.2f
                        val boost = if (i < 8) bass * 0.25f else 0f
                        currentArray[i] = (baseWave + harmonic + boost).coerceIn(0.12f, 1.0f)
                    }
                } else {
                    // Decay smoothly to baseline
                    for (i in currentArray.indices) {
                        currentArray[i] = (currentArray[i] * 0.85f).coerceAtLeast(0.04f)
                    }
                }
                _visualizerBars.value = currentArray
                delay(40)
            }
        }
    }

    // Equalizer controls
    fun setEqualizerEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        settingsRepository.setEqualizerEnabled(enabled)
        _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
    }

    fun setBandLevel(bandIndex: Short, levelMdB: Short) {
        try {
            equalizer?.setBandLevel(bandIndex, levelMdB)
        } catch (ignored: Exception) {}
        settingsRepository.setBandLevel(bandIndex.toInt(), levelMdB)
        settingsRepository.setEqualizerPreset("Personalizado")

        val updatedBands = _equalizerState.value.bands.map {
            if (it.bandIndex == bandIndex) it.copy(currentLevelMdB = levelMdB) else it
        }
        _equalizerState.value = _equalizerState.value.copy(
            bands = updatedBands,
            currentPreset = "Personalizado"
        )
    }

    fun applyPreset(presetName: String) {
        val bands = _equalizerState.value.bands
        val levels: List<Short> = when (presetName) {
            "Pop" -> listOf(-150, 200, 500, 300, -200)
            "Rock" -> listOf(500, 300, -200, 400, 600)
            "Jazz" -> listOf(300, 100, 150, 250, 400)
            "Clássico" -> listOf(400, 250, -100, 300, 400)
            "Eletrônica" -> listOf(650, 400, 0, 350, 600)
            "Hip-Hop" -> listOf(700, 500, 100, 200, 400)
            "Acústico" -> listOf(350, 200, 200, 400, 300)
            "Grave Reforçado" -> listOf(900, 600, 200, 0, -100)
            else -> listOf(0, 0, 0, 0, 0)
        }

        val updatedBands = bands.mapIndexed { index, band ->
            val level = levels.getOrElse(index) { 0.toShort() }
            try {
                equalizer?.setBandLevel(band.bandIndex, level)
            } catch (ignored: Exception) {}
            settingsRepository.setBandLevel(band.bandIndex.toInt(), level)
            band.copy(currentLevelMdB = level)
        }

        val targetBass = if (presetName == "Grave Reforçado") 850 else if (presetName == "Eletrônica") 700 else 400
        setBassBoost(targetBass)

        settingsRepository.setEqualizerPreset(presetName)
        _equalizerState.value = _equalizerState.value.copy(
            currentPreset = presetName,
            bands = updatedBands
        )
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
        } catch (ignored: Exception) {}
        settingsRepository.setBassBoostStrength(clamped)
        _equalizerState.value = _equalizerState.value.copy(bassBoost = clamped)
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(clamped.toShort())
            }
        } catch (ignored: Exception) {}
        settingsRepository.setVirtualizerStrength(clamped)
        _equalizerState.value = _equalizerState.value.copy(virtualizer = clamped)
    }

    fun resetEqualizer() {
        applyPreset("Normal")
        setBassBoost(400)
        setVirtualizer(300)
    }

    private fun stopCurrentPlayback() {
        progressTrackingJob?.cancel()
        sampleSynthJob?.cancel()

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (ignored: Exception) {}
        mediaPlayer = null

        try {
            sampleAudioTrack?.stop()
            sampleAudioTrack?.release()
        } catch (ignored: Exception) {}
        sampleAudioTrack = null
    }

    private fun startForegroundService() {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_UPDATE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun onSongDeleted(songId: Long) {
        val currentSong = _playbackState.value.currentSong
        val newQueue = _playbackState.value.queue.filter { it.id != songId }
        val newIndex = newQueue.indexOfFirst { it.id == currentSong?.id }

        if (currentSong?.id == songId) {
            if (newQueue.isNotEmpty()) {
                val nextSong = newQueue.getOrNull(newIndex.coerceAtLeast(0)) ?: newQueue.first()
                playSong(nextSong, newQueue)
            } else {
                pause()
                stopCurrentPlayback()
                _playbackState.value = _playbackState.value.copy(
                    currentSong = null,
                    queue = emptyList(),
                    queueIndex = -1,
                    isPlaying = false,
                    currentPositionMs = 0L,
                    durationMs = 0L
                )
                updateForegroundService()
            }
        } else {
            _playbackState.value = _playbackState.value.copy(
                queue = newQueue,
                queueIndex = newIndex
            )
        }
    }

    private fun updateForegroundService() {
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_UPDATE
        }
        context.startService(intent)
    }
}

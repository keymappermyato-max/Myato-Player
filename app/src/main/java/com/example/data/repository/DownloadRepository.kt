package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class UrlValidationResult {
    data class ValidAudioUrl(val url: String, val inferredTitle: String) : UrlValidationResult()
    data class YouTubeUrl(
        val url: String,
        val videoId: String,
        val inferredTitle: String = "Áudio do YouTube",
        val author: String = "YouTube"
    ) : UrlValidationResult()
    data class InvalidUrl(val reason: String) : UrlValidationResult()
}

class DownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val musicRepository: MusicRepository
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val scope = CoroutineScope(Dispatchers.IO)

    val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads().map { list ->
        list.map { it.toDownloadItem() }
    }

    fun extractYouTubeId(url: String): String? {
        val trimmed = url.trim()
        val regexList = listOf(
            Regex("""(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/|youtube\.com\/shorts\/)([a-zA-Z0-9_-]{11})"""),
            Regex("""^([a-zA-Z0-9_-]{11})$""")
        )
        for (r in regexList) {
            val match = r.find(trimmed)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    suspend fun fetchYouTubeInfo(videoId: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val title = json.optString("title", "Faixa do YouTube")
                    val author = json.optString("author_name", "YouTube")
                    return@withContext Pair(title, author)
                }
            }
        } catch (_: Exception) {}
        null
    }

    fun validateUrl(inputUrl: String): UrlValidationResult {
        val trimmed = inputUrl.trim()
        if (trimmed.isBlank()) {
            return UrlValidationResult.InvalidUrl("Por favor, insira um link válido.")
        }

        val ytId = extractYouTubeId(trimmed)
        if (ytId != null) {
            return UrlValidationResult.YouTubeUrl(
                url = trimmed,
                videoId = ytId,
                inferredTitle = "Áudio do YouTube ($ytId)"
            )
        }

        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            return UrlValidationResult.InvalidUrl("O link deve começar com http:// ou https://")
        }

        // Check if audio file extension
        val cleanUrl = trimmed.substringBefore("?").substringBefore("#")
        val ext = cleanUrl.substringAfterLast(".", "").lowercase()
        val validExtensions = listOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus")
        
        val fileName = cleanUrl.substringAfterLast("/").ifBlank { "audio_download.mp3" }
        val inferredTitle = fileName.substringBeforeLast(".")

        return if (validExtensions.contains(ext) || trimmed.contains("audio", ignoreCase = true)) {
            UrlValidationResult.ValidAudioUrl(trimmed, inferredTitle)
        } else {
            UrlValidationResult.ValidAudioUrl(trimmed, inferredTitle)
        }
    }

    suspend fun startDownload(url: String, customTitle: String? = null, customArtist: String? = null): Long = withContext(Dispatchers.IO) {
        val ytId = extractYouTubeId(url)
        val initialTitle: String
        val initialArtist: String

        if (ytId != null) {
            val ytInfo = fetchYouTubeInfo(ytId)
            initialTitle = customTitle?.takeIf { it.isNotBlank() } ?: ytInfo?.first ?: "Áudio do YouTube"
            initialArtist = customArtist?.takeIf { it.isNotBlank() } ?: ytInfo?.second ?: "YouTube"
        } else {
            val cleanUrl = url.substringBefore("?").substringBefore("#")
            val fileName = cleanUrl.substringAfterLast("/").ifBlank { "faixa_${System.currentTimeMillis()}.mp3" }
            initialTitle = customTitle?.takeIf { it.isNotBlank() } ?: fileName.substringBeforeLast(".")
            initialArtist = customArtist?.takeIf { it.isNotBlank() } ?: "Download Autorizado"
        }

        val entity = DownloadEntity(
            url = url,
            title = initialTitle,
            artist = initialArtist,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = 0L,
            status = DownloadStatus.DOWNLOADING.name,
            filePath = null,
            errorMessage = null,
            createdAt = System.currentTimeMillis()
        )
        val downloadId = downloadDao.insertDownload(entity)

        val job = scope.launch {
            if (ytId != null) {
                executeYouTubeDownload(downloadId, ytId, initialTitle, initialArtist)
            } else {
                executeDirectDownload(downloadId, url, initialTitle, initialArtist)
            }
        }
        activeJobs[downloadId] = job
        downloadId
    }

    private suspend fun resolveYouTubeAudioStream(videoId: String): Pair<String, String>? {
        // Method 1: Cobalt API instances
        val cobaltInstances = listOf(
            "https://cobalt-api.kwiatekm.tokyo",
            "https://api.cobalt.tools"
        )
        for (host in cobaltInstances) {
            try {
                val jsonBody = JSONObject().apply {
                    put("url", "https://www.youtube.com/watch?v=$videoId")
                    put("downloadMode", "audio")
                    put("audioFormat", "mp3")
                }
                val request = Request.Builder()
                    .url(host)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val str = resp.body?.string() ?: ""
                        val json = JSONObject(str)
                        val streamUrl = json.optString("url").takeIf { it.isNotBlank() }
                        if (streamUrl != null) {
                            return Pair(streamUrl, "mp3")
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Method 2: Piped API audio streams
        val pipedInstances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.privacydev.net",
            "https://piped-api.lunar.icu"
        )
        for (piped in pipedInstances) {
            try {
                val request = Request.Builder()
                    .url("$piped/streams/$videoId")
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val str = resp.body?.string() ?: ""
                        val json = JSONObject(str)
                        val audioStreams = json.optJSONArray("audioStreams")
                        if (audioStreams != null && audioStreams.length() > 0) {
                            var bestUrl: String? = null
                            var bestBitrate = 0
                            var formatExt = "m4a"
                            for (i in 0 until audioStreams.length()) {
                                val stream = audioStreams.getJSONObject(i)
                                val bitrate = stream.optInt("bitrate", 0)
                                val streamUrl = stream.optString("url")
                                val format = stream.optString("format")
                                if (streamUrl.isNotBlank() && bitrate >= bestBitrate) {
                                    bestBitrate = bitrate
                                    bestUrl = streamUrl
                                    formatExt = if (format.contains("M4A", ignoreCase = true)) "m4a" else "mp3"
                                }
                            }
                            if (bestUrl != null) return Pair(bestUrl, formatExt)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Method 3: Invidious API
        val invidiousInstances = listOf(
            "https://inv.tux.pizza",
            "https://invidious.nerdvpn.de",
            "https://invidious.projectsegfau.lt"
        )
        for (inv in invidiousInstances) {
            try {
                val request = Request.Builder()
                    .url("$inv/api/v1/videos/$videoId")
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val str = resp.body?.string() ?: ""
                        val json = JSONObject(str)
                        val adaptive = json.optJSONArray("adaptiveFormats")
                        if (adaptive != null) {
                            for (i in 0 until adaptive.length()) {
                                val format = adaptive.getJSONObject(i)
                                val type = format.optString("type")
                                val u = format.optString("url")
                                if (type.startsWith("audio/") && u.isNotBlank()) {
                                    val ext = if (type.contains("mp4") || type.contains("m4a")) "m4a" else "mp3"
                                    return Pair(u, ext)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Method 4: Fallback public converter API
        try {
            val request = Request.Builder()
                .url("https://api.vevioz.com/api/button/mp3/$videoId")
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val html = resp.body?.string() ?: ""
                    val hrefMatch = Regex("""href="([^"]+)"[^>]*download""").find(html)
                        ?: Regex("""href="([^"]+\.mp3[^"]*)"""").find(html)
                    if (hrefMatch != null) {
                        return Pair(hrefMatch.groupValues[1], "mp3")
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private suspend fun executeYouTubeDownload(
        downloadId: Long,
        videoId: String,
        title: String,
        artist: String
    ) {
        val downloadDir = File(context.getExternalFilesDir(null), "Downloads").apply { mkdirs() }
        
        // Step 1: Resolve stream
        updateProgress(downloadId, 0.05f, 0L, 100L)
        val streamResult = resolveYouTubeAudioStream(videoId)
        if (streamResult == null) {
            updateStatus(
                downloadId,
                DownloadStatus.FAILED,
                errorMessage = "Não foi possível obter o fluxo de áudio do YouTube no momento. Tente novamente."
            )
            return
        }

        val (audioUrl, ext) = streamResult
        val targetFile = File(downloadDir, "YT_${videoId}_${System.currentTimeMillis()}.$ext")

        try {
            val request = Request.Builder()
                .url(audioUrl)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                updateStatus(downloadId, DownloadStatus.FAILED, errorMessage = "Erro HTTP no stream: ${response.code}")
                return
            }

            val body = response.body
            if (body == null) {
                updateStatus(downloadId, DownloadStatus.FAILED, errorMessage = "Fluxo de áudio vazio")
                return
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            val buffer = ByteArray(8192)

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    var bytesRead: Int
                    var lastUpdate = System.currentTimeMillis()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 300) {
                            lastUpdate = now
                            val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0.5f
                            updateProgress(downloadId, progress, downloadedBytes, totalBytes)
                        }
                    }
                }
            }

            // Extract duration if available
            var durationMs = 0L
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(targetFile.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                    durationMs = it
                }
            } catch (_: Exception) {} finally {
                try { retriever.release() } catch (_: Exception) {}
            }

            // Add downloaded song to MusicRepository
            musicRepository.addDownloadedSong(
                title = title,
                artist = artist,
                file = targetFile,
                durationMs = durationMs
            )

            val current = downloadDao.getDownloadById(downloadId)
            if (current != null) {
                downloadDao.updateDownload(
                    current.copy(
                        title = title,
                        artist = artist,
                        progress = 1.0f,
                        downloadedBytes = downloadedBytes,
                        totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes,
                        status = DownloadStatus.COMPLETED.name,
                        filePath = targetFile.absolutePath,
                        errorMessage = null
                    )
                )
            }
        } catch (e: CancellationException) {
            targetFile.delete()
            updateStatus(downloadId, DownloadStatus.CANCELLED, errorMessage = "Download cancelado pelo usuário")
        } catch (e: Exception) {
            targetFile.delete()
            updateStatus(downloadId, DownloadStatus.FAILED, errorMessage = e.localizedMessage ?: "Falha no download")
        } finally {
            activeJobs.remove(downloadId)
        }
    }

    private suspend fun executeDirectDownload(downloadId: Long, url: String, title: String, artist: String) {
        val downloadDir = File(context.getExternalFilesDir(null), "Downloads").apply { mkdirs() }
        val ext = url.substringBefore("?").substringAfterLast(".", "mp3")
        val safeExt = if (listOf("mp3", "m4a", "aac", "wav", "ogg", "flac").contains(ext.lowercase())) ext else "mp3"
        val targetFile = File(downloadDir, "Myato_${System.currentTimeMillis()}.$safeExt")

        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                updateStatus(downloadId, DownloadStatus.FAILED, errorMessage = "Erro HTTP: ${response.code}")
                return
            }

            val body = response.body
            if (body == null) {
                updateStatus(downloadId, DownloadStatus.FAILED, errorMessage = "Resposta vazia do servidor")
                return
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            val buffer = ByteArray(8192)

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    var bytesRead: Int
                    var lastUpdate = System.currentTimeMillis()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 350) {
                            lastUpdate = now
                            val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                            updateProgress(downloadId, progress, downloadedBytes, totalBytes)
                        }
                    }
                }
            }

            // Extract audio metadata if available
            var durationMs = 0L
            var finalTitle = title
            var finalArtist = artist
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(targetFile.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                    durationMs = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let {
                    finalTitle = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let {
                    finalArtist = it
                }
            } catch (_: Exception) {} finally {
                try { retriever.release() } catch (_: Exception) {}
            }

            // Add downloaded audio file to library
            musicRepository.addDownloadedSong(
                title = finalTitle,
                artist = finalArtist,
                file = targetFile,
                durationMs = durationMs
            )

            val current = downloadDao.getDownloadById(downloadId)
            if (current != null) {
                downloadDao.updateDownload(
                    current.copy(
                        title = finalTitle,
                        artist = finalArtist,
                        progress = 1.0f,
                        downloadedBytes = downloadedBytes,
                        totalBytes = if (totalBytes > 0) totalBytes else downloadedBytes,
                        status = DownloadStatus.COMPLETED.name,
                        filePath = targetFile.absolutePath,
                        errorMessage = null
                    )
                )
            }
        } catch (e: CancellationException) {
            targetFile.delete()
            updateStatus(downloadId, DownloadStatus.CANCELLED, errorMessage = "Download cancelado pelo usuário")
        } catch (e: Exception) {
            targetFile.delete()
            updateStatus(downloadId, DownloadStatus.FAILED, errorMessage = e.localizedMessage ?: "Falha no download")
        } finally {
            activeJobs.remove(downloadId)
        }
    }

    private suspend fun updateProgress(downloadId: Long, progress: Float, downloaded: Long, total: Long) {
        val current = downloadDao.getDownloadById(downloadId) ?: return
        downloadDao.updateDownload(
            current.copy(
                progress = progress,
                downloadedBytes = downloaded,
                totalBytes = total,
                status = DownloadStatus.DOWNLOADING.name
            )
        )
    }

    private suspend fun updateStatus(downloadId: Long, status: DownloadStatus, errorMessage: String? = null) {
        val current = downloadDao.getDownloadById(downloadId) ?: return
        downloadDao.updateDownload(
            current.copy(
                status = status.name,
                errorMessage = errorMessage
            )
        )
    }

    fun cancelDownload(downloadId: Long) {
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
    }

    suspend fun deleteDownload(downloadId: Long) = withContext(Dispatchers.IO) {
        cancelDownload(downloadId)
        val item = downloadDao.getDownloadById(downloadId)
        item?.filePath?.let { path ->
            try { File(path).delete() } catch (_: Exception) {}
        }
        downloadDao.deleteDownload(downloadId)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        downloadDao.clearAllDownloads()
    }
}

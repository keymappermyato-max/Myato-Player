package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.DownloadItem
import com.example.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val artist: String,
    val progress: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val status: String,
    val filePath: String?,
    val errorMessage: String?,
    val createdAt: Long
) {
    fun toDownloadItem(): DownloadItem = DownloadItem(
        id = id,
        url = url,
        title = title,
        artist = artist,
        progress = progress,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        status = try { DownloadStatus.valueOf(status) } catch (e: Exception) { DownloadStatus.FAILED },
        filePath = filePath,
        errorMessage = errorMessage,
        createdAt = createdAt
    )

    companion object {
        fun fromDownloadItem(item: DownloadItem): DownloadEntity = DownloadEntity(
            id = item.id,
            url = item.url,
            title = item.title,
            artist = item.artist,
            progress = item.progress,
            downloadedBytes = item.downloadedBytes,
            totalBytes = item.totalBytes,
            status = item.status.name,
            filePath = item.filePath,
            errorMessage = item.errorMessage,
            createdAt = item.createdAt
        )
    }
}

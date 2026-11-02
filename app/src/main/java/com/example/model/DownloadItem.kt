package com.example.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadItem(
    val id: Long = 0,
    val url: String,
    val title: String,
    val artist: String = "Download Autorizado",
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() {
            if (totalBytes <= 0) return "Desconhecido"
            val mb = totalBytes.toDouble() / (1024 * 1024)
            return "%.2f MB".format(mb)
        }
}

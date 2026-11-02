package com.example.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val coverUri: String? = null,
    val songCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

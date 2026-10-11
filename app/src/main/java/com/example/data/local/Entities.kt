package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sourceUriOrName: String,
    val importDate: Long = System.currentTimeMillis(),
    val channelCount: Int = 0,
    val isActive: Boolean = true
)

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["groupTitle"]),
        Index(value = ["isFavorite"]),
        Index(value = ["streamUrl"])
    ]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String = "General",
    val tvgId: String? = null,
    val tvgName: String? = null,
    val isFavorite: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "watch_history",
    indices = [
        Index(value = ["lastWatchedTimestamp"]),
        Index(value = ["streamUrl"], unique = true)
    ]
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelId: Long = 0,
    val channelName: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String = "General",
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
)

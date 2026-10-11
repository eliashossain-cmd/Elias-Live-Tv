package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.ChannelEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.ParseResult
import com.example.data.parser.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.io.InputStream

class TvRepository(private val database: AppDatabase) {

    private val playlistDao = database.playlistDao()
    private val channelDao = database.channelDao()
    private val watchHistoryDao = database.watchHistoryDao()

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    fun getActivePlaylist(): Flow<PlaylistEntity?> = playlistDao.getActivePlaylist()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getChannelsForActivePlaylist(): Flow<List<ChannelEntity>> {
        return playlistDao.getActivePlaylist().flatMapLatest { active ->
            if (active != null) {
                channelDao.getChannelsByPlaylist(active.id)
            } else {
                flowOf(emptyList())
            }
        }
    }

    fun getFavoriteChannels(): Flow<List<ChannelEntity>> = channelDao.getFavoriteChannels()

    fun getWatchHistory(limit: Int = 50): Flow<List<WatchHistoryEntity>> =
        watchHistoryDao.getRecentHistory(limit)

    suspend fun importPlaylistFromContent(
        title: String,
        sourceName: String,
        content: String,
        baseUrl: String? = null,
        replaceExisting: Boolean = false
    ): ParseResult = withContext(Dispatchers.IO) {
        val parseResult = M3uParser.parse(content, baseUrl)
        if (parseResult.validChannelsCount > 0) {
            persistImportedChannels(
                title = title,
                sourceName = sourceName,
                parseResult = parseResult,
                replaceExisting = replaceExisting
            )
        }
        parseResult
    }

    suspend fun importPlaylistFromStream(
        title: String,
        sourceName: String,
        inputStream: InputStream,
        baseUrl: String? = null,
        replaceExisting: Boolean = false
    ): ParseResult = withContext(Dispatchers.IO) {
        val parseResult = M3uParser.parse(inputStream, baseUrl)
        if (parseResult.validChannelsCount > 0) {
            persistImportedChannels(
                title = title,
                sourceName = sourceName,
                parseResult = parseResult,
                replaceExisting = replaceExisting
            )
        }
        parseResult
    }

    private suspend fun persistImportedChannels(
        title: String,
        sourceName: String,
        parseResult: ParseResult,
        replaceExisting: Boolean
    ) {
        // Find existing favorites to preserve them across re-import
        val existingFavUrls = try {
            val active = playlistDao.getActivePlaylistSync()
            if (active != null) {
                channelDao.getChannelsByPlaylistSync(active.id)
                    .filter { it.isFavorite }
                    .map { it.streamUrl }
                    .toSet()
            } else emptySet()
        } catch (_: Exception) {
            emptySet()
        }

        if (replaceExisting) {
            val active = playlistDao.getActivePlaylistSync()
            if (active != null) {
                channelDao.deleteChannelsByPlaylist(active.id)
                playlistDao.deletePlaylist(active.id)
            }
        }

        // Deactivate other playlists to set the newly imported one as active
        playlistDao.deactivateAllPlaylists()

        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(
                title = title.ifBlank { "Playlist ${System.currentTimeMillis()}" },
                sourceUriOrName = sourceName,
                importDate = System.currentTimeMillis(),
                channelCount = parseResult.validChannelsCount,
                isActive = true
            )
        )

        val entities = parseResult.channels.mapIndexed { index, parsed ->
            ChannelEntity(
                playlistId = playlistId,
                name = parsed.name,
                streamUrl = parsed.streamUrl,
                logoUrl = parsed.logoUrl,
                groupTitle = parsed.groupTitle,
                tvgId = parsed.tvgId,
                tvgName = parsed.tvgName,
                isFavorite = existingFavUrls.contains(parsed.streamUrl),
                orderIndex = index
            )
        }

        channelDao.insertChannels(entities)
    }

    suspend fun toggleFavorite(channelId: Long, currentFavStatus: Boolean, streamUrl: String) =
        withContext(Dispatchers.IO) {
            val newStatus = !currentFavStatus
            channelDao.setFavorite(channelId, newStatus)
            channelDao.setFavoriteByStreamUrl(streamUrl, newStatus)
        }

    suspend fun recordWatch(channel: ChannelEntity) = withContext(Dispatchers.IO) {
        watchHistoryDao.insertOrUpdate(
            WatchHistoryEntity(
                channelId = channel.id,
                channelName = channel.name,
                streamUrl = channel.streamUrl,
                logoUrl = channel.logoUrl,
                groupTitle = channel.groupTitle,
                lastWatchedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearWatchHistory() = withContext(Dispatchers.IO) {
        watchHistoryDao.clearHistory()
    }

    suspend fun switchActivePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deactivateAllPlaylists()
        playlistDao.activatePlaylist(playlistId)
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        channelDao.deleteChannelsByPlaylist(playlistId)
        playlistDao.deletePlaylist(playlistId)
        // If there are other playlists, activate the most recent one
        val remaining = playlistDao.getAllPlaylists()
        // will be observed automatically
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        channelDao.deleteAllChannels()
        playlistDao.deleteAllPlaylists()
        watchHistoryDao.clearHistory()
    }

    /**
     * Loads a curated, legal public demo playlist so the user can test playback immediately.
     */
    suspend fun loadDemoSamplePlaylist(): ParseResult {
        val sampleM3u = """
#EXTM3U x-tvg-url="https://iptv-org.github.io/epg/guides/us.xml"
#EXTINF:-1 tvg-id="NASA-TV" tvg-name="NASA TV Media" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg" group-title="Science & Space",NASA TV Public HD
https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8

#EXTINF:-1 tvg-id="NASA-ISS" tvg-name="NASA ISS Live" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg" group-title="Science & Space",NASA ISS Earth Views
https://ntv2.akamaized.net/hls/live/2014076/NASA-NTV2-HLS/master.m3u8

#EXTINF:-1 tvg-id="Bloomberg" tvg-name="Bloomberg Quicktake" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/5/53/Bloomberg_News_logo.svg" group-title="News",Bloomberg TV
https://bloomberg.com/media-manifest/streams/us.m3u8

#EXTINF:-1 tvg-id="DW-English" tvg-name="Deutsche Welle English" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/7/75/Deutsche_Welle_symbol_2012.svg" group-title="News",DW English Live
https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8

#EXTINF:-1 tvg-id="France24-EN" tvg-name="France 24 English" tvg-logo="https://upload.wikimedia.org/wikipedia/en/6/65/France_24_logo.svg" group-title="News",France 24 English
https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8

#EXTINF:-1 tvg-id="Euronews-EN" tvg-name="Euronews English" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/4/47/Euronews_2016_logo.svg" group-title="News",Euronews English
https://euronews-euronews-world-1-us.plex.tv/hls/main.m3u8

#EXTINF:-1 tvg-id="RedBullTV" tvg-name="Red Bull TV" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/d/d3/Red_Bull_logo.svg" group-title="Sports & Action",Red Bull TV Live
https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8

#EXTINF:-1 tvg-id="AlJazeera-EN" tvg-name="Al Jazeera English" tvg-logo="https://upload.wikimedia.org/wikipedia/en/f/f2/Al_Jazeera_English_logo.svg" group-title="News",Al Jazeera English
https://live-hls-web-aje.getaj.net/AJE/01.m3u8

#EXTINF:-1 tvg-id="Tastemade" tvg-name="Tastemade" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/7/76/Tastemade_logo.png" group-title="Lifestyle & Food",Tastemade TV
https://tastemadegptv-klowdtv.amagi.tv/playlist.m3u8
        """.trimIndent()

        return importPlaylistFromContent(
            title = "Demo Public Channels",
            sourceName = "Free Public Web Broadcasts",
            content = sampleM3u,
            replaceExisting = false
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: TvRepository? = null

        fun getInstance(context: Context): TvRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val repo = TvRepository(db)
                INSTANCE = repo
                repo
            }
        }
    }
}

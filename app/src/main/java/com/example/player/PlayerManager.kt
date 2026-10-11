package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.local.ChannelEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

data class PlayerState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentChannel: ChannelEntity? = null,
    val isLive: Boolean = true,
    val isPlaying: Boolean = false,
    val errorMessage: String? = null,
    val canRetry: Boolean = true,
    val resizeMode: Int = 0 // 0 = Fit, 3 = Zoom/Crop, 4 = Fill
)

@OptIn(UnstableApi::class)
class PlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var retryCount = 0
    private val maxRetries = 2

    fun getPlayer(): ExoPlayer {
        return exoPlayer ?: createPlayer().also { exoPlayer = it }
    }

    private fun createPlayer(): ExoPlayer {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setUserAgent("EliasLiveTV/1.0 (Linux; Android; ExoPlayer)")

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 3000,
                /* maxBufferMs = */ 15000,
                /* bufferForPlaybackMs = */ 1500,
                /* bufferForPlaybackAfterRebufferMs = */ 3000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isLiveStream = player.isCurrentMediaItemLive || player.duration == C.TIME_UNSET
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _playerState.value = _playerState.value.copy(
                            status = PlaybackStatus.BUFFERING,
                            isLive = isLiveStream,
                            errorMessage = null
                        )
                    }
                    Player.STATE_READY -> {
                        retryCount = 0
                        val isPlaying = player.isPlaying
                        _playerState.value = _playerState.value.copy(
                            status = if (isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED,
                            isPlaying = isPlaying,
                            isLive = isLiveStream,
                            errorMessage = null
                        )
                    }
                    Player.STATE_ENDED -> {
                        _playerState.value = _playerState.value.copy(
                            status = PlaybackStatus.ENDED,
                            isPlaying = false
                        )
                    }
                    Player.STATE_IDLE -> {
                        if (_playerState.value.status != PlaybackStatus.ERROR) {
                            _playerState.value = _playerState.value.copy(
                                status = PlaybackStatus.IDLE,
                                isPlaying = false
                            )
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.value = _playerState.value.copy(
                    isPlaying = isPlaying,
                    status = if (isPlaying) PlaybackStatus.PLAYING else {
                        if (_playerState.value.status == PlaybackStatus.PLAYING) PlaybackStatus.PAUSED
                        else _playerState.value.status
                    }
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                val detailedMessage = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Connection timed out. Check your internet connection or stream availability."
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                        "Stream server returned an error (${error.errorCodeName}). The broadcast might be offline or geographically restricted."
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                    PlaybackException.ERROR_CODE_DECODING_FAILED ->
                        "Video codec unsupported on this device."
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                        "Stream manifest or playlist is malformed or expired."
                    else ->
                        "Playback error: ${error.localizedMessage ?: "Stream unavailable or restricted."}"
                }

                _playerState.value = _playerState.value.copy(
                    status = PlaybackStatus.ERROR,
                    isPlaying = false,
                    errorMessage = detailedMessage,
                    canRetry = true
                )
            }
        })

        return player
    }

    fun playChannel(channel: ChannelEntity) {
        val player = getPlayer()
        retryCount = 0
        _playerState.value = _playerState.value.copy(
            currentChannel = channel,
            status = PlaybackStatus.BUFFERING,
            errorMessage = null,
            isPlaying = true
        )

        try {
            val uri = Uri.parse(channel.streamUrl.trim())
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMediaId(channel.id.toString())
                .build()

            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            _playerState.value = _playerState.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = "Failed to open stream URL: ${e.localizedMessage ?: "Invalid stream URL"}"
            )
        }
    }

    fun retry() {
        val channel = _playerState.value.currentChannel ?: return
        playChannel(channel)
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun cycleResizeMode() {
        // 0 = RESIZE_MODE_FIT, 3 = RESIZE_MODE_ZOOM, 4 = RESIZE_MODE_FILL
        val next = when (_playerState.value.resizeMode) {
            0 -> 3 // Zoom / Crop
            3 -> 4 // Stretch
            else -> 0 // Fit
        }
        _playerState.value = _playerState.value.copy(resizeMode = next)
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun stop() {
        exoPlayer?.stop()
        _playerState.value = _playerState.value.copy(
            status = PlaybackStatus.IDLE,
            isPlaying = false
        )
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
        _playerState.value = PlayerState()
    }
}

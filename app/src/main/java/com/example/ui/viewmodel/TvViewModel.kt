package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChannelEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.WatchHistoryEntity
import com.example.data.model.ParseResult
import com.example.data.repository.TvRepository
import com.example.player.PlayerManager
import com.example.player.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

enum class ChannelLayoutMode {
    GRID,
    LIST
}

enum class SortMode {
    PLAYLIST_ORDER,
    NAME_ASC,
    NAME_DESC
}

enum class AppScreen {
    HOME,
    CATEGORIES,
    FAVORITES,
    RECENT,
    PLAYLISTS,
    SETTINGS,
    PLAYER
}

data class ImportUiState(
    val isImporting: Boolean = false,
    val showResultDialog: Boolean = false,
    val result: ParseResult? = null,
    val playlistTitle: String = "",
    val error: String? = null
)

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TvRepository.getInstance(application)
    val playerManager = PlayerManager(application)

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Preferences (Stored via SharedPreferences)
    private val prefs = application.getSharedPreferences("elias_live_tv_prefs", Context.MODE_PRIVATE)

    private val _themePreference = MutableStateFlow(prefs.getString("theme", "dark") ?: "dark")
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    private val _layoutMode = MutableStateFlow(
        if (prefs.getString("layout", "grid") == "list") ChannelLayoutMode.LIST else ChannelLayoutMode.GRID
    )
    val layoutMode: StateFlow<ChannelLayoutMode> = _layoutMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow("All")
    val selectedGroup: StateFlow<String> = _selectedGroup.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.PLAYLIST_ORDER)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    // Import State
    private val _importState = MutableStateFlow(ImportUiState())
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    // Player State
    val playerState: StateFlow<PlayerState> = playerManager.playerState

    // Database flows
    val activePlaylist: StateFlow<PlaylistEntity?> = repository.getActivePlaylist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChannelsForActivePlaylist: StateFlow<List<ChannelEntity>> = repository.getChannelsForActivePlaylist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteChannels: StateFlow<List<ChannelEntity>> = repository.getFavoriteChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.getWatchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered & Sorted channels for display on Home / Browse
    val displayedChannels: StateFlow<List<ChannelEntity>> = combine(
        allChannelsForActivePlaylist,
        _searchQuery,
        _selectedGroup,
        _sortMode
    ) { channels, query, group, sort ->
        var list = channels

        // 1. Group filter
        if (group != "All") {
            list = list.filter { it.groupTitle.equals(group, ignoreCase = true) }
        }

        // 2. Search query filter
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.groupTitle.lowercase().contains(q) ||
                (it.tvgId?.lowercase()?.contains(q) == true)
            }
        }

        // 3. Sorting
        when (sort) {
            SortMode.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
            SortMode.PLAYLIST_ORDER -> list.sortedBy { it.orderIndex }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Distinct groups list with "All" prepended
    val availableGroups: StateFlow<List<String>> = combine(allChannelsForActivePlaylist) { channels ->
        val groups = channels[0].map { it.groupTitle }.distinct().sorted()
        listOf("All") + groups
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedGroup(group: String) {
        _selectedGroup.value = group
    }

    fun toggleLayoutMode() {
        val newMode = if (_layoutMode.value == ChannelLayoutMode.GRID) ChannelLayoutMode.LIST else ChannelLayoutMode.GRID
        _layoutMode.value = newMode
        prefs.edit().putString("layout", if (newMode == ChannelLayoutMode.LIST) "list" else "grid").apply()
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun setThemePreference(theme: String) {
        _themePreference.value = theme
        prefs.edit().putString("theme", theme).apply()
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(channel.id, channel.isFavorite, channel.streamUrl)
        }
    }

    fun playChannel(channel: ChannelEntity) {
        playerManager.playChannel(channel)
        viewModelScope.launch {
            repository.recordWatch(channel)
        }
        _currentScreen.value = AppScreen.PLAYER
    }

    fun playPreviousChannel() {
        val current = playerState.value.currentChannel ?: return
        val list = displayedChannels.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playChannel(list[currentIndex - 1])
        } else if (currentIndex == 0) {
            playChannel(list.last())
        }
    }

    fun playNextChannel() {
        val current = playerState.value.currentChannel ?: return
        val list = displayedChannels.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex in 0 until list.lastIndex) {
            playChannel(list[currentIndex + 1])
        } else if (currentIndex == list.lastIndex) {
            playChannel(list.first())
        }
    }

    fun importPlaylistFromUri(uri: Uri, title: String, replaceExisting: Boolean) {
        _importState.value = ImportUiState(isImporting = true)
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _importState.value = ImportUiState(
                        isImporting = false,
                        error = "Unable to read selected playlist file."
                    )
                    return@launch
                }

                val parseResult = repository.importPlaylistFromStream(
                    title = title.ifBlank { uri.lastPathSegment ?: "My Playlist" },
                    sourceName = uri.lastPathSegment ?: "Local File",
                    inputStream = inputStream,
                    replaceExisting = replaceExisting
                )

                _importState.value = ImportUiState(
                    isImporting = false,
                    showResultDialog = true,
                    result = parseResult,
                    playlistTitle = title
                )
            } catch (e: Exception) {
                _importState.value = ImportUiState(
                    isImporting = false,
                    error = "Failed to import playlist: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun importPlaylistFromText(content: String, title: String, replaceExisting: Boolean) {
        _importState.value = ImportUiState(isImporting = true)
        viewModelScope.launch {
            try {
                val parseResult = repository.importPlaylistFromContent(
                    title = title.ifBlank { "Imported Playlist" },
                    sourceName = "Direct Text",
                    content = content,
                    replaceExisting = replaceExisting
                )

                _importState.value = ImportUiState(
                    isImporting = false,
                    showResultDialog = true,
                    result = parseResult,
                    playlistTitle = title
                )
            } catch (e: Exception) {
                _importState.value = ImportUiState(
                    isImporting = false,
                    error = "Failed to parse playlist text: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun loadDemoChannels() {
        _importState.value = ImportUiState(isImporting = true)
        viewModelScope.launch {
            try {
                val parseResult = repository.loadDemoSamplePlaylist()
                _importState.value = ImportUiState(
                    isImporting = false,
                    showResultDialog = true,
                    result = parseResult,
                    playlistTitle = "Demo Public Channels"
                )
            } catch (e: Exception) {
                _importState.value = ImportUiState(
                    isImporting = false,
                    error = "Failed to load demo channels: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun dismissImportDialog() {
        _importState.value = ImportUiState()
    }

    fun switchActivePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.switchActivePlaylist(playlistId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            repository.clearWatchHistory()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            playerManager.stop()
            repository.clearAllData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ChannelEntity
import com.example.ui.components.CategoryChips
import com.example.ui.components.ChannelGridCard
import com.example.ui.components.ChannelListCard
import com.example.ui.components.DeveloperFooter
import com.example.ui.components.EmptyPlaylistState
import com.example.ui.components.ImportPlaylistDialog
import com.example.ui.components.SearchBarComponent
import com.example.ui.theme.CyanPrimary
import com.example.ui.viewmodel.ChannelLayoutMode
import com.example.ui.viewmodel.TvViewModel

@Composable
fun HomeScreen(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    val activePlaylist by viewModel.activePlaylist.collectAsStateWithLifecycle()
    val displayedChannels by viewModel.displayedChannels.collectAsStateWithLifecycle()
    val availableGroups by viewModel.availableGroups.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val layoutMode by viewModel.layoutMode.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        HomeTopBar(
            playlistName = activePlaylist?.title,
            channelCount = activePlaylist?.channelCount ?: 0,
            onImportClick = { showImportDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (activePlaylist == null || activePlaylist?.channelCount == 0) {
            // Empty state
            EmptyPlaylistState(
                onImportClick = { showImportDialog = true },
                onDemoClick = { viewModel.loadDemoChannels() },
                modifier = Modifier.weight(1f)
            )
        } else {
            // Search and sort row
            SearchBarComponent(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                layoutMode = layoutMode,
                onToggleLayout = { viewModel.toggleLayoutMode() },
                sortMode = sortMode,
                onSortModeChange = { viewModel.setSortMode(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Categories horizontal bar
            CategoryChips(
                categories = availableGroups,
                selectedCategory = selectedGroup,
                onCategorySelected = { viewModel.setSelectedGroup(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Channels List or Grid
            Box(modifier = Modifier.weight(1f)) {
                if (displayedChannels.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No channels matching \"$searchQuery\"" else "No channels in this category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (layoutMode == ChannelLayoutMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = displayedChannels,
                            key = { it.id }
                        ) { channel ->
                            ChannelGridCard(
                                channel = channel,
                                isPlaying = playerState.currentChannel?.id == channel.id,
                                onChannelClick = { viewModel.playChannel(it) },
                                onFavoriteClick = { viewModel.toggleFavorite(it) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = displayedChannels,
                            key = { it.id }
                        ) { channel ->
                            ChannelListCard(
                                channel = channel,
                                isPlaying = playerState.currentChannel?.id == channel.id,
                                onChannelClick = { viewModel.playChannel(it) },
                                onFavoriteClick = { viewModel.toggleFavorite(it) }
                            )
                        }
                    }
                }
            }
        }

        // Developer footer
        DeveloperFooter()
    }

    if (showImportDialog || importState.isImporting || importState.showResultDialog || importState.error != null) {
        ImportPlaylistDialog(
            importState = importState,
            onDismiss = {
                showImportDialog = false
                viewModel.dismissImportDialog()
            },
            onFileSelected = { uri, title, replace ->
                showImportDialog = false
                viewModel.importPlaylistFromUri(uri, title, replace)
            },
            onTextImport = { content, title, replace ->
                showImportDialog = false
                viewModel.importPlaylistFromText(content, title, replace)
            }
        )
    }
}

@Composable
fun HomeTopBar(
    playlistName: String?,
    channelCount: Int,
    onImportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CyanPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Elias Live TV",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (playlistName != null) {
                    Text(
                        text = "$playlistName • $channelCount Channels",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Button(
            onClick = onImportClick,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanPrimary,
                contentColor = Color(0xFF00363D)
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag("topbar_import_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Import",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.RecentScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EliasLiveTvTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TvViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TvViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming intent (Sharesheet / File open from file manager)
        handleIncomingIntent(intent)

        setContent {
            val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

            EliasLiveTvTheme(themePreference = themePreference) {
                if (currentScreen == AppScreen.PLAYER) {
                    PlayerScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                } else {
                    BackHandler(enabled = currentScreen != AppScreen.HOME) {
                        viewModel.navigateTo(AppScreen.HOME)
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                        bottomBar = {
                            EliasBottomNavigation(
                                currentScreen = currentScreen,
                                onSelectScreen = { viewModel.navigateTo(it) }
                            )
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "screen_transition"
                            ) { screen ->
                                when (screen) {
                                    AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                                    AppScreen.CATEGORIES -> CategoriesScreen(viewModel = viewModel)
                                    AppScreen.FAVORITES -> FavoritesScreen(viewModel = viewModel)
                                    AppScreen.RECENT -> RecentScreen(viewModel = viewModel)
                                    AppScreen.PLAYLISTS -> PlaylistsScreen(viewModel = viewModel)
                                    AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                                    AppScreen.PLAYER -> { /* Handled above */ }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val data: Uri? = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)

        if ((Intent.ACTION_VIEW == action || Intent.ACTION_SEND == action) && data != null) {
            val filename = data.lastPathSegment ?: "Imported Playlist"
            viewModel.importPlaylistFromUri(data, filename, replaceExisting = false)
        }
    }
}

data class NavItem(
    val screen: AppScreen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun EliasBottomNavigation(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit
) {
    val items = listOf(
        NavItem(AppScreen.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavItem(AppScreen.CATEGORIES, "Categories", Icons.Filled.Category, Icons.Outlined.Category),
        NavItem(AppScreen.FAVORITES, "Favorites", Icons.Filled.Star, Icons.Outlined.StarBorder),
        NavItem(AppScreen.RECENT, "Recent", Icons.Filled.History, Icons.Outlined.History),
        NavItem(AppScreen.PLAYLISTS, "Playlists", Icons.Filled.Folder, Icons.Outlined.Folder),
        NavItem(AppScreen.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        items.forEach { item ->
            val isSelected = currentScreen == item.screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectScreen(item.screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyanPrimary,
                    selectedTextColor = CyanPrimary,
                    indicatorColor = CyanPrimary.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_${item.title.lowercase()}")
            )
        }
    }
}

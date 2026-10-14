package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.screens.CleanScreen
import com.example.ui.screens.GameBoosterScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MonitorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShizukuHubScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.HyperPulseTheme
import com.example.ui.viewmodel.CleanViewModel
import com.example.ui.viewmodel.GameBoosterViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.MonitorViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.ShizukuViewModel
import com.example.ui.viewmodel.ToolsViewModel

enum class MainDestination {
    HOME,
    CLEAN,
    MONITOR,
    GAME
}

enum class SubDestination {
    TOOLS,
    SETTINGS,
    SHIZUKU
}

data class NavigationTab(
    val destination: MainDestination,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private val homeViewModel by viewModels<HomeViewModel>()
    private val cleanViewModel by viewModels<CleanViewModel>()
    private val monitorViewModel by viewModels<MonitorViewModel>()
    private val gameViewModel by viewModels<GameBoosterViewModel>()
    private val toolsViewModel by viewModels<ToolsViewModel>()
    private val shizukuViewModel by viewModels<ShizukuViewModel>()
    private val settingsViewModel by viewModels<SettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDark by settingsViewModel.isDarkTheme.collectAsState()
            val isAmoled by settingsViewModel.isAmoled.collectAsState()

            HyperPulseTheme(darkTheme = isDark, isAmoled = isAmoled) {
                var currentMainTab by remember { mutableStateOf(MainDestination.HOME) }
                var currentSubScreen by remember { mutableStateOf<SubDestination?>(null) }

                BackHandler(enabled = currentSubScreen != null || currentMainTab != MainDestination.HOME) {
                    if (currentSubScreen != null) {
                        currentSubScreen = null
                    } else {
                        currentMainTab = MainDestination.HOME
                    }
                }

                // Exactly 4 items in NavigationBar
                val tabs = listOf(
                    NavigationTab(MainDestination.HOME, "Home", Icons.Outlined.Home, "nav_home"),
                    NavigationTab(MainDestination.CLEAN, "Clean", Icons.Outlined.CleaningServices, "nav_clean"),
                    NavigationTab(MainDestination.MONITOR, "Monitor", Icons.Outlined.MonitorHeart, "nav_monitor"),
                    NavigationTab(MainDestination.GAME, "Game", Icons.Outlined.SportsEsports, "nav_game")
                )

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWideScreen = maxWidth >= 600.dp

                    if (isWideScreen && currentSubScreen == null) {
                        // Tablet / Landscape: NavigationRail
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                tabs.forEach { tab ->
                                    val isSelected = currentMainTab == tab.destination
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = { currentMainTab = tab.destination },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title
                                            )
                                        },
                                        label = { Text(tab.title) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }

                            AppContentPane(
                                currentMainTab = currentMainTab,
                                currentSubScreen = currentSubScreen,
                                homeViewModel = homeViewModel,
                                cleanViewModel = cleanViewModel,
                                monitorViewModel = monitorViewModel,
                                gameViewModel = gameViewModel,
                                toolsViewModel = toolsViewModel,
                                shizukuViewModel = shizukuViewModel,
                                settingsViewModel = settingsViewModel,
                                onSelectTab = { currentMainTab = it },
                                onOpenSub = { currentSubScreen = it },
                                onCloseSub = { currentSubScreen = null },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Phone: Bottom NavigationBar
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                if (currentSubScreen == null) {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ) {
                                        tabs.forEach { tab ->
                                            val isSelected = currentMainTab == tab.destination
                                            NavigationBarItem(
                                                selected = isSelected,
                                                onClick = { currentMainTab = tab.destination },
                                                icon = {
                                                    Icon(
                                                        imageVector = tab.icon,
                                                        contentDescription = tab.title
                                                    )
                                                },
                                                label = { Text(tab.title) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                                ),
                                                modifier = Modifier.testTag(tab.testTag)
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            AppContentPane(
                                currentMainTab = currentMainTab,
                                currentSubScreen = currentSubScreen,
                                homeViewModel = homeViewModel,
                                cleanViewModel = cleanViewModel,
                                monitorViewModel = monitorViewModel,
                                gameViewModel = gameViewModel,
                                toolsViewModel = toolsViewModel,
                                shizukuViewModel = shizukuViewModel,
                                settingsViewModel = settingsViewModel,
                                onSelectTab = { currentMainTab = it },
                                onOpenSub = { currentSubScreen = it },
                                onCloseSub = { currentSubScreen = null },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppContentPane(
    currentMainTab: MainDestination,
    currentSubScreen: SubDestination?,
    homeViewModel: HomeViewModel,
    cleanViewModel: CleanViewModel,
    monitorViewModel: MonitorViewModel,
    gameViewModel: GameBoosterViewModel,
    toolsViewModel: ToolsViewModel,
    shizukuViewModel: ShizukuViewModel,
    settingsViewModel: SettingsViewModel,
    onSelectTab: (MainDestination) -> Unit,
    onOpenSub: (SubDestination) -> Unit,
    onCloseSub: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentSubScreen != null) {
        when (currentSubScreen) {
            SubDestination.TOOLS -> ToolsScreen(
                viewModel = toolsViewModel,
                onBack = onCloseSub,
                modifier = modifier
            )
            SubDestination.SETTINGS -> SettingsScreen(
                viewModel = settingsViewModel,
                onBack = onCloseSub,
                modifier = modifier
            )
            SubDestination.SHIZUKU -> ShizukuHubScreen(
                viewModel = shizukuViewModel,
                onBack = onCloseSub,
                modifier = modifier
            )
        }
    } else {
        when (currentMainTab) {
            MainDestination.HOME -> HomeScreen(
                viewModel = homeViewModel,
                onNavigateToClean = { onSelectTab(MainDestination.CLEAN) },
                onNavigateToGame = { onSelectTab(MainDestination.GAME) },
                onNavigateToMonitor = { onSelectTab(MainDestination.MONITOR) },
                onNavigateToTools = { onOpenSub(SubDestination.TOOLS) },
                onNavigateToSettings = { onOpenSub(SubDestination.SETTINGS) },
                modifier = modifier
            )
            MainDestination.CLEAN -> CleanScreen(
                viewModel = cleanViewModel,
                modifier = modifier
            )
            MainDestination.MONITOR -> MonitorScreen(
                viewModel = monitorViewModel,
                modifier = modifier
            )
            MainDestination.GAME -> GameBoosterScreen(
                viewModel = gameViewModel,
                modifier = modifier
            )
        }
    }
}

package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.GameProfileEntity
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SettingRow
import com.example.ui.theme.HyperPulseTheme
import com.example.ui.theme.Spacing
import com.example.ui.viewmodel.GameBoosterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameBoosterScreen(
    viewModel: GameBoosterViewModel,
    modifier: Modifier = Modifier
) {
    val games by viewModel.gameProfiles.collectAsState()
    val selectedGame by viewModel.selectedGame.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Game Booster", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add Game",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        GameBoosterContent(
            games = games,
            selectedGame = selectedGame,
            onSelectGame = { viewModel.selectGame(it) },
            onSelectMode = { viewModel.setMode(it) },
            onStartGame = { selectedGame?.let { viewModel.launchWithBooster(it) } },
            onOpenCapabilities = { showBottomSheet = true },
            modifier = Modifier.padding(innerPadding)
        )

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenPadding, vertical = Spacing.md)
                ) {
                    Text(
                        text = "Hardware Capabilities & Status",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Real system telemetry and governor control",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    SettingRow(title = "Shizuku Service", subtitle = "Connected in ADB shell mode (UID 2000)")
                    SettingRow(title = "In-game frame rate", subtitle = "Direct dumpsys SurfaceFlinger telemetry")
                    SettingRow(title = "Thermal auto-downgrade", subtitle = "Reverts to Turbo if device exceeds 45 °C")
                    SettingRow(title = "Wi-Fi performance lock", subtitle = "Acquires high-performance lock during sessions")
                    Spacer(modifier = Modifier.height(Spacing.xl))
                }
            }
        }

        if (showAddDialog) {
            var pkgName by remember { mutableStateOf("") }
            var appName by remember { mutableStateOf("") }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Game") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = appName,
                            onValueChange = { appName = it },
                            label = { Text("Game title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = pkgName,
                            onValueChange = { pkgName = it },
                            label = { Text("Package (e.g. com.example.game)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (pkgName.isNotBlank() && appName.isNotBlank()) {
                                viewModel.addCustomGame(pkgName.trim(), appName.trim())
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameBoosterContent(
    games: List<GameProfileEntity>,
    selectedGame: GameProfileEntity?,
    onSelectGame: (GameProfileEntity) -> Unit,
    onSelectMode: (String) -> Unit,
    onStartGame: () -> Unit,
    onOpenCapabilities: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf("Balanced", "Performance", "Turbo", "Turbo++")
    val currentMode = selectedGame?.mode ?: "Turbo"
    val modeIndex = modes.indexOfFirst { it.equals(currentMode, ignoreCase = true) }.coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quiet Capability & Shizuku Status Chip
        Row(
            modifier = Modifier
                .clickable { onOpenCapabilities() }
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Shizuku Shell • Tap for capabilities",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // 1. Mode selector as segmented buttons (Balanced / Performance / Turbo / Turbo++)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            modes.forEachIndexed { index, modeName ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    onClick = { onSelectMode(modeName) },
                    selected = index == modeIndex
                ) {
                    Text(
                        text = modeName,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

        // 2. Game grid as simple icon tiles with names
        SectionHeader(
            title = "Games",
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(games) { game ->
                val isSelected = selectedGame?.packageName == game.packageName
                MinimalGameTile(
                    game = game,
                    isSelected = isSelected,
                    onClick = { onSelectGame(game) }
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // 3. One Start button
        PrimaryActionButton(
            text = "Start",
            onClick = onStartGame,
            enabled = selectedGame != null,
            modifier = Modifier
                .padding(bottom = Spacing.md)
                .testTag("game_primary_start")
        )
    }
}

@Composable
fun MinimalGameTile(
    game: GameProfileEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.SportsEsports,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = game.appName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun GameBoosterPreviewLight() {
    HyperPulseTheme(darkTheme = false) {
        GameBoosterContent(
            games = listOf(
                GameProfileEntity("pkg1", "Cyber Arena", "Turbo"),
                GameProfileEntity("pkg2", "Racer Velocity", "Performance"),
                GameProfileEntity("pkg3", "Shadow Quest", "Balanced")
            ),
            selectedGame = GameProfileEntity("pkg1", "Cyber Arena", "Turbo"),
            onSelectGame = {},
            onSelectMode = {},
            onStartGame = {},
            onOpenCapabilities = {}
        )
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun GameBoosterPreviewDark() {
    HyperPulseTheme(darkTheme = true) {
        GameBoosterContent(
            games = listOf(
                GameProfileEntity("pkg1", "Cyber Arena", "Turbo"),
                GameProfileEntity("pkg2", "Racer Velocity", "Performance")
            ),
            selectedGame = GameProfileEntity("pkg1", "Cyber Arena", "Turbo"),
            onSelectGame = {},
            onSelectMode = {},
            onStartGame = {},
            onOpenCapabilities = {}
        )
    }
}

@Preview(name = "AMOLED Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun GameBoosterPreviewAmoled() {
    HyperPulseTheme(darkTheme = true, isAmoled = true) {
        GameBoosterContent(
            games = listOf(
                GameProfileEntity("pkg1", "Cyber Arena", "Turbo")
            ),
            selectedGame = GameProfileEntity("pkg1", "Cyber Arena", "Turbo"),
            onSelectGame = {},
            onSelectMode = {},
            onStartGame = {},
            onOpenCapabilities = {}
        )
    }
}

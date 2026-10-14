package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemetry.HealthCategory
import com.example.telemetry.SystemTelemetry
import com.example.ui.components.MetricRing
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatChip
import com.example.ui.theme.HyperPulseTheme
import com.example.ui.theme.Spacing
import com.example.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToClean: () -> Unit,
    onNavigateToGame: () -> Unit,
    onNavigateToMonitor: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val isBoosting by viewModel.isBoosting.collectAsState()
    val lastBoostResult by viewModel.lastBoostResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(lastBoostResult) {
        lastBoostResult?.let { res ->
            snackbarHostState.showSnackbar("Freed ${res.ramFreedMb} MB RAM")
            viewModel.dismissBoostResult()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HyperPulse",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        HomeScreenContent(
            telemetry = telemetry,
            isBoosting = isBoosting,
            onBoost = { viewModel.triggerOneTapBoost() },
            onNavigateToClean = onNavigateToClean,
            onNavigateToGame = onNavigateToGame,
            onNavigateToMonitor = onNavigateToMonitor,
            onNavigateToTools = onNavigateToTools,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun HomeScreenContent(
    telemetry: SystemTelemetry,
    isBoosting: Boolean,
    onBoost: () -> Unit,
    onNavigateToClean: () -> Unit,
    onNavigateToGame: () -> Unit,
    onNavigateToMonitor: () -> Unit,
    onNavigateToTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.md))

        // 1. One large circular health ring
        MetricRing(
            score = telemetry.healthScore,
            category = telemetry.healthCategory,
            size = 200.dp
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // 2. One Boost button beneath it
        PrimaryActionButton(
            text = if (isBoosting) "Optimizing…" else "Boost",
            onClick = onBoost,
            isLoading = isBoosting,
            modifier = Modifier.testTag("home_primary_boost")
        )

        Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

        // 3. Three small stat chips (RAM, Storage, Temp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            StatChip(
                label = "RAM",
                value = "${(telemetry.ramUsedMb / 1024f).let { "%.1f".format(it) }} GB",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToMonitor
            )
            StatChip(
                label = "Storage",
                value = "${"%.0f".format(telemetry.storageFreeGb)} GB free",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToClean
            )
            StatChip(
                label = "Temp",
                value = "${telemetry.batteryTempC.toInt()} °C",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToTools
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

        // 4. Short list of 4 quick tools
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            SectionHeader(title = "Tools")

            MinimalToolRow(
                title = "Junk cleaner",
                subtitle = "Reclaim temporary cache and logs",
                icon = Icons.Outlined.CleaningServices,
                onClick = onNavigateToClean
            )
            MinimalToolRow(
                title = "Game booster",
                subtitle = "Performance profiles and in-game HUD",
                icon = Icons.Outlined.SportsEsports,
                onClick = onNavigateToGame
            )
            MinimalToolRow(
                title = "Hardware monitor",
                subtitle = "CPU clock frequencies, battery and network",
                icon = Icons.Outlined.MonitorHeart,
                onClick = onNavigateToMonitor
            )
            MinimalToolRow(
                title = "All utilities",
                subtitle = "Cooler, benchmark, battery optimizer",
                icon = Icons.Outlined.AcUnit,
                onClick = onNavigateToTools
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}

@Composable
fun MinimalToolRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun HomeScreenPreviewLight() {
    HyperPulseTheme(darkTheme = false) {
        HomeScreenContent(
            telemetry = SystemTelemetry(
                healthScore = 92,
                healthCategory = HealthCategory.OPTIMAL,
                ramUsedMb = 3400,
                storageFreeGb = 64f,
                batteryTempC = 31f
            ),
            isBoosting = false,
            onBoost = {},
            onNavigateToClean = {},
            onNavigateToGame = {},
            onNavigateToMonitor = {},
            onNavigateToTools = {}
        )
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun HomeScreenPreviewDark() {
    HyperPulseTheme(darkTheme = true) {
        HomeScreenContent(
            telemetry = SystemTelemetry(
                healthScore = 78,
                healthCategory = HealthCategory.GOOD,
                ramUsedMb = 4800,
                storageFreeGb = 32f,
                batteryTempC = 36f
            ),
            isBoosting = false,
            onBoost = {},
            onNavigateToClean = {},
            onNavigateToGame = {},
            onNavigateToMonitor = {},
            onNavigateToTools = {}
        )
    }
}

@Preview(name = "AMOLED Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun HomeScreenPreviewAmoled() {
    HyperPulseTheme(darkTheme = true, isAmoled = true) {
        HomeScreenContent(
            telemetry = SystemTelemetry(
                healthScore = 88,
                healthCategory = HealthCategory.OPTIMAL,
                ramUsedMb = 3100,
                storageFreeGb = 82f,
                batteryTempC = 29f
            ),
            isBoosting = false,
            onBoost = {},
            onNavigateToClean = {},
            onNavigateToGame = {},
            onNavigateToMonitor = {},
            onNavigateToTools = {}
        )
    }
}

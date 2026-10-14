package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.components.SectionHeader
import com.example.ui.components.SettingRow
import com.example.ui.theme.HyperPulseTheme
import com.example.ui.theme.Spacing
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isAmoled by viewModel.isAmoled.collectAsState()
    val persistentNotification by viewModel.persistentNotification.collectAsState()
    val autoBoost by viewModel.autoBoost.collectAsState()
    val autoBoostThreshold by viewModel.autoBoostThreshold.collectAsState()
    val thermalAlertThreshold by viewModel.thermalAlertThreshold.collectAsState()

    val routineCleanEnabled by viewModel.routineCleanEnabled.collectAsState()
    val routineCleanIdleOnly by viewModel.routineCleanIdleOnly.collectAsState()
    val routineCleanChargingOnly by viewModel.routineCleanChargingOnly.collectAsState()
    val routineCleanIntervalDays by viewModel.routineCleanIntervalDays.collectAsState()
    val lastRoutineCleanSummary by viewModel.lastRoutineCleanSummary.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        SettingsScreenContent(
            isDarkTheme = isDarkTheme,
            isAmoled = isAmoled,
            persistentNotification = persistentNotification,
            autoBoost = autoBoost,
            autoBoostThreshold = autoBoostThreshold,
            thermalAlertThreshold = thermalAlertThreshold,
            routineCleanEnabled = routineCleanEnabled,
            routineCleanIdleOnly = routineCleanIdleOnly,
            routineCleanChargingOnly = routineCleanChargingOnly,
            routineCleanIntervalDays = routineCleanIntervalDays,
            lastRoutineCleanSummary = lastRoutineCleanSummary,
            onToggleDarkTheme = { viewModel.toggleDarkTheme(it) },
            onToggleAmoled = { viewModel.toggleAmoled(it) },
            onTogglePersistentNotification = { viewModel.togglePersistentNotification(it) },
            onToggleAutoBoost = { viewModel.toggleAutoBoost(it) },
            onSetAutoBoostThreshold = { viewModel.setAutoBoostThreshold(it) },
            onSetThermalAlertThreshold = { viewModel.setThermalAlertThreshold(it) },
            onToggleRoutineClean = { viewModel.toggleRoutineClean(it) },
            onToggleRoutineCleanIdle = { viewModel.toggleRoutineCleanIdle(it) },
            onToggleRoutineCleanCharging = { viewModel.toggleRoutineCleanCharging(it) },
            onSetRoutineCleanIntervalDays = { viewModel.setRoutineCleanIntervalDays(it) },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    isDarkTheme: Boolean,
    isAmoled: Boolean,
    persistentNotification: Boolean,
    autoBoost: Boolean,
    autoBoostThreshold: Int,
    thermalAlertThreshold: Int,
    routineCleanEnabled: Boolean,
    routineCleanIdleOnly: Boolean,
    routineCleanChargingOnly: Boolean,
    routineCleanIntervalDays: Int,
    lastRoutineCleanSummary: String,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleAmoled: (Boolean) -> Unit,
    onTogglePersistentNotification: (Boolean) -> Unit,
    onToggleAutoBoost: (Boolean) -> Unit,
    onSetAutoBoostThreshold: (Int) -> Unit,
    onSetThermalAlertThreshold: (Int) -> Unit,
    onToggleRoutineClean: (Boolean) -> Unit,
    onToggleRoutineCleanIdle: (Boolean) -> Unit,
    onToggleRoutineCleanCharging: (Boolean) -> Unit,
    onSetRoutineCleanIntervalDays: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Spacer(modifier = Modifier.height(Spacing.xs))

        // Appearance
        SectionHeader(title = "Appearance")
        SettingRow(
            title = "Dark theme",
            subtitle = "Neutral dark surfaces",
            trailing = {
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onToggleDarkTheme
                )
            }
        )
        SettingRow(
            title = "AMOLED true black",
            subtitle = "Zero-pixel black background for OLED screens",
            trailing = {
                Switch(
                    checked = isAmoled,
                    onCheckedChange = onToggleAmoled
                )
            }
        )

        Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

        // Telemetry & Notifications
        SectionHeader(title = "Monitoring")
        SettingRow(
            title = "Status bar telemetry",
            subtitle = "Persistent notification with live CPU and RAM",
            trailing = {
                Switch(
                    checked = persistentNotification,
                    onCheckedChange = onTogglePersistentNotification
                )
            }
        )

        Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

        // Routine Low-Usage Clean
        SectionHeader(title = "Scheduled Maintenance")
        SettingRow(
            title = "Automatic low-usage clean",
            subtitle = "Runs cache clearing and RAM optimization during quiet hours",
            trailing = {
                Switch(
                    checked = routineCleanEnabled,
                    onCheckedChange = onToggleRoutineClean
                )
            }
        )

        if (routineCleanEnabled) {
            SettingRow(
                title = "Device idle only",
                subtitle = "Only cleans when screen is off and device is undisturbed",
                trailing = {
                    Switch(
                        checked = routineCleanIdleOnly,
                        onCheckedChange = onToggleRoutineCleanIdle
                    )
                }
            )

            SettingRow(
                title = "Charging only",
                subtitle = "Preserves battery by requiring power connection",
                trailing = {
                    Switch(
                        checked = routineCleanChargingOnly,
                        onCheckedChange = onToggleRoutineCleanCharging
                    )
                }
            )

            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val intervals = listOf(1 to "Daily", 3 to "Every 3 days", 7 to "Weekly")
                val selectedIndex = intervals.indexOfFirst { it.first == routineCleanIntervalDays }.coerceAtLeast(0)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    intervals.forEachIndexed { index, (days, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = intervals.size),
                            onClick = { onSetRoutineCleanIntervalDays(days) },
                            selected = index == selectedIndex
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            SettingRow(
                title = "Last completed clean",
                subtitle = lastRoutineCleanSummary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

        // Real-time Automation
        SectionHeader(title = "Load Triggers")
        SettingRow(
            title = "Auto-boost on high memory",
            subtitle = "Frees background cache when RAM exceeds threshold",
            trailing = {
                Switch(
                    checked = autoBoost,
                    onCheckedChange = onToggleAutoBoost
                )
            }
        )

        if (autoBoost) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "RAM threshold: $autoBoostThreshold%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = autoBoostThreshold.toFloat(),
                    onValueChange = { onSetAutoBoostThreshold(it.toInt()) },
                    valueRange = 70f..95f
                )
            }
        }

        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Thermal alert threshold: $thermalAlertThreshold °C",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = thermalAlertThreshold.toFloat(),
                onValueChange = { onSetThermalAlertThreshold(it.toInt()) },
                valueRange = 38f..52f
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

        // Policy & Transparency
        SectionHeader(title = "About & Compliance")
        Text(
            text = "HyperPulse measures actual kernel telemetry without synthetic or fabricated numbers. Background optimization strictly adheres to Android ActivityManager standards.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun SettingsScreenPreviewLight() {
    HyperPulseTheme(darkTheme = false) {
        SettingsScreenContent(
            isDarkTheme = false,
            isAmoled = false,
            persistentNotification = true,
            autoBoost = true,
            autoBoostThreshold = 85,
            thermalAlertThreshold = 44,
            routineCleanEnabled = true,
            routineCleanIdleOnly = true,
            routineCleanChargingOnly = true,
            routineCleanIntervalDays = 1,
            lastRoutineCleanSummary = "Cleaned 142 MB RAM, 48 MB storage on Oct 14, 3:15 AM",
            onToggleDarkTheme = {},
            onToggleAmoled = {},
            onTogglePersistentNotification = {},
            onToggleAutoBoost = {},
            onSetAutoBoostThreshold = {},
            onSetThermalAlertThreshold = {},
            onToggleRoutineClean = {},
            onToggleRoutineCleanIdle = {},
            onToggleRoutineCleanCharging = {},
            onSetRoutineCleanIntervalDays = {}
        )
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun SettingsScreenPreviewDark() {
    HyperPulseTheme(darkTheme = true) {
        SettingsScreenContent(
            isDarkTheme = true,
            isAmoled = false,
            persistentNotification = false,
            autoBoost = true,
            autoBoostThreshold = 80,
            thermalAlertThreshold = 42,
            routineCleanEnabled = true,
            routineCleanIdleOnly = true,
            routineCleanChargingOnly = true,
            routineCleanIntervalDays = 1,
            lastRoutineCleanSummary = "Cleaned 110 MB RAM, 32 MB storage on Oct 14, 4:00 AM",
            onToggleDarkTheme = {},
            onToggleAmoled = {},
            onTogglePersistentNotification = {},
            onToggleAutoBoost = {},
            onSetAutoBoostThreshold = {},
            onSetThermalAlertThreshold = {},
            onToggleRoutineClean = {},
            onToggleRoutineCleanIdle = {},
            onToggleRoutineCleanCharging = {},
            onSetRoutineCleanIntervalDays = {}
        )
    }
}

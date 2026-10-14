package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SettingRow
import com.example.ui.theme.HyperPulseTheme
import com.example.ui.theme.Spacing
import com.example.ui.viewmodel.ToolsViewModel

data class ToolDefinition(
    val title: String,
    val description: String,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val runningApps by viewModel.runningApps.collectAsState()
    val isCooling by viewModel.isCooling.collectAsState()
    val benchmarkResult by viewModel.benchmarkResult.collectAsState()
    val isBenchmarking by viewModel.isBenchmarking.collectAsState()

    var activeBottomSheetTool by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Tools", style = MaterialTheme.typography.titleLarge) },
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
        ToolsScreenContent(
            onOpenTool = { activeBottomSheetTool = it },
            modifier = Modifier.padding(innerPadding)
        )

        activeBottomSheetTool?.let { toolName ->
            ModalBottomSheet(
                onDismissRequest = { activeBottomSheetTool = null },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenPadding, vertical = Spacing.md)
                ) {
                    Text(
                        text = toolName,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    when (toolName) {
                        "RAM Booster" -> {
                            Text(
                                text = "Running background processes (${runningApps.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            runningApps.take(6).forEach { appItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(appItem.appName, style = MaterialTheme.typography.bodyLarge)
                                        Text("${appItem.memoryMb} MB", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = { viewModel.stopApp(appItem) }) {
                                        Text("Stop")
                                    }
                                }
                            }
                        }

                        "Device Cooler" -> {
                            Text(
                                text = "Current thermal reading: ${telemetry.batteryTempC.toInt()} °C",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "Mitigates background tasks and CPU frequency spikes to lower internal thermal load.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            PrimaryActionButton(
                                text = if (isCooling) "Cooling…" else "Cool down",
                                onClick = { viewModel.triggerCooling() },
                                isLoading = isCooling
                            )
                        }

                        "Battery Optimizer" -> {
                            Text(
                                text = "Select power profile",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            SettingRow(
                                title = "Normal",
                                subtitle = "Standard Android power curve",
                                onClick = { viewModel.setBatteryProfile("Normal") }
                            )
                            SettingRow(
                                title = "Power saver",
                                subtitle = "Limits background refresh and animations",
                                onClick = { viewModel.setBatteryProfile("Power Saver") }
                            )
                            SettingRow(
                                title = "Ultra endurance",
                                subtitle = "Strict sleep mode for maximum battery life",
                                onClick = { viewModel.setBatteryProfile("Ultra") }
                            )
                        }

                        "System Benchmark" -> {
                            if (benchmarkResult != null) {
                                val res = benchmarkResult!!
                                Text(
                                    text = "Score: ${res.totalScore}",
                                    style = MaterialTheme.typography.displayMedium
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                MinimalStatRow(label = "CPU compute", value = "${res.cpuScore}")
                                MinimalStatRow(label = "RAM copy", value = "${res.ramScore}")
                                MinimalStatRow(label = "Storage I/O", value = "${res.storageScore}")
                            } else {
                                Text(
                                    text = "Measures mathematical throughput, memory bandwidth, and file cache I/O.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(Spacing.lg))
                            PrimaryActionButton(
                                text = if (isBenchmarking) "Running test…" else "Run benchmark",
                                onClick = { viewModel.runBenchmark() },
                                isLoading = isBenchmarking
                            )
                        }

                        "Privacy Advisor" -> {
                            Text(
                                text = "Audit of apps accessing Camera, Microphone, and Location",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            SettingRow(title = "Camera access", subtitle = "Audited permissions across installed packages")
                            SettingRow(title = "Microphone access", subtitle = "Background audio recording permissions")
                            SettingRow(title = "Location access", subtitle = "Fine and coarse GPS telemetry access")
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.xl))
                }
            }
        }
    }
}

@Composable
fun ToolsScreenContent(
    onOpenTool: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tools = listOf(
        ToolDefinition("RAM Booster", "Inspect and stop background memory usage", "Optimization"),
        ToolDefinition("Device Cooler", "Thermal sensors and thermal throttling", "Optimization"),
        ToolDefinition("Battery Optimizer", "Profiles for endurance and charge limits", "Optimization"),
        ToolDefinition("System Benchmark", "Compute and memory throughput scoring", "Diagnostics"),
        ToolDefinition("Privacy Advisor", "Inspect sensitive device permissions", "Security")
    )

    val grouped = tools.groupBy { it.category }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        grouped.forEach { (category, items) ->
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                SectionHeader(title = category)
            }

            items(items) { tool ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTool(tool.title) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tool.description,
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
        }

        item {
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun ToolsScreenPreviewLight() {
    HyperPulseTheme(darkTheme = false) {
        ToolsScreenContent(onOpenTool = {})
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun ToolsScreenPreviewDark() {
    HyperPulseTheme(darkTheme = true) {
        ToolsScreenContent(onOpenTool = {})
    }
}

@Preview(name = "AMOLED Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun ToolsScreenPreviewAmoled() {
    HyperPulseTheme(darkTheme = true, isAmoled = true) {
        ToolsScreenContent(onOpenTool = {})
    }
}

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemetry.DeviceSpecs
import com.example.telemetry.SystemTelemetry
import com.example.ui.components.MinimalLineChart
import com.example.ui.components.SectionHeader
import com.example.ui.theme.HyperPulseTheme
import com.example.ui.theme.Spacing
import com.example.ui.viewmodel.MonitorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val deviceSpecs by viewModel.deviceSpecs.collectAsState()
    val pingMs by viewModel.pingMs.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Monitor", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        MonitorScreenContent(
            telemetry = telemetry,
            deviceSpecs = deviceSpecs,
            pingMs = pingMs,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreenContent(
    telemetry: SystemTelemetry,
    deviceSpecs: DeviceSpecs?,
    pingMs: Int,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("CPU", "RAM", "Battery", "Network")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.xs))

        // Segmented tabs (CPU / RAM / Battery / Network)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                    onClick = { selectedIndex = index },
                    selected = index == selectedIndex
                ) {
                    Text(text = label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

        when (selectedIndex) {
            0 -> {
                // CPU Tab
                MetricHeroSection(
                    value = "${telemetry.cpuPercent.toInt()}",
                    unit = "%",
                    label = "CPU load"
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Clean line graph (2dp, no grid, no fill)
                MinimalLineChart(
                    points = listOf(14f, 18f, 22f, telemetry.cpuPercent, 20f, telemetry.cpuPercent),
                    lineColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )

                Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

                SectionHeader(title = "Details", modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(Spacing.xs))
                MinimalStatRow(label = "Physical cores", value = "${telemetry.cpuCores}")
                MinimalStatRow(label = "Governor", value = telemetry.cpuGovernor)
                MinimalStatRow(label = "Processor", value = deviceSpecs?.socModel ?: "ARM64")
                MinimalStatRow(label = "Thermal status", value = telemetry.thermalStatus)
            }

            1 -> {
                // RAM Tab
                val usedGb = "%.1f".format(telemetry.ramUsedMb / 1024f)
                val totalGb = "%.1f".format(telemetry.ramTotalMb / 1024f)

                MetricHeroSection(
                    value = usedGb,
                    unit = "GB",
                    label = "RAM used of $totalGb GB"
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                MinimalLineChart(
                    points = listOf(50f, 52f, 55f, telemetry.ramPercent, 58f, telemetry.ramPercent),
                    lineColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )

                Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

                SectionHeader(title = "Details", modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(Spacing.xs))
                MinimalStatRow(label = "Used memory", value = "${telemetry.ramUsedMb} MB")
                MinimalStatRow(label = "Free memory", value = "${telemetry.ramFreeMb} MB")
                MinimalStatRow(label = "Memory pressure", value = "${telemetry.ramPercent.toInt()} %")
            }

            2 -> {
                // Battery Tab
                MetricHeroSection(
                    value = "${telemetry.batteryPercent}",
                    unit = "%",
                    label = "${telemetry.batteryStatus} • ${telemetry.batteryTempC.toInt()} °C"
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                MinimalLineChart(
                    points = listOf(telemetry.batteryTempC - 2f, telemetry.batteryTempC - 1f, telemetry.batteryTempC, telemetry.batteryTempC),
                    lineColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )

                Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

                SectionHeader(title = "Details", modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(Spacing.xs))
                MinimalStatRow(label = "Health", value = telemetry.batteryHealth)
                MinimalStatRow(label = "Voltage", value = "${telemetry.batteryVoltageMv} mV")
                MinimalStatRow(label = "Current", value = "${telemetry.batteryCurrentMa} mA")
                MinimalStatRow(label = "Temperature", value = "${telemetry.batteryTempC} °C")
            }

            3 -> {
                // Network Tab
                val downSpeed = if (telemetry.networkDownKbps > 1024) {
                    "%.1f".format(telemetry.networkDownKbps / 1024f) to "Mbps"
                } else {
                    "${telemetry.networkDownKbps.toInt()}" to "Kbps"
                }

                MetricHeroSection(
                    value = downSpeed.first,
                    unit = downSpeed.second,
                    label = "Download throughput"
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                MinimalLineChart(
                    points = listOf(80f, 140f, 110f, telemetry.networkDownKbps.coerceAtLeast(10f), 95f),
                    lineColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )

                Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

                SectionHeader(title = "Details", modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(Spacing.xs))
                MinimalStatRow(label = "Upload throughput", value = "${telemetry.networkUpKbps.toInt()} Kbps")
                MinimalStatRow(label = "Latency / Ping", value = "$pingMs ms")
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}

@Composable
fun MetricHeroSection(
    value: String,
    unit: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = Spacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = " $unit",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MinimalStatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun MonitorScreenPreviewLight() {
    HyperPulseTheme(darkTheme = false) {
        MonitorScreenContent(
            telemetry = SystemTelemetry(
                cpuPercent = 28f,
                cpuCores = 8,
                cpuGovernor = "schedutil",
                ramUsedMb = 3800,
                ramTotalMb = 8192,
                ramPercent = 46f,
                batteryPercent = 74,
                batteryTempC = 32f,
                networkDownKbps = 3400f
            ),
            deviceSpecs = null,
            pingMs = 28
        )
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun MonitorScreenPreviewDark() {
    HyperPulseTheme(darkTheme = true) {
        MonitorScreenContent(
            telemetry = SystemTelemetry(
                cpuPercent = 45f,
                cpuCores = 8,
                ramUsedMb = 5120,
                ramTotalMb = 8192,
                ramPercent = 62f,
                batteryPercent = 60,
                batteryTempC = 35f,
                networkDownKbps = 1200f
            ),
            deviceSpecs = null,
            pingMs = 34
        )
    }
}

@Preview(name = "AMOLED Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun MonitorScreenPreviewAmoled() {
    HyperPulseTheme(darkTheme = true, isAmoled = true) {
        MonitorScreenContent(
            telemetry = SystemTelemetry(
                cpuPercent = 16f,
                cpuCores = 8,
                ramUsedMb = 3100,
                ramTotalMb = 8192,
                ramPercent = 38f,
                batteryPercent = 90,
                batteryTempC = 29f,
                networkDownKbps = 450f
            ),
            deviceSpecs = null,
            pingMs = 22
        )
    }
}

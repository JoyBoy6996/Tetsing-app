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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.shizuku.ShizukuState
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SettingRow
import com.example.ui.theme.HyperPulseTheme
import com.example.ui.theme.Spacing
import com.example.ui.theme.StatusGood
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.ShizukuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuHubScreen(
    viewModel: ShizukuViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.shizukuState.collectAsState()
    val lastOutput by viewModel.lastOutput.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Privilege Hub", style = MaterialTheme.typography.titleLarge) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Spacer(modifier = Modifier.height(Spacing.xs))

            SectionHeader(title = "Connection Status")

            val statusText = when (state) {
                ShizukuState.READY_ROOT -> "Connected (Root superuser UID 0)"
                ShizukuState.READY_SHELL -> "Connected (ADB shell mode UID 2000)"
                ShizukuState.PERMISSION_REQUIRED -> "Running (Permission required)"
                ShizukuState.NOT_RUNNING -> "Service not active"
                ShizukuState.NOT_INSTALLED -> "Shizuku not installed"
            }

            val statusColor = when (state) {
                ShizukuState.READY_ROOT, ShizukuState.READY_SHELL -> StatusGood
                ShizukuState.PERMISSION_REQUIRED -> StatusWarning
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            SettingRow(
                title = "Privilege level",
                subtitle = statusText,
                trailing = {
                    if (state == ShizukuState.PERMISSION_REQUIRED) {
                        TextButton(onClick = { viewModel.requestPermission() }) {
                            Text("Grant")
                        }
                    } else {
                        TextButton(onClick = { viewModel.refreshState() }) {
                            Text("Refresh")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

            SectionHeader(title = "Available Capabilities")
            SettingRow(title = "FPS dumpsys", subtitle = "SurfaceFlinger frame timing without root")
            SettingRow(title = "System cache trim", subtitle = "pm trim-caches across installed apps")
            SettingRow(title = "Game mode command", subtitle = "cmd game mode performance")
            SettingRow(title = "Appops background control", subtitle = "Safely restrict background run permissions")

            Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

            SectionHeader(title = "Actions")
            PrimaryActionButton(
                text = "Trim system caches",
                onClick = { viewModel.executeTrimCaches() }
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            TextButton(
                onClick = { viewModel.revertAllTweaks() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Revert all tweaks to system default",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            lastOutput?.let { out ->
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = out,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun ShizukuHubPreviewLight() {
    HyperPulseTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingRow(title = "Privilege level", subtitle = "Connected (ADB shell mode UID 2000)")
            SettingRow(title = "FPS dumpsys", subtitle = "SurfaceFlinger frame timing without root")
        }
    }
}

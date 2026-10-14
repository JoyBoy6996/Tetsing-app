package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.JunkCategory
import com.example.engine.JunkScanResult
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SectionHeader
import com.example.ui.theme.HyperPulseTheme
import com.example.ui.theme.Spacing
import com.example.ui.theme.StatusGood
import com.example.ui.viewmodel.CleanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanScreen(
    viewModel: CleanViewModel,
    modifier: Modifier = Modifier
) {
    val isScanning by viewModel.isScanning.collectAsState()
    val isCleaning by viewModel.isCleaning.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val cleanedBytes by viewModel.cleanedBytes.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Clean", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { viewModel.startScan() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Rescan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        CleanScreenContent(
            isScanning = isScanning,
            isCleaning = isCleaning,
            scanResult = scanResult,
            selectedCategories = selectedCategories,
            cleanedBytes = cleanedBytes,
            onToggleCategory = { viewModel.toggleCategory(it) },
            onClean = { viewModel.cleanSelected() },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun CleanScreenContent(
    isScanning: Boolean,
    isCleaning: Boolean,
    scanResult: JunkScanResult?,
    selectedCategories: Set<JunkCategory>,
    cleanedBytes: Long?,
    onToggleCategory: (JunkCategory) -> Unit,
    onClean: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalReclaimable = scanResult?.let { res ->
        res.categorySizes
            .filterKeys { it in selectedCategories }
            .values.sum()
    } ?: 0L

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.lg))

        // 1. One total-reclaimable number
        if (cleanedBytes != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = Spacing.md)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = StatusGood,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Storage cleaned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatBytesClean(cleanedBytes),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = Spacing.md)
            ) {
                Text(
                    text = "Reclaimable",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = if (isScanning) "…" else formatBytesClean(totalReclaimable),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // 2. One Clean button
        PrimaryActionButton(
            text = if (isCleaning) "Cleaning…" else "Clean",
            onClick = onClean,
            isLoading = isCleaning,
            enabled = !isScanning && totalReclaimable > 0 && cleanedBytes == null,
            modifier = Modifier.testTag("clean_primary_action")
        )

        Spacer(modifier = Modifier.height(Spacing.sectionSpacing))

        // 3. Categories as a simple checklist with sizes
        SectionHeader(
            title = "Categories",
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            JunkCategory.entries.forEach { category ->
                val size = scanResult?.categorySizes?.get(category) ?: 0L
                val isChecked = selectedCategories.contains(category)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleCategory(category) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleCategory(category) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Text(
                        text = formatBytesClean(size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}

fun formatBytesClean(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb >= 1024) {
        val gb = mb / 1024
        "%.1f GB".format(gb)
    } else {
        "%.0f MB".format(mb)
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun CleanScreenPreviewLight() {
    HyperPulseTheme(darkTheme = false) {
        CleanScreenContent(
            isScanning = false,
            isCleaning = false,
            scanResult = JunkScanResult(
                categorySizes = mapOf(
                    JunkCategory.CACHE to 320 * 1024 * 1024L,
                    JunkCategory.TEMP_LOGS to 45 * 1024 * 1024L,
                    JunkCategory.APK_LEFTOVERS to 180 * 1024 * 1024L,
                    JunkCategory.THUMBNAILS to 64 * 1024 * 1024L,
                    JunkCategory.EMPTY_FOLDERS to 0L
                ),
                totalBytes = 609 * 1024 * 1024L,
                items = emptyList()
            ),
            selectedCategories = setOf(JunkCategory.CACHE, JunkCategory.TEMP_LOGS, JunkCategory.APK_LEFTOVERS),
            cleanedBytes = null,
            onToggleCategory = {},
            onClean = {}
        )
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun CleanScreenPreviewDark() {
    HyperPulseTheme(darkTheme = true) {
        CleanScreenContent(
            isScanning = false,
            isCleaning = false,
            scanResult = JunkScanResult(
                categorySizes = mapOf(
                    JunkCategory.CACHE to 320 * 1024 * 1024L,
                    JunkCategory.TEMP_LOGS to 45 * 1024 * 1024L
                ),
                totalBytes = 365 * 1024 * 1024L,
                items = emptyList()
            ),
            selectedCategories = setOf(JunkCategory.CACHE),
            cleanedBytes = null,
            onToggleCategory = {},
            onClean = {}
        )
    }
}

@Preview(name = "AMOLED Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun CleanScreenPreviewAmoled() {
    HyperPulseTheme(darkTheme = true, isAmoled = true) {
        CleanScreenContent(
            isScanning = false,
            isCleaning = false,
            scanResult = null,
            selectedCategories = emptySet(),
            cleanedBytes = 512 * 1024 * 1024L,
            onToggleCategory = {},
            onClean = {}
        )
    }
}

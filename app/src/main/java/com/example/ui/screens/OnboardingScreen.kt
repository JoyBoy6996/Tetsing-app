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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.components.PrimaryActionButton
import com.example.ui.theme.HyperPulseTheme
import com.example.ui.theme.Spacing

data class OnboardingStep(
    val headline: String,
    val sentence: String,
    val permissionReason: String? = null
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        OnboardingStep(
            headline = "Real telemetry",
            sentence = "Direct kernel CPU, RAM, and thermal readings without synthetic or fabricated numbers."
        ),
        OnboardingStep(
            headline = "Live status notifications",
            sentence = "Keep live telemetry in your notification shade with one-tap memory optimization.",
            permissionReason = "Used exclusively to display real-time CPU and RAM in your status bar."
        ),
        OnboardingStep(
            headline = "Game optimization",
            sentence = "Per-game performance profiles, high refresh rate locks, and minimal floating HUD.",
            permissionReason = "Allows launching games with dedicated performance modes."
        )
    )

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screenPadding, vertical = Spacing.xl),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Top Step Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentStepIndex + 1} of ${steps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (currentStepIndex < steps.size - 1) {
                    TextButton(onClick = onComplete) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Middle: One Headline & One Sentence
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = step.headline,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = step.sentence,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (step.permissionReason != null) {
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    Text(
                        text = step.permissionReason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Bottom Primary Action
            PrimaryActionButton(
                text = if (currentStepIndex == steps.size - 1) "Get started" else "Continue",
                onClick = {
                    if (currentStepIndex < steps.size - 1) {
                        currentStepIndex++
                    } else {
                        onComplete()
                    }
                }
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun OnboardingPreviewLight() {
    HyperPulseTheme(darkTheme = false) {
        OnboardingScreen(onComplete = {})
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun OnboardingPreviewDark() {
    HyperPulseTheme(darkTheme = true) {
        OnboardingScreen(onComplete = {})
    }
}

@Preview(name = "AMOLED Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun OnboardingPreviewAmoled() {
    HyperPulseTheme(darkTheme = true, isAmoled = true) {
        OnboardingScreen(onComplete = {})
    }
}

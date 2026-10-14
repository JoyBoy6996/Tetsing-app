package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Single unified corner radius scale
val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),    // Inputs, chips, pills
    large = RoundedCornerShape(24.dp),     // Flat filled-tonal cards
    extraLarge = CircleShape              // Full-round primary action buttons
)

package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppSettings

@Composable
fun AudioVisualizer(
    levels: FloatArray,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    barSpacing: Dp = 4.dp
) {
    val settings = LocalAppSettings.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .testTag("audio_visualizer_canvas")
    ) {
        val totalBars = levels.size
        if (totalBars == 0) return@Canvas

        val spacingPx = barSpacing.toPx()
        val totalSpacing = spacingPx * (totalBars - 1)
        val barWidth = ((size.width - totalSpacing) / totalBars).coerceAtLeast(3f)

        val gradientBrush = Brush.verticalGradient(
            colors = listOf(primaryColor, secondaryColor),
            startY = 0f,
            endY = size.height
        )

        for (i in 0 until totalBars) {
            val level = if (settings.enableAnimations) levels[i] else 0.25f
            val barHeight = (size.height * level).coerceIn(4f, size.height)
            val x = i * (barWidth + spacingPx)
            val y = (size.height - barHeight) / 2f

            drawRoundRect(
                brush = gradientBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

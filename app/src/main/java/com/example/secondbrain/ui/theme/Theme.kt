package com.example.secondbrain.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SecondBrainColorScheme = darkColorScheme(
    primary                = SBPrimary,
    onPrimary              = SBOnPrimary,
    primaryContainer       = SBPrimaryContainer,
    onPrimaryContainer     = SBOnPrimaryContainer,
    background             = SBBackground,
    onBackground           = SBOnSurface,
    surface                = SBSurfaceLow,
    onSurface              = SBOnSurface,
    surfaceVariant         = SBSurface,
    onSurfaceVariant       = SBOnSurfaceVariant,
    surfaceContainer       = SBSurface,
    surfaceContainerLow    = SBSurfaceLow,
    surfaceContainerLowest = SBSurfaceLowest,
    surfaceContainerHigh   = SBSurfaceHigh,
    surfaceContainerHighest= SBSurfaceHighest,
    outline                = SBOutline,
    outlineVariant         = SBOutlineVariant,
    error                  = SBDanger,
    onError                = SBOnPrimary,
)

@Composable
fun SecondBrainTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SecondBrainColorScheme,
        typography  = Typography,
        content     = content
    )
}

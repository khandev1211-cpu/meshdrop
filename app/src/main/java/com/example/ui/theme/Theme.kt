package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CosmicCyan,
    onPrimary = SpaceBlack,
    secondary = ElectricBlue,
    onSecondary = GlowWhite,
    tertiary = CyberGreen,
    onTertiary = SpaceBlack,
    background = SpaceBlack,
    onBackground = GlowWhite,
    surface = DeepSpaceNavy,
    onSurface = GlowWhite,
    surfaceVariant = CardSlate,
    onSurfaceVariant = GlowWhite,
    outline = BorderCyan
)

// Auxiliary light slate for light mode surfaceVariant
val ColorSlateLight = androidx.compose.ui.graphics.Color(0xFFF1F5F9)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = GlowWhite,
    secondary = NebulaPurple,
    onSecondary = GlowWhite,
    tertiary = CyberGreen,
    onTertiary = GlowWhite,
    background = GlowWhite,
    onBackground = SpaceBlack,
    surface = GlowWhite,
    onSurface = SpaceBlack,
    surfaceVariant = ColorSlateLight,
    onSurfaceVariant = SpaceBlack,
    outline = BorderCyan
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor false to preserve the high-tech custom branded MeshDrop identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Enforce dark theme for the high-security "cyber room" aesthetic!
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

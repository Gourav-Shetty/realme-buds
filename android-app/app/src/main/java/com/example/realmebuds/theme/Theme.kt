package com.example.realmebuds.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RealmeLightColorScheme = lightColorScheme(
    primary = RealmeCircleActive,
    onPrimary = Color.White,
    secondary = RealmeSwitchActive,
    onSecondary = Color.White,
    background = RealmePageBg,
    onBackground = RealmeTextDark,
    surface = RealmeCardWhite,
    onSurface = RealmeTextDark,
    surfaceVariant = RealmeCircleInactive,
    onSurfaceVariant = RealmeTextGrey,
    outline = RealmeDivider
)

@Composable
fun RealmeBudsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RealmeLightColorScheme,
        typography = Typography,
        content = content
    )
}

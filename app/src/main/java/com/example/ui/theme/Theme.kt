package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CraftColorScheme = darkColorScheme(
    primary = MinecraftGreen,
    secondary = MinecraftButtonGrey,
    background = MinecraftStoneBackground,
    surface = MinecraftDirtBackground,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our authentic Minecraft craft palette directly!
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CraftColorScheme,
        typography = MinecraftTypography,
        content = content
    )
}

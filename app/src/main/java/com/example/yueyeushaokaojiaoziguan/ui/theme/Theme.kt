package com.example.yueyeushaokaojiaoziguan.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BrandAmber,
    secondary = BrandOrange,
    tertiary = BrandAmber,
    background = DarkCharcoal,
    surface = Color(0xFF2A2A3E),
    onPrimary = DarkCharcoal,
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    secondary = BrandOrangeDark,
    tertiary = BrandAmber,
    background = WarmWhite,
    surface = Color.White,
    surfaceVariant = SoftCream,
    onBackground = DarkCharcoal,
    onSurface = DarkCharcoal,
    onSurfaceVariant = MediumGray
)

@Composable
fun YueyeushaokaojiaoziguanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

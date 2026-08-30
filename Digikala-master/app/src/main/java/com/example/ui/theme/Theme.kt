package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = DigikalaRedLight,
    onPrimary = Color.White,
    primaryContainer = DigikalaRedDark,
    onPrimaryContainer = Color.White,
    secondary = DigikalaCyan,
    background = DigikalaDarkBg,
    surface = DigikalaDarkSurface,
    onBackground = Color.White,
    onSurface = Color.White,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DigikalaRed,
    onPrimary = Color.White,
    primaryContainer = DigikalaRedLight,
    onPrimaryContainer = Color.White,
    secondary = DigikalaCyan,
    background = DigikalaGeometricBg,
    surface = DigikalaGeometricSurface,
    onBackground = DigikalaTextDark,
    onSurface = DigikalaTextDark,
    outline = DigikalaBorder,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Keep true brand colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

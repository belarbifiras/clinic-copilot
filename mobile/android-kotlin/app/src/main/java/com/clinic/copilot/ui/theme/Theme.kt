package com.clinic.copilot.ui.theme

import android.app.Activity
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
    primary = CcNavy,
    secondary = CcBlue,
    tertiary = CcTurquoise,
)

private val LightColorScheme = lightColorScheme(
    primary = CcNavy,
    secondary = CcBlue,
    tertiary = CcTurquoise,
    background = CcBackground,
    surface = CcBackground,
)

@Composable
fun ClinicCopilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // dynamicColor désactivé par défaut : les couleurs Clinic Copilot prévalent sur Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
        content = content,
    )
}
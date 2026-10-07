package com.smsplanner.app.ui.theme

import android.content.Context

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ============================================================
// THÈME SOMBRE
// ============================================================

private val DarkColorScheme = darkColorScheme(

    primary = GreenPrimary,
    onPrimary = Black,

    secondary = DarkSurfaceVariant,
    onSecondary = DarkOnSurface,

    tertiary = DarkSurfaceVariant,
    onTertiary = DarkOnSurface,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkSurface,
    onSurface = DarkOnSurface,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,

    outline = DarkOutline,

    error = ErrorRed
)


// ============================================================
// THÈME CLAIR
// ============================================================

private val LightColorScheme = lightColorScheme(

    primary = GreenPrimary,
    onPrimary = Black,

    secondary = LightSurfaceVariant,
    onSecondary = LightOnSurface,

    tertiary = LightSurfaceVariant,
    onTertiary = LightOnSurface,

    background = LightBackground,
    onBackground = LightOnBackground,

    surface = LightSurface,
    onSurface = LightOnSurface,

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,

    outline = LightOutline,

    error = ErrorRed
)


// ============================================================
// PRÉFÉRENCE D'APPARENCE
// ============================================================

object ThemePreference {

    private const val PREFS_NAME =
        "smsplanner_theme"

    private const val THEME_KEY =
        "theme_mode"

    const val SYSTEM =
        "system"

    const val LIGHT =
        "light"

    const val DARK =
        "dark"


    fun getTheme(
        context: Context
    ): String {

        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                THEME_KEY,
                SYSTEM
            )
            ?: SYSTEM
    }


    fun setTheme(
        context: Context,
        theme: String
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                THEME_KEY,
                theme
            )
            .apply()
    }
}


// ============================================================
// THÈME SMS PLANNER
// ============================================================

@Composable
fun SMSPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colorScheme =

        if (darkTheme) {

            DarkColorScheme

        } else {

            LightColorScheme
        }


    MaterialTheme(

        colorScheme =
            colorScheme,

        typography =
            Typography,

        content =
            content
    )
}
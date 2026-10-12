package com.maxeydev.picklejot.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LIGHT_COLOR_SCHEME: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF007A43),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE1F5E9),
        onPrimaryContainer = Color(0xFF0B3A24),
        secondary = Color(0xFF456B58),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE7F7ED),
        onSecondaryContainer = Color(0xFF102B1E),
        tertiary = Color(0xFF8A5A00),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFE8B8),
        onTertiaryContainer = Color(0xFF2B1B00),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        background = Color(0xFFF3F8F4),
        onBackground = Color(0xFF14231C),
        surface = Color(0xFFF3F8F4),
        onSurface = Color(0xFF14231C),
        surfaceVariant = Color(0xFFDCE1DD),
        onSurfaceVariant = Color(0xFF40554A),
        surfaceTint = Color(0xFF007A43),
        inverseSurface = Color(0xFF26332C),
        inverseOnSurface = Color(0xFFEAF2ED),
        inversePrimary = Color(0xFF6FD69B),
        outline = Color(0xFF6B7A71),
        outlineVariant = Color(0xFFD2DED6),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFDDE5E0),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF8FBF9),
        surfaceContainer = Color(0xFFEEF3EF),
        surfaceContainerHigh = Color(0xFFE7EEE9),
        surfaceContainerHighest = Color(0xFFE0E8E3),
    )

private val DARK_COLOR_SCHEME: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF6FD69B),
        onPrimary = Color(0xFF00391F),
        primaryContainer = Color(0xFF00552F),
        onPrimaryContainer = Color(0xFFB4F2CB),
        secondary = Color(0xFFB0CCBB),
        onSecondary = Color(0xFF1B352A),
        secondaryContainer = Color(0xFF32493C),
        onSecondaryContainer = Color(0xFFCCE8D7),
        tertiary = Color(0xFFF2C15C),
        onTertiary = Color(0xFF402D00),
        tertiaryContainer = Color(0xFF5C4200),
        onTertiaryContainer = Color(0xFFFFDF9E),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0E1712),
        onBackground = Color(0xFFE0E9E3),
        surface = Color(0xFF0E1712),
        onSurface = Color(0xFFE0E9E3),
        surfaceVariant = Color(0xFF2B3A31),
        onSurfaceVariant = Color(0xFFB5C4BA),
        surfaceTint = Color(0xFF6FD69B),
        inverseSurface = Color(0xFFE0E9E3),
        inverseOnSurface = Color(0xFF26332C),
        inversePrimary = Color(0xFF007A43),
        outline = Color(0xFF85968B),
        outlineVariant = Color(0xFF34433A),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF2F3B34),
        surfaceDim = Color(0xFF0E1712),
        surfaceContainerLowest = Color(0xFF0A120E),
        surfaceContainerLow = Color(0xFF141F19),
        surfaceContainer = Color(0xFF18251E),
        surfaceContainerHigh = Color(0xFF212F27),
        surfaceContainerHighest = Color(0xFF2B3A31),
    )

private val BASE_TYPOGRAPHY = Typography()

private val PICKLEJOT_TYPOGRAPHY: Typography =
    BASE_TYPOGRAPHY.copy(
        headlineLarge =
            BASE_TYPOGRAPHY.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
        headlineMedium =
            BASE_TYPOGRAPHY.headlineMedium.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        headlineSmall =
            BASE_TYPOGRAPHY.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
        titleLarge =
            BASE_TYPOGRAPHY.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = BASE_TYPOGRAPHY.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = BASE_TYPOGRAPHY.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodySmall = BASE_TYPOGRAPHY.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = BASE_TYPOGRAPHY.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = BASE_TYPOGRAPHY.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall =
            BASE_TYPOGRAPHY.labelSmall.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    )

private val PICKLEJOT_SHAPES =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

@Composable
fun PicklejotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DARK_COLOR_SCHEME else LIGHT_COLOR_SCHEME
    val picklejotColors = if (darkTheme) DARK_PICKLEJOT_COLORS else LIGHT_PICKLEJOT_COLORS
    CompositionLocalProvider(LocalPicklejotColors provides picklejotColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PICKLEJOT_TYPOGRAPHY,
            shapes = PICKLEJOT_SHAPES,
            content = content,
        )
    }
}

@Composable
fun StatusBarIcons(useLightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) {
        return
    }
    DisposableEffect(view, useLightIcons) {
        val window = view.context.findWindow()
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = !useLightIcons
        onDispose {
            if (controller != null && previous != null) {
                controller.isAppearanceLightStatusBars = previous
            }
        }
    }
}

private tailrec fun Context.findWindow(): Window? =
    when (this) {
        is Activity -> window
        is ContextWrapper -> baseContext.findWindow()
        else -> null
    }

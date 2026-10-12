package com.maxeydev.picklejot.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
class PicklejotColors(
    val header: Color,
    val headerTop: Color,
    val headerBottom: Color,
    val headerChip: Color,
    val onHeader: Color,
    val onHeaderMuted: Color,
    val headerAccent: Color,
    val headerPill: Color,
    val onHeaderPill: Color,
    val streakFlame: Color,
    val winRow: Color,
    val winBadge: Color,
    val onWinBadge: Color,
    val lossBadge: Color,
    val onLossBadge: Color,
    val cardBorder: Color,
    val countBadge: Color,
    val onCountBadge: Color,
    val activityNone: Color,
    val activityLow: Color,
    val activityMid: Color,
    val activityHigh: Color,
    val activityPeak: Color,
)

internal val LIGHT_PICKLEJOT_COLORS =
    PicklejotColors(
        header = Color(0xFF007A43),
        headerTop = Color(0xFF00381F),
        headerBottom = Color(0xFF1F8557),
        headerChip = Color(0x24FFFFFF),
        onHeader = Color(0xFFFFFFFF),
        onHeaderMuted = Color(0xFFDEF9E9),
        headerAccent = Color(0xFFA5E8C1),
        headerPill = Color(0xFF1F8557),
        onHeaderPill = Color(0xFFFFFFFF),
        streakFlame = Color(0xFFFFB84D),
        winRow = Color(0xFFD9F1E2),
        winBadge = Color(0xFF007A43),
        onWinBadge = Color(0xFFFFFFFF),
        lossBadge = Color(0xFFDCE1DD),
        onLossBadge = Color(0xFF4D5651),
        cardBorder = Color(0xFFDDE7E1),
        countBadge = Color(0xFFC7F4D5),
        onCountBadge = Color(0xFF0B3A24),
        activityNone = Color(0xFFE8EFEB),
        activityLow = Color(0xFFA5E8C1),
        activityMid = Color(0xFF4FBF85),
        activityHigh = Color(0xFF1F8557),
        activityPeak = Color(0xFF00592F),
    )

internal val DARK_PICKLEJOT_COLORS =
    PicklejotColors(
        header = Color(0xFF0B4A2C),
        headerTop = Color(0xFF052E1C),
        headerBottom = Color(0xFF0B4A2C),
        headerChip = Color(0x24FFFFFF),
        onHeader = Color(0xFFFFFFFF),
        onHeaderMuted = Color(0xFFCDEEDB),
        headerAccent = Color(0xFFA5E8C1),
        headerPill = Color(0xFF1E6B47),
        onHeaderPill = Color(0xFFFFFFFF),
        streakFlame = Color(0xFFFFB84D),
        winRow = Color(0xFF1A3A26),
        winBadge = Color(0xFF1F7A4D),
        onWinBadge = Color(0xFFFFFFFF),
        lossBadge = Color(0xFF34433A),
        onLossBadge = Color(0xFFC9D6CE),
        cardBorder = Color(0xFF34433A),
        countBadge = Color(0xFFB4F2CB),
        onCountBadge = Color(0xFF00391F),
        activityNone = Color(0xFF26332B),
        activityLow = Color(0xFF1E6B47),
        activityMid = Color(0xFF3FA773),
        activityHigh = Color(0xFF7BD6A3),
        activityPeak = Color(0xFFC7F4D5),
    )

internal val LocalPicklejotColors = staticCompositionLocalOf { LIGHT_PICKLEJOT_COLORS }

object PicklejotTheme {
    val colors: PicklejotColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPicklejotColors.current
}

package com.maxeydev.picklejot.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private const val LIGHT_SURFACE_LUMINANCE = 0.5f
private const val LARGE_FONT_SCALE = 1.3f
private val ROW_MIN_HEIGHT = 56.dp
private val ROW_ICON_SIZE = 24.dp
private val ROW_DIVIDER_INSET = 56.dp
private val VALUE_MAX_WIDTH = 160.dp

internal val SETTINGS_CARD_SHAPE = RoundedCornerShape(20.dp)

@Composable
internal fun settingsCardColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.surface.luminance() > LIGHT_SURFACE_LUMINANCE) {
        scheme.surfaceContainerLowest
    } else {
        scheme.surfaceContainerLow
    }
}

@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = SETTINGS_CARD_SHAPE,
        color = settingsCardColor(),
        border = BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
internal fun SettingsRow(
    @DrawableRes icon: Int,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    value: String? = null,
    valueModifier: Modifier = Modifier,
    showDivider: Boolean = false,
    navigates: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val isLarge = LocalDensity.current.fontScale > LARGE_FONT_SCALE
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showDivider) {
            HorizontalDivider(
                color = PicklejotTheme.colors.cardBorder,
                modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
            )
        }
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .heightIn(min = ROW_MIN_HEIGHT)
                    .padding(horizontal = PicklejotSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.lg),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(ROW_ICON_SIZE),
            )
            Column(modifier = Modifier.weight(1f).padding(vertical = PicklejotSpacing.sm)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                supporting?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (value != null && isLarge) {
                    RowValue(text = value, isLarge = true, modifier = valueModifier)
                }
            }
            if (value != null && !isLarge) {
                RowValue(text = value, isLarge = false, modifier = valueModifier)
            }
            trailing?.invoke(this)
            if (navigates) {
                TrailingChevron()
            }
        }
    }
}

@Composable
private fun RowValue(
    text: String,
    isLarge: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = if (isLarge) 2 else 1,
        overflow = TextOverflow.Ellipsis,
        modifier = if (isLarge) modifier else modifier.widthIn(max = VALUE_MAX_WIDTH),
    )
}

@Composable
internal fun TrailingChevron() {
    TrailingIcon(R.drawable.ic_chevron_right)
}

@Composable
internal fun TrailingIcon(
    @DrawableRes icon: Int,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(ROW_ICON_SIZE),
    )
}

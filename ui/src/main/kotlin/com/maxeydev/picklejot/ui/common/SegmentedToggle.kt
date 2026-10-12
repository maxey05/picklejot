package com.maxeydev.picklejot.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val MIN_TOUCH_TARGET = 48.dp
private val PILL_INSET = 3.dp
private val PILL_OUTLINE = 1.5.dp
private val PILL_SHAPE = RoundedCornerShape(8.dp)
private const val PILL_SLIDE_MILLIS = 240
private const val PILL_FADE_MILLIS = 120
private val PillEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun Modifier.atSlot(position: () -> Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative((position() * placeable.width).roundToInt(), 0)
        }
    }

@Composable
internal fun <T> SegmentedToggle(
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    optionTag: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val selectedIndex = options.indexOf(selected)
    val position = remember { Animatable(selectedIndex.coerceAtLeast(0).toFloat()) }
    val pillAlpha = remember { Animatable(if (selectedIndex >= 0) 1f else 0f) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex < 0) {
            pillAlpha.animateTo(0f, tween(durationMillis = PILL_FADE_MILLIS))
        } else if (pillAlpha.value == 0f) {
            position.snapTo(selectedIndex.toFloat())
            pillAlpha.animateTo(1f, tween(durationMillis = PILL_FADE_MILLIS))
        } else {
            position.animateTo(selectedIndex.toFloat(), tween(durationMillis = PILL_SLIDE_MILLIS, easing = PillEasing))
        }
    }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(shape)
                .border(if (isError) 2.dp else 1.dp, if (isError) colors.error else colors.outlineVariant, shape)
                .selectableGroup(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(1f / options.size)
                    .fillMaxHeight()
                    .atSlot { position.value }
                    .graphicsLayer { alpha = pillAlpha.value }
                    .padding(PILL_INSET)
                    .background(colors.primaryContainer, PILL_SHAPE)
                    .border(PILL_OUTLINE, colors.primary, PILL_SHAPE),
        )
        Row {
            options.forEach { option ->
                val isSelected = option == selected
                val labelColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = MIN_TOUCH_TARGET)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onSelected(option) },
                            )
                            .testTag(optionTag(option)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = optionLabel(option),
                        style = MaterialTheme.typography.labelLarge,
                        color = labelColor,
                    )
                }
            }
        }
    }
}

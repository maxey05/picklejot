package com.maxeydev.picklejot.ui.dashboard

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.stats.ProInsights
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private const val DRAW_MILLIS = 1300
private const val DRAW_DELAY_MILLIS = 150
private const val PERCENT_SCALE = 100f
private const val AREA_ALPHA = 0.14f
private const val AREA_START = 0.55f
private const val DOT_START = 0.9f
private const val AVERAGE_START = 0.1f
private val CHART_HEIGHT = 120.dp
private val CHART_PADDING = 6.dp
private val LINE_WIDTH = 2.5.dp
private val GUIDE_WIDTH = 1.dp
private val AVERAGE_WIDTH = 1.5.dp
private val DOT_RADIUS = 5.dp
private val DASH_ON = 6.dp
private val DASH_OFF = 4.dp
private val AXIS_LABEL_WIDTH = 36.dp
private val AXIS_PERCENTS = listOf(100, 50, 0)
private val LEGEND_WIDTH = 14.dp
private val LEGEND_HEIGHT = 3.dp
private val DRAW_EASING = CubicBezierEasing(0.45f, 0.05f, 0.25f, 1f)

@Composable
internal fun FormChart(
    insights: ProInsights,
    modifier: Modifier = Modifier,
) {
    val reveal = rememberSectionReveal("pro_form")
    val progress = rememberRevealProgress(reveal.revealed, DRAW_MILLIS, DRAW_DELAY_MILLIS, DRAW_EASING)
    val form = insights.form
    val description =
        if (form.size < 2) {
            ""
        } else {
            stringResource(R.string.stats_pro_form_a11y, form.size, form.first(), form.last())
        }

    ProCard(reveal = reveal, modifier = modifier.testTag(DashboardTestTags.STATS_PRO_FORM)) {
        ProCardHeader(
            title = stringResource(R.string.stats_pro_form_title),
            subtitle = stringResource(R.string.stats_pro_form_subtitle),
        )
        if (form.size < 2) {
            Text(
                text = stringResource(R.string.stats_pro_form_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val average = insights.overall.winPercent
            Row(horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm)) {
                AxisLabels()
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(CHART_HEIGHT)
                            .semantics { contentDescription = description }
                            .formPlot(form = form, average = average, progress = { progress.value }),
                )
            }
            Row(modifier = Modifier.fillMaxWidth().clearAndSetSemantics {}) {
                Text(
                    text = stringResource(R.string.stats_pro_form_then, form.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.stats_pro_form_now, form.last()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FormLegend(average = average)
            Text(
                text = takeaway(form),
                style = MaterialTheme.typography.bodyMedium,
                modifier =
                    Modifier.graphicsLayer {
                        alpha = ((progress.value - DOT_START) / (1f - DOT_START)).coerceIn(0f, 1f)
                    },
            )
        }
    }
}

@Composable
private fun takeaway(form: List<Int>): String {
    val change = form.last() - form.first()
    return when {
        change > 0 -> stringResource(R.string.stats_pro_form_up, change, form.size)
        change < 0 -> stringResource(R.string.stats_pro_form_down, -change, form.size)
        else -> stringResource(R.string.stats_pro_form_flat, form.size)
    }
}

@Composable
private fun AxisLabels() {
    Column(
        modifier = Modifier.width(AXIS_LABEL_WIDTH).height(CHART_HEIGHT).clearAndSetSemantics {},
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
    ) {
        AXIS_PERCENTS.forEach {
            Text(
                text = stringResource(R.string.stats_percent, it),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FormLegend(average: Int?) {
    Row(
        modifier = Modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(color = MaterialTheme.colorScheme.primary, label = stringResource(R.string.stats_pro_form_rolling))
        if (average != null) {
            LegendItem(
                color = PicklejotTheme.colors.streakFlame,
                label = stringResource(R.string.stats_pro_form_average, average),
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
    ) {
        Box(modifier = Modifier.width(LEGEND_WIDTH).height(LEGEND_HEIGHT).background(color, RoundedCornerShape(2.dp)))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Modifier.formPlot(
    form: List<Int>,
    average: Int?,
    progress: () -> Float,
): Modifier {
    val lineColor = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    val ringColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val averageColor = PicklejotTheme.colors.streakFlame
    return drawWithCache {
        val pad = CHART_PADDING.toPx()
        val plotHeight = size.height - 2 * pad
        val points =
            form.mapIndexed { index, value ->
                Offset(
                    x = size.width * index / (form.size - 1),
                    y = pad + plotHeight * (1f - value / PERCENT_SCALE),
                )
            }
        val line =
            Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
        val area =
            Path().apply {
                moveTo(points.first().x, size.height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, size.height)
                close()
            }
        val measure = PathMeasure().apply { setPath(line, false) }
        val length = measure.length
        val segment = Path()
        val dash = PathEffect.dashPathEffect(floatArrayOf(DASH_ON.toPx(), DASH_OFF.toPx()))
        val topY = pad
        val middleY = pad + plotHeight / 2f
        val bottomY = pad + plotHeight
        onDrawBehind {
            val p = progress()
            drawLine(guideColor, Offset(0f, topY), Offset(size.width, topY), GUIDE_WIDTH.toPx())
            drawLine(guideColor, Offset(0f, bottomY), Offset(size.width, bottomY), GUIDE_WIDTH.toPx())
            drawLine(
                color = guideColor,
                start = Offset(0f, middleY),
                end = Offset(size.width, middleY),
                strokeWidth = GUIDE_WIDTH.toPx(),
                pathEffect = dash,
            )
            if (average != null) {
                val averageY = pad + plotHeight * (1f - average / PERCENT_SCALE)
                drawLine(
                    color = averageColor.copy(alpha = ((p - AVERAGE_START) / AVERAGE_START).coerceIn(0f, 1f)),
                    start = Offset(0f, averageY),
                    end = Offset(size.width, averageY),
                    strokeWidth = AVERAGE_WIDTH.toPx(),
                    pathEffect = dash,
                )
            }
            val areaAlpha = ((p - AREA_START) / (1f - AREA_START)).coerceIn(0f, 1f)
            drawPath(area, lineColor.copy(alpha = AREA_ALPHA * areaAlpha))
            segment.reset()
            measure.getSegment(0f, length * p, segment, true)
            drawPath(
                path = segment,
                color = lineColor,
                style = Stroke(width = LINE_WIDTH.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            val dotScale = ((p - DOT_START) / (1f - DOT_START)).coerceIn(0f, 1f)
            if (dotScale > 0f) {
                drawCircle(ringColor, radius = (DOT_RADIUS + 2.dp).toPx() * dotScale, center = points.last())
                drawCircle(lineColor, radius = DOT_RADIUS.toPx() * dotScale, center = points.last())
            }
        }
    }
}

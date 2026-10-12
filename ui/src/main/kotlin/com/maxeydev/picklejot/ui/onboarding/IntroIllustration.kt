package com.maxeydev.picklejot.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotTheme
import kotlin.math.min

internal const val INTRO_VIEW_WIDTH = 390f

@Immutable
private data class IntroPalette(
    val blob: Color,
    val card: Color,
    val cardBorder: Color,
    val green: Color,
    val greenDeep: Color,
    val greenMid: Color,
    val mint: Color,
    val mintSoft: Color,
    val flame: Color,
    val flameDeep: Color,
    val flameSoft: Color,
    val ink: Color,
    val muted: Color,
    val emptyCell: Color,
    val loss: Color,
    val onLoss: Color,
    val onGreen: Color,
)

@Immutable
private data class IntroArtLabels(
    val win: String,
    val loss: String,
    val score: String,
    val partner: String,
    val photo: String,
    val save: String,
    val month: String,
    val monthRecord: String,
    val winRate: String,
    val winRateValue: String,
    val cardScore: String,
    val cardMeta: String,
    val wordmark: String,
)

private enum class Anchor { START, MIDDLE, END }

private val UnitDensity = Density(density = 1f, fontScale = 1f)

private val CalendarRows =
    listOf(
        listOf(0, 1, 0, 2, 0, 1, 0),
        listOf(1, 0, 3, 0, 1, 2, 0),
        listOf(0, 2, 0, 1, 0, 3, 1),
        listOf(1, 0, 2, 3, 0, 0, 0),
    )

private val CalendarLevels = CalendarRows.flatten()

private val BarHeights = listOf(28f, 44f, 64f, 88f)

private fun svgPath(data: String): Path = PathParser().parsePathString(data).toPath()

private val FlameOuter by lazy {
    svgPath("M0 -24 C12 -8 20 2 16 14 C12 25 -12 25 -16 14 C-20 2 -9 -4 -7 -13 C-2 -7 0 -4 0 -24 Z")
}
private val FlameInner by lazy {
    svgPath("M0 -2 C6 6 9 10 7 15 C5 20 -6 20 -8 15 C-10 10 -4 7 -3 2 C-1 5 0 6 0 -2 Z")
}
private val TrendLine by lazy { svgPath("M10 92 L40 72 L72 58 L116 16") }
private val TrendHead by lazy { svgPath("M102 14 L116 16 L114 30") }
private val Heart by lazy {
    svgPath("M12 20.5s-8-4.8-8-10.4A4.6 4.6 0 0 1 12 7.3a4.6 4.6 0 0 1 8 2.8c0 5.6-8 10.4-8 10.4z")
}
private val CourtShape by lazy { svgPath("M40.84 54 L119.16 54 L146 126 L24 126 Z") }

@Composable
internal fun IntroIllustration(
    page: IntroPage,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
) {
    if (page == IntroPage.PRIVACY) {
        PrivacyMascot(topInset = topInset, modifier = modifier)
        return
    }
    val palette = introPalette()
    val labels = introArtLabels()
    val measurer = rememberTextMeasurer()
    val description = stringResource(page.illustrationDescription)
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        val fit =
            min(
                size.width / INTRO_VIEW_WIDTH,
                (size.height - topInset.toPx()) / page.viewportHeight,
            )
        val top = size.height - page.viewportHeight * fit
        withTransform(
            {
                translate((size.width - INTRO_VIEW_WIDTH * fit) / 2f, top)
                scale(fit, fit, Offset.Zero)
            },
        ) {
            when (page) {
                IntroPage.LOGGING -> drawLoggingScene(palette, measurer, labels)
                IntroPage.STATS -> drawStatsScene(palette, measurer, labels)
                IntroPage.SHARE -> drawShareScene(palette, measurer, labels)
                IntroPage.PRIVACY -> Unit
            }
        }
    }
}

@Composable
private fun introPalette(): IntroPalette {
    val scheme = MaterialTheme.colorScheme
    val brand = PicklejotTheme.colors
    return IntroPalette(
        blob = scheme.secondaryContainer,
        card = scheme.surfaceBright,
        cardBorder = brand.cardBorder,
        green = brand.winBadge,
        greenDeep = Color(0xFF0B4A2C),
        greenMid = Color(0xFF4CB27D),
        mint = brand.headerAccent,
        mintSoft = brand.countBadge,
        flame = brand.streakFlame,
        flameDeep = Color(0xFFFF9F1C),
        flameSoft = scheme.tertiaryContainer,
        ink = scheme.onSurface,
        muted = scheme.onSurfaceVariant,
        emptyCell = scheme.surfaceContainerHigh,
        loss = brand.lossBadge,
        onLoss = brand.onLossBadge,
        onGreen = Color.White,
    )
}

@Composable
private fun introArtLabels(): IntroArtLabels =
    IntroArtLabels(
        win = stringResource(R.string.intro_art_win),
        loss = stringResource(R.string.intro_art_loss),
        score = stringResource(R.string.intro_art_score),
        partner = stringResource(R.string.intro_art_partner),
        photo = stringResource(R.string.intro_art_photo),
        save = stringResource(R.string.intro_art_save),
        month = stringResource(R.string.intro_art_month),
        monthRecord = stringResource(R.string.intro_art_month_record),
        winRate = stringResource(R.string.intro_art_win_rate),
        winRateValue = stringResource(R.string.stats_percent, 73),
        cardScore = stringResource(R.string.intro_art_card_score),
        cardMeta = stringResource(R.string.intro_art_card_meta),
        wordmark = stringResource(R.string.intro_art_wordmark),
    )

private fun DrawScope.box(
    color: Color,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
) {
    drawRoundRect(color, Offset(x, y), Size(width, height), CornerRadius(radius, radius))
}

private fun DrawScope.boxOutline(
    color: Color,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
    strokeWidth: Float,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(width, height),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = strokeWidth),
    )
}

private fun DrawScope.shadow(
    palette: IntroPalette,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
) {
    repeat(3) { step ->
        val grow = (step + 1) * 4f
        box(
            color = palette.greenDeep.copy(alpha = 0.05f),
            x = x - grow,
            y = y - grow + 10f,
            width = width + grow * 2f,
            height = height + grow * 2f,
            radius = radius + grow,
        )
    }
}

private fun DrawScope.line(
    color: Color,
    from: Offset,
    to: Offset,
    width: Float,
) {
    drawLine(color = color, start = from, end = to, strokeWidth = width, cap = StrokeCap.Round)
}

private fun DrawScope.strokePath(
    path: Path,
    color: Color,
    width: Float,
) {
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.label(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    baseline: Float,
    fontSize: Float,
    color: Color,
    weight: FontWeight = FontWeight.Bold,
    anchor: Anchor = Anchor.START,
) {
    val layout =
        measurer.measure(
            text = text,
            style = TextStyle(color = color, fontSize = fontSize.sp, fontWeight = weight),
            softWrap = false,
            maxLines = 1,
            density = UnitDensity,
        )
    val width = layout.size.width.toFloat()
    val left =
        when (anchor) {
            Anchor.START -> x
            Anchor.MIDDLE -> x - width / 2f
            Anchor.END -> x - width
        }
    drawText(layout, topLeft = Offset(left, baseline - layout.firstBaseline))
}

private fun DrawScope.phoneFrame(
    palette: IntroPalette,
    width: Float,
    height: Float,
    radius: Float,
) {
    shadow(palette, 0f, 0f, width, height, radius)
    box(palette.greenDeep, 0f, 0f, width, height, radius)
    box(palette.card, 6f, 6f, width - 12f, height - 12f, radius - 6f)
}

private fun DrawScope.optionalRow(
    palette: IntroPalette,
    measurer: TextMeasurer,
    text: String,
    y: Float,
) {
    drawCircle(palette.blob, 8f, Offset(28f, y + 8f))
    line(palette.green, Offset(24f, y + 8f), Offset(32f, y + 8f), 1.8f)
    line(palette.green, Offset(28f, y + 4f), Offset(28f, y + 12f), 1.8f)
    label(measurer, text, 42f, y + 12f, 11f, palette.muted, FontWeight.Medium)
}

private fun DrawScope.drawLoggingScene(
    palette: IntroPalette,
    measurer: TextMeasurer,
    labels: IntroArtLabels,
) {
    translate(118f, 92f) {
        phoneFrame(palette, 154f, 268f, 30f)
        box(palette.cardBorder, 59f, 24f, 36f, 4f, 2f)
        box(palette.emptyCell, 20f, 44f, 70f, 6f, 3f)
        box(palette.mint, 19f, 61f, 56f, 56f, 15f)
        box(palette.green, 22f, 64f, 50f, 50f, 12f)
        label(measurer, labels.win, 47f, 97f, 22f, palette.onGreen, FontWeight.Bold, Anchor.MIDDLE)
        box(palette.loss, 82f, 64f, 50f, 50f, 12f)
        label(measurer, labels.loss, 107f, 97f, 22f, palette.onLoss, FontWeight.Bold, Anchor.MIDDLE)
        optionalRow(palette, measurer, labels.score, 130f)
        optionalRow(palette, measurer, labels.partner, 154f)
        optionalRow(palette, measurer, labels.photo, 178f)
        box(palette.green, 20f, 222f, 114f, 26f, 13f)
        label(measurer, labels.save, 77f, 239f, 11f, palette.onGreen, FontWeight.SemiBold, Anchor.MIDDLE)
    }
}

private fun DrawScope.drawStatsScene(
    palette: IntroPalette,
    measurer: TextMeasurer,
    labels: IntroArtLabels,
) {
    translate(65f, 104f) {
        shadow(palette, 0f, 0f, 260f, 159f, 20f)
        box(palette.card, 0f, 0f, 260f, 159f, 20f)
        boxOutline(palette.cardBorder, 0f, 0f, 260f, 159f, 20f, 1.5f)
        label(measurer, labels.month, 16f, 28f, 14f, palette.ink, FontWeight.Bold)
        label(measurer, labels.monthRecord, 244f, 28f, 13f, palette.muted, FontWeight.SemiBold, Anchor.END)
        val cellWidth = (228f - 30f) / 7f
        CalendarLevels.forEachIndexed { index, level ->
            val column = index % 7
            val row = index / 7
            box(
                color = calendarColor(palette, level),
                x = 16f + column * (cellWidth + 5f),
                y = 44f + row * 19f,
                width = cellWidth,
                height = 14f,
                radius = 3f,
            )
        }
        label(measurer, labels.winRate, 16f, 138f, 12f, palette.muted, FontWeight.Normal)
        box(palette.emptyCell, 72f, 131f, 56f, 6f, 3f)
        box(palette.green, 72f, 131f, 41f, 6f, 3f)
        label(measurer, labels.winRateValue, 166f, 138f, 12f, palette.ink, FontWeight.Bold, Anchor.END)
    }
    translate(55f, 95f) {
        drawCircle(palette.card, 25f, Offset.Zero)
        drawCircle(palette.flameSoft, 22f, Offset.Zero)
        withTransform({ scale(0.7f, 0.7f, Offset.Zero) }) {
            drawPath(FlameOuter, palette.flameDeep)
            drawPath(FlameInner, palette.flame)
        }
    }
    val barColors = listOf(palette.mintSoft, palette.mint, palette.greenMid, palette.green)
    BarHeights.forEachIndexed { index, barHeight ->
        val x = 238f + index * 27f
        val y = 306f - barHeight
        box(palette.card, x - 3f, y - 3f, 27f, barHeight + 6f, 7f)
        box(barColors[index], x, y, 21f, barHeight, 4f)
    }
    translate(226f, 188f) {
        strokePath(TrendLine, palette.flame, 3.5f)
        strokePath(TrendHead, palette.flame, 3.5f)
    }
}

private fun calendarColor(
    palette: IntroPalette,
    level: Int,
): Color =
    when (level) {
        1 -> palette.mintSoft
        2 -> palette.greenMid
        3 -> palette.green
        else -> palette.emptyCell
    }

private fun DrawScope.drawHeart(
    color: Color,
    x: Float,
    y: Float,
    size: Float,
) {
    withTransform(
        {
            translate(x, y)
            scale(size / 24f, size / 24f, Offset.Zero)
        },
    ) {
        drawPath(Heart, color)
    }
}

private fun DrawScope.drawShareScene(
    palette: IntroPalette,
    measurer: TextMeasurer,
    labels: IntroArtLabels,
) {
    drawHeart(palette.flame, 58f, 116f, 38f)
    drawHeart(palette.greenMid, 96f, 74f, 22f)
    withTransform({ rotate(-5f, Offset(195f, 194f)) }) {
        translate(110f, 74f) {
            shadow(palette, 0f, 0f, 170f, 240f, 20f)
            box(palette.card, 0f, 0f, 170f, 240f, 20f)
            boxOutline(palette.cardBorder, 0f, 0f, 170f, 240f, 20f, 1.5f)
            box(palette.mint, 10f, 10f, 150f, 116f, 12f)
            drawCircle(palette.flame, 9f, Offset(137f, 31f))
            drawPath(CourtShape, palette.green)
            box(palette.mintSoft, 84f, 54f, 2f, 72f, 0f)
            box(palette.greenDeep, 18f, 74f, 134f, 3f, 1f)
            box(palette.green, 10f, 134f, 24f, 24f, 6f)
            label(measurer, labels.win, 22f, 151f, 12f, palette.onGreen, FontWeight.Bold, Anchor.MIDDLE)
            label(measurer, labels.cardScore, 42f, 153f, 24f, palette.ink, FontWeight.Bold)
            label(measurer, labels.cardMeta, 10f, 175f, 10f, palette.muted, FontWeight.Medium)
            box(palette.emptyCell, 10f, 186f, 110f, 5f, 2.5f)
            box(palette.emptyCell, 10f, 199f, 80f, 5f, 2.5f)
            label(measurer, labels.wordmark, 10f, 225f, 10f, palette.green, FontWeight.Bold)
        }
    }
    translate(320f, 244f) {
        drawCircle(palette.green.copy(alpha = 0.18f), 30f, Offset(0f, 6f))
        drawCircle(palette.green, 26f, Offset.Zero)
        line(palette.onGreen, Offset(-4.5f, -1.4f), Offset(4.5f, -6.7f), 2.4f)
        line(palette.onGreen, Offset(-4.5f, 1.4f), Offset(4.5f, 6.7f), 2.4f)
        drawCircle(palette.onGreen, 3f, Offset(-7.2f, 0f), style = Stroke(width = 2.4f))
        drawCircle(palette.onGreen, 3f, Offset(7.2f, -8.4f), style = Stroke(width = 2.4f))
        drawCircle(palette.onGreen, 3f, Offset(7.2f, 8.4f), style = Stroke(width = 2.4f))
    }
}

package com.maxeydev.picklejot.ui.match.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.Match
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.domain.person.Person
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.ResultBadge
import com.maxeydev.picklejot.ui.match.currentLocale
import com.maxeydev.picklejot.ui.match.durationShort
import com.maxeydev.picklejot.ui.match.formatLabel
import com.maxeydev.picklejot.ui.match.formatMatchDate
import com.maxeydev.picklejot.ui.match.formatMatchTime
import com.maxeydev.picklejot.ui.match.playerInitials
import com.maxeydev.picklejot.ui.match.resultLabel
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTheme
import java.io.File
import java.util.Locale
import kotlin.time.Duration

private val DETAIL_PHOTO_SIZE = 120.dp
private val SUMMARY_BADGE_SIZE = 48.dp
private val GAME_BADGE_SIZE = 24.dp
private val AVATAR_SIZE = 40.dp
private val INFO_ICON_SIZE = 20.dp
private val INFO_LABEL_WIDTH = 72.dp
private val INFO_ROW_MIN_HEIGHT = 56.dp
private val PLAYER_ROW_MIN_HEIGHT = 64.dp
private val GAME_ROW_MIN_HEIGHT = 52.dp
private val GAME_LABEL_MIN_WIDTH = 28.dp
private val GAME_NUMBER_MIN_WIDTH = 28.dp
private val PROGRESS_BAR_HEIGHT = 8.dp
private val INFO_DIVIDER_INSET = 48.dp
private val PLAYER_DIVIDER_INSET = 68.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    state: MatchDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { DetailTitle(matchNumber = state.matchNumber) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    DetailAction(
                        icon = R.drawable.ic_share,
                        description = stringResource(R.string.action_share),
                        enabled = state.match != null,
                        onClick = onShare,
                        modifier = Modifier.testTag(MatchDetailTestTags.SHARE),
                    )
                    DetailAction(
                        icon = R.drawable.ic_edit,
                        description = stringResource(R.string.action_edit),
                        enabled = state.match != null,
                        onClick = onEdit,
                        modifier = Modifier.testTag(MatchDetailTestTags.EDIT),
                    )
                    DetailAction(
                        icon = R.drawable.ic_delete,
                        description = stringResource(R.string.action_delete),
                        enabled = state.match != null,
                        onClick = onDeleteRequested,
                        modifier = Modifier.testTag(MatchDetailTestTags.DELETE),
                    )
                },
            )
        },
    ) { innerPadding ->
        state.match?.let { match ->
            MatchDetailContent(
                match = match,
                duration = state.duration,
                endsNextDay = state.endsNextDay,
                photoPaths = state.photoPaths,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
    if (state.isConfirmingDelete) {
        DeleteConfirmationDialog(onConfirm = onDeleteConfirmed, onDismiss = onDeleteDismissed)
    }
}

@Composable
private fun DetailTitle(matchNumber: Int) {
    val title = stringResource(R.string.detail_title)
    val text =
        if (matchNumber > 0) {
            "$title ${stringResource(R.string.detail_match_number, matchNumber)}"
        } else {
            title
        }
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun DetailAction(
    icon: Int,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MatchDetailContent(
    match: Match,
    duration: Duration?,
    endsNextDay: Boolean,
    photoPaths: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = PicklejotSpacing.gutter,
                    end = PicklejotSpacing.gutter,
                    top = PicklejotSpacing.sm,
                    bottom = PicklejotSpacing.xxl,
                ),
        verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.xl),
    ) {
        SummaryCard(match = match, duration = duration, endsNextDay = endsNextDay)
        if (match.games.isNotEmpty()) {
            GamesSection(games = match.games.sortedBy { it.gameNumber })
        }
        if (match.partner != null || match.opponents.isNotEmpty()) {
            PlayersSection(partner = match.partner, opponents = match.opponents)
        }
        match.notes?.let { notes ->
            DetailSection(title = stringResource(R.string.label_notes)) {
                BorderedCard {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(PicklejotSpacing.lg),
                    )
                }
            }
        }
        if (photoPaths.isNotEmpty()) {
            DetailSection(
                title = stringResource(R.string.label_photos),
                trailing = pluralStringResource(R.plurals.photo_count, photoPaths.size, photoPaths.size),
            ) {
                DetailPhotos(photoPaths)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    match: Match,
    duration: Duration?,
    endsNextDay: Boolean,
) {
    val locale = currentLocale()
    val isWin = match.result == MatchResult.WIN
    val stripColor = if (isWin) PicklejotTheme.colors.winRow else MaterialTheme.colorScheme.surfaceContainer
    val rows =
        listOfNotNull(
            InfoEntry(R.drawable.ic_calendar, stringResource(R.string.label_date), formatMatchDate(match.date, locale)),
            timeRangeText(match, endsNextDay, locale)?.let {
                InfoEntry(R.drawable.ic_schedule, stringResource(R.string.label_time), it)
            },
            duration?.let { InfoEntry(R.drawable.ic_timer, stringResource(R.string.label_played), durationShort(it)) },
            match.location?.let { InfoEntry(R.drawable.ic_location, stringResource(R.string.detail_where), it) },
            match.paddle?.let { InfoEntry(R.drawable.ic_paddle, stringResource(R.string.label_paddle), it) },
        )
    BorderedCard(shape = MaterialTheme.shapes.large) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(stripColor)
                        .padding(horizontal = PicklejotSpacing.lg, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.lg),
            ) {
                ResultBadge(
                    result = match.result,
                    size = SUMMARY_BADGE_SIZE,
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text =
                        stringResource(
                            R.string.home_match_headline,
                            resultLabel(match.result),
                            formatLabel(match.format),
                        ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
            }
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(
                        color = PicklejotTheme.colors.cardBorder,
                        modifier = Modifier.padding(start = INFO_DIVIDER_INSET),
                    )
                }
                InfoRow(icon = row.icon, label = row.label, value = row.value)
            }
        }
    }
}

private class InfoEntry(
    val icon: Int,
    val label: String,
    val value: String,
)

@Composable
private fun timeRangeText(
    match: Match,
    endsNextDay: Boolean,
    locale: Locale,
): String? {
    val start = match.startTime?.let { formatMatchTime(it, locale) }
    val end = match.endTime?.let { formatMatchTime(it, locale) }
    val range =
        when {
            start != null && end != null -> stringResource(R.string.detail_time_range, start, end)
            else -> start ?: end
        }
    return if (range != null && endsNextDay) stringResource(R.string.time_range_next_day, range) else range
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoRow(
    icon: Int,
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = INFO_ROW_MIN_HEIGHT)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = PicklejotSpacing.lg, vertical = PicklejotSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.md),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(INFO_ICON_SIZE),
        )
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.md),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = INFO_LABEL_WIDTH),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun GamesSection(games: List<GameScore>) {
    val gamesWon = games.count { it.myScore > it.opponentScore }
    DetailSection(
        title = stringResource(R.string.detail_games_title),
        trailing = stringResource(R.string.detail_games_won, gamesWon, games.size).takeIf { games.size > 1 },
    ) {
        BorderedCard(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(vertical = PicklejotSpacing.xs)) {
                GameColumnHeader()
                games.forEachIndexed { index, game ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = PicklejotTheme.colors.cardBorder,
                            modifier = Modifier.padding(horizontal = PicklejotSpacing.lg),
                        )
                    }
                    GameRow(game)
                }
            }
        }
    }
}

@Composable
private fun GameColumnHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { }
                .padding(horizontal = PicklejotSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(modifier = Modifier.widthIn(min = GAME_LABEL_MIN_WIDTH))
        Text(
            text = stringResource(R.string.detail_column_you),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = GAME_NUMBER_MIN_WIDTH),
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.opponent_score),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = GAME_NUMBER_MIN_WIDTH),
        )
        Spacer(modifier = Modifier.size(GAME_BADGE_SIZE))
    }
}

@Composable
private fun GameRow(game: GameScore) {
    val isWon = game.myScore > game.opponentScore
    val total = game.myScore + game.opponentScore
    val fraction = if (total > 0) game.myScore.toFloat() / total else 0f
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.score_line, game.gameNumber, game.myScore, game.opponentScore)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = GAME_ROW_MIN_HEIGHT)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(horizontal = PicklejotSpacing.lg, vertical = PicklejotSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.detail_game_short, game.gameNumber),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.widthIn(min = GAME_LABEL_MIN_WIDTH),
        )
        GameNumber(value = game.myScore, isBold = isWon)
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(PROGRESS_BAR_HEIGHT)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(colors.outlineVariant),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(colors.onSurfaceVariant),
            )
        }
        GameNumber(value = game.opponentScore, isBold = !isWon)
        ResultBadge(
            result = if (isWon) MatchResult.WIN else MatchResult.LOSS,
            size = GAME_BADGE_SIZE,
            shape = MaterialTheme.shapes.extraSmall,
            textStyle = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun GameNumber(
    value: Int,
    isBold: Boolean,
) {
    Text(
        text = value.toString(),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(min = GAME_NUMBER_MIN_WIDTH),
    )
}

@Composable
private fun PlayersSection(
    partner: Person?,
    opponents: List<Person>,
) {
    DetailSection(title = stringResource(R.string.label_players)) {
        BorderedCard(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(bottom = PicklejotSpacing.xs)) {
                if (partner != null) {
                    PlayerGroupHeading(text = stringResource(R.string.players_heading_partner))
                    PlayerRow(name = partner.displayName, isPartner = true)
                    if (opponents.isNotEmpty()) {
                        HorizontalDivider(color = PicklejotTheme.colors.cardBorder)
                    }
                }
                if (opponents.isNotEmpty()) {
                    PlayerGroupHeading(
                        text =
                            stringResource(
                                if (opponents.size > 1) {
                                    R.string.players_heading_opponents
                                } else {
                                    R.string.players_heading_opponent
                                },
                            ),
                    )
                    opponents.forEachIndexed { index, opponent ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = PicklejotTheme.colors.cardBorder,
                                modifier = Modifier.padding(start = PLAYER_DIVIDER_INSET),
                            )
                        }
                        PlayerRow(name = opponent.displayName, isPartner = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerGroupHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = PicklejotSpacing.lg, end = PicklejotSpacing.lg, top = PicklejotSpacing.md)
                .semantics { heading() },
    )
}

@Composable
private fun PlayerRow(
    name: String,
    isPartner: Boolean,
) {
    val colors = PicklejotTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = PLAYER_ROW_MIN_HEIGHT)
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = PicklejotSpacing.lg, vertical = PicklejotSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.md),
    ) {
        Surface(
            shape = CircleShape,
            color = if (isPartner) colors.countBadge else colors.lossBadge,
            contentColor = if (isPartner) colors.onCountBadge else colors.onLossBadge,
            modifier = Modifier.size(AVATAR_SIZE),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = playerInitials(name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PicklejotSpacing.xs),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            trailing?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        content()
    }
}

@Composable
private fun BorderedCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
        content = content,
    )
}

@Composable
private fun DetailPhotos(photoPaths: List<String>) {
    var viewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).testTag(MatchDetailTestTags.PHOTOS),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        photoPaths.forEachIndexed { index, path ->
            AsyncImage(
                model = File(path),
                contentDescription = stringResource(R.string.photo_position, index + 1, photoPaths.size),
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(DETAIL_PHOTO_SIZE)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClickLabel = stringResource(R.string.photo_view)) { viewerIndex = index },
            )
        }
    }
    viewerIndex?.let { index ->
        PhotoViewer(photoPaths = photoPaths, startIndex = index, onDismiss = { viewerIndex = null })
    }
}

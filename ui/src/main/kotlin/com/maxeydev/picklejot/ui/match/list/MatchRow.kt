package com.maxeydev.picklejot.ui.match.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.match.GameScore
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.MatchThumbnail
import com.maxeydev.picklejot.ui.common.ResultBadge
import com.maxeydev.picklejot.ui.match.currentLocale
import com.maxeydev.picklejot.ui.match.formatLabel
import com.maxeydev.picklejot.ui.match.formatMatchDateLong
import com.maxeydev.picklejot.ui.match.formatMatchDateShort
import com.maxeydev.picklejot.ui.match.resultLabel
import com.maxeydev.picklejot.ui.match.todayInDeviceZone
import com.maxeydev.picklejot.ui.theme.PicklejotTheme
import java.util.Locale

private val ROW_MIN_HEIGHT = 84.dp
private val DATE_COLUMN_MIN_WIDTH = 48.dp

@Composable
fun MatchRow(
    state: MatchRowUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = currentLocale()
    val sentence = matchRowSentence(state, locale)
    val cardShape = MaterialTheme.shapes.large
    val rowColor =
        if (state.result == MatchResult.WIN) {
            PicklejotTheme.colors.winRow
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(rowColor)
                .border(width = 1.dp, color = PicklejotTheme.colors.cardBorder, shape = cardShape)
                .heightIn(min = ROW_MIN_HEIGHT)
                .clickable(onClickLabel = stringResource(R.string.open_match_details), onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = sentence }
                .testTag(MatchListTestTags.row(state.id))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ResultColumn(state = state, locale = locale)
        MatchRowText(state = state, modifier = Modifier.weight(1f))
        state.thumbnailPath?.let { path ->
            MatchThumbnail(path = path, modifier = Modifier.testTag(MatchListTestTags.THUMBNAIL))
        }
        if (state.games.isNotEmpty()) {
            ScoreColumn(games = state.games, modifier = Modifier.testTag(MatchListTestTags.SCORES))
        }
    }
}

@Composable
private fun ResultColumn(
    state: MatchRowUiState,
    locale: Locale,
) {
    val today = remember { todayInDeviceZone() }
    val dateText =
        if (state.date == today) {
            stringResource(R.string.list_date_today)
        } else {
            formatMatchDateShort(state.date, today, locale)
        }
    Column(
        modifier = Modifier.widthIn(min = DATE_COLUMN_MIN_WIDTH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ResultBadge(result = state.result, modifier = Modifier.testTag(MatchListTestTags.RESULT_BADGE))
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.testTag(MatchListTestTags.ROW_DATE),
        )
    }
}

@Composable
private fun MatchRowText(
    state: MatchRowUiState,
    modifier: Modifier = Modifier,
) {
    val format = formatLabel(state.format)
    val versus = versusLine(state.opponentNames)
    val details =
        when {
            state.partnerName != null -> stringResource(R.string.list_format_with_partner, format, state.partnerName)
            versus != null -> format
            else -> null
        }
    Column(
        modifier = modifier.testTag(MatchListTestTags.TEXT_COLUMN),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = versus ?: stringResource(R.string.list_match_untitled, format),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(MatchListTestTags.HEADLINE),
        )
        if (details != null) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(MatchListTestTags.DETAILS),
            )
        }
        if (state.location != null) {
            Text(
                text = state.location,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(MatchListTestTags.LOCATION),
            )
        }
    }
}

@Composable
private fun versusLine(opponentNames: List<String>): String? =
    when (opponentNames.size) {
        0 -> null
        1 -> stringResource(R.string.list_versus_one, opponentNames[0])
        else -> stringResource(R.string.list_versus_two, opponentNames[0], opponentNames[1])
    }

@Composable
private fun ScoreColumn(
    games: List<GameScore>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        games.forEach { game ->
            val isGameWon = game.myScore > game.opponentScore
            Text(
                text = stringResource(R.string.list_score, game.myScore, game.opponentScore),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isGameWon) FontWeight.Bold else FontWeight.Normal,
                color =
                    if (isGameWon) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                textAlign = TextAlign.End,
                modifier = Modifier.testTag(MatchListTestTags.SCORE_ENTRY),
            )
        }
    }
}

@Composable
private fun matchRowSentence(
    state: MatchRowUiState,
    locale: Locale,
): String {
    val separator = stringResource(R.string.a11y_row_separator)
    val opponents =
        when (state.opponentNames.size) {
            0 -> null
            1 -> stringResource(R.string.a11y_against_one, state.opponentNames[0])
            else -> stringResource(R.string.a11y_against_two, state.opponentNames[0], state.opponentNames[1])
        }
    val partner = state.partnerName?.let { stringResource(R.string.a11y_with_partner, it) }
    val location = state.location?.let { stringResource(R.string.a11y_at_location, it) }
    val scores =
        if (state.games.isEmpty()) {
            null
        } else {
            val spokenGames =
                state.games
                    .map { game -> stringResource(R.string.a11y_score, game.myScore, game.opponentScore) }
                    .joinToString(separator)
            stringResource(R.string.a11y_scores, spokenGames)
        }
    val photo =
        if (state.thumbnailPath == null) {
            null
        } else {
            stringResource(R.string.a11y_with_photo)
        }
    return listOfNotNull(
        resultLabel(state.result),
        formatLabel(state.format),
        formatMatchDateLong(state.date, locale),
        opponents,
        partner,
        location,
        scores,
        photo,
    ).joinToString(separator)
}

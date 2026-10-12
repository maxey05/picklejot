package com.maxeydev.picklejot.ui.match.edit

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.match.MatchFormat
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.AddPill
import com.maxeydev.picklejot.ui.common.AutocompleteField
import com.maxeydev.picklejot.ui.common.PillChip
import com.maxeydev.picklejot.ui.common.SegmentedToggle
import com.maxeydev.picklejot.ui.match.formatLabel
import com.maxeydev.picklejot.ui.match.resultLabel
import com.maxeydev.picklejot.ui.paywall.UpgradePrompt
import com.maxeydev.picklejot.ui.paywall.UpgradeReason
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import com.maxeydev.picklejot.ui.theme.PicklejotTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

private val ICON_SIZE = 20.dp
private val MESSAGE_ICON_SIZE = 18.dp
private val MIN_TOUCH_TARGET = 48.dp
private const val FIELDS_EXPAND_MILLIS = 260
private const val SPINNER_DELAY_MILLIS = 300L

private val FieldsExpandSpec: FiniteAnimationSpec<IntSize> =
    tween(durationMillis = FIELDS_EXPAND_MILLIS, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchEditScreen(
    state: MatchEditUiState,
    actions: MatchEditActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) R.string.edit_title_existing else R.string.edit_title_new,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = actions.onClose) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = actions.onSave,
                        enabled = state.isSaveTappable,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier.padding(end = 12.dp).testTag(MatchEditTestTags.SAVE),
                    ) {
                        Text(text = stringResource(R.string.action_save), style = MaterialTheme.typography.labelLarge)
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            DelayedSpinner(modifier = Modifier.fillMaxSize().padding(innerPadding))
        } else {
            MatchEditForm(
                state = state,
                actions = actions,
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
            )
        }
        state.upgradePrompt?.let { reason ->
            UpgradePrompt(
                reason = reason,
                onDismiss = actions.onUpgradePromptDismissed,
                onSeePro = actions.onSeePro.takeIf { reason != UpgradeReason.PRO_PHOTO_LIMIT },
            )
        }
    }
}

@Composable
private fun DelayedSpinner(modifier: Modifier = Modifier) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SPINNER_DELAY_MILLIS)
        isVisible = true
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isVisible) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun MatchEditForm(
    state: MatchEditUiState,
    actions: MatchEditActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = PicklejotSpacing.gutter,
                    end = PicklejotSpacing.gutter,
                    top = PicklejotSpacing.sm,
                    bottom = PicklejotSpacing.xxl,
                ),
        verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.md),
    ) {
        SetupCard(state = state, actions = actions)
        PlayersCard(state = state, actions = actions)
        ScoresCard(
            games = state.games,
            onGameAdded = actions.onGameAdded,
            onGameRemoved = actions.onGameRemoved,
            onGameScoresChanged = actions.onGameScoresChanged,
        )
        FormCard {
            SectionLabel(icon = R.drawable.ic_photo_camera, text = stringResource(R.string.label_photos))
            PhotoPickerSection(
                photos = state.photos,
                hasPhotoError = state.hasPhotoError,
                actions = actions.photoActions,
            )
        }
        LocationCard(state = state, actions = actions)
        NotesCard(notes = state.notes, onNotesChanged = actions.onNotesChanged)
    }
}

@Composable
private fun FormCard(
    modifier: Modifier = Modifier,
    spacing: Dp = PicklejotSpacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(PicklejotSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

@Composable
private fun SectionLabel(
    @DrawableRes icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    tag: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().semantics { heading() },
        horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (isError) colors.error else colors.primary,
            modifier = Modifier.size(ICON_SIZE),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isError) colors.error else colors.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (tag != null) {
            Surface(shape = CircleShape, color = colors.tertiaryContainer, contentColor = colors.onTertiaryContainer) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = PicklejotSpacing.sm, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun FieldMessage(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (isError) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.padding(top = 1.dp).size(MESSAGE_ICON_SIZE),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) colors.error else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun SetupCard(
    state: MatchEditUiState,
    actions: MatchEditActions,
) {
    FormCard(spacing = PicklejotSpacing.lg) {
        Column(verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm)) {
            SectionLabel(icon = R.drawable.ic_players, text = stringResource(R.string.label_format))
            SegmentedToggle(
                options = listOf(MatchFormat.SINGLES, MatchFormat.DOUBLES),
                selected = state.format,
                optionLabel = { format -> formatLabel(format) },
                optionTag = { format ->
                    if (format == MatchFormat.SINGLES) {
                        MatchEditTestTags.FORMAT_SINGLES
                    } else {
                        MatchEditTestTags.FORMAT_DOUBLES
                    }
                },
                onSelected = actions.onFormatSelected,
            )
            if (state.clearedOnFormatSwitch) {
                FieldMessage(text = stringResource(R.string.format_switch_cleared))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm)) {
            SectionLabel(
                icon = R.drawable.ic_trophy,
                text = stringResource(R.string.label_result),
                isError = state.showResultError,
                tag = stringResource(R.string.label_result_required).takeIf { state.result == null },
            )
            SegmentedToggle(
                options = listOf(MatchResult.WIN, MatchResult.LOSS),
                selected = state.result,
                optionLabel = { result -> resultLabel(result) },
                optionTag = { result ->
                    if (result == MatchResult.WIN) MatchEditTestTags.RESULT_WIN else MatchEditTestTags.RESULT_LOSS
                },
                onSelected = actions.onResultSelected,
                isError = state.showResultError,
            )
            if (state.showResultError) {
                FieldMessage(
                    text = stringResource(R.string.result_required_message),
                    isError = true,
                    modifier = Modifier.testTag(MatchEditTestTags.RESULT_ERROR),
                )
            }
            state.advisory?.let { advisory ->
                FieldMessage(
                    text =
                        when (advisory) {
                            MatchResult.WIN -> stringResource(R.string.advisory_suggests_win)
                            MatchResult.LOSS -> stringResource(R.string.advisory_suggests_loss)
                        },
                )
            }
        }
        state.date?.let { date ->
            Column(verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.xs)) {
                SectionLabel(icon = R.drawable.ic_calendar, text = stringResource(R.string.label_when))
                DateTimeField(
                    date = date,
                    startTime = state.startTime,
                    endTime = state.endTime,
                    onDateSelected = actions.onDateSelected,
                    onStartTimeChanged = actions.onStartTimeChanged,
                    onEndTimeChanged = actions.onEndTimeChanged,
                )
            }
        }
    }
}

@Composable
private fun PlayersCard(
    state: MatchEditUiState,
    actions: MatchEditActions,
) {
    val partnerSlots = state.personSlots.filter { it.slot == PersonSlot.PARTNER }
    val opponentSlots = state.personSlots.filter { it.slot != PersonSlot.PARTNER }
    FormCard {
        SectionLabel(icon = R.drawable.ic_person, text = stringResource(R.string.label_players))
        Column(
            modifier = Modifier.animateContentSize(animationSpec = FieldsExpandSpec),
            verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.lg),
        ) {
            if (partnerSlots.isNotEmpty()) {
                PlayerGroup(
                    heading = stringResource(R.string.players_heading_partner),
                    slots = partnerSlots,
                    state = state,
                    actions = actions,
                )
            }
            PlayerGroup(
                heading = stringResource(R.string.players_heading_opponents),
                slots = opponentSlots,
                state = state,
                actions = actions,
            )
        }
    }
}

@Composable
private fun PlayerGroup(
    heading: String,
    slots: List<PersonSlotUiState>,
    state: MatchEditUiState,
    actions: MatchEditActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.md)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        slots.forEach { slot ->
            val target = SuggestionTarget.forSlot(slot.slot)
            PersonSlotField(
                slot = slot,
                format = state.format,
                suggestions = state.suggestionsFor(target),
                onNameChanged = { name -> actions.onPersonNameChanged(slot.slot, name) },
                onSuggestionSelected = { suggestion -> actions.onSuggestionSelected(target, suggestion) },
                onFocusChanged = { isFocused -> actions.onSuggestionFocusChanged(target, isFocused) },
            )
        }
    }
}

@Composable
private fun ScoresCard(
    games: List<GameScoreRowUiState>,
    onGameAdded: () -> Unit,
    onGameRemoved: (Int) -> Unit,
    onGameScoresChanged: (Int, String, String) -> Unit,
) {
    FormCard {
        SectionLabel(icon = R.drawable.ic_scoreboard, text = stringResource(R.string.label_scores))
        games.forEachIndexed { index, row ->
            GameScoreRow(
                row = row,
                onScoresChanged = { myScore, opponentScore ->
                    onGameScoresChanged(index, myScore, opponentScore)
                },
                onRemove = { onGameRemoved(index) },
            )
        }
        AddPill(
            label = stringResource(R.string.add_game_pill),
            description = stringResource(R.string.add_game),
            onClick = onGameAdded,
            modifier = Modifier.testTag(MatchEditTestTags.ADD_GAME),
        )
    }
}

@Composable
private fun LocationCard(
    state: MatchEditUiState,
    actions: MatchEditActions,
) {
    FormCard {
        SectionLabel(icon = R.drawable.ic_location, text = stringResource(R.string.label_location))
        AutocompleteField(
            value = state.location,
            onValueChange = actions.onLocationChanged,
            suggestions = state.suggestionsFor(SuggestionTarget.LOCATION),
            onSuggestionSelected = { actions.onSuggestionSelected(SuggestionTarget.LOCATION, it) },
            onFocusChanged = { actions.onSuggestionFocusChanged(SuggestionTarget.LOCATION, it) },
            label = stringResource(R.string.location_hint),
            fieldTestTag = MatchEditTestTags.LOCATION,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
        )
        RecentLocations(
            recent = state.recentLocations,
            current = state.location,
            onPicked = actions.onLocationChanged,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentLocations(
    recent: List<String>,
    current: String,
    onPicked: (String) -> Unit,
) {
    val choices = recent.filterNot { it.equals(current.trim(), ignoreCase = true) }
    if (choices.isEmpty()) {
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.location_recent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        choices.forEach { place ->
            PillChip(
                label = place,
                isSelected = false,
                onClick = { onPicked(place) },
            )
        }
    }
}

@Composable
private fun NotesCard(
    notes: String,
    onNotesChanged: (String) -> Unit,
) {
    val placeholders = stringArrayResource(R.array.notes_placeholders)
    val placeholderIndex = rememberSaveable { Random.nextInt(placeholders.size) }
    FormCard {
        SectionLabel(icon = R.drawable.ic_notes, text = stringResource(R.string.label_notes))
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChanged,
            placeholder = { Text(placeholders[placeholderIndex % placeholders.size]) },
            minLines = 3,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().testTag(MatchEditTestTags.NOTES),
        )
    }
}

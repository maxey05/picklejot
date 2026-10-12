package com.maxeydev.picklejot.ui.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.share.CardDetail
import com.maxeydev.picklejot.domain.share.CardFormat
import com.maxeydev.picklejot.domain.share.CardLayout
import com.maxeydev.picklejot.domain.share.CardRatio
import com.maxeydev.picklejot.domain.share.CardTheme
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.SegmentedToggle
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val CONTROL_HEIGHT = 50.dp
private val ROW_HEIGHT = 56.dp
private val CHEVRON_SIZE = 20.dp

@Composable
fun VariantPicker(
    format: CardFormat,
    layout: CardLayout,
    hasPhoto: Boolean,
    isPro: Boolean,
    hiddenDetails: Set<CardDetail>,
    availableDetails: Set<CardDetail>,
    actions: VariantPickerActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag(ShareTestTags.VARIANT_PICKER),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledControl(label = stringResource(R.string.card_ratio_label), modifier = Modifier.weight(1f)) {
                RatioSegments(selected = format.ratio, onSelected = actions.onRatioSelected)
            }
            LabeledControl(label = stringResource(R.string.card_theme_label), modifier = Modifier.weight(1f)) {
                StyleDropdown(selected = format.theme, isPro = isPro, onSelected = actions.onThemeSelected)
            }
        }
        val details = CardDetail.entries.filter { it in availableDetails }
        if (details.isNotEmpty() || hasPhoto) {
            ShowOnCard(
                details = details,
                hiddenDetails = hiddenDetails,
                hasPhoto = hasPhoto,
                showsPhoto = layout == CardLayout.PHOTO,
                actions = actions,
            )
        }
    }
}

@Composable
private fun LabeledControl(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        content()
    }
}

@Composable
private fun RatioSegments(
    selected: CardRatio,
    onSelected: (CardRatio) -> Unit,
) {
    SegmentedToggle(
        options = CardRatio.entries,
        selected = selected,
        optionLabel = { ratio -> stringResource(ratioLabel(ratio)) },
        optionTag = { ratio -> ShareTestTags.ratio(ratio) },
        onSelected = onSelected,
    )
}

@Composable
private fun StyleDropdown(
    selected: CardTheme,
    isPro: Boolean,
    onSelected: (CardTheme) -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedName = stringResource(themeLabel(selected))
    val description = stringResource(R.string.card_style_button, selectedName)
    Box {
        Surface(
            onClick = { isExpanded = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = CONTROL_HEIGHT)
                    .testTag(ShareTestTags.STYLE_BUTTON)
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                    },
        ) {
            Row(
                modifier = Modifier.heightIn(min = CONTROL_HEIGHT).padding(start = 16.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = selectedName, style = MaterialTheme.typography.labelLarge)
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    modifier = Modifier.size(CHEVRON_SIZE),
                )
            }
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            CardTheme.entries.forEach { theme ->
                DropdownMenuItem(
                    text = { Text(themeText(theme, isPro)) },
                    onClick = {
                        isExpanded = false
                        onSelected(theme)
                    },
                    trailingIcon =
                        if (theme == selected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            null
                        },
                    modifier = Modifier.testTag(ShareTestTags.theme(theme)),
                )
            }
        }
    }
}

@Composable
private fun ShowOnCard(
    details: List<CardDetail>,
    hiddenDetails: Set<CardDetail>,
    hasPhoto: Boolean,
    showsPhoto: Boolean,
    actions: VariantPickerActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.card_show_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() },
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
        ) {
            Column {
                details.forEachIndexed { index, detail ->
                    if (index > 0) {
                        InsetDivider()
                    }
                    SwitchRow(
                        label = stringResource(detailLabel(detail)),
                        isChecked = detail !in hiddenDetails,
                        onCheckedChange = { actions.onDetailShownChanged(detail, it) },
                        modifier = Modifier.testTag(ShareTestTags.detail(detail)),
                    )
                }
                if (hasPhoto) {
                    if (details.isNotEmpty()) {
                        InsetDivider()
                    }
                    SwitchRow(
                        label = stringResource(R.string.card_detail_photo),
                        isChecked = showsPhoto,
                        onCheckedChange = { shown ->
                            actions.onLayoutSelected(if (shown) CardLayout.PHOTO else CardLayout.NO_PHOTO)
                        },
                        modifier = Modifier.testTag(ShareTestTags.PHOTO_SWITCH),
                    )
                }
            }
        }
    }
}

@Composable
private fun InsetDivider() {
    HorizontalDivider(color = PicklejotTheme.colors.cardBorder, modifier = Modifier.padding(start = 16.dp))
}

@Composable
private fun SwitchRow(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ROW_HEIGHT)
                .toggleable(value = isChecked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = isChecked, onCheckedChange = null)
    }
}

private fun detailLabel(detail: CardDetail): Int =
    when (detail) {
        CardDetail.GAME_SCORES -> R.string.card_detail_game_scores
        CardDetail.OPPONENTS -> R.string.card_detail_opponents
        CardDetail.PARTNER -> R.string.card_detail_partner
        CardDetail.LOCATION -> R.string.card_detail_location
    }

private fun ratioLabel(ratio: CardRatio): Int =
    when (ratio) {
        CardRatio.TALL -> R.string.card_ratio_tall
        CardRatio.SQUARE -> R.string.card_ratio_square
    }

private fun themeLabel(theme: CardTheme): Int =
    when (theme) {
        CardTheme.DARK -> R.string.card_theme_dark
        CardTheme.LIGHT -> R.string.card_theme_light
        CardTheme.COURT -> R.string.card_theme_court
        CardTheme.SUNSET -> R.string.card_theme_sunset
    }

@Composable
private fun themeText(
    theme: CardTheme,
    isPro: Boolean,
): String {
    val name = stringResource(themeLabel(theme))
    return if (theme.requiresPro && !isPro) stringResource(R.string.card_theme_pro_locked, name) else name
}

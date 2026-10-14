package com.maxeydev.picklejot.ui.settings

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.maxeydev.picklejot.domain.entitlement.FreeTier
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing

private const val DRAWER_WIDTH_FRACTION = 0.84f
private const val SCRIM_ALPHA = 0.48f
private val DRAWER_MAX_WIDTH = 360.dp
private val DRAWER_SHAPE = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
private val PRO_ROW_MIN_HEIGHT = 72.dp
private val PRO_ICON_SIZE = 24.dp

@Composable
fun SettingsDrawerHost(
    open: Boolean,
    onDismiss: () -> Unit,
    drawer: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BackHandler(enabled = open, onBack = onDismiss)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val drawerWidth = min(maxWidth * DRAWER_WIDTH_FRACTION, DRAWER_MAX_WIDTH)
        content()
        AnimatedVisibility(visible = open, enter = fadeIn(), exit = fadeOut()) {
            val closeLabel = stringResource(R.string.settings_close)
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = closeLabel,
                            onClick = onDismiss,
                        ).testTag(SettingsTestTags.SCRIM),
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = slideInHorizontally { fullWidth -> fullWidth },
            exit = slideOutHorizontally { fullWidth -> fullWidth },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            drawer(Modifier.width(drawerWidth).fillMaxHeight())
        }
    }
}

@Composable
fun SettingsDrawer(
    state: SettingsUiState,
    versionName: String,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.settings_title)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = DRAWER_SHAPE,
        shadowElevation = 12.dp,
        modifier = modifier.semantics { paneTitle = title }.testTag(SettingsTestTags.SCREEN),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End),
                    ),
        ) {
            DrawerHeader(title = title, onClose = actions.onClose)
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = PicklejotSpacing.lg, end = PicklejotSpacing.lg, bottom = PicklejotSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PicklejotSpacing.md),
            ) {
                ProCard(hasPro = state.hasPro, savedMatches = state.savedMatches, onSeePro = actions.onSeePro)
                SettingsCard {
                    NameRow(
                        displayName = state.displayName,
                        nameDraft = state.nameDraft,
                        canSave = state.canSaveName,
                        onNameChanged = actions.onNameChanged,
                        onSave = actions.onSaveName,
                        onCancel = actions.onNameEditCancelled,
                    )
                    DarkModeRow(
                        darkTheme = state.darkTheme,
                        onDarkThemeChanged = actions.onDarkThemeChanged,
                        showDivider = true,
                    )
                    SoundEffectsRow(
                        enabled = state.soundEffects,
                        onEnabledChanged = actions.onSoundEffectsChanged,
                        showDivider = true,
                    )
                    ReminderToggle(
                        enabled = state.reminderEnabled,
                        onEnable = actions.onEnableReminder,
                        onDisable = actions.onDisableReminder,
                        showDivider = true,
                    )
                    if (state.reminderEnabled) {
                        ReminderTimeRow(time = state.reminderTime, onTimeChanged = actions.onReminderTimeChanged)
                    }
                }
                DataCard(state = state, actions = actions)
                InfoCard(versionName = versionName, actions = actions)
                EraseDataSection(
                    isErasing = state.isErasing,
                    eraseFailed = state.eraseFailed,
                    savedMatches = state.savedMatches,
                    onOpenBackup = actions.onOpenBackup,
                    onEraseConfirmed = actions.onEraseConfirmed,
                    onEraseFailureDismissed = actions.onEraseFailureDismissed,
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    title: String,
    onClose: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = PicklejotSpacing.xl, end = PicklejotSpacing.sm, top = PicklejotSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        IconButton(onClick = onClose, modifier = Modifier.testTag(SettingsTestTags.CLOSE)) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.settings_close),
            )
        }
    }
}

@Composable
private fun ProCard(
    hasPro: Boolean,
    savedMatches: Int,
    onSeePro: () -> Unit,
) {
    val clickModifier =
        if (hasPro) {
            Modifier
        } else {
            Modifier.clickable(role = Role.Button, onClick = onSeePro).testTag(SettingsTestTags.SEE_PRO)
        }
    Surface(
        shape = SETTINGS_CARD_SHAPE,
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag(SettingsTestTags.PRO_CARD),
    ) {
        Row(
            modifier =
                clickModifier
                    .heightIn(min = PRO_ROW_MIN_HEIGHT)
                    .padding(horizontal = PicklejotSpacing.lg, vertical = PicklejotSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.lg),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_crown),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(PRO_ICON_SIZE),
            )
            Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { }) {
                Text(
                    text = stringResource(R.string.settings_pro_heading),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        if (hasPro) {
                            stringResource(R.string.settings_pro_row_active)
                        } else {
                            stringResource(
                                R.string.settings_pro_row_free,
                                savedMatches.coerceAtMost(FreeTier.MATCH_LIMIT),
                                FreeTier.MATCH_LIMIT,
                            )
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(SettingsTestTags.PRO_STATUS),
                )
            }
            if (!hasPro) {
                TrailingChevron()
            }
        }
    }
}

@Composable
private fun DataCard(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    val context = LocalContext.current
    SettingsCard {
        SettingsRow(
            icon = R.drawable.ic_upload,
            label = stringResource(R.string.settings_backup_row),
            navigates = true,
            modifier =
                Modifier
                    .clickable(role = Role.Button, onClick = actions.onOpenBackup)
                    .testTag(SettingsTestTags.BACKUP_MORE_INFO),
        )
        SettingsRow(
            icon = R.drawable.ic_delete,
            label = stringResource(R.string.settings_clear_cache),
            value = state.cacheBytes?.let { bytes -> Formatter.formatShortFileSize(context, bytes) },
            valueModifier = Modifier.testTag(SettingsTestTags.CACHE_SIZE),
            showDivider = true,
            modifier =
                Modifier
                    .clickable(enabled = !state.isClearingCache, role = Role.Button, onClick = actions.onClearCache)
                    .testTag(SettingsTestTags.CLEAR_CACHE),
        )
    }
}

@Composable
private fun InfoCard(
    versionName: String,
    actions: SettingsActions,
) {
    SettingsCard {
        SettingsRow(
            icon = R.drawable.ic_shield,
            label = stringResource(R.string.settings_privacy),
            navigates = true,
            modifier =
                Modifier
                    .clickable(role = Role.Button, onClick = actions.onOpenPrivacy)
                    .testTag(SettingsTestTags.PRIVACY_OPEN),
        )
        SettingsRow(
            icon = R.drawable.ic_notes,
            label = stringResource(R.string.settings_terms),
            navigates = true,
            showDivider = true,
            modifier =
                Modifier
                    .clickable(role = Role.Button, onClick = actions.onOpenTerms)
                    .testTag(SettingsTestTags.TERMS_OPEN),
        )
        SettingsRow(
            icon = R.drawable.ic_star,
            label = stringResource(R.string.settings_rate_us),
            showDivider = true,
            modifier =
                Modifier
                    .clickable(role = Role.Button, onClick = actions.onRateUs)
                    .testTag(SettingsTestTags.RATE_US),
            trailing = { TrailingIcon(R.drawable.ic_open_in_new) },
        )
        SettingsRow(
            icon = R.drawable.ic_heart,
            label = stringResource(R.string.settings_support),
            navigates = true,
            showDivider = true,
            modifier =
                Modifier
                    .clickable(role = Role.Button, onClick = actions.onOpenSupport)
                    .testTag(SettingsTestTags.SUPPORT_OPEN),
        )
        SettingsRow(
            icon = R.drawable.ic_info,
            label = stringResource(R.string.settings_about),
            value = stringResource(R.string.settings_version_short, versionName),
            navigates = true,
            showDivider = true,
            modifier =
                Modifier
                    .clickable(role = Role.Button, onClick = actions.onOpenAbout)
                    .testTag(SettingsTestTags.ABOUT_OPEN),
        )
    }
}

package com.maxeydev.picklejot.ui.settings

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.maxeydev.picklejot.ui.R

@Composable
fun SoundEffectsRow(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    SettingsRow(
        icon = R.drawable.ic_sound_effects,
        label = stringResource(R.string.settings_sound_effects_label),
        showDivider = showDivider,
        modifier =
            modifier
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChanged)
                .testTag(SettingsTestTags.SOUND_EFFECTS_TOGGLE),
    ) {
        Switch(checked = enabled, onCheckedChange = null)
    }
}

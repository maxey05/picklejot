package com.maxeydev.picklejot.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.backup.ExportPromptReason
import com.maxeydev.picklejot.ui.R

@Composable
fun ExportPromptBanner(
    reason: ExportPromptReason?,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (reason == null) {
        return
    }
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().testTag(BackupTestTags.PROMPT_BANNER),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    text =
                        when (reason) {
                            ExportPromptReason.MATCHES_ADDED -> stringResource(R.string.export_prompt_matches_added)
                            ExportPromptReason.PERIODIC -> stringResource(R.string.export_prompt_periodic)
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                TextButton(
                    onClick = onExport,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(BackupTestTags.PROMPT_ACTION),
                ) {
                    Text(stringResource(R.string.export_prompt_action))
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.testTag(BackupTestTags.PROMPT_DISMISS)) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.export_prompt_dismiss),
                )
            }
        }
    }
}

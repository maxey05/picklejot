package com.maxeydev.picklejot.ui.settings

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.ui.R
import java.time.ZoneId

@Composable
fun LastExportText(
    lastExportAt: AppInstant?,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val locale = LocalConfiguration.current.locales[0]
    val text =
        if (lastExportAt == null) {
            stringResource(R.string.backup_never)
        } else {
            formatBackupDate(lastExportAt, ZoneId.systemDefault(), locale)
        }
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier.testTag(BackupTestTags.LAST_EXPORT),
    )
}

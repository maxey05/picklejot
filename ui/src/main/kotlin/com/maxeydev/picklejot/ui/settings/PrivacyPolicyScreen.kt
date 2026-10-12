package com.maxeydev.picklejot.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private class PolicySection(
    @StringRes val heading: Int,
    @StringRes val body: Int,
)

private val POLICY_SECTIONS =
    listOf(
        PolicySection(R.string.privacy_stores_heading, R.string.privacy_stores_body),
        PolicySection(R.string.privacy_permissions_heading, R.string.privacy_permissions_body),
        PolicySection(R.string.privacy_google_heading, R.string.privacy_google_body),
        PolicySection(R.string.privacy_exports_heading, R.string.privacy_exports_body),
        PolicySection(R.string.privacy_delete_heading, R.string.privacy_delete_body),
        PolicySection(R.string.privacy_children_heading, R.string.privacy_children_body),
        PolicySection(R.string.privacy_changes_heading, R.string.privacy_changes_body),
    )

private val SUMMARY_POINTS =
    listOf(
        R.string.privacy_point_account,
        R.string.privacy_point_tracking,
        R.string.privacy_point_sharing,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(PrivacyTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SummaryCard()
            Text(
                text = stringResource(R.string.privacy_effective),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier = Modifier.testTag(PrivacyTestTags.SECTIONS),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                POLICY_SECTIONS.forEach { section -> PolicySectionCard(section) }
            }
        }
    }
}

@Composable
private fun SummaryCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PicklejotTheme.colors.header,
        contentColor = PicklejotTheme.colors.onHeader,
        modifier = Modifier.fillMaxWidth().testTag(PrivacyTestTags.SUMMARY),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.privacy_summary),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            SUMMARY_POINTS.forEach { point ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = PicklejotTheme.colors.headerAccent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(point),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PicklejotTheme.colors.onHeaderMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun PolicySectionCard(section: PolicySection) {
    Surface(
        shape = SETTINGS_CARD_SHAPE,
        color = settingsCardColor(),
        border = BorderStroke(1.dp, PicklejotTheme.colors.cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(section.heading),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(section.body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

package com.maxeydev.picklejot.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.AnimatedMascot
import com.maxeydev.picklejot.ui.common.Mascot

private val HERO_SHAPE = RoundedCornerShape(20.dp)
private val HERO_MASCOT_SIZE = 96.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    hasPro: Boolean,
    onBack: () -> Unit,
    onSeePro: () -> Unit,
    onRateUs: () -> Unit,
    onShareApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(SupportTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.support_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
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
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SupportHero()
            Text(
                text = stringResource(R.string.support_ways_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp).semantics { heading() },
            )
            SettingsCard {
                if (hasPro) {
                    SettingsRow(
                        icon = R.drawable.ic_heart,
                        label = stringResource(R.string.support_pro_thanks_title),
                        supporting = stringResource(R.string.support_pro_thanks_supporting),
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag(SupportTestTags.PRO_THANKS),
                    )
                } else {
                    SettingsRow(
                        icon = R.drawable.ic_crown,
                        label = stringResource(R.string.support_pro_title),
                        supporting = stringResource(R.string.support_pro_supporting),
                        iconTint = MaterialTheme.colorScheme.primary,
                        navigates = true,
                        modifier =
                            Modifier
                                .clickable(role = Role.Button, onClick = onSeePro)
                                .testTag(SupportTestTags.PRO),
                    )
                }
                SettingsRow(
                    icon = R.drawable.ic_star,
                    label = stringResource(R.string.support_rate_title),
                    supporting = stringResource(R.string.support_rate_supporting),
                    showDivider = true,
                    modifier =
                        Modifier
                            .clickable(role = Role.Button, onClick = onRateUs)
                            .testTag(SupportTestTags.RATE),
                    trailing = { TrailingIcon(R.drawable.ic_open_in_new) },
                )
                SettingsRow(
                    icon = R.drawable.ic_share,
                    label = stringResource(R.string.support_share_title),
                    supporting = stringResource(R.string.support_share_supporting),
                    showDivider = true,
                    modifier =
                        Modifier
                            .clickable(role = Role.Button, onClick = onShareApp)
                            .testTag(SupportTestTags.SHARE),
                )
                SettingsRow(
                    icon = R.drawable.ic_heart,
                    label = stringResource(R.string.support_donate_title),
                    iconTint = MaterialTheme.colorScheme.primary,
                    showDivider = true,
                    modifier = Modifier.testTag(SupportTestTags.DONATE),
                )
            }
        }
    }
}

@Composable
private fun SupportHero() {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = HERO_SHAPE,
        color = colors.primaryContainer,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth().testTag(SupportTestTags.HERO),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimatedMascot(mascot = Mascot.HEAD_JOY, modifier = Modifier.size(HERO_MASCOT_SIZE))
            Text(
                text = stringResource(R.string.support_hero_heading),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.support_hero_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

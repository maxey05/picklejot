package com.maxeydev.picklejot.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.paywall.UpgradePrompt
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val PREVIEW_HEIGHT = 360.dp
private val SHARE_BUTTON_HEIGHT = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePreviewScreen(
    state: SharePreviewUiState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    variantActions: VariantPickerActions,
    onDismissUpgrade: () -> Unit,
    onSeePro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    state.upgradeReason?.let { reason ->
        UpgradePrompt(reason = reason, onDismiss = onDismissUpgrade, onSeePro = onSeePro)
    }
    Scaffold(
        modifier = modifier.testTag(ShareTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.share_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() },
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
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PreviewPane(state = state, onRetry = onRetry)
                VariantPicker(
                    format = state.format,
                    layout = state.layout,
                    hasPhoto = state.hasPhoto,
                    isPro = state.isPro,
                    hiddenDetails = state.hiddenDetails,
                    availableDetails = state.availableDetails,
                    actions = variantActions,
                )
                if (state.hasShareFailed) {
                    Text(
                        text = stringResource(R.string.share_send_failed),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            HorizontalDivider(color = PicklejotTheme.colors.cardBorder)
            Button(
                onClick = onShare,
                enabled = state.canShare,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
                        .heightIn(min = SHARE_BUTTON_HEIGHT)
                        .testTag(ShareTestTags.SHARE),
            ) {
                Icon(painter = painterResource(R.drawable.ic_share), contentDescription = null)
                Text(text = stringResource(R.string.share_action), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun PreviewPane(
    state: SharePreviewUiState,
    onRetry: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val card = state.card
            when {
                card != null -> {
                    val image = remember(card) { card.asImageBitmap() }
                    val description = stringResource(R.string.share_preview_description, state.cardDescription)
                    Image(
                        bitmap = image,
                        contentDescription = description,
                        contentScale = ContentScale.Fit,
                        modifier =
                            Modifier
                                .aspectRatio(card.width.toFloat() / card.height)
                                .clip(MaterialTheme.shapes.medium)
                                .testTag(ShareTestTags.PREVIEW),
                    )
                }
                state.hasFailed ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.testTag(ShareTestTags.FAILED),
                    ) {
                        Text(
                            text = stringResource(R.string.share_failed),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        OutlinedButton(onClick = onRetry, modifier = Modifier.testTag(ShareTestTags.RETRY)) {
                            Text(stringResource(R.string.share_retry))
                        }
                    }
                else ->
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .testTag(ShareTestTags.RENDERING)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                    )
            }
        }
    }
}

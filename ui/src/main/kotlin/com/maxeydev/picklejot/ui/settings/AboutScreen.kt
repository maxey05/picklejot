package com.maxeydev.picklejot.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.AnimatedMascot
import com.maxeydev.picklejot.ui.common.Mascot
import com.maxeydev.picklejot.ui.dashboard.motionScale
import com.maxeydev.picklejot.ui.sound.Cue
import com.maxeydev.picklejot.ui.sound.LocalSoundEffects
import com.maxeydev.picklejot.ui.theme.PicklejotFonts
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val HERO_SHAPE = RoundedCornerShape(20.dp)
private val CHECK_BADGE_SIZE = 22.dp
private val CHECK_ICON_SIZE = 14.dp
private val MASCOT_TOUCH_SIZE = 112.dp
private val MASCOT_SIZE = 104.dp
private const val MASCOT_POP_SCALE = 1.1f
private const val MASCOT_POP_UP_MILLIS = 140
private const val MASCOT_POP_DOWN_MILLIS = 260

private val MASCOT_FACES =
    listOf(
        Mascot.HEAD_SMILE,
        Mascot.HEAD_JOY,
        Mascot.HEAD_WINK,
        Mascot.HEAD_CHEER,
    )

private val WORDMARK_STYLE =
    TextStyle(
        fontFamily = PicklejotFonts.wordmark,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        textAlign = TextAlign.Center,
    )

private val PRIVACY_POINTS =
    listOf(
        R.string.about_privacy_point_account,
        R.string.about_privacy_point_device,
        R.string.about_privacy_point_tracking,
        R.string.about_privacy_point_network,
        R.string.about_privacy_point_permissions,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(AboutTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about_title),
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
            AboutHero(versionName = versionName)
            PrivacyCard()
            LicencesCard()
        }
    }
}

@Composable
private fun AboutHero(versionName: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = HERO_SHAPE,
        color = colors.primaryContainer,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PettableMascot()
            Text(
                text = stringResource(R.string.about_app_name),
                style = WORDMARK_STYLE,
                color = colors.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.about_version, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.testTag(AboutTestTags.VERSION),
            )
        }
    }
}

@Composable
private fun PettableMascot() {
    var pets by rememberSaveable { mutableIntStateOf(0) }
    val sound = LocalSoundEffects.current
    val scale = remember { Animatable(1f) }
    val description = stringResource(R.string.about_mascot_description)
    LaunchedEffect(pets) {
        if (pets > 0 && motionScale() > 0f) {
            scale.animateTo(MASCOT_POP_SCALE, tween(durationMillis = MASCOT_POP_UP_MILLIS))
            scale.animateTo(1f, tween(durationMillis = MASCOT_POP_DOWN_MILLIS))
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(MASCOT_TOUCH_SIZE)
                .clip(CircleShape)
                .clickable(role = Role.Button) {
                    sound.play(Cue.DUCK_PETS[pets % Cue.DUCK_PETS.size])
                    pets++
                }
                .semantics { contentDescription = description }
                .testTag(AboutTestTags.MASCOT),
    ) {
        AnimatedMascot(
            mascot = MASCOT_FACES[pets % MASCOT_FACES.size],
            modifier =
                Modifier
                    .size(MASCOT_SIZE)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    },
        )
    }
}

@Composable
private fun PrivacyCard() {
    SettingsCard(modifier = Modifier.testTag(AboutTestTags.PRIVACY)) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.about_privacy_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            PRIVACY_POINTS.forEach { point ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    CheckBadge()
                    Text(
                        text = stringResource(point),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckBadge() {
    Box(
        modifier =
            Modifier
                .size(CHECK_BADGE_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(CHECK_ICON_SIZE),
        )
    }
}

@Composable
private fun LicencesCard() {
    SettingsCard {
        Column(modifier = Modifier.padding(bottom = 4.dp).testTag(AboutTestTags.LICENCES)) {
            Text(
                text = stringResource(R.string.about_licences_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier =
                    Modifier
                        .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp)
                        .semantics { heading() },
            )
            OPEN_SOURCE_LIBRARIES.forEachIndexed { index, library ->
                if (index > 0) {
                    HorizontalDivider(color = PicklejotTheme.colors.cardBorder)
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(text = library.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = library.license,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

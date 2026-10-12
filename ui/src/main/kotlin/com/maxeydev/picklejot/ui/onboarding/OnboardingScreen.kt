package com.maxeydev.picklejot.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.common.DisplayNameField
import com.maxeydev.picklejot.ui.sound.Cue
import com.maxeydev.picklejot.ui.sound.LocalSoundEffects
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private const val PAGE_SLIDE_MILLIS = 420
private const val ILLUSTRATION_PARALLAX = 0.118f
private const val SHAKE_MILLIS = 380

private val PageSlideSpec: AnimationSpec<Float> =
    tween(durationMillis = PAGE_SLIDE_MILLIS, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))

private val ShakeEasing = CubicBezierEasing(0.36f, 0.07f, 0.19f, 0.97f)

private val ShakeKeys =
    listOf(0f to 0f, 0.15f to -9f, 0.35f to 8f, 0.55f to -5f, 0.75f to 3f, 1f to 0f)

private fun shakeOffset(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    val end = ShakeKeys.indexOfFirst { it.first >= clamped }.coerceAtLeast(1)
    val (startAt, startValue) = ShakeKeys[end - 1]
    val (endAt, endValue) = ShakeKeys[end]
    return lerp(startValue, endValue, ShakeEasing.transform((clamped - startAt) / (endAt - startAt)))
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onNameChanged: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
) {
    val pages = IntroPage.entries
    val pagerState = rememberPagerState(initialPage = initialPage) { pages.size }
    val scope = rememberCoroutineScope()
    val sound = LocalSoundEffects.current
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            Cue.ONBOARDING_STEPS.getOrNull(page)?.let { cue -> sound.play(cue) }
        }
    }
    val scrollStates = pages.map { rememberScrollState() }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val showPage: (Int) -> Unit =
        { target -> scope.launch { pagerState.animateScrollToPage(target, animationSpec = PageSlideSpec) } }
    BackHandler(enabled = pagerState.currentPage > 0) { showPage(pagerState.currentPage - 1) }
    Scaffold(
        modifier = modifier.testTag(OnboardingTestTags.SCREEN),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .imePadding(),
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                IntroBackdrop(
                    pagerState = pagerState,
                    scrollStates = scrollStates,
                    topInset = topInset,
                    modifier = Modifier.fillMaxSize(),
                )
                HorizontalPager(
                    state = pagerState,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .testTag(OnboardingTestTags.PAGER),
                    verticalAlignment = Alignment.Top,
                ) { index ->
                    IntroPageContent(
                        page = pages[index],
                        state = state,
                        scrollState = scrollStates[index],
                        topInset = topInset,
                        pageOffset = { index - (pagerState.currentPage + pagerState.currentPageOffsetFraction) },
                        onNameChanged = onNameChanged,
                        onContinue = onContinue,
                    )
                }
            }
            IntroFooter(
                pagerState = pagerState,
                state = state,
                onNext = { showPage(pagerState.currentPage + 1) },
                onSkip = { showPage(pages.lastIndex) },
                onContinue = onContinue,
            )
        }
    }
}

@Composable
private fun IntroPageContent(
    page: IntroPage,
    state: OnboardingUiState,
    scrollState: ScrollState,
    topInset: Dp,
    pageOffset: () -> Float,
    onNameChanged: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val shake = remember { Animatable(0f) }
    val sound = LocalSoundEffects.current
    LaunchedEffect(state.nameRequiredAttempts) {
        if (state.nameRequiredAttempts > 0) {
            sound.play(Cue.NAME_REQUIRED)
            shake.snapTo(0f)
            shake.animateTo(1f, tween(durationMillis = SHAKE_MILLIS, easing = LinearEasing))
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val illustrationHeight = introIllustrationHeight(page, maxHeight, topInset)
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IntroIllustration(
                page = page,
                topInset = topInset,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(illustrationHeight)
                        .graphicsLayer { translationX = pageOffset() * size.width * ILLUSTRATION_PARALLAX },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(page.title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .semantics { heading() }
                            .testTag(OnboardingTestTags.title(page)),
                )
                Text(
                    text = introBody(page),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (page == IntroPage.PRIVACY) {
                Spacer(modifier = Modifier.height(20.dp))
                DisplayNameField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    onDone = onContinue,
                    modifier =
                        Modifier
                            .offset { IntOffset(shakeOffset(shake.value).dp.roundToPx(), 0) }
                            .padding(horizontal = 24.dp)
                            .widthIn(max = 280.dp)
                            .testTag(OnboardingTestTags.NAME_FIELD),
                    required = false,
                    labelAbove = true,
                    showRequiredError = state.showNameRequired,
                )
                if (state.saveFailed) {
                    Text(
                        text = stringResource(R.string.onboarding_save_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier =
                            Modifier
                                .padding(horizontal = 24.dp)
                                .testTag(OnboardingTestTags.SAVE_ERROR),
                    )
                }
            }
        }
    }
}

@Composable
private fun introBody(page: IntroPage): AnnotatedString {
    val lead = page.lead?.let { stringResource(it) }
    val body = stringResource(page.body)
    val leadColor = MaterialTheme.colorScheme.onSurface
    return buildAnnotatedString {
        if (lead != null) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = leadColor)) { append(lead) }
            append(" ")
        }
        append(body)
    }
}

@Composable
private fun IntroFooter(
    pagerState: PagerState,
    state: OnboardingUiState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    val onLastPage = pagerState.currentPage == pagerState.pageCount - 1
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PageIndicator(
            pageCount = pagerState.pageCount,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(bottom = 20.dp),
        )
        if (onLastPage) {
            val readyColors = ButtonDefaults.buttonColors()
            val colors =
                if (state.canContinue) {
                    readyColors
                } else {
                    readyColors.copy(
                        containerColor = readyColors.disabledContainerColor,
                        contentColor = readyColors.disabledContentColor,
                    )
                }
            Button(
                onClick = onContinue,
                enabled = !state.isSaving,
                colors = colors,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag(OnboardingTestTags.CONTINUE),
            ) {
                Text(text = stringResource(R.string.onboarding_continue), fontWeight = FontWeight.Bold)
            }
            Text(
                text = stringResource(R.string.onboarding_backup_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .padding(top = 14.dp)
                        .testTag(OnboardingTestTags.BACKUP_NOTE),
            )
        } else {
            Button(
                onClick = onNext,
                modifier =
                    Modifier
                        .widthIn(min = 220.dp)
                        .heightIn(min = 52.dp)
                        .testTag(OnboardingTestTags.NEXT),
            ) {
                Text(text = stringResource(R.string.onboarding_next), fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onSkip,
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .widthIn(min = 96.dp)
                        .heightIn(min = 48.dp)
                        .testTag(OnboardingTestTags.SKIP),
            ) {
                Text(text = stringResource(R.string.onboarding_skip), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.onboarding_page_indicator, currentPage + 1, pageCount)
    Row(
        modifier =
            modifier
                .semantics(mergeDescendants = true) { contentDescription = description }
                .testTag(OnboardingTestTags.PAGE_INDICATOR),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 22.dp else 8.dp,
                label = "introIndicatorWidth",
            )
            Box(
                modifier =
                    Modifier
                        .size(width = width, height = 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
            )
        }
    }
}

package com.maxeydev.picklejot.ui.match.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotSpacing
import kotlinx.coroutines.launch
import java.io.File

private const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val OPEN_SCALE = 0.85f
private const val OPEN_MILLIS = 300
private const val OPEN_FADE_MILLIS = 150
private const val DISABLED_ALPHA = 0.38f
private const val INACTIVE_DOT_ALPHA = 0.4f
private val DISMISS_DRAG_DISTANCE = 96.dp
private val DOT_SIZE = 8.dp
private val OpenEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
internal fun PhotoViewer(
    photoPaths: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, photoPaths.lastIndex)) { photoPaths.size }
    var isZoomed by remember { mutableStateOf(false) }
    var isShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isShown = true }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .dismissOnDragDown(isEnabled = !isZoomed, onDismiss = onDismiss),
        ) {
            AnimatedVisibility(
                visible = isShown,
                enter =
                    fadeIn(tween(OPEN_FADE_MILLIS)) +
                        scaleIn(initialScale = OPEN_SCALE, animationSpec = tween(OPEN_MILLIS, easing = OpenEasing)),
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isZoomed,
                    key = { index -> photoPaths[index] },
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    ZoomablePhoto(
                        path = photoPaths[page],
                        description = stringResource(R.string.photo_position, page + 1, photoPaths.size),
                        onZoomChanged = { zoomed -> isZoomed = zoomed },
                    )
                }
            }
            ViewerControls(
                pagerState = pagerState,
                photoCount = photoPaths.size,
                onClose = onDismiss,
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            )
        }
    }
}

private fun Modifier.dismissOnDragDown(
    isEnabled: Boolean,
    onDismiss: () -> Unit,
): Modifier =
    pointerInput(isEnabled) {
        if (isEnabled) {
            val threshold = DISMISS_DRAG_DISTANCE.toPx()
            var total = 0f
            detectVerticalDragGestures(
                onDragStart = { total = 0f },
                onVerticalDrag = { _, delta -> total += delta },
                onDragEnd = {
                    if (total > threshold) {
                        onDismiss()
                    }
                    total = 0f
                },
                onDragCancel = { total = 0f },
            )
        }
    }

@Composable
private fun ZoomablePhoto(
    path: String,
    description: String,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val transformState =
        rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
            val maxX = containerSize.width * (newScale - 1f) / 2f
            val maxY = containerSize.height * (newScale - 1f) / 2f
            offset =
                if (newScale == 1f) {
                    Offset.Zero
                } else {
                    Offset(
                        x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                        y = (offset.y + panChange.y).coerceIn(-maxY, maxY),
                    )
                }
            scale = newScale
        }
    LaunchedEffect(scale) { onZoomChanged(scale > 1f) }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                .transformable(state = transformState, canPan = { scale > 1f })
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = DOUBLE_TAP_ZOOM
                            }
                        },
                    )
                },
    ) {
        AsyncImage(
            model = File(path),
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
        )
    }
}

@Composable
private fun ViewerControls(
    pagerState: PagerState,
    photoCount: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val current = pagerState.currentPage
    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.photo_viewer_close),
                    tint = Color.White,
                )
            }
            Text(
                text = stringResource(R.string.photo_viewer_counter, current + 1, photoCount),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(start = PicklejotSpacing.xs).semantics { heading() },
            )
        }
        if (photoCount > 1) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PicklejotSpacing.lg, vertical = PicklejotSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PagerStepButton(
                    icon = R.drawable.ic_chevron_left,
                    description = stringResource(R.string.photo_viewer_previous),
                    isEnabled = current > 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(current - 1) } },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PicklejotSpacing.sm)) {
                    repeat(photoCount) { index ->
                        val alpha = if (index == current) 1f else INACTIVE_DOT_ALPHA
                        Box(
                            modifier =
                                Modifier
                                    .size(DOT_SIZE)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = alpha)),
                        )
                    }
                }
                PagerStepButton(
                    icon = R.drawable.ic_chevron_right,
                    description = stringResource(R.string.photo_viewer_next),
                    isEnabled = current < photoCount - 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(current + 1) } },
                )
            }
        }
    }
}

@Composable
private fun PagerStepButton(
    icon: Int,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = isEnabled) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = Color.White.copy(alpha = if (isEnabled) 1f else DISABLED_ALPHA),
        )
    }
}

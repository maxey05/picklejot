package com.maxeydev.picklejot.ui.common

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import java.io.File

val MATCH_THUMBNAIL_SIZE: Dp = 56.dp

@Composable
fun MatchThumbnail(
    path: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { MATCH_THUMBNAIL_SIZE.roundToPx() }
    val request = remember(context, path, sizePx) { thumbnailRequest(context, File(path), sizePx) }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(MATCH_THUMBNAIL_SIZE).clip(MaterialTheme.shapes.small),
    )
}

fun thumbnailRequest(
    context: Context,
    file: File,
    sizePx: Int,
): ImageRequest =
    ImageRequest
        .Builder(context)
        .data(file)
        .size(sizePx, sizePx)
        .build()

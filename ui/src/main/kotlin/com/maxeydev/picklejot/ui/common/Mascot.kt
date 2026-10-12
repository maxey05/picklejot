package com.maxeydev.picklejot.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.maxeydev.picklejot.ui.R

enum class Mascot(
    @DrawableRes val drawableRes: Int,
) {
    READY(R.drawable.mascot_ready),
    SMASH(R.drawable.mascot_smash),
    BALL_BUDDY(R.drawable.mascot_ball_buddy),
    HATCHLING(R.drawable.mascot_hatchling),
    HEAD_SMILE(R.drawable.mascot_head_smile),
    HEAD_JOY(R.drawable.mascot_head_joy),
    HEAD_WINK(R.drawable.mascot_head_wink),
    HEAD_CHEER(R.drawable.mascot_head_cheer),
    HEAD_OOPS(R.drawable.mascot_head_oops),
}

@Composable
fun MascotImage(
    mascot: Mascot,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(mascot.drawableRes),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

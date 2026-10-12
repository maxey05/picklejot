package com.maxeydev.picklejot.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maxeydev.picklejot.domain.match.MatchResult
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.theme.PicklejotTheme

private val DEFAULT_BADGE_SIZE = 40.dp

@Composable
fun ResultBadge(
    result: MatchResult,
    modifier: Modifier = Modifier,
    size: Dp = DEFAULT_BADGE_SIZE,
    shape: Shape = MaterialTheme.shapes.small,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val isWin = result == MatchResult.WIN
    val colors = PicklejotTheme.colors
    Surface(
        color = if (isWin) colors.winBadge else colors.lossBadge,
        contentColor = if (isWin) colors.onWinBadge else colors.onLossBadge,
        shape = shape,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(if (isWin) R.string.result_letter_win else R.string.result_letter_loss),
                style = textStyle,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

package com.maxeydev.picklejot.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.maxeydev.picklejot.ui.common.duck.BallBuddyDuck
import com.maxeydev.picklejot.ui.common.duck.DuckExpression
import com.maxeydev.picklejot.ui.common.duck.DuckHead
import com.maxeydev.picklejot.ui.common.duck.HatchlingDuck
import com.maxeydev.picklejot.ui.common.duck.ReadyDuck
import com.maxeydev.picklejot.ui.common.duck.SmashDuck
import com.maxeydev.picklejot.ui.common.duck.WorkingDuck

@Composable
fun AnimatedMascot(
    mascot: Mascot,
    modifier: Modifier = Modifier,
) {
    when (mascot) {
        Mascot.READY -> ReadyDuck(modifier)
        Mascot.SMASH -> SmashDuck(modifier)
        Mascot.BALL_BUDDY -> BallBuddyDuck(modifier)
        Mascot.HATCHLING -> HatchlingDuck(modifier)
        Mascot.HEAD_SMILE,
        Mascot.HEAD_JOY,
        Mascot.HEAD_WINK,
        Mascot.HEAD_CHEER,
        Mascot.HEAD_OOPS,
        -> DuckHead(expression = mascot.expression(), modifier = modifier)
    }
}

@Composable
fun WorkingMascot(modifier: Modifier = Modifier) {
    WorkingDuck(modifier)
}

private fun Mascot.expression(): DuckExpression =
    when (this) {
        Mascot.HEAD_JOY -> DuckExpression.JOY
        Mascot.HEAD_WINK -> DuckExpression.WINK
        Mascot.HEAD_CHEER -> DuckExpression.CHEER
        Mascot.HEAD_OOPS -> DuckExpression.OOPS
        else -> DuckExpression.SMILE
    }

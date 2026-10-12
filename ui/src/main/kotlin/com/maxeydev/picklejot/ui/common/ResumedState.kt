package com.maxeydev.picklejot.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

@Composable
fun rememberIsResumed(owner: LifecycleOwner): Boolean {
    var isResumed by remember(owner) {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(owner) {
        val observer =
            LifecycleEventObserver { source, _ ->
                isResumed = source.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return isResumed
}

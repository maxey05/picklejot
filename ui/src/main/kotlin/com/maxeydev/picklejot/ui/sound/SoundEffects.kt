package com.maxeydev.picklejot.ui.sound

import androidx.compose.runtime.staticCompositionLocalOf

interface SoundEffects {
    fun play(
        cue: Cue,
        volume: Float = 1f,
    )

    fun preview(cue: Cue)
}

object NoSoundEffects : SoundEffects {
    override fun play(
        cue: Cue,
        volume: Float,
    ) = Unit

    override fun preview(cue: Cue) = Unit
}

val LocalSoundEffects = staticCompositionLocalOf<SoundEffects> { NoSoundEffects }

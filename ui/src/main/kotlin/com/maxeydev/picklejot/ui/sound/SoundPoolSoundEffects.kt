package com.maxeydev.picklejot.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

private const val TAG = "SoundEffects"
private const val MAX_STREAMS = 4
private const val RATE_VARIATION = 0.03f
private const val LOAD_PRIORITY = 1
private const val NO_LOOP = 0
private const val FAILED_STREAM = 0

class SoundPoolSoundEffects(
    private val context: Context,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val random: Random = Random.Default,
) : SoundEffects {
    private val policy = CuePolicy()
    private val soundIds = ConcurrentHashMap<Cue, Int>()
    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
    private val accessibility = context.getSystemService(AccessibilityManager::class.java)

    @Volatile
    private var enabled = true
    private var celebrationStream = FAILED_STREAM

    private val pool: SoundPool by lazy {
        SoundPool
            .Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()
            .also { created ->
                created.setOnLoadCompleteListener { _, soundId, status ->
                    if (status == 0) {
                        loadedSoundIds.add(soundId)
                    } else {
                        Log.w(TAG, "sound $soundId failed to load, status $status")
                    }
                }
            }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun preload() {
        Cue.entries.forEach { cue ->
            soundIds.getOrPut(cue) {
                pool.load(context, cue.resId, LOAD_PRIORITY).also { id ->
                    if (id == FAILED_STREAM) {
                        Log.w(TAG, "cue ${cue.name} could not be queued for loading")
                    }
                }
            }
        }
    }

    override fun play(
        cue: Cue,
        volume: Float,
    ) {
        if (enabled) {
            start(cue, volume)
        }
    }

    override fun preview(cue: Cue) {
        start(cue, 1f)
    }

    private fun start(
        cue: Cue,
        volume: Float,
    ) {
        val soundId = soundIds[cue]
        if (soundId == null) {
            preload()
            return
        }
        if (soundId !in loadedSoundIds) {
            return
        }
        val touchExploration = accessibility?.isTouchExplorationEnabled == true
        val decision = policy.decide(cue, clock(), touchExploration)
        if (decision !is CueDecision.Play) {
            return
        }
        if (decision.stopsActiveCelebration) {
            pool.stop(celebrationStream)
        }
        val rate = if (cue.kind == CueKind.TAP) 1f + (random.nextFloat() * 2f - 1f) * RATE_VARIATION else 1f
        val level = (volume * decision.volume).coerceIn(0f, 1f)
        val stream = pool.play(soundId, level, level, cue.kind.ordinal, NO_LOOP, rate)
        if (stream == FAILED_STREAM) {
            Log.w(TAG, "cue ${cue.name} did not start")
        }
        if (cue.kind == CueKind.CELEBRATION && stream != FAILED_STREAM) {
            celebrationStream = stream
        }
    }
}

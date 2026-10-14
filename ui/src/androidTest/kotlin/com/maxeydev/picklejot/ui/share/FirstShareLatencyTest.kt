package com.maxeydev.picklejot.ui.share

import android.os.Bundle
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val FIRST_SHARE_BUDGET_MILLIS = 1_500L

@RunWith(AndroidJUnit4::class)
class FirstShareLatencyTest {
    private fun report(
        key: String,
        millis: Long,
    ) {
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply { putLong(key, millis) })
        println("AC-10.24 $key=${millis}ms")
    }

    @Test
    fun the_first_share_after_prewarming_renders_within_budget() {
        val warmer = onMain { WebViewWarmer(targetContext).also { it.warm() } }
        try {
            runBlocking { withContext(Dispatchers.Main) { warmer.obtain().awaitTemplateLoaded() } }
            val card = sampleCard(photo = photoDataUri(base = 128, spread = 90, width = 2048, height = 1536))

            val started = SystemClock.elapsedRealtime()
            val result = runBlocking { CardRenderer(warmer).render(card) }
            val elapsed = SystemClock.elapsedRealtime() - started

            report("prewarmedFirstShareMillis", elapsed)
            assertTrue("render failed: $result", result is CardRenderResult.Rendered)
            assertTrue("first share took ${elapsed}ms", elapsed < FIRST_SHARE_BUDGET_MILLIS)
        } finally {
            onMain { warmer.discard() }
        }
    }

    @Test
    fun a_share_with_no_prewarming_still_succeeds_and_is_measured() {
        val warmer = WebViewWarmer(targetContext)
        try {
            val started = SystemClock.elapsedRealtime()
            val result = runBlocking { CardRenderer(warmer).render(sampleCard()) }
            val elapsed = SystemClock.elapsedRealtime() - started

            report("coldShareMillis", elapsed)
            assertTrue("render failed: $result", result is CardRenderResult.Rendered)
        } finally {
            onMain { warmer.discard() }
        }
    }
}

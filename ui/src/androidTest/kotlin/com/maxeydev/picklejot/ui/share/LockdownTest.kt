package com.maxeydev.picklejot.ui.share

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val LOCKDOWN_PAGE = "file:///android_asset/lockdown/index.html"
private const val REMOTE = "https://invalid.picklejot.test/"

@RunWith(AndroidJUnit4::class)
class LockdownTest {
    private lateinit var warmer: WebViewWarmer

    @Before
    fun setUp() {
        warmer = onMain { WebViewWarmer(targetContext, templateUrl = LOCKDOWN_PAGE).also { it.warm() } }
    }

    @After
    fun tearDown() {
        onMain { warmer.discard() }
    }

    @Test
    fun a_page_that_reaches_for_a_remote_url_never_gets_it() {
        val result = runBlocking { CardRenderer(warmer).render(sampleCard()) }

        assertTrue("render failed: $result", result is CardRenderResult.Rendered)
        val outcome = JSONObject((result as CardRenderResult.Rendered).diagnostics)
        listOf("image", "fetch", "xhr").forEach { channel ->
            assertNotEquals("$channel reached the network", "loaded", outcome.getString(channel))
        }
        val blocked = onMain { warmer.obtain().client.blockedUrls.toList() }
        listOf("style.css", "tracker.png", "beacon", "xhr").forEach { path ->
            assertTrue("$path was not intercepted: $blocked", blocked.contains(REMOTE + path))
        }
    }

    @Test
    fun the_card_webview_runs_with_file_and_content_access_off() {
        val settings = onMain { warmer.obtain().webView.settings }

        assertEquals(false, onMain { settings.allowFileAccess })
        assertEquals(false, onMain { settings.allowContentAccess })
        assertEquals(false, onMain { settings.domStorageEnabled })
        assertEquals(100, onMain { settings.textZoom })
        assertEquals(false, onMain { settings.useWideViewPort })
    }

    @Test
    fun the_real_card_renders_with_the_network_unavailable_to_it() {
        val cardWarmer = onMain { WebViewWarmer(targetContext).also { it.warm() } }
        try {
            val result = runBlocking { CardRenderer(cardWarmer).render(sampleCard()) }

            assertTrue("render failed: $result", result is CardRenderResult.Rendered)
            assertTrue(onMain { cardWarmer.obtain().client.blockedUrls.isEmpty() })
        } finally {
            onMain { cardWarmer.discard() }
        }
    }
}

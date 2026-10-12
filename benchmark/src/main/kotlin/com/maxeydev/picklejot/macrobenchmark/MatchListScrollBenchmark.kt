package com.maxeydev.picklejot.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.maxeydev.picklejot.benchmark"
private const val MATCH_LIST_RESOURCE_ID = "match_list"
private const val EXPECTED_MATCH_COUNT = 1_000
private const val ITERATIONS = 5
private const val FLINGS_PER_ITERATION = 8
private const val LIST_TIMEOUT_MILLIS = 15_000L
private const val GESTURE_MARGIN_FRACTION = 5

@RunWith(AndroidJUnit4::class)
class MatchListScrollBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Before
    fun seedOneThousandMatchesWithThumbnails() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val output = device.executeShellCommand("content call --uri content://$TARGET_PACKAGE.seed --method seed")
        check(output.contains("seeded_match_count=$EXPECTED_MATCH_COUNT")) {
            "The 1,000-match seed fixture did not load. The seed call returned: $output"
        }
    }

    @Test
    fun `scroll_1000_matches_fully_compiled`() = scrollMatchList(CompilationMode.Full())

    @Test
    fun `scroll_1000_matches_uncompiled`() = scrollMatchList(CompilationMode.None())

    private fun scrollMatchList(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            iterations = ITERATIONS,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                check(device.wait(Until.hasObject(By.res(MATCH_LIST_RESOURCE_ID)), LIST_TIMEOUT_MILLIS)) {
                    "The match list never appeared, so there was nothing to scroll."
                }
            },
        ) {
            val list = device.findObject(By.res(MATCH_LIST_RESOURCE_ID))
            list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_FRACTION)
            repeat(FLINGS_PER_ITERATION) {
                list.fling(Direction.DOWN)
                device.waitForIdle()
            }
        }
}

package com.maxeydev.picklejot.notification

import android.app.Notification
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.maxeydev.picklejot.ui.R
import com.maxeydev.picklejot.ui.notification.StreakNotification
import com.maxeydev.picklejot.ui.notification.StreakReminderNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreakNotificationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val notifier = StreakReminderNotifier(context)

    private fun build(skipAvailable: Boolean): Notification {
        val notification = notifier.build(skipAvailable)
        assertNotNull(notification)
        return notification!!
    }

    @Test
    fun the_title_and_body_come_from_string_resources() {
        val plain = build(skipAvailable = false)
        val withSkip = build(skipAvailable = true)

        assertEquals(
            context.getString(R.string.streak_notification_title),
            plain.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
        )
        assertEquals(
            context.getString(R.string.streak_notification_body),
            plain.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        )
        assertEquals(
            context.getString(R.string.streak_notification_body_skip),
            withSkip.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        )
    }

    @Test
    fun the_text_carries_no_numbers_and_no_match_content() {
        listOf(false, true).forEach { skipAvailable ->
            val extras = build(skipAvailable).extras
            val text =
                listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT)
                    .mapNotNull { extras.getCharSequence(it)?.toString() }
                    .joinToString(" ")

            assertFalse("notification text contains a digit: $text", text.any(Char::isDigit))
        }
    }

    @Test
    fun the_notification_is_private_on_the_lock_screen_and_uses_the_reminder_channel() {
        val notification = build(skipAvailable = false)

        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertEquals(StreakNotification.CHANNEL_ID, notification.channelId)
        assertNotNull(notification.contentIntent)
    }

    @Test
    fun tapping_opens_the_app_ready_to_log_a_match() {
        val launch = StreakNotification.launchIntent(context)

        assertNotNull(launch)
        assertEquals(context.packageName, launch!!.component?.packageName)
        assertTrue(StreakNotification.wantsLogging(launch))
        assertTrue(launch.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(launch.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(launch.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
    }

    @Test
    fun an_ordinary_launch_does_not_ask_to_log() {
        assertFalse(StreakNotification.wantsLogging(Intent(Intent.ACTION_MAIN)))
        assertFalse(StreakNotification.wantsLogging(null))
    }
}

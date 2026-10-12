package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.reminder.ReminderNotifier

class FakeReminderNotifier(
    private val posts: Boolean = true,
) : ReminderNotifier {
    val notifications = mutableListOf<Boolean>()

    override fun notifyStreakAtRisk(skipAvailable: Boolean): Boolean {
        notifications += skipAvailable
        return posts
    }
}

package com.maxeydev.picklejot.domain.reminder

interface ReminderNotifier {
    fun notifyStreakAtRisk(skipAvailable: Boolean): Boolean
}

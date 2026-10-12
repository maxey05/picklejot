package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.datetime.AppInstant
import com.maxeydev.picklejot.domain.reminder.ReminderScheduling

class FakeReminderScheduling : ReminderScheduling {
    val scheduled = mutableListOf<AppInstant>()
    var cancelCount = 0
        private set

    override fun scheduleNext(fireAt: AppInstant) {
        scheduled += fireAt
    }

    override fun cancel() {
        cancelCount += 1
    }
}

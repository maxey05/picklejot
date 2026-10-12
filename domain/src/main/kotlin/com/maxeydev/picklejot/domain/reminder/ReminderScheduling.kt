package com.maxeydev.picklejot.domain.reminder

import com.maxeydev.picklejot.domain.datetime.AppInstant

interface ReminderScheduling {
    fun scheduleNext(fireAt: AppInstant)

    fun cancel()
}

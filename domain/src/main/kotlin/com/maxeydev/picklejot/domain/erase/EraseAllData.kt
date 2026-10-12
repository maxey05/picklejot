package com.maxeydev.picklejot.domain.erase

import com.maxeydev.picklejot.domain.reminder.StreakReminder

class EraseAllData(
    private val reminder: StreakReminder,
    private val eraser: LocalDataEraser,
) {
    suspend operator fun invoke() {
        reminder.disable()
        eraser.erase()
    }
}

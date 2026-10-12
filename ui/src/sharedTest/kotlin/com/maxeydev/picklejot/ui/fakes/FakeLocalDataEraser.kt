package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.erase.LocalDataEraser

class FakeLocalDataEraser(
    var failure: Exception? = null,
) : LocalDataEraser {
    var eraseCount = 0
        private set

    override suspend fun erase() {
        failure?.let { throw it }
        eraseCount += 1
    }
}

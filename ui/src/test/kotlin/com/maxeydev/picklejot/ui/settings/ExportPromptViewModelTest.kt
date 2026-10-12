@file:OptIn(ExperimentalCoroutinesApi::class)

package com.maxeydev.picklejot.ui.settings

import com.maxeydev.picklejot.domain.backup.ExportPromptReason
import com.maxeydev.picklejot.ui.fakes.FakeBackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ExportPromptViewModelTest {
    private val backup = FakeBackupRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no prompt is shown until the policy raises one`() {
        val viewModel = ExportPromptViewModel(backup)

        assertNull(viewModel.uiState.value.reason)
    }

    @Test
    fun `a raised prompt appears and a dismissal removes it`() {
        val viewModel = ExportPromptViewModel(backup)

        backup.showPrompt(ExportPromptReason.MATCHES_ADDED)
        assertEquals(ExportPromptReason.MATCHES_ADDED, viewModel.uiState.value.reason)

        viewModel.dismiss()

        assertNull(viewModel.uiState.value.reason)
        assertEquals(1, backup.dismissals)
    }
}

package com.maxeydev.picklejot.ui.fakes

import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.domain.match.MatchSortStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMatchSortStore(
    initial: MatchSort = MatchSort.DEFAULT,
) : MatchSortStore {
    private val sort = MutableStateFlow(initial)

    val saved = mutableListOf<MatchSort>()

    override fun observeSort(): Flow<MatchSort> = sort

    override suspend fun saveSort(sort: MatchSort) {
        saved += sort
        this.sort.value = sort
    }
}

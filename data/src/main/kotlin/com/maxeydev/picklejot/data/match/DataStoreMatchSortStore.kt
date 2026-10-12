package com.maxeydev.picklejot.data.match

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.maxeydev.picklejot.domain.match.MatchSort
import com.maxeydev.picklejot.domain.match.MatchSortStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val MATCH_LIST_SORT = stringPreferencesKey("match_list_sort")

class DataStoreMatchSortStore(
    private val dataStore: DataStore<Preferences>,
) : MatchSortStore {
    override fun observeSort(): Flow<MatchSort> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map { preferences ->
                val stored = preferences[MATCH_LIST_SORT]
                MatchSort.entries.firstOrNull { it.name == stored } ?: MatchSort.DEFAULT
            }.distinctUntilChanged()

    override suspend fun saveSort(sort: MatchSort) {
        dataStore.edit { preferences -> preferences[MATCH_LIST_SORT] = sort.name }
    }
}

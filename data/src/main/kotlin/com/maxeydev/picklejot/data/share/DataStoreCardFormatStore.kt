package com.maxeydev.picklejot.data.share

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.maxeydev.picklejot.domain.share.CardFormat
import com.maxeydev.picklejot.domain.share.CardFormatStore
import com.maxeydev.picklejot.domain.share.CardLayout
import com.maxeydev.picklejot.domain.share.CardRatio
import com.maxeydev.picklejot.domain.share.CardTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val CARD_RATIO = stringPreferencesKey("card_ratio")
private val CARD_THEME = stringPreferencesKey("card_theme")
private val CARD_LAYOUT_OVERRIDE = stringPreferencesKey("card_layout_override")

class DataStoreCardFormatStore(
    private val dataStore: DataStore<Preferences>,
) : CardFormatStore {
    override fun observeFormat(): Flow<CardFormat> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map { preferences ->
                CardFormat(
                    ratio = CardRatio.entries.named(preferences[CARD_RATIO]) ?: CardFormat.DEFAULT.ratio,
                    theme = CardTheme.entries.named(preferences[CARD_THEME]) ?: CardFormat.DEFAULT.theme,
                    layoutOverride = CardLayout.entries.named(preferences[CARD_LAYOUT_OVERRIDE]),
                )
            }.distinctUntilChanged()

    override suspend fun saveFormat(format: CardFormat) {
        dataStore.edit { preferences ->
            preferences[CARD_RATIO] = format.ratio.name
            preferences[CARD_THEME] = format.theme.name
            val override = format.layoutOverride
            if (override == null) {
                preferences.remove(CARD_LAYOUT_OVERRIDE)
            } else {
                preferences[CARD_LAYOUT_OVERRIDE] = override.name
            }
        }
    }
}

private fun <T : Enum<T>> List<T>.named(stored: String?): T? = firstOrNull { it.name == stored }

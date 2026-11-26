package com.cinewall.app.model

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrefsOption<T>(
    private val key: Preferences.Key<T>,
    var default: T,
    private val dataStore: DataStore<Preferences>
) {
    fun get(): Flow<T> = dataStore.data.map { it[key] ?: default }

    suspend fun set(value: T)  {
        dataStore.edit { preferences -> preferences[key] = value }
    }
}

fun DataStore<Preferences>.intPrefsOption(key: String, default: Int): PrefsOption<Int> =
    PrefsOption(intPreferencesKey(key), default, this)
fun DataStore<Preferences>.boolPrefsOption(key: String, default: Boolean): PrefsOption<Boolean> =
    PrefsOption(booleanPreferencesKey(key), default, this)

fun DataStore<Preferences>.floatPrefsOption(key: String, default: Float): PrefsOption<Float> =
    PrefsOption(floatPreferencesKey(key), default, this)

fun DataStore<Preferences>.longPrefsOption(key: String, default: Long): PrefsOption<Long> =
    PrefsOption(longPreferencesKey(key), default, this)

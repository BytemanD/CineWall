package com.cinewall.app.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.cinewall.app.model.*

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")


class ConfigRepository(context: Context) {

    val autoSubtitle = context.settingsDataStore.boolPrefsOption("autoSubtitle", false);
    val autoScanAfterStarted = context.settingsDataStore.boolPrefsOption("autoScanAfterStarted", false);
    val autoScrapAfterScan = context.settingsDataStore.boolPrefsOption("autoScrapAfterScan", false);
    val autoScrollDelay = context.settingsDataStore.intPrefsOption("autoScrollDelay", 5000);
}
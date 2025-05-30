package com.genesys.cloud.messenger.sample.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.pushDataStore: DataStore<Preferences> by preferencesDataStore(name = "push_data_store")
package com.genesys.cloud.messenger.sample.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

const val PUSH_NOTIFICATIONS_LOG_TAG = "GMMS_PushNotifications"

val Context.pushDataStore: DataStore<Preferences> by preferencesDataStore(name = "PushNotifications")

val PUSH_NOTIFICATIONS_DEVICE_TOKEN_DATA_KEY = stringPreferencesKey("pushNotificationsDeviceTokenDataKey")
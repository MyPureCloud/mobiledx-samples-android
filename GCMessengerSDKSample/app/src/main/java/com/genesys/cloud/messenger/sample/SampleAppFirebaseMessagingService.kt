package com.genesys.cloud.messenger.sample

import android.util.Log
import androidx.datastore.preferences.core.edit
import com.genesys.cloud.messenger.sample.data.PUSH_NOTIFICATIONS_DEVICE_TOKEN_DATA_KEY
import com.genesys.cloud.messenger.sample.data.PUSH_NOTIFICATIONS_LOG_TAG
import com.genesys.cloud.messenger.sample.data.pushDataStore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking

class SampleAppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(PUSH_NOTIFICATIONS_LOG_TAG, "New token received: $token")
        runBlocking {
            pushDataStore.edit { settings ->
                settings[PUSH_NOTIFICATIONS_DEVICE_TOKEN_DATA_KEY] = token
                Log.d(PUSH_NOTIFICATIONS_LOG_TAG, "Token saved: $token")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(
            PUSH_NOTIFICATIONS_LOG_TAG,
            "onMessageReceived(${message.notification?.title} - ${message.notification?.body})"
        )
    }
}
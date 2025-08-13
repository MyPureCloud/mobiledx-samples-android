package com.genesys.cloud.messenger.sample

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("MissingFirebaseInstanceTokenRefresh") // We access device token on a different way.
class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val PUSH_NOTIFICATION_RECEIVED = "pushNotificationReceived"
        const val EXTRA_KEY_REMOTE_MESSAGE_TITLE = "extraKeyRemoteMessageTitle"
        const val EXTRA_KEY_REMOTE_MESSAGE_BODY = "extraKeyRemoteMessageBody"
        private const val LOG_TAG = "InternalFirebaseMS"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(
            LOG_TAG,
            "onMessageReceived(${message.notification?.title} - ${message.notification?.body})"
        )
        if (isNotGenesysMessage(message.data)) {
            Log.i(LOG_TAG, "Firebase message is not a Genesys message, we ignore this.")
            return
        }
        message.notification?.let { notification ->
            val intent = Intent()
            intent.action = PUSH_NOTIFICATION_RECEIVED
            intent.putExtra(EXTRA_KEY_REMOTE_MESSAGE_TITLE, notification.title)
            intent.putExtra(EXTRA_KEY_REMOTE_MESSAGE_BODY, notification.body)
            sendBroadcast(intent)
            Log.d(LOG_TAG, "Broadcast message sent.")
        }
    }

    private fun isNotGenesysMessage(data: Map<String, String>): Boolean {
        return data["deeplink"] != "genesys-messaging"
    }
}
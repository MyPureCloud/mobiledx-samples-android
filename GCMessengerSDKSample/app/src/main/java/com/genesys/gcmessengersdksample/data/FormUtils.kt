package com.genesys.gcmessengersdksample.data

import android.util.Log
import com.genesys.cloud.integration.messenger.MessengerAccount
import com.genesys.gcmessengersdksample.data.defs.DataKeys
import com.genesys.gcmessengersdksample.presentation.chat_form.ChatFormFragment
import com.google.gson.JsonObject

fun JsonObject.toMessengerAccount(): MessengerAccount {
    return MessengerAccount(
        deploymentId = getString(DataKeys.DeploymentId) ?: "",
        domain = getString(DataKeys.Domain) ?: ""
    ).apply {
        tokenStoreKey = getString(DataKeys.TokenStoreKey).orEmpty()
        logging = get(DataKeys.Logging)?.asBoolean ?: false
    }
}

fun JsonObject.getString(key: String?): String? {
    return try {
        key?.let { get(it)?.asString }
    } catch (exception: IllegalStateException) { // being thrown by the 'JsonElement' casting
        Log.w(ChatFormFragment.TAG, exception.message ?: "Unable to parse field")
        null
    }
}


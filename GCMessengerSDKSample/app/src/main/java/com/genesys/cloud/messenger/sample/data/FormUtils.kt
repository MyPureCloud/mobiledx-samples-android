package com.genesys.cloud.messenger.sample.data

import android.util.Log
import com.genesys.cloud.integration.messenger.MessengerAccount
import com.genesys.cloud.messenger.sample.data.defs.DataKeys
import com.genesys.cloud.messenger.sample.chat_form.ChatFormFragment
import com.google.gson.JsonObject

fun JsonObject.toMessengerAccount(): MessengerAccount {
    return MessengerAccount(
        deploymentId = getString(DataKeys.DeploymentId) ?: "",
        domain = getString(DataKeys.Domain) ?: ""
    ).apply {
        logging = get(DataKeys.Logging)?.asBoolean ?: false
        customAttributes = mapOf("Company" to "Genesys")
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


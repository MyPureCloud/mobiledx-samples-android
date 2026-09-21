package com.genesys.cloud.messenger.sample.data

import android.content.Context
import android.util.Log
import com.genesys.cloud.integration.messenger.MessengerAccount
import com.genesys.cloud.messenger.sample.BuildConfig
import com.genesys.cloud.messenger.sample.data.defs.DataKeys
import com.genesys.cloud.messenger.sample.chat_form.ChatFormFragment
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

private const val DEFAULT_SESSION_EXPIRATION_SECONDS = 300L

/**
 * Resolves the [MessengerAccount] backing this raw account form data via [SampleAccountHolder], so
 * chat, availability and push actions reuse the same account instance tracking was initiated on
 * (see [SampleAccountHolder]) instead of building a new one for every action.
 */
fun JsonObject.toMessengerAccount(context: Context): MessengerAccount {
    val deploymentId = getString(DataKeys.DeploymentId) ?: ""
    val domain = getString(DataKeys.Domain) ?: ""
    val authCode = getString(DataKeys.AuthCode)

    // authenticationInfo has a private setter and can't be cleared; force a fresh instance when
    // the form drops auth but the cached one has it.
    if (authCode == null && SampleAccountHolder.account?.authenticationInfo != null) {
        SampleAccountHolder.clear()
    }

    return SampleAccountHolder.getOrUpdate(deploymentId, domain, context) {
        logging = get(DataKeys.Logging)?.asBoolean ?: true
        customAttributes = getString(DataKeys.CustomAttributes).toMap() ?: emptyMap()
        authCode?.let {
            setAuthenticationInfo(it, BuildConfig.SIGN_IN_REDIRECT_URI, BuildConfig.CODE_VERIFIER)
        }
        sessionExpirationNoticeInterval =
            getString(DataKeys.SessionExpirationNoticeInterval)?.toLongOrNull()?.takeIf { it > 0 }
                ?: DEFAULT_SESSION_EXPIRATION_SECONDS
    }
}

fun String?.toMap(): Map<String, String>? {
    return try {
        Gson().fromJson(this, object : TypeToken<Map<String, String>>() {}.type)
    } catch (e: JsonSyntaxException) {
        return null
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


package com.genesys.mobile.messenger.sdk.gcmessengersdksample.data

import android.util.Log
import android.widget.RadioButton
import android.widget.RadioGroup
import com.genesys.cloud.integration.async.AsyncAccount
import com.genesys.cloud.integration.async.core.UserInfo
import com.genesys.cloud.integration.async.userInfo
import com.genesys.cloud.integration.bold.BoldAccount
import com.genesys.cloud.integration.bot.BotAccount
import com.genesys.cloud.integration.messenger.MessengerAccount
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.defs.ChatType
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.defs.DataKeys
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.defs.FieldProps
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.presentation.chat_form.ChatFormFragment
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

fun JsonObject.toBotAccount(): BotAccount {
    return BotAccount(
        getString(DataKeys.Accesskey).orEmpty(), getString(DataKeys.AccountName),
        getString(DataKeys.KB), getString(DataKeys.Server)
    ).apply {

        getString(DataKeys.Context)?.let {
            contexts = Gson().fromJson<Map<String, String>>(it, Map::class.java)
        }
    }
}

fun JsonObject.toLiveAccount(): BoldAccount {
    return BoldAccount(getString(DataKeys.Accesskey).orEmpty()).apply {
        get(DataKeys.SkipPrechat)?.asBoolean?.takeIf { it }?.let { skipPrechat() }
    }
}

fun JsonObject.toMessengerAccount(): MessengerAccount {
    return MessengerAccount(
        deploymentId = getString(DataKeys.DeploymentId) ?: "",
        domain = getString(DataKeys.Domain) ?: ""
    ).apply {
        tokenStoreKey = getString(DataKeys.TokenStoreKey).orEmpty()
        logging = get(DataKeys.Logging)?.asBoolean ?: false
    }
}

fun JsonObject.toAsyncAccount(): AsyncAccount {

    val infoJson = getAsJsonObject(DataKeys.Info)
    return AsyncAccount(
        getString(DataKeys.Accesskey).orEmpty(),
        infoJson?.getString(DataKeys.AppId).orEmpty()
    ).apply {
        info.userInfo =
            (infoJson?.getString(DataKeys.UserId)?.takeIf { it.isNotEmpty() }?.let { UserInfo(it) }
                ?: UserInfo()).apply {
                infoJson?.let { infoJson ->
                    infoJson.getString(DataKeys.Email)?.let { email = it }
                    infoJson.getString(DataKeys.Phone)?.let { phoneNumber = it }
                    infoJson.getString(DataKeys.FirstName)?.let { firstName = it }
                    infoJson.getString(DataKeys.LastName)?.let { lastName = it }
                    infoJson.getString(DataKeys.CountryAbbrev)?.let { countryAbbrev = it }
                }
            }
    }
}

internal fun JsonObject?.orDefault(@ChatType chatType: String): JsonObject {
    return this ?: Gson().fromJson(
        when (chatType) {
            ChatType.Live -> Gson().toJson(Accounts.defaultBoldAccount)
            ChatType.Messenger -> Gson().toJson(Accounts.defaultMessengerAccount)
            ChatType.Bot -> Gson().toJson(Accounts.defaultBotAccount)
            else -> ""
        }, JsonObject::class.java
    ).toNeededInfo(chatType)
}

internal fun JsonObject.toNeededInfo(@ChatType chatType: String): JsonObject {
    return when (chatType) {
        ChatType.Live -> toNeededLiveInfo()
        ChatType.Messenger -> toNeededMessengerInfo()
        ChatType.Bot -> toNeededBotInfo()
        else -> JsonObject()
    }
}

internal fun JsonObject.toNeededMessengerInfo(): JsonObject {
    return JsonObject().apply {
        this@toNeededMessengerInfo.getAsJsonObject(DataKeys.Info).let { info ->
            info.getAsJsonObject("configurations").let {
                it.copySimpleProp(DataKeys.DeploymentId, this)
                it.copySimpleProp(DataKeys.Domain, this)
                it.copySimpleProp(DataKeys.TokenStoreKey, this)
                this.addProperty(DataKeys.Logging, it.get(DataKeys.Logging).asBoolean)
            }
        }
    }
}

/*
* internal fun JsonObject.toNeededAsyncInfo(): JsonObject {
    return JsonObject().apply {
        this@toNeededAsyncInfo.let { fullInfo ->

            fullInfo.copySimpleProp(Accesskey, this)

            fullInfo.getAsJsonObject(Info).let { info ->
                info.copySimpleProp(UserId, this)

                info.getAsJsonObject("configurations").copySimpleProp(AppId, this)

                info.getAsJsonObject("extraData")?.let { extraData ->
                    extraData.copySimpleProp(Email, this)
                    extraData.copySimpleProp(Phone, this)
                    extraData.copySimpleProp(FirstName, this)
                    extraData.copySimpleProp(LastName, this)
                    extraData.copySimpleProp(CountryAbbrev, this)
                }
            }
        }

    }*/

internal fun JsonObject.toNeededLiveInfo(): JsonObject {
    return JsonObject().apply {
        this@toNeededLiveInfo.copySimpleProp(DataKeys.Accesskey, this)
    }
}

internal fun JsonObject.toNeededBotInfo(): JsonObject {
    return JsonObject().apply {
        this@toNeededBotInfo.let { fullInfo ->
            fullInfo.copySimpleProp(DataKeys.AccountName, this)
            fullInfo.copySimpleProp(DataKeys.KB, this)
            fullInfo.copySimpleProp(DataKeys.Accesskey, this)
            fullInfo.copySimpleProp(DataKeys.Server, this)
        }
    }
}

fun JsonObject.copySimpleProp(key: String?, other: JsonObject) {
    getString(key)?.let { other.addProperty(key, it) }
}

fun JsonObject.getString(key: String?): String? {
    return try {
        key?.let { get(it)?.asString }
    } catch (exception: IllegalStateException) { // being thrown by the 'JsonElement' casting
        Log.w(ChatFormFragment.TAG, exception.message ?: "Unable to parse field")
        null
    }
}

fun JsonArray.applyValues(accountObject: JsonObject?): JsonArray {
    return this.apply {
        accountObject?.let { accountObject ->
            onEach {
                it.toObject()?.let { fieldObject ->
                    val key =
                        fieldObject.getString(FieldProps.Key) // -> Gets the key of the specific field data

                    accountObject.getString(key)?.let { value ->
                        fieldObject.addProperty(
                            FieldProps.Value,
                            value
                        ) // -> Gets the value of the same key from the account data
                    }
                }
            }
        }
    }
}

fun Pair<String, String>.isEmpty(): Boolean {
    return first.isEmpty() || second.isEmpty()
}

fun JsonElement.toObject(catchEmpty: Boolean = false): JsonObject? {
    return try {
        this.asJsonObject
    } catch (exception: IllegalStateException) { // being thrown by the 'JsonElement' casting
        Log.w(ChatFormFragment.TAG, exception.message ?: "Unable to parse field")
        if (catchEmpty) JsonObject() else null
    }
}

fun RadioGroup.getSelectedText(): String? =
    findViewById<RadioButton>(this.checkedRadioButtonId).text?.toString()

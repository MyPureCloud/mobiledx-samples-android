package com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.repositories

import android.content.Context
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.defs.ChatType
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.orDefault
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.lang.ref.WeakReference


class JsonSampleRepository(context: Context) : SampleRepository {

    private val wContext: WeakReference<Context> = WeakReference<Context>(context)

    override val continuityRepository: ContinuityRepository
        get() = object : ContinuityRepository {

            override fun saveSessionToken(key: String, sessionToken: String?) {
                wContext.get()?.getSharedPreferences("bot_chat_session", 0)?.let { shared ->
                    val editor = shared.edit()
                    editor.putString(
                        key, sessionToken.toString()
                    )
                    editor.apply()
                }
            }

            override fun getSessionToken(key: String): String? {
                return wContext.get()?.getSharedPreferences("bot_chat_session", 0)
                    ?.getString(key, null)
            }
        }

    private fun getSaved(@ChatType chatType: String): JsonObject? {
        return wContext.get()?.getSharedPreferences("accounts", 0)?.getString(chatType, null)
            ?.let { Gson().fromJson(it, JsonObject::class.java) }
    }

    override fun getSavedAccount(@ChatType chatType: String): JsonObject {
        return chatType.takeIf { it != ChatType.ChatSelection }
            ?.let { getSaved(it).orDefault(chatType) } ?: JsonObject()
    }

    override fun saveAccount(accountData: Any?, @ChatType chatType: String) {

        wContext.get()?.getSharedPreferences("accounts", 0)?.let { shared ->
            val editor = shared.edit()
            editor.putString(
                chatType,
                (accountData as? JsonObject).toString()
            )
            editor.apply()
        }
    }

    override fun isRestorable(@ChatType chatType: String): Boolean {
        return chatType == ChatType.ContinueLast || getSaved(chatType) != null
    }
}

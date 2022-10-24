package com.genesys.gcmessengersdksample.data.repositories

import android.content.Context
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

    private fun getSaved(): JsonObject? {
        val sharedPreferences = wContext.get()?.getSharedPreferences("accounts", 0)
        return sharedPreferences?.getString("account", null)
            ?.let { Gson().fromJson(it, JsonObject::class.java) }
    }

    override fun getSavedAccount(): JsonObject {
        return getSaved() ?: JsonObject()
    }

    override fun saveAccount(accountData: Any?) {

        wContext.get()?.getSharedPreferences("accounts", 0)?.let { shared ->
            val editor = shared.edit()
            editor.putString("account", (accountData as? JsonObject).toString())
            editor.apply()
        }
    }

    override fun isRestorable(): Boolean {
        return getSaved() != null
    }
}

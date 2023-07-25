package com.genesys.cloud.messenger.sample.data.repositories

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.lang.ref.WeakReference

class JsonSampleRepository(context: Context) : SampleRepository {

    private val wContext: WeakReference<Context> = WeakReference<Context>(context)

    override fun getSavedAccount(): JsonObject {

        val sharedPreferences = wContext.get()?.getSharedPreferences("accounts", 0)
        val saved = sharedPreferences?.getString("account", null)
            ?.let { Gson().fromJson(it, JsonObject::class.java) }

        return saved ?: JsonObject()
    }

    override fun saveAccount(accountData: Any?) {

        wContext.get()?.getSharedPreferences("accounts", 0)?.let { shared ->
            val editor = shared.edit()
            editor.putString("account", (accountData as? JsonObject).toString())
            editor.apply()
        }
    }
}

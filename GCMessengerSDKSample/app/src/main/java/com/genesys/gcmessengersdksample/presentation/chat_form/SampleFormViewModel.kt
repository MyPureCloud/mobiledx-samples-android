package com.genesys.gcmessengersdksample.presentation.chat_form

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.genesys.cloud.integration.bot.BotChatSettings
import com.genesys.cloud.integration.core.AccountInfo
import com.genesys.cloud.integration.core.configuration.ChatSettings
import com.genesys.cloud.integration.messenger.MessengerSettings
import com.genesys.gcmessengersdksample.data.SampleData
import com.genesys.gcmessengersdksample.data.defs.DataKeys
import com.genesys.gcmessengersdksample.data.repositories.SampleRepository
import com.genesys.gcmessengersdksample.data.toMessengerAccount
import com.genesys.gcmessengersdksample.data.toObject
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class SampleFormViewModel(private val sampleRepository: SampleRepository) : ViewModel() {

    private val accountData: JsonObject?
        get() = _sampleData.value?.account

    private var _sampleData: MutableLiveData<SampleData> = MutableLiveData()
    val sampleData: LiveData<SampleData> = _sampleData

    private var _chatType: MutableLiveData<String> = MutableLiveData()

    private var _formData: MutableLiveData<JsonArray> = MutableLiveData()
    val formData: LiveData<JsonArray> = _formData

    val account: AccountInfo?
        get() = accountData?.toMessengerAccount()


    /**
     * true is the user pressed on the restore button
     */
    private var restoreRequest: Boolean = false

    /**
     * Chat settings - in case chat form contains chat settings data
     */
    var chatSettings: ChatSettings = MessengerSettings()

    fun getFormField(index: Int): JsonObject? {
        return _formData.value?.takeIf { it.size() > 0 }?.get(index)?.toObject()
    }

    fun onAccountData(accountData: JsonObject) {

        try {
            accountData.remove(DataKeys.ChatTypeKey)?.asString?.let { _chatType.value = it }
            accountData.remove(DataKeys.Restore)?.asBoolean?.let { restoreRequest = it }
            accountData.remove(DataKeys.Welcome)?.asString?.let {
                (chatSettings as? BotChatSettings)?.welcomeArticleId = it
            }
        } catch (exception: IllegalStateException) {
            // being thrown by the 'JsonElement' casting
            Log.w(ChatFormFragment.TAG, exception.message ?: "Unable to parse field")
        }


        accountData.takeIf { it.size() > 0 }?.let {
            _sampleData.value = SampleData(accountData, restoreRequest)
            sampleRepository.saveAccount(accountData)
        }
    }
}
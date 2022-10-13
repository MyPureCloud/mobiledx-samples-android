package com.genesys.mobile.messenger.sdk.gcmessengersdksample.presentation.chat_form

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.genesys.cloud.integration.bold.core.BoldChatSettings
import com.genesys.cloud.integration.bot.BotChatSettings
import com.genesys.cloud.integration.core.AccountInfo
import com.genesys.cloud.integration.core.configuration.ChatSettings
import com.genesys.cloud.integration.messenger.MessengerSettings
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.*
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.defs.ChatType
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.defs.DataKeys
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.repositories.SampleRepository
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class SampleFormViewModel(private val sampleRepository: SampleRepository) : ViewModel(){

    private val accountData: JsonObject?
        get() = _sampleData.value?.account

    private var _sampleData: MutableLiveData<SampleData> = MutableLiveData()
    val sampleData: LiveData<SampleData> = _sampleData

    private var _chatType: MutableLiveData<String> = MutableLiveData()
    val chatType: LiveData<String> = _chatType

    private var _formData: MutableLiveData<JsonArray> = MutableLiveData()
    val formData: LiveData<JsonArray> = _formData

    /**
     * @returns true if the account found by the repository
     */
    fun checkRestorable(): Boolean {
        return sampleRepository.isRestorable(getChatType())
    }

    val account: AccountInfo?
        get() = when (chatType.value) {
            ChatType.Live -> accountData?.toLiveAccount()
            ChatType.Messenger -> accountData?.toMessengerAccount()
            ChatType.Bot -> accountData?.toBotAccount()
            else -> null
        }

    /**
     * true is the user pressed on the restore button
     */
    var restoreRequest: Boolean = false

    /**
     * Chat settings - in case chat form contains chat settings data
     */
    var chatSettings: ChatSettings = ChatSettings()

    fun getAccountDataByKey(key: String): String? {
        return accountData?.getString(key)
    }

    fun getFormField(index: Int): JsonObject? {
        return _formData.value?.takeIf { it.size() > 0 }?.get(index)?.toObject()
    }

    fun updateChatType(@ChatType chatType: String) {
        _chatType.value = chatType
        chatSettings = when (chatType) {
            ChatType.Live -> BoldChatSettings()
            ChatType.Messenger -> MessengerSettings()
            ChatType.Bot -> BotChatSettings()
            else -> ChatSettings()
        }
    }

    private fun getChatType() = chatType.value ?: ChatType.ChatSelection

    fun createFormFields(extraFields: List<FormFieldFactory.FormField>? = null) {
        _formData.value = FormDataFactory.createFormFields(getChatType(), extraFields)
            .applyValues(sampleRepository.getSavedAccount(getChatType()) as? JsonObject)
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
            sampleRepository.saveAccount(accountData, getChatType())
        }
    }
}
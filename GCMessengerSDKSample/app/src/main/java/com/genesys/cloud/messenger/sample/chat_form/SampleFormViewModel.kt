package com.genesys.cloud.messenger.sample.chat_form

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesys.cloud.messenger.sample.data.SampleUIState
import com.genesys.cloud.messenger.sample.data.repositories.SampleRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

class SampleFormViewModel(private val sampleRepository: SampleRepository) : ViewModel() {

    private var _uiState: MutableLiveData<SampleUIState> = MutableLiveData()
    val uiState: LiveData<SampleUIState> = _uiState

    fun loadSavedAccount() {
        viewModelScope.launch {
            _uiState.value = SampleUIState(
                sampleRepository.getSavedAccount() as JsonObject,
                startChat = false,
                testAvailability = false
            )
        }
    }

    fun startChat(accountData: JsonObject) {
        processAccountData(accountData, startChat = true, testAvailability = false)
    }

    fun testChatAvailability(accountData: JsonObject) {
        processAccountData(accountData, startChat = false, testAvailability = true)
    }

    private fun processAccountData(
        accountData: JsonObject,
        startChat: Boolean,
        testAvailability: Boolean
    ) {
        accountData.takeIf { it.size() > 0 }?.let {
            _uiState.value = SampleUIState(
                accountData,
                startChat = startChat,
                testAvailability = testAvailability
            )
            sampleRepository.saveAccount(accountData)
        }
    }
}
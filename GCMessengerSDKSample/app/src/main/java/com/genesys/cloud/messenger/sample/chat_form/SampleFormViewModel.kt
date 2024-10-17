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

    var redirectUri: String = ""
    var codeVerifier: String? = null
    private var _authCode: MutableLiveData<String> = MutableLiveData()
    val authCode: LiveData<String> = _authCode

    val isAuthenticated: Boolean get() = !authCode.value.isNullOrEmpty()

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

    fun setAuthCode(authCode: String, redirectUri: String, codeVerifier: String?){
        _authCode.value = authCode
        this.redirectUri = redirectUri
        this.codeVerifier = codeVerifier
    }

    fun clearAuthCode(){
        _authCode.value = ""
        this.redirectUri = ""
        this.codeVerifier = null
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
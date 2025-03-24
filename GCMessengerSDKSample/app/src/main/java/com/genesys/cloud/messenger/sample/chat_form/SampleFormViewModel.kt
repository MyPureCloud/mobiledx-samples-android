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

    private val _uiState: MutableLiveData<SampleUIState> = MutableLiveData()
    val uiState: LiveData<SampleUIState> = _uiState

    var redirectUri: String = ""
    var codeVerifier: String? = null
    private val _authCode: MutableLiveData<String> = MutableLiveData()
    val authCode: LiveData<String> = _authCode

    val isAuthenticated: Boolean get() = !authCode.value.isNullOrEmpty()

    private var latestTypedDeploymentId: String = ""
    private val pushEnabledForDeployment: MutableMap<String,Boolean> = mutableMapOf()
    private val _pushEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    val pushEnabled: LiveData<Boolean> = _pushEnabled

    fun loadSavedAccount() {
        viewModelScope.launch {
            _uiState.value = SampleUIState(
                account = sampleRepository.getSavedAccount() as JsonObject
            )
        }
    }

    fun startChat(accountData: JsonObject) {
        processAccountData(accountData = accountData, startChat = true)
    }

    fun testChatAvailability(accountData: JsonObject) {
        processAccountData(accountData, testAvailability = true)
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

    fun changePushEnablement(accountData: JsonObject) {
        if (_pushEnabled.value == true) {
            processAccountData(accountData, disablePush = true)
        } else {
            processAccountData(accountData, enablePush = true)
        }
    }

    fun setPushEnabled(value: Boolean){
        pushEnabledForDeployment[latestTypedDeploymentId] = value
        _pushEnabled.value = value
    }

    fun updateLatestTypedDeploymentId(deploymentId: String) {
        latestTypedDeploymentId = deploymentId
        val existingEnablement = pushEnabledForDeployment.entries.singleOrNull { entry -> entry.key == deploymentId }
        if (existingEnablement == null) {
            _pushEnabled.value = false
        } else {
            _pushEnabled.value = existingEnablement.value
        }
    }

    private fun processAccountData(
        accountData: JsonObject,
        startChat: Boolean = false,
        testAvailability: Boolean = false,
        enablePush: Boolean = false,
        disablePush: Boolean = false
    ) {
        accountData.takeIf { it.size() > 0 }?.let {
            _uiState.value = SampleUIState(
                accountData,
                startChat = startChat,
                testAvailability = testAvailability,
                enablePush = enablePush,
                disablePush = disablePush
            )
            sampleRepository.saveAccount(accountData)
        }
    }
}
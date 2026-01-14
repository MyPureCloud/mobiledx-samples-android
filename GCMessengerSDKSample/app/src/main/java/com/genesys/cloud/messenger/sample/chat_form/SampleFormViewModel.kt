package com.genesys.cloud.messenger.sample.chat_form

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesys.cloud.messenger.sample.data.SampleUIState
import com.genesys.cloud.messenger.sample.data.repositories.SampleRepository
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class SampleFormViewModel(private val sampleRepository: SampleRepository) : ViewModel() {

    private val _uiState: MutableLiveData<SampleUIState> = MutableLiveData()
    val uiState: LiveData<SampleUIState> = _uiState

    var redirectUri: String = ""
    var codeVerifier: String? = null
    private val _authCode: MutableLiveData<String> = MutableLiveData()
    val authCode: LiveData<String> = _authCode

    var isAuthenticated: Boolean = false

    val hasAuthCode: Boolean get() = !authCode.value.isNullOrEmpty()

    private var latestTypedDeploymentId: String = ""
    private val pushEnabledForDeployment: MutableMap<String,Boolean> = mutableMapOf()
    private val _pushEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    val pushEnabled: LiveData<Boolean> = _pushEnabled

    val isImplicitFlowEnabled : Boolean
        get() = _uiState.value?.enableImplicitFlow == true

    private val _idToken = MutableStateFlow<String?>(null)
    val idToken: StateFlow<String?> = _idToken.asStateFlow()

    private val _nonce = MutableStateFlow<String>(UUID.randomUUID().toString())
    val nonce = _nonce.asStateFlow()

    private val _isReauthorizationInProgress = MutableStateFlow<Boolean>(false)
    val isReauthorizationInProgress = _isReauthorizationInProgress.asStateFlow()

    init {
        _pushEnabled.value = sampleRepository.savedPushConfig
    }

    fun loadSavedAccount() {
        viewModelScope.launch {
            val savedAccount = sampleRepository.getSavedAccount() as JsonObject
            _uiState.value = _uiState.value?.copy(
                account = savedAccount
            ) ?: SampleUIState(
                account = savedAccount
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

    fun setIdToken(newIdToken: String) {
        _idToken.update { newIdToken }
    }

    fun setNonce(newNonce: String) {
        _nonce.update { newNonce }
    }

    fun setReAuthorizationProgress(isReAuthorizationRequired: Boolean) {
        _isReauthorizationInProgress.update { isReAuthorizationRequired}
    }

    fun clearAuthCode(){
        _authCode.value = ""
        this.redirectUri = ""
        this.codeVerifier = null
    }

    fun clearIdToken() {
        _idToken.update { null }
        _nonce.update { UUID.randomUUID().toString() }
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
        sampleRepository.savedPushConfig = value
    }

    fun setImplicitFlowEnabled(isEnabled : Boolean) {
        _uiState.value = _uiState.value?.copy(
            enableImplicitFlow = isEnabled
        ) ?: SampleUIState(
            account = null,
            enableImplicitFlow = isEnabled
        )
    }

    fun updateLatestTypedDeploymentId(deploymentId: String) {
        latestTypedDeploymentId = deploymentId
        val existingEnablement = pushEnabledForDeployment.entries.singleOrNull { entry -> entry.key == deploymentId }
        if (existingEnablement == null) {
            _pushEnabled.value = sampleRepository.savedPushConfig
        } else {
            _pushEnabled.value = existingEnablement.value
        }
    }

    private fun processAccountData(
        accountData: JsonObject,
        startChat: Boolean = false,
        testAvailability: Boolean = false,
        enablePush: Boolean = false,
        disablePush: Boolean = false,
        implicitEnabled: Boolean = false
    ) {
        accountData.takeIf { it.size() > 0 }?.let {
            _uiState.value = _uiState.value?.copy(
                account = accountData,
                startChat = startChat,
                testAvailability = testAvailability,
                enablePush = enablePush,
                disablePush = disablePush,
                enableImplicitFlow = isImplicitFlowEnabled
            ) ?: SampleUIState(
                account = accountData,
                startChat = startChat,
                testAvailability = testAvailability,
                enablePush = enablePush,
                disablePush = disablePush,
                enableImplicitFlow = implicitEnabled,
            )
            sampleRepository.saveAccount(accountData)
        }
    }
}
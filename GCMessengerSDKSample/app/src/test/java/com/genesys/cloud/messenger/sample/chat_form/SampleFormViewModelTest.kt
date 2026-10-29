package com.genesys.cloud.messenger.sample.chat_form

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.genesys.cloud.messenger.sample.data.defs.DataKeys
import com.genesys.cloud.messenger.sample.data.repositories.SampleRepository
import com.google.gson.JsonObject
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SampleFormViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val mockRepository = mockk<SampleRepository>(relaxed = true)
    private lateinit var subject: SampleFormViewModel

    private val accountData = JsonObject().apply {
        addProperty(DataKeys.DeploymentId, "test-deployment-id")
        addProperty(DataKeys.Domain, "test.domain.com")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { mockRepository.savedPushConfig } returns false
        every { mockRepository.getSavedAccount() } returns accountData
        subject = SampleFormViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // region Push Enablement Tests

    @Test
    fun changePushEnablement_whenPushDisabled_requestsEnablePush() {
        //given & when
        subject.changePushEnablement(accountData)

        //then
        assertTrue(subject.uiState.value!!.enablePush)
        assertFalse(subject.uiState.value!!.disablePush)
    }

    @Test
    fun changePushEnablement_whenPushEnabled_requestsDisablePush() {
        //given
        subject.setPushEnabled(true)

        //when
        subject.changePushEnablement(accountData)

        //then
        assertTrue(subject.uiState.value!!.disablePush)
        assertFalse(subject.uiState.value!!.enablePush)
    }

    @Test
    fun clearPushEnablement_whenEnablePushRequested_clearsRequest() {
        //given
        subject.changePushEnablement(accountData)

        //when
        subject.clearPushEnablement()

        //then
        assertFalse(subject.uiState.value!!.enablePush)
    }

    @Test
    fun clearPushEnablement_whenDisablePushRequested_clearsRequest() {
        //given
        subject.setPushEnabled(true)
        subject.changePushEnablement(accountData)

        //when
        subject.clearPushEnablement()

        //then
        assertFalse(subject.uiState.value!!.disablePush)
    }

    @Test
    fun clearPushEnablement_whenNoRequestPending_doesNotEmitNewState() {
        //given
        subject.changePushEnablement(accountData)
        subject.clearPushEnablement()
        val stateAfterFirstClear = subject.uiState.value

        //when
        subject.clearPushEnablement()

        //then
        assertTrue(stateAfterFirstClear === subject.uiState.value)
    }

    @Test
    fun clearPushEnablement_whenAccountDataReloaded_keepsPushRequestConsumed() = runTest {
        //given
        subject.changePushEnablement(accountData)
        subject.clearPushEnablement()

        //when
        subject.loadSavedAccount()

        //then
        assertFalse(subject.uiState.value!!.enablePush)
        assertFalse(subject.uiState.value!!.disablePush)
    }

    @Test
    fun loadSavedAccount_whenPushRequestNotConsumed_replaysPushRequest() = runTest {
        //given
        subject.changePushEnablement(accountData)

        //when
        subject.loadSavedAccount()

        //then
        assertTrue(subject.uiState.value!!.enablePush)
    }

    // endregion

    // region Start Chat Tests

    @Test
    fun clearStartChat_whenAccountDataReloaded_keepsStartChatConsumed() = runTest {
        //given
        subject.startChat(accountData)
        subject.clearStartChat()

        //when
        subject.loadSavedAccount()

        //then
        assertFalse(subject.uiState.value!!.startChat)
    }

    @Test
    fun loadSavedAccount_whenStartChatNotConsumed_replaysStartChat() = runTest {
        //given
        subject.startChat(accountData)

        //when
        subject.loadSavedAccount()

        //then
        assertTrue(subject.uiState.value!!.startChat)
    }

    // endregion
}

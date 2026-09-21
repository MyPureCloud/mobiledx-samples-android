package com.genesys.cloud.messenger.sample.data

import android.content.Context
import com.genesys.cloud.messenger.sample.data.defs.DataKeys
import com.google.gson.JsonObject
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormUtilsTest {

    private val mockContext = mockk<Context>(relaxed = true)

    @After
    fun tearDown() {
        SampleAccountHolder.clear()
    }

    @Test
    fun toMessengerAccount_whenLoggingKeyMissing_defaultsToTrue() {
        //given
        val accountJson = JsonObject().apply {
            addProperty(DataKeys.DeploymentId, "test-deployment-id")
            addProperty(DataKeys.Domain, "test.domain.com")
        }

        //when
        val account = accountJson.toMessengerAccount(mockContext)

        //then
        assertTrue(account.logging)
    }

    @Test
    fun toMessengerAccount_whenLoggingKeyIsFalse_respectsExplicitValue() {
        //given
        val accountJson = JsonObject().apply {
            addProperty(DataKeys.DeploymentId, "test-deployment-id-2")
            addProperty(DataKeys.Domain, "test.domain.com")
            addProperty(DataKeys.Logging, false)
        }

        //when
        val account = accountJson.toMessengerAccount(mockContext)

        //then
        assertFalse(account.logging)
    }
}

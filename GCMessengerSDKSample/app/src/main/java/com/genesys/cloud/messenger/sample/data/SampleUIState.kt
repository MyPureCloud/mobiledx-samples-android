package com.genesys.cloud.messenger.sample.data

import com.google.gson.JsonObject

data class SampleUIState(
    val account: JsonObject?,
    val startChat: Boolean = false,
    val testAvailability: Boolean = false,
    val enablePush: Boolean = false,
    val disablePush: Boolean = false,
    val enableImplicitFlow: Boolean = false
)
package com.genesys.gcmessengersdksample.data

import com.google.gson.JsonObject

data class SampleUIState(
    val account: JsonObject?,
    val startChat: Boolean,
    val testAvailability: Boolean
)
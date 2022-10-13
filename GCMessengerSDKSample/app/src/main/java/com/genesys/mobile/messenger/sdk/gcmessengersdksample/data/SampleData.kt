package com.genesys.mobile.messenger.sdk.gcmessengersdksample.data

import com.google.gson.JsonObject

data class SampleData(

    /**
     * The Account (Bot/Bold/Async)
     */
    val account: JsonObject?,

    /**
     * true if there was a restoration request
     */
    val restoreRequest: Boolean

)
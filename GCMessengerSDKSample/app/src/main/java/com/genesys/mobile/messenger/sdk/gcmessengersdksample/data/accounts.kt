package com.genesys.mobile.messenger.sdk.gcmessengersdksample.data

import com.genesys.cloud.integration.async.AsyncAccount
import com.genesys.cloud.integration.async.core.UserInfo
import com.genesys.cloud.integration.async.userInfo
import com.genesys.cloud.integration.bold.BoldAccount
import com.genesys.cloud.integration.bot.BotAccount
import com.genesys.cloud.integration.messenger.MessengerAccount
import java.util.*

object Accounts {

    private val formalBoldAccount: BoldAccount
        get() = BoldAccount("2300000001700000000:2278936004449775473:sHkdAhpSpMO/cnqzemsYUuf2iFOyPUYV") // Mobile

    private val fameBoldAccount: BoldAccount
        get() = BoldAccount("2300000001700000000:2279148490312878292:grCCPGyzmyITEocnaE+owvjtbasV16eV") // Fame

    val defaultBoldAccount: BoldAccount
        get() = fameBoldAccount

    private val formalBotAccount: BotAccount
        get() = BotAccount(
            "",
            "nanorep",
            "English",
            "" //https://eu1-1.nanorep.com/console/login.html
        )

    private val testBotAccount: BotAccount
        get() = BotAccount(
            "", "nanorep",
            "English", "mobilestaging"
        )

    val defaultBotAccount: BotAccount
        get() = testBotAccount

    val defaultMessengerAccount =
        MessengerAccount(
            deploymentId = "f6dd00eb-349b-4f12-95a4-9bdd24ee607c",
            domain = "inindca.com"
        ).apply {
            tokenStoreKey = "com.genesys.messenger.poc"
            logging = true
        }

    val defaultAsyncAccount = AsyncAccount(
        "2300000001700000000:2279533687831071375:MlVOftOF/UFUUqPPSbMSDAnQjITxOrQW:gamma",
        "MobileAsyncStagingNew12345"
    ).apply {
        info.userInfo = UserInfo(UUID.randomUUID().toString()).apply {
            firstName = "Android"
            lastName = "Samples"
            email = "android.samples@bold.com"
            phoneNumber = "111-111-1111"
        }
    }
}
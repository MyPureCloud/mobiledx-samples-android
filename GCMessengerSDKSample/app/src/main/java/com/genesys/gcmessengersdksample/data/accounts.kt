package com.genesys.gcmessengersdksample.data

import com.genesys.cloud.integration.async.AsyncAccount
import com.genesys.cloud.integration.async.core.UserInfo
import com.genesys.cloud.integration.async.userInfo
import com.genesys.cloud.integration.bold.BoldAccount
import com.genesys.cloud.integration.bot.BotAccount
import com.genesys.cloud.integration.messenger.MessengerAccount
import java.util.*

object Accounts {

    private val formalBoldAccount: BoldAccount
        get() = BoldAccount("") // Mobile

    private val fameBoldAccount: BoldAccount
        get() = BoldAccount("") // Fame

    val defaultBoldAccount: BoldAccount
        get() = fameBoldAccount

    private val formalBotAccount: BotAccount
        get() = BotAccount(
            "",
            "",
            "",
            "" //https://eu1-1.nanorep.com/console/login.html
        )

    private val testBotAccount: BotAccount
        get() = BotAccount(
            "", "",
            "", ""
        )

    val defaultBotAccount: BotAccount
        get() = testBotAccount

    val defaultMessengerAccount =
        MessengerAccount(
            deploymentId = "",
            domain = ""
        ).apply {
            tokenStoreKey = ""
            logging = true
        }

    val defaultAsyncAccount = AsyncAccount(
        "",
        ""
    ).apply {
        info.userInfo = UserInfo(UUID.randomUUID().toString()).apply {
            firstName = ""
            lastName = ""
            email = ""
            phoneNumber = ""
        }
    }
}
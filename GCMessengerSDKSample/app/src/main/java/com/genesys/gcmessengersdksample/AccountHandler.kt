package com.genesys.gcmessengersdksample

import com.genesys.cloud.core.utils.Completion
import com.genesys.cloud.integration.core.AccountInfo
import com.genesys.cloud.ui.structure.handlers.AccountInfoProvider

class AccountHandler(var enableContinuity: Boolean = false) : AccountInfoProvider {

    private val accounts: MutableMap<String, AccountInfo> = mutableMapOf()

    override fun provide(info: AccountInfo, callback: Completion<AccountInfo>) {
        val account = if (enableContinuity) accounts[info.apiKey] else info
        callback.onComplete(account ?: info)
    }

    override fun update(account: AccountInfo) {
        accounts[account.apiKey]?.run {
            update(account)
        } ?: kotlin.run {
            accounts[account.apiKey] = account
        }
    }
}

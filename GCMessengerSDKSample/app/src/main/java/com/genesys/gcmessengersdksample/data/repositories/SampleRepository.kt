package com.genesys.gcmessengersdksample.data.repositories

import com.genesys.gcmessengersdksample.data.defs.ChatType

interface SampleRepository {

    /**
     * Gets the prev account data from the shared properties, If null it returns the default account
     * @param chatType Is being used as the saved account's key
     */
    fun getSavedAccount(@ChatType chatType: String): Any?

    /**
     * If changed, updates the shared properties to include the updated account details
     * @param chatType Is being used as the saved account's key
     */
    fun saveAccount(accountData: Any?, @ChatType chatType: String)

    /**
     * Checks if the account is restorable
     * @param chatType Is being used as the saved account's key
     * @return true if the account found
     */
    fun isRestorable(@ChatType chatType: String): Boolean


    val continuityRepository: ContinuityRepository

}

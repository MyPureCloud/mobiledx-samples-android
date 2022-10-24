package com.genesys.gcmessengersdksample.data.repositories

interface SampleRepository {

    /**
     * Gets the prev account data from the shared properties, If null it returns the default account
     * @param chatType Is being used as the saved account's key
     */
    fun getSavedAccount(): Any?

    /**
     * If changed, updates the shared properties to include the updated account details
     * @param chatType Is being used as the saved account's key
     */
    fun saveAccount(accountData: Any?)

    /**
     * Checks if the account is restorable
     * @param chatType Is being used as the saved account's key
     * @return true if the account found
     */
    fun isRestorable(): Boolean


    val continuityRepository: ContinuityRepository

}

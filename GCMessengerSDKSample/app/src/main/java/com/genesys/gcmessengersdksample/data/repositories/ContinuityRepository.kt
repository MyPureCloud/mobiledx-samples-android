package com.genesys.gcmessengersdksample.data.repositories

/**
 * A repository that in charge of the chat continuity data
 */
interface ContinuityRepository {
    /**
     * Saves the SessionToken to the shared properties
     */
    fun saveSessionToken(key: String, sessionToken: String?)

    /**
     * Gets a saved SessionToken from the shared properties
     */
    fun getSessionToken(key: String): String?
}
package com.genesys.cloud.messenger.sample.data.repositories

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.genesys.cloud.integration.messenger.MessengerAccount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PushNotificationsRepository(private val context: Context) {

    companion object {
        private const val LOG_TAG = "PushNotificationsRepo"

        private val LAST_SUCCESSFUL_REGISTRATION_DEPLOYMENT_ID =
            stringPreferencesKey("last_successful_registration_deployment_id")
        private val LAST_SUCCESSFUL_REGISTRATION_DOMAIN =
            stringPreferencesKey("last_successful_registration_domain")
    }

    suspend fun saveAccount(account: MessengerAccount) {
        context.pushDataStore.edit { data ->
            data[LAST_SUCCESSFUL_REGISTRATION_DEPLOYMENT_ID] = account.deploymentId
            data[LAST_SUCCESSFUL_REGISTRATION_DOMAIN] = account.domain
        }
    }

    suspend fun removeAccount(account: MessengerAccount) {
        context.pushDataStore.edit { data ->
            if (data[LAST_SUCCESSFUL_REGISTRATION_DEPLOYMENT_ID] == account.deploymentId
                && data[LAST_SUCCESSFUL_REGISTRATION_DOMAIN] == account.domain
            ) {
                data.remove(LAST_SUCCESSFUL_REGISTRATION_DEPLOYMENT_ID)
                data.remove(LAST_SUCCESSFUL_REGISTRATION_DOMAIN)
                Log.d(LOG_TAG, "Stored deployment registration was removed from pushDataStore.")
            } else {
                Log.d(LOG_TAG, "pushDataStore has a different or empty deployment registration.")
                Log.d(
                    LOG_TAG,
                    "pushDataStore: ${data[LAST_SUCCESSFUL_REGISTRATION_DEPLOYMENT_ID]} - ${data[LAST_SUCCESSFUL_REGISTRATION_DOMAIN]}"
                )
                Log.d(LOG_TAG, "accountInfo:   ${account.deploymentId} - ${account.domain}")
            }
        }
    }

    suspend fun getAccount(): MessengerAccount? {
        return context.pushDataStore.data
            .map { preferences ->
                val deploymentId = preferences[LAST_SUCCESSFUL_REGISTRATION_DEPLOYMENT_ID]
                val domain = preferences[LAST_SUCCESSFUL_REGISTRATION_DOMAIN]
                if (deploymentId != null && domain != null) {
                    deploymentId to domain
                    MessengerAccount(deploymentId, domain)
                } else {
                    Log.d(LOG_TAG, "Previous deviceToken registration not found")
                    null
                }
            }.first()
    }
}
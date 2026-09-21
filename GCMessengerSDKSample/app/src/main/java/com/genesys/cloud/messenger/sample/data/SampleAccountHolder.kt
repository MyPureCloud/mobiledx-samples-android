package com.genesys.cloud.messenger.sample.data

import android.content.Context
import com.genesys.cloud.integration.messenger.MessengerAccount

/**
 * Process-wide holder for the single [MessengerAccount] instance the sample app currently operates
 * on. Chat, availability, push and tracking use cases all reuse this same instance instead of
 * building a brand new [MessengerAccount] on every action, so mutable state such as the active
 * tracker, journey identity and auth info stays consistent across screens.
 *
 * A new instance is only created when the deployment id or domain actually changes, since that
 * represents a genuinely different account.
 */
object SampleAccountHolder {

    var account: MessengerAccount? = null
        private set

    /**
     * Returns the current [MessengerAccount] when it already targets [deploymentId]/[domain],
     * applying [configure] to it. Builds and stores a new instance (replacing any previous one)
     * when the deployment id or domain don't match, or when none exists yet.
     */
    fun getOrUpdate(
        deploymentId: String,
        domain: String,
        context: Context,
        configure: MessengerAccount.() -> Unit = {}
    ): MessengerAccount {
        val current = account
        val target = if (current != null && current.matches(deploymentId, domain)) {
            current
        } else {
            MessengerAccount(deploymentId, domain, context.applicationContext).also { account = it }
        }

        return target.apply(configure)
    }

    fun clear() {
        account = null
    }
}

@Suppress("DEPRECATION")
private fun MessengerAccount.matches(deploymentId: String, domain: String): Boolean =
    this.deploymentId == deploymentId && this.domain == domain

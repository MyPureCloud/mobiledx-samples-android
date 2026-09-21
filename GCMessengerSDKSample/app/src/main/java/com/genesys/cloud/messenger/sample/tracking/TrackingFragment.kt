package com.genesys.cloud.messenger.sample.tracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.genesys.cloud.integration.messenger.MessengerAccount
import com.genesys.cloud.integration.messenger.tracking.MessengerTracking
import com.genesys.cloud.messenger.sample.MainActivity
import com.genesys.cloud.messenger.sample.R
import com.genesys.cloud.messenger.sample.data.SampleAccountHolder

/**
 * Demonstrates and drives Mobile Tracking journey events after the tracker has been initiated on a
 * [MessengerAccount] (see [SampleAccountHolder]).
 *
 * The UI is intentionally minimal so it can be driven from an automation harness (e.g. Appium): two
 * free-text inputs ([TrackingInputParser]) and one button per [TrackingAction].
 */
class TrackingFragment : Fragment() {

    private val account: MessengerAccount?
        get() = SampleAccountHolder.account

    private val tracking: MessengerTracking?
        get() = account?.tracking

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            TrackingScreen(onAction = ::onAction)
        }
    }

    private fun onAction(action: TrackingAction, setters: String, traits: String): String {
        when (action) {
            TrackingAction.CLEAR -> {
                account?.resetTrackingIdentity()
                return getString(R.string.tracking_cleared)
            }

            TrackingAction.START_CHAT -> {
                val account = account ?: return getString(R.string.tracking_not_available)
                (activity as? MainActivity)?.startChatOnTrackingAccount(account)
                return getString(R.string.tracking_chat_starting)
            }

            else -> Unit
        }

        val tracker = tracking ?: return getString(R.string.tracking_not_available)
        val pairs = TrackingInputParser.parsePairs(setters)
        val screenName = TrackingInputParser.screenName(pairs) ?: DEFAULT_SCREEN_NAME
        val attributes = TrackingInputParser.attributes(pairs)
        val eventTraits = TrackingInputParser.traits(traits)

        return when (action) {
            TrackingAction.APPLY_SETTERS -> {
                TrackingInputParser.applyConfig(tracker, pairs)
                getString(R.string.tracking_setters_applied)
            }

            TrackingAction.SCREEN_VIEWED -> {
                tracker.screenViewed(
                    screenName = screenName,
                    attributes = attributes,
                    searchQuery = TrackingInputParser.searchQuery(pairs),
                    traits = eventTraits,
                )
                getString(R.string.tracking_event_sent, screenName)
            }

            TrackingAction.SEARCH_PERFORMED -> {
                tracker.searchPerformed(screenName, attributes, eventTraits)
                getString(R.string.tracking_event_sent, screenName)
            }

            TrackingAction.CUSTOM_EVENT -> {
                val eventName = TrackingInputParser.eventName(pairs) ?: DEFAULT_CUSTOM_EVENT_NAME
                tracker.customEvent(eventName, screenName, attributes, eventTraits)
                getString(R.string.tracking_event_sent, eventName)
            }

            TrackingAction.SET_TRAITS -> {
                if (eventTraits == null) {
                    getString(R.string.tracking_traits_empty)
                } else {
                    tracker.setTraits(eventTraits)
                    getString(R.string.tracking_traits_set)
                }
            }

            TrackingAction.GET_SESSION_ID -> {
                val sessionId = tracker.getSessionId()
                if (sessionId.isNullOrEmpty()) {
                    getString(R.string.tracking_session_id_none)
                } else {
                    // Avoid setPrimaryClip — some devices show a blocking system clipboard overlay.
                    getString(R.string.tracking_session_id, sessionId)
                }
            }
        }
    }

    companion object {
        const val TAG = "TrackingFragment"
        private const val DEFAULT_SCREEN_NAME = "HomeScreen"
        private const val DEFAULT_CUSTOM_EVENT_NAME = "custom_event"
    }
}

package com.genesys.gcmessengersdksample

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.genesys.cloud.core.model.StatementScope
import com.genesys.cloud.core.utils.NRError
import com.genesys.cloud.core.utils.runMain
import com.genesys.cloud.core.utils.snack
import com.genesys.cloud.core.utils.toast
import com.genesys.cloud.integration.core.AccountInfo
import com.genesys.cloud.integration.core.StateEvent
import com.genesys.cloud.ui.structure.controller.*
import com.genesys.gcmessengersdksample.data.repositories.JsonSampleRepository
import com.genesys.gcmessengersdksample.data.toMessengerAccount
import com.genesys.gcmessengersdksample.databinding.ActivityMainBinding
import com.genesys.gcmessengersdksample.presentation.chat_form.ChatFormFragment
import com.genesys.gcmessengersdksample.presentation.chat_form.SampleFormViewModel
import com.genesys.gcmessengersdksample.presentation.chat_form.SampleFormViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), ChatEventListener {

    //region - companion object
    companion object {
        private const val TAG = "MainActivity"
        const val CONVERSATION_FRAGMENT_TAG = "conversation_fragment"
    }
    //endregion

    //region - attributes
    private val viewModel: SampleFormViewModel by viewModels {
        SampleFormViewModelFactory(
            JsonSampleRepository(
                applicationContext
            )
        )
    }
    private lateinit var binding: ActivityMainBinding
    private val hasActiveChats get() = chatController?.hasOpenChats() == true
    private var chatController: ChatController? = null
    private var endMenu: MenuItem? = null
    private var selfBack: Boolean = false
    //endregion

    //region - lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.uiState.observe(this@MainActivity) { sampleData ->

            onAccountDataReady()

            sampleData.account?.let { accountRawJson ->

                val messengerAccount = accountRawJson.toMessengerAccount()

                if (sampleData.startChat) {
                    createChat(messengerAccount)
                } else if (sampleData.testAvailability) {
                    checkAvailability(messengerAccount)
                }
            }
        }

        if (findChatFragment() == null) {
            val fragment = ChatFormFragment()
            showFragment(fragment, ChatFormFragment.TAG)
        }
    }

    override fun onPause() {
        super.onPause()
        waitingVisibility(false)
    }

    override fun onStop() {
        if (isFinishing) {
            destructChat()
        }
        super.onStop()
    }

    override fun onBackPressed() {
        when (supportFragmentManager.backStackEntryCount) {
            1 -> {
                selfBack = true
            }
            0 -> {
                chatController?.destruct()
                finish()
            }
        }

        if (!supportFragmentManager.isStateSaved) super.onBackPressed()
    }
    //endregion

    //region - menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        this.endMenu =
            menu?.findItem(R.id.end_current_chat)

        if (hasActiveChats) {
            enableMenu(
                endMenu,
                true
            )
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.end_current_chat -> {
                chatController?.endChat(false)
                item.isEnabled = hasActiveChats
                return true
            }

            R.id.destruct_chat -> {
                destructChat()
                enableMenu(endMenu, false)
                item.isEnabled = false
                return true
            }

            else -> {
            }
        }
        return false
    }

    private fun enableMenu(menuItem: MenuItem?, enable: Boolean) {
        if (menuItem != null) {
            menuItem.isEnabled = enable
            if (enable && !menuItem.isVisible) menuItem.isVisible = true
        }
    }
    //endregion

    //region - functionality
    private fun destructChat() {
        chatController?.takeUnless { it.wasDestructed }?.run {
            terminateChat()
            destruct()
        }
    }

    private fun createChat(account: AccountInfo, chatStartError: (() -> Unit)? = null) {
        waitingVisibility(true)

        if (chatController?.wasDestructed != false) {

            chatController = ChatController.Builder(this)
                .apply {
                    chatEventListener(this@MainActivity)
                }.build(account, object : ChatLoadedListener {

                    override fun onComplete(result: ChatLoadResponse) {

                        Log.d(TAG, "createChat: ChatLoadedListener.complete")

                        val error = result.error ?: let {

                            if (result.fragment == null) {
                                NRError(NRError.EmptyError, "Chat UI failed to init")
                            } else {
                                null
                            }
                        }

                        error?.let {
                            chatStartError?.invoke()
                            onError(it)

                        } ?: openConversationFragment(result.fragment!!)
                    }
                })
        } else {
            Log.d(TAG, "createChat: start chat with current ChatController")
            chatController?.startChat(account)
        }
    }

    private fun openConversationFragment(fragment: Fragment) {
        if (isFinishing || supportFragmentManager.isStateSaved) return

        val chatFragment = findChatFragment()

        if (chatFragment != null) {

            if (chatFragment.isAdded) {
                removeChatFragment()
            } else {
                return
            }
        }

        showFragment(fragment, CONVERSATION_FRAGMENT_TAG, true)
    }

    private fun showFragment(fragment: Fragment, tag: String, addToBackStack: Boolean = false) {
        supportFragmentManager.beginTransaction().replace(R.id.content_main, fragment, tag).apply {
            if (addToBackStack) {
                addToBackStack(tag)
            }
        }.commit()
    }

    private fun removeChatFragment() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (supportFragmentManager.takeUnless { it.isDestroyed || it.isStateSaved }
                        ?.popBackStackImmediate(
                            CONVERSATION_FRAGMENT_TAG,
                            0
                        ) == false && supportFragmentManager.backStackEntryCount >= 1) onBackPressed()

            } catch (ex: IllegalStateException) {
                ex.printStackTrace()
            }
        }
    }

    private fun checkAvailability(accountInfo: AccountInfo) {
        ChatAvailability.checkAvailability(accountInfo) {
            toast(
                this, "Chat availability status returned ${it.isAvailable}",
            )
        }
    }

    private fun waitingVisibility(visible: Boolean) {
        binding.progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun findChatFragment(): Fragment? =
        supportFragmentManager.findFragmentByTag(CONVERSATION_FRAGMENT_TAG)

    private fun onAccountDataReady() {
        supportFragmentManager.popBackStack(
            ChatFormFragment.TAG,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }
    //endregion

    //region - ChatEventListener
    override fun onError(error: NRError) {
        super.onError(error)

        waitingVisibility(false)

        var message = "!!!! Error:${error.errorCode}: ${error.description ?: error.reason}"

        when (error.errorCode) {
            NRError.ConfigurationsError, NRError.ConversationCreationError -> {

                if (error.reason == NRError.NotEnabled) {
                    message = getString(R.string.chat_disabled_error)
                }
                Log.e(
                    TAG,
                    "!!!!! Chat ${error.scope} can't be created: $message"
                )
            }
        }
        toast(this, message, Toast.LENGTH_SHORT)
    }

    override fun onChatStateChanged(stateEvent: StateEvent) {

        Log.d(TAG, "${stateEvent.scope} chat in state: ${stateEvent.state}")

        when (stateEvent.state) {
            StateEvent.Started -> {
                waitingVisibility(false)
                enableMenu(endMenu, hasActiveChats)

            }

            StateEvent.ChatWindowLoaded -> {
                if (chatController?.getScope() == StatementScope.BoldScope) waitingVisibility(false)
            }

            StateEvent.ChatWindowDetached -> {
                if (!selfBack && supportFragmentManager.backStackEntryCount == 0) {
                    finish()
                }
                selfBack = false
            }

            StateEvent.Unavailable -> runMain {
                waitingVisibility(false)
                toast(
                    this,
                    "${stateEvent.scope} chat ${stateEvent.state}: ${stateEvent.data}",
                    Toast.LENGTH_SHORT
                )
            }

            StateEvent.Idle -> {
                enableMenu(endMenu, false)
                removeChatFragment()
            }

            StateEvent.Reconnected -> {
                window.decorView.snack(getString(com.genesys.cloud.ui.R.string.async_chat_reconnect))
            }

            StateEvent.Disconnected -> {
                runMain {
                    AlertDialog.Builder(this@MainActivity).apply {
                        setTitle("Chat got disconnected")
                        setMessage(
                            "We were not able to restore chat connection.\nMake sure your device is connected.\nWould you like to continue with the chat or dismiss it?"
                        )
                        setCancelable(false)
                        setPositiveButton("Continue") { dialog, _ ->
                            chatController?.restoreChat(findChatFragment())
                            dialog.dismiss()
                        }
                        setNegativeButton("Dismiss") { dialog, _ ->
                            chatController?.endChat()
                            dialog.dismiss()
                        }
                    }.show()
                }
            }
        }
    }
    //endregion
}
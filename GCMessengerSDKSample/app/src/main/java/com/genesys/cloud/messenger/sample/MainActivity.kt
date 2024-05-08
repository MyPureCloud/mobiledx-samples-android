package com.genesys.cloud.messenger.sample

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.genesys.cloud.integration.messenger.InternalError
import com.genesys.cloud.messenger.sample.chat_form.ChatFormFragment
import com.genesys.cloud.messenger.sample.chat_form.SampleFormViewModel
import com.genesys.cloud.messenger.sample.chat_form.SampleFormViewModelFactory
import com.genesys.cloud.messenger.sample.data.repositories.JsonSampleRepository
import com.genesys.cloud.messenger.sample.data.toMessengerAccount
import com.genesys.cloud.messenger.sample.databinding.ActivityMainBinding
import com.genesys.cloud.ui.structure.controller.*
import com.google.android.material.snackbar.Snackbar
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

    //region - sealed classes
    sealed class ChatState {
        object FirstCreation : ChatState()
        object AfterRotationCreation : ChatState()
        object AfterActivityRecreation : ChatState()
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
    private var dismissChatSnackBar: Snackbar? = null

    private var shouldDefaultBack: Boolean = false
    private val mOnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backPressed()
        }
    }

    //endregion

    //region - lifecycle

    @androidx.annotation.OptIn(androidx.core.os.BuildCompat.PrereleaseSdkCheck::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.uiState.observe(this@MainActivity) { uiState ->

            onAccountDataReady()

            uiState.account?.let { accountRawJson ->

                val messengerAccount = accountRawJson.toMessengerAccount()

                if (uiState.startChat) {
                    createChat(messengerAccount)
                } else if (uiState.testAvailability) {
                    checkAvailability(messengerAccount)
                }
            }
        }

        onBackPressedDispatcher.addCallback(this@MainActivity, mOnBackPressedCallback)

        val existingChatFragment = findChatFragment()

        val state = when {
            existingChatFragment == null -> ChatState.FirstCreation
            viewModel.uiState.value == null -> ChatState.AfterActivityRecreation
            else -> ChatState.AfterRotationCreation
        }

        when (state) {
            ChatState.FirstCreation -> {
                val fragment = ChatFormFragment()
                showFragment(fragment, ChatFormFragment.TAG)
            }

            ChatState.AfterRotationCreation -> {
                // Do nothing, the uiState observer will be triggered again with the previous data
            }

            ChatState.AfterActivityRecreation -> {
                viewModel.loadSavedAccount()
                viewModel.uiState.value?.account?.let { account ->
                    viewModel.startChat(account)
                }
            }
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

    fun backPressed() {
        when (supportFragmentManager.backStackEntryCount) {
            1 -> {
                shouldDefaultBack = true
            }
            0 -> {
                chatController?.destruct()
                finish()
            }
        }
    }

    override fun onBackPressed() {

        dismissChatSnackBar?.takeIf { it.isShown }?.dismiss()

        val fragmentBack = supportFragmentManager.backStackEntryCount > 0
        mOnBackPressedCallback.isEnabled = !fragmentBack
        shouldDefaultBack = fragmentBack

        onBackPressedDispatcher.onBackPressed()
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

    override fun onDestroy() {
        destructChat()
        super.onDestroy()
        mOnBackPressedCallback.remove()
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

                    private fun restoreExistingChatFragmentOr(actionIfNotFound: () -> Unit) {

                        findChatFragment()?.takeIf { it.isAdded }?.let {
                            chatController?.restoreChat(it)
                        } ?: actionIfNotFound()
                    }

                    private fun handleChatStartError(error: NRError) {
                        chatStartError?.invoke()
                        onError(error)
                    }

                    override fun onComplete(result: ChatLoadResponse) {
                        Log.d(TAG, "createChat: ChatLoadedListener.complete")

                        result.error?.let { error ->
                            when (error.errorCode) {
                                // If the error is a conversation creation error
                                NRError.ConversationCreationError -> {
                                    restoreExistingChatFragmentOr {
                                        // If no chat fragment exists, handle chat start error
                                        handleChatStartError(error)
                                    }
                                }
                                else -> {
                                    // If the error is not a conversation creation error, handle chat start error
                                    handleChatStartError(error)
                                }
                            }
                            return
                        }

                        // If there's no error in the response, check if an existing chat fragment can be found
                        restoreExistingChatFragmentOr {
                            // If no chat fragment exists, open a new conversation fragment
                            openConversationFragment(result.fragment!!)
                        }
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
                        ) == false && supportFragmentManager.backStackEntryCount >= 1) backPressed()

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

        val message = "Error: ${error.errorCode}: ${error.description ?: error.reason}"

        when (error.errorCode) {
            NRError.ConfigurationsError, NRError.ConversationCreationError -> {

                Log.e(
                    TAG,
                    "!!!!! Chat ${error.scope} can't be created: $message"
                )

                if (findChatFragment()?.isVisible == true) {
                    onBackPressed()
                }
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
                if (!shouldDefaultBack && supportFragmentManager.backStackEntryCount == 0) {
                    finish()
                }
                shouldDefaultBack = false
            }

            StateEvent.Ended -> {
                // as in case of `Dismiss` press during disconnection
                if(supportFragmentManager.backStackEntryCount > 0){
                    onBackPressed()
                }
            }

            StateEvent.Unavailable -> runMain {
                waitingVisibility(false)
                toast(this, InternalError.DeploymentInactiveStatusError.format(), Toast.LENGTH_SHORT)
            }

            StateEvent.Idle -> {
                enableMenu(endMenu, false)
                removeChatFragment()
                waitingVisibility(false)
            }

            StateEvent.Reconnected -> runMain {
                binding.snackBarLayout.snack(getString(R.string.chat_connection_recovered))
            }

            StateEvent.Reconnecting -> runMain {
                dismissChatSnackBar = Snackbar.make(
                    binding.snackBarLayout,
                    R.string.chat_connection_lost, Snackbar.LENGTH_INDEFINITE
                ).apply {
                    setAction(R.string.dismiss) {
                        chatController?.endChat()
                    }
                    this.setBackgroundTint(Color.parseColor("#ff6600"))
                    this.setActionTextColor(Color.YELLOW)
                }.also { it.show() }
            }

            StateEvent.Disconnected -> runMain {
                AlertDialog.Builder(this@MainActivity).apply {
                    setTitle("Chat was disconnected")
                    setMessage(
                        "We were not able to restore chat connection.\nMake sure your device is connected.")
                    setCancelable(false)
                    setPositiveButton("Reconnect Chat") { dialog, _ ->
                        chatController?.reconnectChat()
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
    //endregion
}
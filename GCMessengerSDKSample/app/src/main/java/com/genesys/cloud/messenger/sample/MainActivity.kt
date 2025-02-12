package com.genesys.cloud.messenger.sample

import android.content.Intent
import android.net.Uri
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
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.genesys.cloud.core.model.StatementScope
import com.genesys.cloud.core.utils.NRError
import com.genesys.cloud.core.utils.getAs
import com.genesys.cloud.core.utils.runMain
import com.genesys.cloud.core.utils.snack
import com.genesys.cloud.core.utils.toast
import com.genesys.cloud.integration.core.AccountInfo
import com.genesys.cloud.integration.core.EndedReason
import com.genesys.cloud.integration.core.StateEvent
import com.genesys.cloud.integration.messenger.InternalError
import com.genesys.cloud.integration.messenger.MessengerAccount
import com.genesys.cloud.messenger.sample.chat_form.ChatFormFragment
import com.genesys.cloud.messenger.sample.chat_form.OktaAuthenticationFragment
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
import java.io.File

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
    private var clearConversationMenu: MenuItem? = null
    private var logoutMenu: MenuItem? = null
    private var reconnectingChatSnackBar: Snackbar? = null

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

        viewModel.authCode.observe(this@MainActivity) {
            if (!viewModel.isAuthenticated){
                onLogout()
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
                val fragment = createChatFormFragment()
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

        reconnectingChatSnackBar?.takeIf { it.isShown }?.dismiss()

        val fragmentBack = supportFragmentManager.backStackEntryCount > 0
        mOnBackPressedCallback.isEnabled = !fragmentBack
        shouldDefaultBack = fragmentBack

        onBackPressedDispatcher.onBackPressed()
    }

    //endregion

    //region - menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        this.endMenu = menu?.findItem(R.id.end_current_chat)
        this.clearConversationMenu = menu?.findItem(R.id.clear_conversation)
        this.logoutMenu = menu?.findItem(R.id.logout)

        updateMenuVisibility()

        return true
    }

    private fun updateMenuVisibility(){
        endMenu?.isVisible = hasActiveChats
        clearConversationMenu?.isVisible = hasActiveChats
        logoutMenu?.isVisible = viewModel.isAuthenticated
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

            R.id.clear_conversation -> {
                showClearConversationDialog()
                return true
            }

            R.id.logout -> {
                showFragment(OktaAuthenticationFragment.newLogoutInstance(),
                    OktaAuthenticationFragment.TAG, true)
                return true
            }

            else -> {
            }
        }
        return false
    }

    private fun showClearConversationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_conversation_dialog_title)
            .setMessage(R.string.clear_conversation_dialog_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear_conversation_dialog_positive_button) { _, _ -> chatController?.clearConversation()}
            .create()
            .show()
    }

    private fun onLogout() {
        chatController?.logoutFromAuthenticatedSession()
    }

    //endregion

    //region - functionality
    private fun destructChat() {
        chatController?.takeUnless { it.wasDestructed }?.run {
            terminateChat()
            destruct()
        }
    }

    private fun createChatFormFragment(): ChatFormFragment {
        return ChatFormFragment().apply {
            openFragment = { fragment, tag ->
                showFragment(fragment, tag, true)
            }
        }
    }

    private fun createChat(account: AccountInfo, chatStartError: (() -> Unit)? = null) {
        waitingVisibility(true)

        if (account is MessengerAccount && viewModel.isAuthenticated){
            account.setAuthenticationInfo(viewModel.authCode.value!!, viewModel.redirectUri, viewModel.codeVerifier)
        }

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
                Log.e(TAG,"!!!!! Chat ${error.scope} can't be created: $message")
                if (findChatFragment()?.isVisible == true) {
                    onBackPressed()
                }
            }

            NRError.GeneralError -> {
                removeChatFragment()
            }
        }
        toast(this, message, Toast.LENGTH_SHORT)
    }

    override fun onChatStateChanged(stateEvent: StateEvent) {

        Log.d(TAG, "${stateEvent.scope} chat in state: ${stateEvent.state}")

        when (stateEvent.state) {
            StateEvent.Started -> {
                waitingVisibility(false)
                updateMenuVisibility()
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

            StateEvent.Closed -> {
                onChatClosed(stateEvent.data.getAs<EndedReason>())
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
                removeChatFragment()
                waitingVisibility(false)
                updateMenuVisibility()
            }

            StateEvent.Reconnected -> runMain {
                binding.snackBarLayout.snack(getString(R.string.chat_connection_recovered))
            }

            StateEvent.Reconnecting -> runMain {
                reconnectingChatSnackBar = Snackbar.make(
                    binding.snackBarLayout,
                    R.string.chat_connection_reconnecting, Snackbar.LENGTH_INDEFINITE
                ).also { it.show() }
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

    override fun onUrlLinkSelected(url: String) {
        toast(this, "Url link selected: $url", Toast.LENGTH_SHORT)

        try {
            val intent = if (isFileUrl(url)) {
                val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", File(url))

                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_VIEW).setData(Uri.parse(url))
            }
            startActivity(intent)

        } catch (e: Exception) {
            Log.w(TAG, "failed to activate link on default app: " + e.message)
        }
    }

    private fun isFileUrl(url: String): Boolean {
        return url.startsWith("/")
    }


    private fun onChatClosed(reason: EndedReason?) {
        removeChatFragment()
        updateMenuVisibility()
        when (reason) {
            EndedReason.SessionLimitReached -> "You have been logged out because the session limit was exceeded."
            EndedReason.Logout -> "Logout successful."
            EndedReason.ConversationCleared -> "Conversation was cleared."
            else -> "Chat was closed. ($reason)"
        }.let { message ->
            toast(this, message, Toast.LENGTH_LONG)
        }
    }
    //endregion
}
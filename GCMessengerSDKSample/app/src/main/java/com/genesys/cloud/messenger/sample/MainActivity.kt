package com.genesys.cloud.messenger.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.genesys.cloud.core.model.StatementScope
import com.genesys.cloud.core.utils.IOScope
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
import com.genesys.cloud.messenger.sample.data.FloatingSnackbar
import com.genesys.cloud.messenger.sample.data.PermissionHandler
import com.genesys.cloud.messenger.sample.data.PermissionHandler.Companion.PERMISSION_POST_NOTIFICATIONS
import com.genesys.cloud.messenger.sample.data.repositories.JsonSampleRepository
import com.genesys.cloud.messenger.sample.data.toMessengerAccount
import com.genesys.cloud.messenger.sample.databinding.ActivityMainBinding
import com.genesys.cloud.ui.structure.controller.*
import com.genesys.cloud.ui.structure.controller.pushnotifications.ChatPushNotificationIntegration
import com.google.android.gms.tasks.Tasks
import com.genesys.cloud.ui.structure.controller.auth.AuthenticationStatus
import com.genesys.cloud.ui.structure.elements.ChatElement
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    private var minimizeMenu: MenuItem? = null
    private var endMenu: MenuItem? = null
    private var clearConversationMenu: MenuItem? = null
    private var logoutMenu: MenuItem? = null
    private var reconnectingChatSnackBar: Snackbar? = null

    private var shouldDefaultBack: Boolean = false

    private var pushNotificationBroadcastReceiver: BroadcastReceiver? = null
    private val permissionHandler = PermissionHandler(this)
    //endregion

    //region - lifecycle

    @OptIn(androidx.core.os.BuildCompat.PrereleaseSdkCheck::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = Build.VERSION.SDK_INT >= 35

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val root = findViewById<FrameLayout>(R.id.main_layout)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply padding so UI elements aren’t overlapped
            view.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )

            insets
        }

        viewModel.uiState.observe(this@MainActivity) { uiState ->

            onAccountDataReady()

            uiState.account?.let { accountRawJson ->

                val messengerAccount = accountRawJson.toMessengerAccount()

                if (uiState.startChat) {
                    prepareAndCreateChat(messengerAccount)
                } else if (uiState.testAvailability) {
                    checkAvailability(messengerAccount)
                } else if (uiState.enablePush) {
                    enablePushNotifications(messengerAccount)
                } else if (uiState.disablePush) {
                    disablePushNotifications(messengerAccount)
                }
            }
        }

        viewModel.authCode.observe(this@MainActivity) {
            if (viewModel.isAuthenticated && !viewModel.hasAuthCode) {
                onLogout()
            }
        }

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
                if (existingChatFragment != null) {
                    supportFragmentManager.popBackStackImmediate()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (supportFragmentManager.findFragmentByTag(ChatFormFragment.TAG) as? ChatFormFragment)?.openFragment =
            { fragment, tag ->
                showFragment(fragment, tag, true)
            }
    }

    override fun onStart() {
        super.onStart()
        registerPushNotificationReceiver()
    }

    override fun onPause() {
        super.onPause()
        waitingVisibility(false)
    }

    override fun onStop() {
        if (isFinishing) {
            destructChat()
        }
        unregisterReceiver(pushNotificationBroadcastReceiver)
        super.onStop()
    }

    private fun handleBackStackCount() {
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
        chatController?.endChat(false)
        reconnectingChatSnackBar?.takeIf { it.isShown }?.dismiss()

       handleBackStackCount()

        if (!supportFragmentManager.isStateSaved) super.onBackPressed()
    }

    //endregion

    //region - menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        this.minimizeMenu = menu?.findItem(R.id.minimize_chat)
        this.endMenu = menu?.findItem(R.id.end_current_chat)
        this.clearConversationMenu = menu?.findItem(R.id.clear_conversation)
        this.logoutMenu = menu?.findItem(R.id.logout)

        updateMenuVisibility()

        return true
    }

    private fun updateMenuVisibility(){
        minimizeMenu?.isVisible = hasActiveChats
        endMenu?.isVisible = hasActiveChats
        clearConversationMenu?.isVisible = hasActiveChats
        logoutMenu?.isVisible = viewModel.isAuthenticated
    }

    override fun onDestroy() {
        destructChat()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.minimize_chat -> {
                handleBackStackCount()
                super.onBackPressed()
                return true
            }

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
        return ChatFormFragment()
    }

    private fun prepareAndCreateChat(account: AccountInfo, chatStartError: (() -> Unit)? = null) {
        waitingVisibility(true)

        viewModel.isAuthenticated = false

        AuthenticationStatus.shouldAuthorize(
            context = this,
            account = account
        ) { shouldAuthorize ->

            if (shouldAuthorize) {

                if (account is MessengerAccount && viewModel.hasAuthCode) {
                    viewModel.authCode.value?.let {
                            authCode -> account.setAuthenticationInfo(authCode,
                        viewModel.redirectUri, viewModel.codeVerifier)
                    }

                    viewModel.isAuthenticated = true
                } else if(!viewModel.idToken.value.isNullOrEmpty()) {
                    viewModel.idToken.value?.let { idToken->
                        // TODO GMMS-10534 account.setImplicitAuthenticationInfo
                    }
                    viewModel.isAuthenticated = true
                }
            } else {
                viewModel.isAuthenticated = true
            }

            createChat(account, chatStartError)
        }
    }

    private fun createChat(account: AccountInfo, chatStartError: (() -> Unit)? = null) {
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
        showNotificationPermissionIndicator()
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
                        ) == false && supportFragmentManager.backStackEntryCount >= 1) {
                    onBackPressed()
                }

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

    private fun showNotificationPermissionIndicator() {
        if (ContextCompat.checkSelfPermission(this, PERMISSION_POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            FloatingSnackbar.make(
                binding.root,
                R.string.notifications_disabled_indicator_message,
                10000
            ).apply {
                setAction(R.string.settings) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.setData(uri)
                    startActivity(intent)
                    dismiss()
                }
            }.show()
        }
    }

    private fun enablePushNotifications(accountInfo: AccountInfo) {
        Log.d(TAG, "enablePushNotifications()")
        if (ContextCompat.checkSelfPermission(this, PERMISSION_POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionGranted(accountInfo)
        } else {
            permissionHandler.requestPermission(
                PERMISSION_POST_NOTIFICATIONS,
                { notificationPermissionGranted(accountInfo) },
                ::notificationPermissionDenied
            )
        }
    }

    private fun notificationPermissionGranted(accountInfo: AccountInfo) {
        Log.d(TAG, "enablePushNotifications()")
        val deviceToken = retrieveDeviceTokenForPush()
        if (deviceToken != null) {
            Log.d(TAG, "deviceToken read successfully: $deviceToken")
            lifecycleScope.launch {
                ChatPushNotificationIntegration.setPushToken(applicationContext, deviceToken, accountInfo as MessengerAccount)
                    .onSuccess {
                        Log.d(TAG, "ChatPushNotificationIntegration.setPushToken() succeed.")
                        viewModel.setPushEnabled(true)
                        binding.snackBarLayout.snack(
                            "Push Notifications enabled successfully",
                            Snackbar.LENGTH_LONG
                        )
                    }.onFailure {
                        Log.e(TAG, "ChatPushNotificationIntegration.setPushToken() failed.", it.data as? Throwable)
                        handleSetPushTokenFailure(it, accountInfo)
                        binding.snackBarLayout.snack(
                            "Registration for Push Notifications failed",
                            Snackbar.LENGTH_LONG
                        ).apply{
                            view.setOnClickListener { dismiss() }
                        }
                    }
            }
        } else {
            Log.d(TAG, "deviceToken not found")
            binding.snackBarLayout.snack(
                getString(R.string.enable_push_failed_message),
                Snackbar.LENGTH_LONG
            )
        }
    }

    private fun handleSetPushTokenFailure(error: NRError, accountInfo: AccountInfo) {
        when (error.errorCode) {
            NRError.PushDeploymentIdMismatch -> {
                AlertDialog.Builder(this)
                    .setMessage(error.description)
                    .setPositiveButton(R.string.disable_push_text) { _, _ ->
                        disablePushNotifications(accountInfo)
                    }
                    .create()
                    .show()
            }
        }
    }

    private fun notificationPermissionDenied() {
        binding.snackBarLayout.snack(
            getString(R.string.enable_push_failed_message),
            Toast.LENGTH_LONG
        )
    }

    private fun retrieveDeviceTokenForPush(): String? {
        return runBlocking {
            var token :String? = null
            IOScope().launch {
                token = Tasks.await(FirebaseMessaging.getInstance().token)
                Log.d(TAG, "deviceToken received: $token")
            }.join()
            token
        }
    }

    private fun disablePushNotifications(accountInfo: AccountInfo) {
        Log.d(TAG, "disablePushNotifications()")
        (accountInfo as? MessengerAccount)?.let { account ->
            lifecycleScope.launch {
                ChatPushNotificationIntegration.removePushToken(applicationContext, account)
                    .onSuccess {
                        Log.d(TAG, "ChatPushNotificationIntegration.removePushToken() succeed.")
                        viewModel.setPushEnabled(false)
                        binding.snackBarLayout.snack(
                            "Push Notifications disabled successfully",
                            Snackbar.LENGTH_LONG
                        )
                    }.onFailure {
                        Log.e(TAG, "ChatPushNotificationIntegration.removePushToken() failed.", it.data as? Throwable)
                        binding.snackBarLayout.snack(
                            "Unregister from Push Notifications failed",
                            Snackbar.LENGTH_LONG
                        )
                    }
            }
        }
    }

    private fun registerPushNotificationReceiver() {
        pushNotificationBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    pushNotificationReceived(
                        intent.getStringExtra(AppFirebaseMessagingService.EXTRA_KEY_REMOTE_MESSAGE_TITLE),
                        intent.getStringExtra(AppFirebaseMessagingService.EXTRA_KEY_REMOTE_MESSAGE_BODY)
                    )
                } else {
                    Log.e(TAG, "Broadcast message with action ${AppFirebaseMessagingService.PUSH_NOTIFICATION_RECEIVED} received, but Intent is not present.")
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(AppFirebaseMessagingService.PUSH_NOTIFICATION_RECEIVED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pushNotificationBroadcastReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(pushNotificationBroadcastReceiver, intentFilter)
        }
    }

    private fun pushNotificationReceived(messageTitle: String?, messageBody: String?) {
        if (hasActiveChats) return
        if (messageBody == null) return // Message body is a must in a Push notification
        showAlertDialog(messageTitle, messageBody)
    }

    private fun showAlertDialog(messageTitle: String?, messageBody: String) {
        val dialog = AlertDialog.Builder(this)
            .setPositiveButton(com.genesys.cloud.ui.R.string.ok) { _, _ -> }
            .create()
        if (messageTitle == null) {
            dialog.setTitle(messageBody)
        } else {
            dialog.setTitle(messageTitle)
            dialog.setMessage(messageBody)
        }
        dialog.show()
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

            NRError.ClientNotAuthenticatedError -> {
                removeChatFragment()
            }

            NRError.GeneralError -> {
                removeChatFragment()
            }
        }
        toast(this, message, Toast.LENGTH_LONG)
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

            StateEvent.Unavailable -> runMain {
                waitingVisibility(false)
                toast(this, InternalError.DeploymentInactiveStatusError.format(), Toast.LENGTH_SHORT)
            }

            StateEvent.Idle -> {
                removeChatFragment()
                waitingVisibility(false)
                updateMenuVisibility()
                if (supportFragmentManager.backStackEntryCount > 0) {
                    onBackPressed()
                }
            }

            StateEvent.Reconnected -> runMain {
                showSnackbar(getString(R.string.chat_connection_recovered))
            }

            StateEvent.Reconnecting -> runMain {
                reconnectingChatSnackBar = showSnackbar(
                    getString(R.string.chat_connection_reconnecting), Snackbar.LENGTH_INDEFINITE)
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

    override fun onUrlLinkClicked(url: String): Boolean {
        toast(this, "Url link selected: $url", Toast.LENGTH_SHORT)

        try {
            val intent = if (isFileUrl(url)) {
                val uri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    File(url)
                )

                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_VIEW).setData(Uri.parse(url))
            }
            startActivity(intent)
            return true

        } catch (e: Exception) {
            Log.w(TAG, "failed to activate link on default app: " + e.message)
            return false
        }
    }

    private fun isFileUrl(url: String): Boolean {
        return url.startsWith("/")
    }

    private fun onChatClosed(reason: EndedReason?) {
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

    /*override fun onChatElementReceived(chatElement: ChatElement) {
        super.onChatElementReceived(chatElement)
        Log.i(TAG, "onChatElementReceived(${chatElement.text})")
        if (supportFragmentManager.backStackEntryCount == 0) {
            showAlertDialog("New Incoming message", chatElement.text)
        }
    }*/

    private fun showSnackbar(message: String, timeout: Int = Snackbar.LENGTH_LONG): Snackbar {
        val snackbar = Snackbar.make(binding.snackBarLayout,
            message, timeout)

        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { view, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            view.translationY = if (imeVisible) -imeHeight.toFloat() else 0f

            insets
        }

        snackbar.show()
        return snackbar
    }

    //endregion
}

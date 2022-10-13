package com.genesys.mobile.messenger.sdk.gcmessengersdksample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.genesys.cloud.core.model.StatementScope
import com.genesys.cloud.core.utils.NRError
import com.genesys.cloud.core.utils.runMain
import com.genesys.cloud.core.utils.snack
import com.genesys.cloud.core.utils.toast
import com.genesys.cloud.integration.bot.BotChatSettings
import com.genesys.cloud.integration.core.AccountInfo
import com.genesys.cloud.integration.core.StateEvent
import com.genesys.cloud.integration.core.configuration.ChatSettings
import com.genesys.cloud.ui.components.ChatbarUnitConfig
import com.genesys.cloud.ui.structure.annotations.CompoundDrawableLocation
import com.genesys.cloud.ui.structure.configuration.ChatUIProvider
import com.genesys.cloud.ui.structure.configuration.ConfigurationCompletion
import com.genesys.cloud.ui.structure.configuration.ConfigurationsProvider
import com.genesys.cloud.ui.structure.controller.*
import com.genesys.cloud.ui.structure.history.ChatElementListener
import com.genesys.cloud.ui.views.DrawableConfig
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.defs.ChatType
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.defs.DataKeys
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.repositories.JsonSampleRepository
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.databinding.ActivityMainBinding
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.presentation.chat_form.ChatFormFragment
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.presentation.chat_form.SampleFormViewModel
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.presentation.chat_form.SampleFormViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity(), ConfigurationsProvider, ChatElementListener,
    ChatEventListener {

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
    private var accountProvider = AccountHandler(false)
    private var endMenu: MenuItem? = null
    private var selfBack: Boolean = false
    //endregion

    //region - lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.updateChatType(ChatType.Messenger)
        viewModel.createFormFields()

        viewModel.sampleData.observe(this@MainActivity, Observer {

            onAccountDataReady()

            viewModel.account?.let { accountInfo ->
                when (it?.account?.get(DataKeys.Intent)?.asString) {
                    DataKeys.TestChatAvailability -> checkAvailability(accountInfo)
                    else -> createChat(accountInfo)
                }
            }
        })

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
            1 -> { //chatController?.destruct()
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
            menu?.findItem(R.id.end_current_chat) // this.destructMenu = menu?.findItem(R.id.destruct_chat)

        if (hasActiveChats) {
            enableMenu(
                endMenu,
                true
            ) // enableMenu(destructMenu, hasChatController() && !chatController.wasDestructed)
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
                    configurationsProvider(this@MainActivity)
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
//                background = getDrawable(R.drawable.toast)?.apply {
//                    mutate().setColorFilter(if (it.isAvailable) Color.GREEN else Color.RED, PorterDuff.Mode.MULTIPLY)
//                }
            )
        }
    }

    private fun waitingVisibility(visible: Boolean) {
        binding.progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun findChatFragment() : Fragment? = supportFragmentManager.findFragmentByTag(CONVERSATION_FRAGMENT_TAG)

    private fun onAccountDataReady() {
        supportFragmentManager.popBackStack(
            ChatFormFragment.TAG,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }

    fun onAccountReady(account: AccountInfo, chatStartError: (() -> Unit)?) {
        accountProvider.update(account)

        createChat(account, chatStartError)
    }

    private fun isFileUrl(url: String): Boolean {
        return url.startsWith("/")
    }
    //endregion

    //region - ConfigurationsProvider
    override fun provide(
        settings: ChatSettings,
        uiConfiguration: ChatUIProvider,
        scope: StatementScope,
        callback: ConfigurationCompletion
    ) {
        settings.applyOther(viewModel.chatSettings)

        //settings.enabled = false
        // settings.voiceSettings.voiceSupport = VoiceSupport.SpeechRecognition
        settings.requestTimeout = 34500

        (settings as? BotChatSettings)?.apply {
            feedback.enabled = false // welcomeArticleId = BlankArticleId // "919800402"
        }

        callback(settings, getChatUIProvider(uiConfiguration, scope))
    }

    // For UI config tests
    private fun getChatUIProvider(
        chatUIProvider: ChatUIProvider? = null,
        scope: StatementScope
    ): ChatUIProvider {
        return (chatUIProvider ?: ChatUIProvider(this@MainActivity)).apply {

            //fastScrollUIProvider.uiConfig.delta = 10
            //timestampUIProvider.uiConfig.style = TimestampStyle("dd/mm, HH:MM", 16, Color.RED)
            //            timestampUIProvider.uiConfig.enabled = false

            /*chatElementsUIProvider.incomingUIProvider.configure = {
                it.enableTimestampView(false)
                it
            }*/

            /*
            chatElementsUIProvider.incomingUIProvider.readmoreUIProvider.readmoreConfig.apply {
                thresholdRange = null
                threshold = null
            }*/

            /*if (scope == StatementScope.BoldScope) {
                Log.d(TAG, "${this}: Updating Bold UI configurations on App")
                chatElementsUIProvider.systemUIProvider.systemMessageConfig.apply {
                    uiBackground = getDrawable(android.R.drawable.toast_frame)?.mutate()?.apply {
                        setTint(Color.RED)
                    }
                    styleConfig = StyleConfig(size = 14, color = Color.DKGRAY)
                }

                chatElementsUIProvider.systemUIProvider.removableMessageConfig.apply {
                    uiBackground = getDrawable(android.R.drawable.toast_frame)?.mutate()
                    styleConfig = StyleConfig(size = 13, color = Color.RED)
                }

                // fixme: overrides the configured Bold handler changes
                chatElementsUIProvider.incomingUIProvider.configure = { adapter ->
                    adapter.apply {
                        uiBackground?.setTint(Color.YELLOW)
                        enableTimestampView(false)
                    }
                }
            }
*/
            /*if (scope == StatementScope.NanoBotScope) {
                Log.d(TAG, "Updating Bot UI configurations on App")

                chatElementsUIProvider.incomingUIProvider.configure = { adapter ->
                    adapter.uiBackground?.colorFilter = PorterDuffColorFilter(Color.CYAN, PorterDuff.Mode.SRC_ATOP)
                    adapter
                }

                chatElementsUIProvider.incomingUIProvider.persistentOptionsUIProvider.contentStyleConfig = { adapter ->
                    adapter?.setTextBackground(
                        ContextCompat.getDrawable(this@MainActivity, com.genesys.cloud.ui.R.drawable.incoming_text_back)
                            ?.mutate()?.apply {
                                setColorFilter(Color.CYAN, PorterDuff.Mode.SRC_ATOP)
                            }) // needed to make sure the persistent options stays white.
                    adapter?.uiBackground = ContextCompat.getDrawable(this@MainActivity,
                        com.genesys.cloud.ui.R.drawable.incoming_persistent_back)?.mutate()
                    adapter
                }
            }*/

            // typingUIProvider.enabled = false

            chatBarCmpUiProvider.configure = {
                it.configEndCmp(ChatbarUnitConfig().apply {
                    this.drawableConfig =
                        DrawableConfig(getDrawable(com.genesys.cloud.ui.R.drawable.baseline_close_black_18)).apply {
                            compoundDrawablesPadding = 0
                            drawableLocation = CompoundDrawableLocation.RIGHT
                        }
                })
                it
            }

            // uncomment to test `Datestamp` configuration:
            /*datestampUIProvider.uiConfig.datestampFormatFactory = if (scope == StatementScope.BoldScope) {
                FriendlyDatestampFormatFactory(this@MainActivity)
            } else SimpleDatestampFormatFactory()*/
        }
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
                ) //removeChatFragment()
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

            StateEvent.Disconnected -> { //window.decorView.snack(getString(com.genesys.cloud.ui.R.string.chat_disconnection_error))
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

    override fun onUrlLinkSelected(url: String) {
        toast(this, "Url link selected: $url", Toast.LENGTH_SHORT)

        // sample code for handling given link
        try {
            val intent = if (isFileUrl(url)) {
                val uri = FileProvider.getUriForFile(
                    this,
                    "com.genesys.cloud.internal.BuildConfig.APPLICATION_ID" + ".provider",
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

        } catch (e: Exception) {
            Log.w(TAG, "failed to activate link on default app: " + e.message)
        }
    }
    //endregion
}
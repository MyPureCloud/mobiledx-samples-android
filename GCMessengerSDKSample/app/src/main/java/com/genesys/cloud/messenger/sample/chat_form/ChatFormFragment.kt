package com.genesys.cloud.messenger.sample.chat_form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.genesys.cloud.core.utils.toast
import com.genesys.cloud.integration.messenger.MessengerAccount
import com.genesys.cloud.messenger.sample.BuildConfig
import com.genesys.cloud.messenger.sample.R
import com.genesys.cloud.messenger.sample.data.defs.DataKeys
import com.genesys.cloud.messenger.sample.data.repositories.JsonSampleRepository
import com.genesys.cloud.messenger.sample.data.toMap
import com.genesys.cloud.messenger.sample.databinding.FragmentChatFormBinding
import com.genesys.cloud.ui.structure.controller.ChatAvailability
import com.genesys.cloud.ui.structure.controller.auth.AuthenticationStatus
import com.google.gson.JsonObject

class ChatFormFragment : Fragment() {

    //region - attributes
    private val viewModel: SampleFormViewModel by activityViewModels {
        SampleFormViewModelFactory(
            JsonSampleRepository(
                requireActivity().applicationContext
            )
        )
    }
    private lateinit var binding: FragmentChatFormBinding

    internal var onFormReady: (() -> Unit)? = null

    //endregion

    //region - lifecycle
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeSavedAccount()
        binding.startChat.setOnClickListener {
            startChat()
        }
        binding.chatAvailability.setOnClickListener {
            testChatAvailability()
        }
        binding.loginButton.setOnClickListener {
            onLoginClicked()
        }
        binding.pushButton.setOnClickListener {
            onPushClicked()
        }
        binding.versionTextView.text = getString(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        binding.deploymentIdEditText.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                viewModel.updateLatestTypedDeploymentId(text.toString())
            }
        )
        viewModel.updateLatestTypedDeploymentId(binding.deploymentIdEditText.text.toString())
        viewModel.pushEnabled.observe(requireActivity()) { enabled->
            binding.pushButton.text = getString(
                if (enabled) R.string.disable_push_text
                else R.string.enable_push_text
            )
        }

        onFormReady?.invoke()
    }
    //endregion

    //region - functionality
    internal var openFragment: (fragment: Fragment, tag: String) -> Unit = { _, _ -> }

    private fun onLoginClicked() {
        try {
            openFragment.invoke(
                OktaAuthenticationFragment.newLoginInstance(),
                OktaAuthenticationFragment.TAG
            )
        } catch (e: IllegalStateException) {
            toast(requireContext(), e.message ?: "Cannot login.")
        }
    }

    private fun onPushClicked() {
        createAccountData()?.let { accountData -> viewModel.changePushEnablement(accountData) }
    }

    private fun observeSavedAccount() {
        viewModel.uiState.observe(viewLifecycleOwner) { sampleData ->

            if (sampleData.startChat || sampleData.testAvailability) {
                return@observe
            }

            sampleData.account?.let { accountRawJson ->
                val deploymentId = accountRawJson[DataKeys.DeploymentId]?.asString
                val domain = accountRawJson[DataKeys.Domain]?.asString

                setAccountEditTextValue(binding.deploymentIdEditText, deploymentId)
                setAccountEditTextValue(binding.domainNameEditText, domain)

                binding.customAttributesEditText.setText(accountRawJson[DataKeys.CustomAttributes]?.asString)

                accountRawJson[DataKeys.Logging]?.let {
                    binding.loggingSwitch.isEnabled = it.asBoolean
                }

                setLoginButtonState(deploymentId, domain)
            }
        }
        viewModel.loadSavedAccount()
    }

    private fun setAccountEditTextValue(editText: AppCompatEditText, text: String?) {
        editText.setText(text)
        editText.doOnTextChanged { _, _, _, _ ->
            val deploymentId = binding.deploymentIdEditText.text.toString()
            val domain = binding.domainNameEditText.text.toString()

            setLoginButtonState(deploymentId, domain)
        }
    }

    private fun setLoginButtonState(deploymentId: String?, domain: String?) {
        if (deploymentId.isNullOrEmpty() || domain.isNullOrEmpty()) return

        val account = MessengerAccount(deploymentId, domain)

        // Verify that chat is available with actual deployment id and domain.
        // Since we handling text on change for both fields:
        // * there is no reason to call shouldAuthorize on invalid deployment IDs\domains
        // * otherwise just reset UI to default state
        ChatAvailability.checkAvailability(account = account) { res ->
            if (res.isAvailable) {
                setLoginButtonStateImpl(account)
            } else {
                binding.loginButton.visibility = View.VISIBLE
            }
        }
    }

    private fun setLoginButtonStateImpl(account: MessengerAccount) {
        context?.let {
            AuthenticationStatus.shouldAuthorize(
                context = it,
                account = account
            ) { shouldAuthorize ->
                    binding.loginButton.visibility =
                        if (shouldAuthorize) View.VISIBLE else View.INVISIBLE
            }
        }
    }

    private fun startChat() {
        val accountData = createAccountData()
        accountData?.let { viewModel.startChat(accountData) }
    }

    private fun testChatAvailability() {
        val accountData = createAccountData()
        accountData?.let { viewModel.testChatAvailability(it) }
    }

    private fun createAccountData(): JsonObject? {
        val accountData = JsonObject()

        if (binding.deploymentIdEditText.text.toString().isEmpty()) {
            binding.deploymentIdEditText.presentError(getString(R.string.required_error))
            return null
        }

        accountData.addProperty(
            DataKeys.DeploymentId,
            binding.deploymentIdEditText.text.toString()
        )

        if (binding.domainNameEditText.text.toString().isEmpty()) {
            binding.domainNameEditText.presentError(getString(R.string.required_error))
            return null
        }

        accountData.addProperty(
            DataKeys.Domain,
            binding.domainNameEditText.text.toString()
        )

        val customAttributesValue = binding.customAttributesEditText.text.toString()
        if (customAttributesValue.isNotEmpty()) {
            val regex = Regex("""^\s*\{(?:\s*"[^"]*"\s*:\s*"[^"]*"\s*(?:,\s*"[^"]*"\s*:\s*"[^"]*"\s*)*)\}\s*$""")
            val keyValueMap =
                if (regex.matches(customAttributesValue)) customAttributesValue.toMap()
                else null

            if (keyValueMap != null) {
                accountData.addProperty(
                    DataKeys.CustomAttributes,
                    customAttributesValue
                )
            } else {
                binding.customAttributesEditText.presentError(getString(R.string.custom_attributes_error))
                return null
            }
        }

        accountData.addProperty(DataKeys.Logging, binding.loggingSwitch.isEnabled)

        return accountData
    }
    //endregion

    //region - companion object
    companion object {
        const val TAG = "ChatForm"
    }
    //endregion
}

private fun AppCompatEditText.presentError(errorMessage: String) {
    requestFocus()
    error = errorMessage
}

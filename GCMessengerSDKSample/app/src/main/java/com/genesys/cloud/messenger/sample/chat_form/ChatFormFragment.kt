package com.genesys.cloud.messenger.sample.chat_form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.genesys.cloud.messenger.sample.BuildConfig
import com.genesys.cloud.messenger.sample.R
import com.genesys.cloud.messenger.sample.data.defs.DataKeys
import com.genesys.cloud.messenger.sample.data.repositories.JsonSampleRepository
import com.genesys.cloud.messenger.sample.data.toMap
import com.genesys.cloud.messenger.sample.databinding.FragmentChatFormBinding
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
        binding.versionTextView.text = getString(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }
    //endregion

    //region - functionality
    private fun observeSavedAccount() {
        viewModel.uiState.observe(viewLifecycleOwner) { sampleData ->

            if (sampleData.startChat || sampleData.testAvailability) {
                return@observe
            }

            sampleData.account?.let { accountRawJson ->
                binding.deploymentIdEditText.setText(accountRawJson[DataKeys.DeploymentId]?.asString)
                binding.domainNameEditText.setText(accountRawJson[DataKeys.Domain]?.asString)
                binding.customAttributesEditText.setText(accountRawJson[DataKeys.CustomAttributes]?.asString)
                accountRawJson[DataKeys.Logging]?.let {
                    binding.loggingSwitch.isEnabled = it.asBoolean
                }
            }
        }
        viewModel.loadSavedAccount()
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

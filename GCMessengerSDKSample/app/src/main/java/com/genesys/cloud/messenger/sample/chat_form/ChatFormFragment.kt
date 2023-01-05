package com.genesys.cloud.messenger.sample.chat_form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.genesys.cloud.messenger.sample.R
import com.genesys.cloud.messenger.sample.data.defs.DataKeys
import com.genesys.cloud.messenger.sample.data.repositories.JsonSampleRepository
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

        if (!isValid(
                binding.deploymentIdEditText,
                binding.deploymentIdEditText.text.toString(),
                true,
            )
        ) return null
        accountData.addProperty(
            DataKeys.DeploymentId,
            binding.deploymentIdEditText.text.toString()
        )

        if (!isValid(
                binding.domainNameEditText,
                binding.domainNameEditText.text.toString(),
                true
            )
        ) return null
        accountData.addProperty(DataKeys.Domain, binding.domainNameEditText.text.toString())

        accountData.addProperty(DataKeys.Logging, binding.loggingSwitch.isEnabled)

        return accountData
    }

    private fun isValid(
        index: View,
        value: String?,
        required: Boolean
    ): Boolean {

        val presentError: ((message: String) -> Unit) = { message ->
            (index as? TextView)?.apply {
                this.requestFocus()
                error = message
            }
        }

        val requiredCheck = {
            (!(required && value.isNullOrEmpty())).also {
                if (!it) presentError(getString(R.string.required_error))
            }
        }

        return requiredCheck()
    }
    //endregion

    //region - companion object
    companion object {
        const val TAG = "ChatForm"
    }
    //endregion
}
package com.genesys.gcmessengersdksample.presentation.chat_form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.genesys.gcmessengersdksample.R
import com.genesys.gcmessengersdksample.data.defs.DataKeys
import com.genesys.gcmessengersdksample.data.getString
import com.genesys.gcmessengersdksample.data.repositories.JsonSampleRepository
import com.genesys.gcmessengersdksample.databinding.FragmentChatFormBinding
import com.google.gson.JsonObject
import java.util.regex.Pattern

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

        binding.startChat.setOnClickListener {
            collaborateData(DataKeys.StartChat)
        }

        binding.chatAvailability.setOnClickListener {
            collaborateData(DataKeys.TestChatAvailability)

        }
    }
    //endregion

    //region - functionality
    private fun collaborateData(intent: String) {

        val accountData = JsonObject().apply {
            addProperty(DataKeys.Intent, intent)

            val validator = getString("validator")?.toPattern()

            if (!isValid(
                    binding.deploymentIdEditText,
                    binding.deploymentIdEditText.text.toString(),
                    true,
                    validator
                )
            ) return
            addProperty(DataKeys.DeploymentId, binding.deploymentIdEditText.text.toString())

            if (!isValid(
                    binding.domainNameEditText,
                    binding.domainNameEditText.text.toString(),
                    true,
                    validator
                )
            ) return
            addProperty(DataKeys.Domain, binding.domainNameEditText.text.toString())

            if (!isValid(
                    binding.tokenStoreKeyEditText,
                    binding.tokenStoreKeyEditText.text.toString(),
                    false,
                    validator
                )
            ) return
            addProperty(DataKeys.TokenStoreKey, binding.tokenStoreKeyEditText.text.toString())

            addProperty(DataKeys.Logging, binding.loggingSwitch.isEnabled)

        }

        viewModel.onAccountData(accountData)
    }


    private fun isValid(
        index: View,
        value: String?,
        required: Boolean,
        validator: Pattern?
    ): Boolean {

        val presentError: ((message: String) -> Unit) = { message ->
            (index as? TextView)?.apply {
                this.requestFocus()
                error = message
            }
        }

        val validatorCheck = {

            validator?.let { // -> If there is a validator, we check that the value passes (empty is valid)

                (value.isNullOrEmpty() || validator.matcher(value).matches()).also {
                    if (!it) presentError(getString(R.string.validation_error))
                }

            } ?: true

        }

        val requiredCheck = {
            (!(required && value.isNullOrEmpty())).also {
                if (!it) presentError(getString(R.string.required_error))
            }
        }

        return validatorCheck() && requiredCheck()
    }
    //endregion

    //region - companion object
    companion object {
        const val TAG = "ChatForm"
    }
    //endregion
}
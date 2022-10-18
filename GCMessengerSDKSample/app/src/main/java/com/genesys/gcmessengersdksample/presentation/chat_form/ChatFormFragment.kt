package com.genesys.gcmessengersdksample.presentation.chat_form

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.genesys.gcmessengersdksample.R
import com.genesys.gcmessengersdksample.data.defs.DataKeys
import com.genesys.gcmessengersdksample.data.defs.FieldProps
import com.genesys.gcmessengersdksample.data.getSelectedText
import com.genesys.gcmessengersdksample.data.getString
import com.genesys.gcmessengersdksample.data.repositories.JsonSampleRepository
import com.genesys.gcmessengersdksample.data.toObject
import com.genesys.gcmessengersdksample.databinding.FragmentChatFormBinding
import com.genesys.gcmessengersdksample.presentation.custom.ContextBlock
import com.google.gson.Gson
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

        createForm()

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
        }

        binding.formFieldsContainer.getFormFields().forEachIndexed { index, view ->

            viewModel.getFormField(index)?.run {

                when (view) {

                    is EditText -> getString(FieldProps.Key) to view.text.toString()

                    is RadioGroup -> getString(FieldProps.Key) to view.getSelectedText()

                    is SwitchCompat -> getString(FieldProps.Key) to view.isChecked.toString()

                    is ContextBlock -> {
                        val context = try {
                            view.contextHandler.getContext()
                        } catch (ex: AssertionError) {
                            null
                        }
                        context?.let { getString(FieldProps.Key) to Gson().toJson(it).toString() }
                    }

                    else -> null

                }?.let {

                    val isRequired = try {
                        get(FieldProps.Required)?.asBoolean ?: false
                    } catch (exception: IllegalStateException) { // being thrown by the 'JsonElement' casting
                        Log.w(TAG, exception.message ?: "Unable to parse field")
                        false
                    }

                    val validator = getString(FieldProps.Validator)?.toPattern()
                    if (!isValid(index, it.second, isRequired, validator)) return

                    accountData.addProperty(it.first, it.second)
                }
            }
        }

        viewModel.onAccountData(accountData)

    }

    private fun createForm() {
        viewModel.formData.value?.forEach {
            binding.formFieldsContainer.addFormField(it.toObject(true)!!)
        }
    }

    private fun isValid(
        index: Int,
        value: String?,
        required: Boolean,
        validator: Pattern?
    ): Boolean {

        val presentError: ((message: String) -> Unit) = { message ->
            (binding.formFieldsContainer.getFormFields()[index] as? TextView)?.apply {
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
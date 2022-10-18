package com.genesys.gcmessengersdksample.data

import com.genesys.gcmessengersdksample.data.defs.ChatType
import com.genesys.gcmessengersdksample.data.defs.DataKeys
import com.genesys.gcmessengersdksample.data.defs.FieldProps
import com.genesys.gcmessengersdksample.data.defs.FieldType
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object FormDataFactory {

    fun createFormFields(
        @ChatType chatType: String,
        extraFormFields: List<FormFieldFactory.FormField>? = null
    ): JsonArray {

        return (chatType to when (chatType) {
            ChatType.Live -> liveForm
            ChatType.Messenger -> asyncForm
            ChatType.Bot -> botForm
            else -> restorationForm
        }).let { form ->
            extraFormFields?.filter { it.formType == form.first }
                ?.forEach { form.second.addFormField(it) }
            form.second
        }
    }

    private val restorationForm: JsonArray
        get() = JsonArray().apply {
            addFormField(
                FormFieldFactory.TextField(
                    ChatType.ChatSelection,
                    "Choose the Account type"
                )
            )
            addFormField(
                FormFieldFactory.OptionsField(
                    ChatType.ChatSelection, DataKeys.ChatTypeKey,
                    listOf(
                        FormFieldFactory.Option(
                            ChatType.ChatSelection,
                            DataKeys.ChatTypeKey,
                            ChatType.Bot
                        ),
                        FormFieldFactory.Option(
                            ChatType.ChatSelection,
                            DataKeys.ChatTypeKey,
                            ChatType.Live
                        ),
                        FormFieldFactory.Option(
                            ChatType.ChatSelection,
                            DataKeys.ChatTypeKey,
                            ChatType.Messenger
                        )
                    )
                )
            )
        }

    private val botForm: JsonArray
        get() = JsonArray().apply {
            addFormField(FormFieldFactory.TextField(ChatType.Bot, "Account Details"))
            addFormField(
                FormFieldFactory.TextInputField(
                    ChatType.Bot,
                    DataKeys.AccountName,
                    "",
                    "Account name",
                    true
                )
            )
            addFormField(
                FormFieldFactory.TextInputField(
                    ChatType.Bot,
                    DataKeys.KB,
                    "",
                    "Knowledge Base",
                    true
                )
            )
            addFormField(
                FormFieldFactory.TextInputField(
                    ChatType.Bot,
                    DataKeys.Accesskey,
                    "",
                    "Api Key",
                    false
                )
            )
            addFormField(
                FormFieldFactory.TextInputField(
                    ChatType.Bot,
                    DataKeys.Server,
                    "",
                    "Server",
                    false
                )
            )
        }

    private val liveForm: JsonArray
        get() = JsonArray().apply {
            addFormField(FormFieldFactory.TextField(ChatType.Live, "Account Details"))
            addFormField(
                FormFieldFactory.TextInputField(
                    ChatType.Live,
                    DataKeys.Accesskey,
                    "",
                    "Access key",
                    true
                )
            )
            addFormField(FormFieldFactory.SwitchField(ChatType.Live, DataKeys.SkipPrechat, false))
        }

    private val asyncForm: JsonArray
        get() = JsonArray().apply {
            addFormField(FormFieldFactory.TextField(ChatType.Messenger, "Account Details"))
            addFormField(
                FormFieldFactory.TextInputField(
                    ChatType.Messenger,
                    DataKeys.DeploymentId,
                    "",
                    "Deployment id",
                    true
                )
            )
            addFormField(
                FormFieldFactory.TextInputField(
                    ChatType.Messenger,
                    DataKeys.Domain,
                    "",
                    "Domain name",
                    true
                )
            )
            addFormField(
                FormFieldFactory.TextInputField(
                    ChatType.Messenger,
                    DataKeys.TokenStoreKey,
                    "",
                    "Token store key",
                    false
                )
            )
            addFormField(FormFieldFactory.SwitchField(ChatType.Messenger, DataKeys.Logging, false))
        }

    private fun JsonArray.addFormField(formField: FormFieldFactory.FormField) {

        formField.toJson().let { jFormField ->

            if (formField.type == FieldType.Option) {

                find { // -> Searching for a Options field with the same key in order to add

                    it.toObject()?.getString(FieldProps.Key) == formField.key
                            && it.toObject()?.getString(FieldProps.Type) == FieldType.Options

                }?.toObject()?.getAsJsonArray("options")?.add(jFormField)

                    ?: kotlin.run { // -> If not found, it creates a new Options field with the same key and form type

                        addFormField(
                            FormFieldFactory.OptionsField(
                                formField.formType, formField.key,
                                listOf(formField as FormFieldFactory.Option)
                            )

                        )
                    }

            } else {
                add(jFormField)
            }
        }
    }
}

object FormFieldFactory {

    abstract class FormField(
        @ChatType val formType: String,
        @FieldType val type: String,
        val key: String,
        val value: String
    ) {

        fun toJson(): JsonObject {
            return JsonParser.parseString(Gson().toJson(this)).toObject(true)!!
        }
    }

    open class Option(@ChatType formType: String, key: String, text: String) :
        FormField(formType, FieldType.Option, key, text)

    class OptionsField(@ChatType formType: String, key: String, private val options: List<Option>) :
        FormField(formType, FieldType.Options, key, options.toString())

    open class SwitchField(@ChatType formType: String, text: String, checked: Boolean = false) :
        FormField(formType, FieldType.Switch, text, checked.toString())

    open class RestoreSwitch(checked: Boolean = false) :
        SwitchField(ChatType.ChatSelection, DataKeys.Restore, checked)

    class ContextBlock(key: String = DataKeys.Context, value: String = "") :
        FormField(ChatType.Bot, FieldType.ContextBlock, key, value)

    class TextField(@ChatType formType: String, value: String) :
        FormField(formType, FieldType.Title, FieldType.Title, value)

    open class TextInputField(
        @ChatType formType: String,
        key: String,
        value: String,
        val hint: String,
        val required: Boolean
    ) : FormField(formType, FieldType.TextInput, key, value)

    open class PatternInputField(
        @ChatType formType: String,
        key: String,
        value: String,
        hint: String,
        required: Boolean,
        val validator: String
    ) : TextInputField(formType, key, value, hint, required)

    class EmailInputField(
        @ChatType formType: String,
        key: String,
        value: String,
        hint: String,
        required: Boolean
    ) : PatternInputField(
        formType,
        key,
        value,
        hint,
        required,
        android.util.Patterns.EMAIL_ADDRESS.toString()
    )

    class PhoneInputField(
        @ChatType formType: String,
        key: String,
        value: String,
        hint: String,
        required: Boolean
    ) : PatternInputField(
        formType,
        key,
        value,
        hint,
        required,
        android.util.Patterns.PHONE.toString()
    )

}
package com.genesys.mobile.messenger.sdk.gcmessengersdksample.presentation.custom

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.genesys.cloud.core.utils.children
import com.genesys.cloud.core.utils.px
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.R
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.defs.FieldProps
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.defs.FieldType
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.getString
import com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.toObject
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class FormFieldsContainer @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    private var formFields: LinearLayout

    init {
        setPadding(8.px, 8.px, 8.px, 8.px)

        formFields = LinearLayout(context)
        addView(formFields.apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            id = ViewCompat.generateViewId()
        })
    }

    fun getFormFields(): List<View> = formFields.children()

    fun addFormField(fieldData: JsonObject) {

        when (fieldData.getString(FieldProps.Type)) {

            FieldType.Options -> FieldViewFactory.optionsView(
                fieldData.getAsJsonArray("options"),
                context
            )

            FieldType.ContextBlock -> ContextBlock(context).apply {
                initContextBlock(this@FormFieldsContainer)
            }

            FieldType.Title -> FieldViewFactory.titleView(
                fieldData.getString(FieldProps.Value),
                context
            )

            FieldType.TextInput -> FieldViewFactory.inputView(
                fieldData.getString(FieldProps.Value),
                fieldData.getString(FieldProps.Hint), context
            )

            FieldType.Switch -> FieldViewFactory.switchView(
                fieldData.getString(FieldProps.Value),
                fieldData.getString(FieldProps.Key), context
            )

            else -> null
        }?.let { view ->
            this.formFields.addView(view.apply { id = ViewCompat.generateViewId() })
        }
    }

    private object FieldViewFactory {

        fun titleView(value: String?, context: Context): TextView =
            AppCompatTextView(context).apply {
                text = value ?: ""
                textSize = 22f
                setTextColor(ContextCompat.getColor(context, R.color.black))
                setPadding(8.px, 8.px, 8.px, 8.px)
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
            }

        fun inputView(value: String?, hint: String?, context: Context): EditText =

            AppCompatEditText(context).apply {
                setText(value ?: "")
                this.hint = hint ?: ""
            }

        fun switchView(value: String?, key: String?, context: Context): SwitchCompat =

            SwitchCompat(context).apply {
                isChecked = value == "true"
                textSize = 16f
                text = key ?: ""
                val margins = 6.px
                setPadding(margins, margins, margins, margins)
            }

        fun optionsView(options: JsonArray, context: Context): RadioGroup {

            return RadioGroup(context).apply {
                options.forEach {
                    addView(AppCompatRadioButton(context).apply {
                        text = it.toObject()?.getString(FieldProps.Value) ?: ""
                        textSize = 16f
                        id = ViewCompat.generateViewId()
                    })
                }
                (this.children().first() as RadioButton).isChecked = true
            }
        }
    }
}

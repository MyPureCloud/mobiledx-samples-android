package com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.defs;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({FieldProps.Type, FieldProps.Key, FieldProps.Value, FieldProps.Label, FieldProps.Hint,
        FieldProps.FormType, FieldProps.Required, FieldProps.Validator})

public @interface FieldProps {

    String FormType = "formType";
    String Type = "type";
    String Key = "key";
    String Value = "value";
    String Label = "label";
    String Hint = "hint";
    String Required = "required";
    String Validator = "validator";
}
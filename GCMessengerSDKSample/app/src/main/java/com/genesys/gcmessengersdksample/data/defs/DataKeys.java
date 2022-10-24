package com.genesys.gcmessengersdksample.data.defs;


import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({
        DataKeys.ChatTypeKey,
        DataKeys.Restore,
        DataKeys.DeploymentId,
        DataKeys.Domain,
        DataKeys.TokenStoreKey,
        DataKeys.Logging,
        DataKeys.Welcome,
        DataKeys.Intent,
        DataKeys.StartChat,
        DataKeys.TestChatAvailability})

public @interface DataKeys {
    String Welcome = "welcome";
    String TokenStoreKey = "tokenStoreKey";
    String Domain = "domain";
    String DeploymentId = "deploymentId";
    String Logging = "logging";
    String Restore = "Restore if available";
    String ChatTypeKey = "chatType";
    String StartChat = "startChat";
    String TestChatAvailability = "TestChatAvailability";
    String Intent = "Intent";
}
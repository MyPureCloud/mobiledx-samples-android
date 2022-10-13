package com.genesys.mobile.messenger.sdk.gcmessengersdksample.data.defs;


import androidx.annotation.StringDef;

import com.genesys.cloud.integration.core.annotations.SessionInfoKeys;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({DataKeys.AccountName, DataKeys.KB, DataKeys.Server, DataKeys.Accesskey,
        DataKeys.Email, DataKeys.CountryAbbrev, DataKeys.ChatTypeKey,
        DataKeys.AppId, DataKeys.Phone, DataKeys.UserId, DataKeys.LastName,
        DataKeys.FirstName, DataKeys.Department, DataKeys.Restore,
        DataKeys.DeploymentId, DataKeys.Domain, DataKeys.TokenStoreKey, DataKeys.Logging,
        DataKeys.Welcome, DataKeys.Context, DataKeys.Info, DataKeys.SkipPrechat,
        DataKeys.Intent, DataKeys.StartChat, DataKeys.TestChatAvailability})

public @interface DataKeys {
    String AccountName = "account";
    String KB = "kb";
    String Server = "domain";
    String Accesskey = "apiKey";
    String Info = "info";
    String Context = "context";
    String Welcome = "welcome";
    String AppId = "applicationId";
    String TokenStoreKey = "tokenStoreKey";
    String Domain = "domain";
    String DeploymentId = "deploymentId";
    String Logging = "logging";
    String UserId = "id";
    String Restore = "Restore if available";
    String ChatTypeKey = "chatType";
    String FirstName = SessionInfoKeys.FirstName;
    String LastName = SessionInfoKeys.LastName;
    String Email = SessionInfoKeys.Email;
    String CountryAbbrev = SessionInfoKeys.countryAbbrev;
    String Phone = SessionInfoKeys.Phone;
    String Department = SessionInfoKeys.Department;
    String SkipPrechat = "Skip Prechat";
    String StartChat = "startChat";
    String TestChatAvailability = "TestChatAvailability";
    String Intent = "Intent";
}
package com.genesys.cloud.messenger.sample.data.defs;


import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({
        DataKeys.DeploymentId,
        DataKeys.Domain,
        DataKeys.CustomAttributes,
        DataKeys.TokenStoreKey,
        DataKeys.Logging,})

public @interface DataKeys {
    String TokenStoreKey = "tokenStoreKey";
    String Domain = "domain";
    String DeploymentId = "deploymentId";
    String Logging = "logging";
    String CustomAttributes = "customAttributes";
}
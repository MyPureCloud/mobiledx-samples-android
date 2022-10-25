package com.genesys.gcmessengersdksample.data.defs;


import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({
        DataKeys.DeploymentId,
        DataKeys.Domain,
        DataKeys.TokenStoreKey,
        DataKeys.Logging,})

public @interface DataKeys {
    String TokenStoreKey = "tokenStoreKey";
    String Domain = "domain";
    String DeploymentId = "deploymentId";
    String Logging = "logging";
}
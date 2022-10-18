package com.genesys.gcmessengersdksample.data.defs;


import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({ChatType.Live, ChatType.Bot, ChatType.Messenger, ChatType.ChatSelection, ChatType.ContinueLast})
public @interface ChatType {
    String ChatSelection = "Chat selection";
    String Live = "Live Chat";
    String Messenger = "Messenger Chat";
    String Bot = "Bot Chat";
    String ContinueLast = "Continue Last";
}

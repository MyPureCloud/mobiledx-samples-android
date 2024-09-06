package com.genesys.cloud.messenger.sample.chat_form

import android.webkit.WebViewClient

class OktaAuthenticationFragment : WebFragment() {

    override fun provideWebViewClient(): WebViewClient {
        return WebViewClient()
    }

    companion object {

        const val TAG = "OktaAuthenticationFragment"

        @JvmStatic
        fun newInstance() = newInstance("")
    }
}
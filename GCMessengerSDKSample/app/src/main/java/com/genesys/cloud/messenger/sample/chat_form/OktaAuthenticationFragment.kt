package com.genesys.cloud.messenger.sample.chat_form

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.activityViewModels
import com.genesys.cloud.messenger.sample.BuildConfig
import com.genesys.cloud.messenger.sample.data.repositories.JsonSampleRepository

class OktaAuthenticationFragment : WebFragment() {

    private val viewModel: SampleFormViewModel by activityViewModels {
        SampleFormViewModelFactory(
            JsonSampleRepository(
                requireActivity().applicationContext
            )
        )
    }

    override fun provideWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.url?.getQueryParameter("code")?.apply {
                    authCodeReceived()
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
    }

    private fun String.authCodeReceived() {
        viewModel.setAuthCode(this, BuildConfig.SIGN_IN_REDIRECT_URI, BuildConfig.CODE_VERIFIER)
        parentFragmentManager.popBackStack()
    }

    companion object {

        val TAG: String = OktaAuthenticationFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(): OktaAuthenticationFragment {
            val oktaAuthorizeUrl = buildOktaAuthorizeUrl()
            if (oktaAuthorizeUrl == null) {
                throw IllegalStateException("There are no proper okta.properties provided. See Readme.md.")
            } else {
                return OktaAuthenticationFragment().apply {
                    arguments = Bundle().apply {
                        putString(URL, oktaAuthorizeUrl)
                    }
                }
            }
        }

        private fun buildOktaAuthorizeUrl(): String? {
            if (BuildConfig.CLIENT_ID == null
                || BuildConfig.CLIENT_ID == "null"
                || BuildConfig.CLIENT_ID == "INSERT_OKTA_DOMAIN"
            ) {
                return null
            }
            val builder =
                StringBuilder("https://${BuildConfig.OKTA_DOMAIN}/oauth2/default/v1/authorize").apply {
                    append("?client_id=${BuildConfig.CLIENT_ID}")
                    append("&response_type=code")
                    append("&scope=openid%20profile%20offline_access")
                    append("&redirect_uri=${BuildConfig.SIGN_IN_REDIRECT_URI}")
                    append("&state=${BuildConfig.OKTA_STATE}")
                    append("&code_verifier=${BuildConfig.CODE_VERIFIER}")
                    append("&code_challenge_method=${BuildConfig.CODE_CHALLENGE_METHOD}")
                    append("&code_challenge=${BuildConfig.CODE_CHALLENGE}")
                }
            return builder.toString()
        }
    }
}
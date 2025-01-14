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
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url == buildOktaLogoutUrl()) {
                    signoutSuccessful()
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.url?.getQueryParameter("code")?.let {
                    authCodeReceived(it)
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
    }

    private fun authCodeReceived(authCode: String) {
        viewModel.setAuthCode(authCode, BuildConfig.SIGN_IN_REDIRECT_URI, BuildConfig.CODE_VERIFIER)
        parentFragmentManager.popBackStack()
    }

    private fun signoutSuccessful() {
        viewModel.clearAuthCode()
        requireFragmentManager().popBackStack()
    }

    companion object {

        val TAG: String = OktaAuthenticationFragment::class.java.simpleName

        @JvmStatic
        fun newLoginInstance(): OktaAuthenticationFragment {
            val oktaAuthorizeUrl = buildOktaAuthorizeUrl()
            return OktaAuthenticationFragment().apply {
                arguments = Bundle().apply {
                    putString(URL, oktaAuthorizeUrl)
                }
            }
        }

        @JvmStatic
        fun newLogoutInstance(): OktaAuthenticationFragment {
            val oktaLogoutUrl = buildOktaLogoutUrl()
            return OktaAuthenticationFragment().apply {
                arguments = Bundle().apply {
                    putString(URL, oktaLogoutUrl)
                }
            }
        }

        private fun notNullOrEmpty(value: String?, property: String): String {
            if (value.isNullOrEmpty() || value == "null") {
                throw IllegalStateException("Mandatory property $property is missed, check okta.properties.")
            }
            return value
        }

        private fun buildOktaAuthorizeUrl(): String {
            val builder =
                StringBuilder("https://${notNullOrEmpty(BuildConfig.OKTA_DOMAIN, "OKTA_DOMAIN")}/oauth2/default/v1/authorize").apply {
                    append("?client_id=${notNullOrEmpty(BuildConfig.CLIENT_ID, "CLIENT_ID")}")
                    append("&response_type=code")
                    append("&scope=${notNullOrEmpty(BuildConfig.SCOPES, "SCOPES")}")
                    append("&redirect_uri=${notNullOrEmpty(BuildConfig.SIGN_IN_REDIRECT_URI, "SIGN_IN_REDIRECT_URI")}")
                    append("&state=${notNullOrEmpty(BuildConfig.OKTA_STATE, "OKTA_STATE")}")
                    append("&code_verifier=${notNullOrEmpty(BuildConfig.CODE_VERIFIER, "CODE_VERIFIER")}")
                    append("&code_challenge_method=${notNullOrEmpty(BuildConfig.CODE_CHALLENGE_METHOD, "CODE_CHALLENGE_METHOD")}")
                    append("&code_challenge=${notNullOrEmpty(BuildConfig.CODE_CHALLENGE, "CODE_CHALLENGE")}")
                }
            return builder.toString()
        }

        fun buildOktaLogoutUrl(): String{
            val builder =
                StringBuilder("https://${notNullOrEmpty(BuildConfig.OKTA_DOMAIN, "OKTA_DOMAIN")}/login/signout")
            return builder.toString()
        }
    }
}
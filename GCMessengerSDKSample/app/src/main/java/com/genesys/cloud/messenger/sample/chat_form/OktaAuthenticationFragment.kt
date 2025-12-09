package com.genesys.cloud.messenger.sample.chat_form

import android.net.Uri
import android.view.View
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.activityViewModels
import com.genesys.cloud.messenger.sample.BuildConfig
import com.genesys.cloud.messenger.sample.chat_form.OktaAuthenticationFragment.Companion.TAG
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
                }  ?: run {
                    request?.url?.getParam("id_token")?.let {
                        idTokenReceived(it)
                        return true
                    }
                } ?: run {
                    Log.w(TAG, "Neither authCode nor idToken was able to found")
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments?.getString(URL) == buildOktaLogoutUrl()) {
            showProgressBar(true)
        }
    }

    internal fun showProgressBar(show: Boolean) {
        progressBar?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun authCodeReceived(authCode: String) {
        viewModel.setAuthCode(authCode, BuildConfig.SIGN_IN_REDIRECT_URI, BuildConfig.CODE_VERIFIER)
        parentFragmentManager.popBackStack()
    }

    private fun idTokenReceived(idToken: String) {
        viewModel.setIdToken(idToken)
        parentFragmentManager.popBackStack()
    }

    private fun signoutSuccessful() {
        viewModel.clearAuthCode()
        viewModel.clearIdToken()
    }

    override fun onDestroy() {
        super.onDestroy()
        showProgressBar(false)
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

        fun newImplicitLoginInstance(nonce: String): OktaAuthenticationFragment {
            val oktaAuthorizeUrl = buildImplicitOktaAuthorizeUrl(nonce)
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

        private fun buildImplicitOktaAuthorizeUrl(nonce: String): String {
            val builder =
                StringBuilder("https://${notNullOrEmpty(BuildConfig.OKTA_DOMAIN, "OKTA_DOMAIN")}/oauth2/v1/authorize").apply {
                    append("?client_id=${notNullOrEmpty(BuildConfig.CLIENT_ID, "CLIENT_ID")}")
                    append("&response_type=id_token")
                    append("&scope=${notNullOrEmpty(BuildConfig.SCOPES, "SCOPES")}")
                    append("&redirect_uri=${notNullOrEmpty(BuildConfig.SIGN_IN_REDIRECT_URI, "SIGN_IN_REDIRECT_URI")}")
                    append("&state=${notNullOrEmpty(BuildConfig.OKTA_STATE, "OKTA_STATE")}")
                    append("&nonce=${nonce}")
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


private fun Uri.getParam(param: String): String? {
    return fragment?.let { currentFragment ->
        Log.d(TAG, "Fragment: $currentFragment")
        // The fragment is a string like "id_token=eyJraWQiOiJyS0px&access_token=..."

        val params = mutableMapOf<String, String>()
        val pairs = currentFragment.split("&")
        for (pair in pairs) {
            val parts = pair.split("=")
            if (parts.size == 2) {
                val key = Uri.decode(parts[0]) // Decode URL-encoded parts
                val value = Uri.decode(parts[1])
                params[key] = value
            }
        }

        val paramValue = params[param]

        if (paramValue != null) {
            Log.d(TAG, "Extracted $param: $paramValue")
            paramValue
        } else {
            Log.e(TAG, "$param not found in fragment")
            null
        }
    } ?: run {
        Log.e(TAG, "no fragment found")
        null
    }
}


package com.genesys.cloud.messenger.sample.chat_form

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.genesys.cloud.messenger.sample.R

open class WebFragment : Fragment() {

    private var url: String? = null
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            url = it.getString(URL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.web_fragment, container, false)
        webView = view.findViewById(R.id.webView)
        webView?.let {
            it.settings.javaScriptEnabled = true
            it.setWebViewClient(provideWebViewClient())
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        webView?.loadUrl(url!!)
    }

    open fun provideWebViewClient(): WebViewClient {
        return WebViewClient()
    }

    companion object {

        const val TAG = "WebFragment"
        const val URL = "param_url"

        @JvmStatic
        fun newInstance(url: String) =
            WebFragment().apply {
                arguments = Bundle().apply {
                    putString(URL, url)
                }
            }
    }
}
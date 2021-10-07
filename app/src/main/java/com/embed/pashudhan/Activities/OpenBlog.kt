package com.embed.pashudhan.Activities

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.embed.pashudhan.R

class OpenBlog : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_blog)

        val link = intent.getStringExtra("link")!!
        val webview = findViewById<WebView>(R.id.webview)
        webview.settings.javaScriptEnabled = true

        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url!!)
                return true
            }
        }
        webview.loadUrl(link)
        val returnButton = findViewById<ImageButton>(R.id.openBlog_AppBarReturnBtn)
        returnButton.setOnClickListener {
            this.onBackPressed()
        }
    }
}
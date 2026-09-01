package com.example.extensionbrowser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBox: EditText

    private val extensionPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                Toast.makeText(
                    this,
                    "Extension file selected: ${uri.lastPathSegment}. " +
                    "This starter stores the selection only; full Chromium extension execution requires an extension-capable engine.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlBox = findViewById(R.id.urlBox)
        webView = findViewById(R.id.webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.setSupportMultipleWindows(true)
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        urlBox.setText("https://www.google.com")

        findViewById<Button>(R.id.goButton).setOnClickListener {
            var url = urlBox.text.toString().trim()
            if (url.isNotEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                webView.loadUrl(url)
            }
        }

        findViewById<Button>(R.id.importButton).setOnClickListener {
            extensionPicker.launch(
                arrayOf(
                    "application/zip",
                    "application/x-chrome-extension",
                    "application/octet-stream"
                )
            )
        }

        findViewById<Button>(R.id.appsButton).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS))
        }

        webView.loadUrl("https://www.google.com")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}

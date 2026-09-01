package com.tuempresa.appinventario

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var gestureDetector: GestureDetector

    companion object {
        private const val PREFS_NAME = "AppInventarioPrefs"
        private const val KEY_URL = "server_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        setupWebView()
        setupSwipeRefresh()
        setupLongPress()

        val savedUrl = prefs.getString(KEY_URL, null)
        if (savedUrl.isNullOrEmpty()) {
            showUrlDialog(firstTime = true)
        } else {
            loadUrl(savedUrl)
        }
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                swipeRefresh.isRefreshing = false
                Toast.makeText(
                    this@MainActivity,
                    "No se pudo conectar. Mantén pulsada la pantalla para cambiar la IP.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }

    private fun setupLongPress() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                showUrlDialog(firstTime = false)
            }
        })

        webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // deja que el WebView siga procesando el evento normalmente
        }
    }

    private fun showUrlDialog(firstTime: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_url_input, null)
        val input = dialogView.findViewById<EditText>(R.id.etServerUrl)

        val currentUrl = prefs.getString(KEY_URL, "")
        if (!firstTime) input.setText(currentUrl?.removePrefix("http://"))

        val builder = AlertDialog.Builder(this, R.style.AppInventarioDialogTheme)
            .setTitle(if (firstTime) "Configura la IP del servidor" else "Cambiar IP del servidor")
            .setView(dialogView)
            .setCancelable(!firstTime)
            .setPositiveButton("Guardar") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) {
                    val url = if (value.startsWith("http://") || value.startsWith("https://")) {
                        value
                    } else {
                        "http://$value"
                    }
                    prefs.edit().putString(KEY_URL, url).apply()
                    loadUrl(url)
                } else if (firstTime) {
                    showUrlDialog(true)
                }
            }

        if (!firstTime) {
            builder.setNegativeButton("Cancelar", null)
        }

        val dialog = builder.create()
        dialog.show()

        // Refuerzo de color por si el tema no se aplica bien en algún dispositivo
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(resources.getColor(R.color.purple_500, theme))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(resources.getColor(R.color.purple_500, theme))
    }

    private fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

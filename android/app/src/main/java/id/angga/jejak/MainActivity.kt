package id.angga.jejak

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

/** Layar utama: membuka web Jejak Langkah (peta lengkap) di dalam aplikasi. */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private var waitingForOverlay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        Web.setup(web, this)
        web.addJavascriptInterface(Bridge(), "Android")

        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl(BuildConfig.APP_URL)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        web.evaluateJavascript("window.onAndroidResume&&window.onAndroidResume()", null)
        // Kembali dari layar izin: langsung nyalakan bubble kalau izinnya sudah diberikan.
        if (waitingForOverlay && Settings.canDrawOverlays(this)) {
            waitingForOverlay = false
            startBubble()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    private fun startBubble() {
        if (!Settings.canDrawOverlays(this)) {
            waitingForOverlay = true
            Toast.makeText(
                this,
                "Aktifkan \"Tampilkan di atas aplikasi lain\" untuk Jejak Langkah, lalu kembali ke sini",
                Toast.LENGTH_LONG
            ).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        startForegroundService(Intent(this, BubbleService::class.java))
        Toast.makeText(this, "Bubble aktif. Ketuk untuk mencatat, tahan untuk buka peta", Toast.LENGTH_LONG).show()
        moveTaskToBack(true)
    }

    inner class Bridge {
        @JavascriptInterface
        fun startBubble() = runOnUiThread { this@MainActivity.startBubble() }

        @JavascriptInterface
        fun stopBubble() = runOnUiThread {
            stopService(Intent(this@MainActivity, BubbleService::class.java))
        }

        @JavascriptInterface
        fun isBubbleOn(): Boolean = BubbleService.running
    }
}

/** Pengaturan WebView yang dipakai bersama layar utama dan kartu bubble. */
object Web {
    @SuppressLint("SetJavaScriptEnabled")
    fun setup(web: WebView, ctx: Context) {
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.databaseEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (url.startsWith(BuildConfig.APP_URL)) return false
                // Tautan luar dibuka di browser.
                try {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, request.url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {
                }
                return true
            }
        }
    }
}

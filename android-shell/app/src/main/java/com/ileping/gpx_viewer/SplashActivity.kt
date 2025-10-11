package com.ileping.gpx_viewer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity


class SplashActivity : AppCompatActivity() {
    lateinit var webView: WebView
    lateinit var container: LinearLayout
    private val tag = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)
        container = findViewById<LinearLayout>(R.id.splash_container)

        webView = WebView(this).apply {
            settings.apply {
                // JavaScript 支持
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                
                // 存储支持
                domStorageEnabled = true
                databaseEnabled = true
                
                // 缓存设置
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // 图片加载
                blockNetworkImage = false
                loadsImagesAutomatically = true
                
                // 文件访问
                allowFileAccess = true
                allowContentAccess = true
                
                // 跨域支持 - 仅用于开发/调试，生产环境需谨慎使用
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = true
                
                // 混合内容模式
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                // 自适应屏幕
                useWideViewPort = true
                loadWithOverviewMode = true
                
                // 其他设置
                mediaPlaybackRequiresUserGesture = false
                // setGeolocationEnabled(true)  // 如需地理位置，需添加相应权限
            }
            
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            
            // 添加 JavaScript 接口
            addJavascriptInterface(JSBridge(), "Android")

            loadUrl("file:///android_asset/splash.html")
        }

        container.addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )


    }

    // 阻止此页面下的退出
    override fun onBackPressed() {
    }

    override fun onPause() {
        super.onPause()

        webView.destroy();
    }

    fun openMainActivity() {
        val fromMain = intent.getBooleanExtra("FROM_MAIN", false)
        if (fromMain) {
            setResult(Activity.RESULT_OK)
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }

        finish()
    }

    /**
     * JavaScript Bridge - 用于 JS 调用 Native 方法
     */
    inner class JSBridge {
        /**
         * 跳过启动页
         */
        @JavascriptInterface
        fun skipSplash() {
            Log.d(tag, "skipSplash called from JavaScript")
            runOnUiThread {
                openMainActivity()
            }
        }
    }
}
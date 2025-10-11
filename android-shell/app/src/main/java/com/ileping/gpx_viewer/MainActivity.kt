package com.ileping.gpx_viewer

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var _exitTime = 0L
    private val tag = "MainActivity"
    
    // 文件选择相关
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 注册文件选择启动器
        fileChooserLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (filePathCallback == null) return@registerForActivityResult
                
                val results = if (result.resultCode == RESULT_OK && result.data != null) {
                    val dataUri = result.data?.data
                    val clipData = result.data?.clipData
                    
                    when {
                        clipData != null -> {
                            // 多文件选择
                            Array(clipData.itemCount) { i ->
                                clipData.getItemAt(i).uri
                            }
                        }
                        dataUri != null -> {
                            // 单文件选择
                            arrayOf(dataUri)
                        }
                        else -> null
                    }
                } else {
                    null
                }
                
                filePathCallback?.onReceiveValue(results)
                filePathCallback = null
            }

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
                allowFileAccessFromFileURLs = true  // 允许 file:// 跨域访问
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = true  // 允许 file:// 访问任何来源
                
                // 混合内容模式 (允许 HTTPS 页面加载 HTTP 资源)
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                // 自适应屏幕
                useWideViewPort = true
                loadWithOverviewMode = true
                
                // 缩放设置
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false  // 隐藏缩放按钮
                
                // 其他设置
                mediaPlaybackRequiresUserGesture = false  // 允许自动播放媒体
                // setGeolocationEnabled(true)  // 如需地理位置，需添加相应权限
            }
            
            webViewClient = WebViewClient()
            
            // 自定义 WebChromeClient 以支持文件选择和新窗口
            webChromeClient = object : WebChromeClient() {
                // Android 5.0+ 文件选择
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    // 如果之前有未完成的回调，先取消
                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = filePathCallback

                    val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    
                    // 支持多选
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                    
                    try {
                        fileChooserLauncher.launch(intent)
                    } catch (e: Exception) {
                        this@MainActivity.filePathCallback = null
                        Toast.makeText(this@MainActivity, "无法打开文件选择器", Toast.LENGTH_SHORT).show()
                        return false
                    }
                    
                    return true
                }
                
                // 支持 target="_blank" 打开新窗口
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: android.os.Message?
                ): Boolean {
                    val logTag = "MainActivity"
                    // 获取新窗口的 URL
                    val newWebView = WebView(this@MainActivity)
                    newWebView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            // 在当前 WebView 中打开链接
                            url?.let {
                                this@MainActivity.webView.loadUrl(it)
                                Log.d(logTag, "Open new window URL: $it")
                            }
                            return true
                        }
                    }
                    
                    val transport = resultMsg?.obj as? android.webkit.WebView.WebViewTransport
                    transport?.webView = newWebView
                    resultMsg?.sendToTarget()
                    
                    return true
                }
            }
            
            // 设置下载监听器
            setDownloadListener(object : DownloadListener {
                override fun onDownloadStart(
                    url: String?,
                    userAgent: String?,
                    contentDisposition: String?,
                    mimetype: String?,
                    contentLength: Long
                ) {
                    val logTag = "MainActivity"
                    Log.d(logTag, "Download: url=$url, mimeType=$mimetype")
                    downloadFile(url ?: "", userAgent ?: "", contentDisposition ?: "", mimetype ?: "")
                }
            })
            
            // 添加 JavaScript 接口
            addJavascriptInterface(JSBridge(), "Android")

            // 加载 GPX Viewer 应用
            loadUrl("file:///android_asset/gpx-viewer.html")
        }

        findViewById<LinearLayout>(R.id.main_container).addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else if (System.currentTimeMillis() - _exitTime > 2000) {
            Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
            _exitTime = System.currentTimeMillis()
        } else {
            finish();
        }
    }

    /**
     * 处理文件下载
     */
    private fun downloadFile(url: String, userAgent: String, contentDisposition: String, mimeType: String) {
        try {
            // 检查是否是 blob URL 或 data URL
            if (url.startsWith("blob:") || url.startsWith("data:")) {
                Log.w(tag, "Blob/Data URL 不支持通过 DownloadManager 下载: $url")
                runOnUiThread {
                    Toast.makeText(
                        this, 
                        "Blob 文件下载请使用 JSBridge 方式", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
            
            val request = DownloadManager.Request(Uri.parse(url))
            
            // 获取文件名
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            
            // 设置下载参数
            request.apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setDescription("正在下载文件...")
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            
            // 开始下载
            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            
            Toast.makeText(this, "开始下载: $fileName", Toast.LENGTH_SHORT).show()
            Log.d(tag, "Download started: $fileName")
        } catch (e: Exception) {
            Log.e(tag, "Download error", e)
            Toast.makeText(this, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * JavaScript Bridge - 用于 JS 调用 Native 方法
     */
    inner class JSBridge {
        /**
         * 显示 Toast 消息
         */
        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * 获取应用版本名称
         */
        @JavascriptInterface
        fun getVersionName(): String {
            return try {
                packageManager.getPackageInfo(packageName, 0).versionName
            } catch (e: Exception) {
                "Unknown"
            }
        }

        /**
         * 打印日志到 Android Logcat
         */
        @JavascriptInterface
        fun log(tag: String, message: String) {
            Log.d("WebView-$tag", message)
        }

        /**
         * 下载 Base64 编码的文件
         * @param base64Data Base64 编码的文件数据（可以包含 data:mime;base64, 前缀）
         * @param fileName 文件名
         */
        @JavascriptInterface
        fun downloadBase64File(base64Data: String, fileName: String) {
            runOnUiThread {
                try {
                    // 移除 data URL 前缀（如果有）
                    val base64String = if (base64Data.contains(",")) {
                        base64Data.substring(base64Data.indexOf(",") + 1)
                    } else {
                        base64Data
                    }
                    
                    // 解码 Base64
                    val decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                    
                    // 保存到 Downloads 目录
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = java.io.File(downloadsDir, fileName)
                    
                    java.io.FileOutputStream(file).use { fos ->
                        fos.write(decodedBytes)
                    }
                    
                    Toast.makeText(this@MainActivity, "文件已保存: $fileName", Toast.LENGTH_SHORT).show()
                    Log.d(tag, "File saved: ${file.absolutePath}")
                    
                    // 通知媒体库更新
                    val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    intent.data = Uri.fromFile(file)
                    sendBroadcast(intent)
                    
                } catch (e: Exception) {
                    Log.e(tag, "Download Base64 file error", e)
                    Toast.makeText(this@MainActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        /**
         * 在外部浏览器中打开 URL
         * @param url 要打开的 URL
         */
        @JavascriptInterface
        fun openExternalBrowser(url: String) {
            runOnUiThread {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    Log.d(tag, "Open external browser: $url")
                } catch (e: Exception) {
                    Log.e(tag, "Open external browser error", e)
                    Toast.makeText(this@MainActivity, "无法打开浏览器: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
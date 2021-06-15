/*
 * Copyright fobidlim. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tfc.webviewer.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.WindowManager
import android.webkit.*
import android.webkit.WebChromeClient.FileChooserParams
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.tfc.webviewer.R
import com.tfc.webviewer.presenter.WebViewPresenterImpl
import com.tfc.webviewer.util.ClipboardUtils
import com.tfc.webviewer.util.FileUtils
import com.tfc.webviewer.util.PermissionUtils

/**
 * author @fobidlim
 */
class WebViewerActivity : AppCompatActivity(),
    WebViewPresenterImpl.View,
    View.OnClickListener,
    SwipeRefreshLayout.OnRefreshListener,
    DownloadListener,
    OnCreateContextMenuListener {

    private val presenter by lazy { WebViewPresenterImpl(this, this) }

    private var url = ""
    private var filePathCallbackLollipop: ValueCallback<Array<Uri>>? = null
    private var filePathCallbackNormal: ValueCallback<Uri>? = null
    private var downloadManager: DownloadManager? = null
    private var mLastDownloadId: Long = 0

    // Toolbar
    private val tvTitle by lazy { findViewById<TextView>(R.id.toolbar_tv_title) }
    private val tvUrl by lazy { findViewById<TextView>(R.id.toolbar_tv_url) }

    private val coordinatorlayout by lazy {
        findViewById<CoordinatorLayout>(R.id.a_web_viewer_coordinatorlayout)
    }
    private val progressBar by lazy { findViewById<ProgressBar>(R.id.a_web_viewer_pb) }
    private val swipeRefreshLayout by lazy { findViewById<SwipeRefreshLayout>(R.id.a_web_viewer_srl) }
    private val webView by lazy { findViewById<WebView>(R.id.a_web_viewer_wv) }

    // PopupWindow
    private val popupMenu: PopupWindow by lazy { PopupWindow(this) }
    private var rlControlButtons: View? = null
    private val btnMore by lazy { findViewById<ImageButton>(R.id.toolbar_btn_more) }
    private var btnBack: View? = null
    private var btnForward: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.a_web_viewer)
        supportActionBar?.hide()
        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            .apply {
                addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
            }.let {
                registerReceiver(downloadReceiver, it)
            }
        url = intent.getStringExtra(EXTRA_URL) ?: ""
        bindView()
        presenter.validateUrl(url)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadReceiver)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val result = webView.hitTestResult
        presenter.onLongClick(result)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EXTRA_URL, url)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        url = savedInstanceState.getString(EXTRA_URL) ?: ""
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (RESULT_OK == resultCode) {
            when (requestCode) {
                REQUEST_FILE_CHOOSER -> {
                    if (filePathCallbackNormal == null) {
                        return
                    }
                    val result = data?.data
                    filePathCallbackNormal!!.onReceiveValue(result)
                    filePathCallbackNormal = null
                }
                REQUEST_FILE_CHOOSER_FOR_LOLLIPOP -> {
                    if (filePathCallbackLollipop == null) {
                        return
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        filePathCallbackLollipop!!.onReceiveValue(
                            FileChooserParams.parseResult(
                                resultCode,
                                data
                            )
                        )
                    }
                    filePathCallbackLollipop = null
                }
                REQUEST_PERMISSION_SETTING -> {
                    if (downloadUrl != null && downloadMimetype != null) {
                        mLastDownloadId =
                            FileUtils.downloadFile(this, downloadUrl!!, downloadMimetype!!)
                    }
                }
            }
        } else {
            when (requestCode) {
                REQUEST_PERMISSION_SETTING -> {
                    Toast.makeText(
                        this,
                        R.string.write_permission_denied_message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun bindView() {
        // Toolbar
        swipeRefreshLayout.setOnRefreshListener(this@WebViewerActivity)

        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    displayZoomControls = false
                }
                builtInZoomControls = true
                setSupportZoom(true)
                domStorageEnabled = true
            }

            webChromeClient = MyWebChromeClient()
            webViewClient = MyWebViewClient()
            setDownloadListener(this@WebViewerActivity)
            setOnCreateContextMenuListener(this@WebViewerActivity)
        }

        findViewById<ImageButton>(R.id.toolbar_btn_close).setOnClickListener(this@WebViewerActivity)
        btnMore.setOnClickListener(this@WebViewerActivity)

        // PopupWindow
        initPopupMenu()
    }

    private fun initPopupMenu() {
        val view = LayoutInflater.from(this).inflate(R.layout.popup_menu, null)

        popupMenu.apply {
            contentView = view
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(0))
            isFocusable = true
        }

        view.apply {
            rlControlButtons = findViewById(R.id.popup_menu_rl_arrows)
            btnBack = findViewById(R.id.popup_menu_btn_back)
            btnForward = findViewById(R.id.popup_menu_btn_forward)

            btnBack?.setOnClickListener(this@WebViewerActivity)
            btnForward?.setOnClickListener(this@WebViewerActivity)
            findViewById<TextView>(R.id.popup_menu_btn_refresh).setOnClickListener(this@WebViewerActivity)
            findViewById<TextView>(R.id.popup_menu_btn_copy_link).setOnClickListener(this@WebViewerActivity)
            findViewById<TextView>(R.id.popup_menu_btn_open_with_other_browser).setOnClickListener(
                this@WebViewerActivity
            )
            findViewById<TextView>(R.id.popup_menu_btn_share).setOnClickListener(this@WebViewerActivity)
        }
    }

    override fun loadUrl(url: String) {
        webView.loadUrl(url)
        presenter.onReceivedTitle("", this.url)
    }

    override fun close() {
        finish()
    }

    override fun closeMenu() {
        popupMenu.dismiss()
    }

    override fun openMenu() {
        popupMenu.showAsDropDown(btnMore)
    }

    override fun setEnabledGoBackAndGoForward() {
        rlControlButtons?.visibility = View.VISIBLE
    }

    override fun setDisabledGoBackAndGoForward() {
        rlControlButtons?.visibility = View.GONE
    }

    override fun setEnabledGoBack() {
        btnBack?.isEnabled = true
    }

    override fun setDisabledGoBack() {
        btnBack?.isEnabled = false
    }

    override fun setEnabledGoForward() {
        btnForward?.isEnabled = true
    }

    override fun setDisabledGoForward() {
        btnForward?.isEnabled = false
    }

    override fun goBack() {
        webView.goBack()
    }

    override fun goForward() {
        webView.goForward()
    }

    override fun copyLink(url: String) {
        ClipboardUtils.copyText(this, url)
    }

    override fun showToast(toast: Toast) {
        toast.show()
    }

    override fun openBrowser(uri: Uri) {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    override fun openShare(url: String) {
        Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_TEXT, webView.url)
            .setType("text/plain")
            .let {
                startActivity(Intent.createChooser(it, resources.getString(R.string.menu_share)))
            }
    }

    override fun setToolbarTitle(title: String) {
        tvTitle.text = title
    }

    override fun setToolbarUrl(url: String) {
        tvUrl.text = url
    }

    override fun onDownloadStart(url: String) {
        onDownloadStart(url, null, null, "image/jpeg", 0)
    }

    override fun setProgressBar(progress: Int) {
        progressBar.progress = progress
    }

    override fun setRefreshing(refreshing: Boolean) {
        swipeRefreshLayout.isRefreshing = refreshing
    }

    override fun openEmail(email: String) {
        Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null))
            .let {
                startActivity(Intent.createChooser(it, getString(R.string.email)))
            }
    }

    override fun openPopup(url: String) {
        Intent(this, WebViewerActivity::class.java)
            .putExtra(EXTRA_URL, url)
            .let {
                startActivity(it)
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (PermissionUtils.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE == requestCode) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                if (downloadUrl != null && downloadMimetype != null) {
                    mLastDownloadId =
                        FileUtils.downloadFile(this, downloadUrl!!, downloadMimetype!!)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!shouldShowRequestPermissionRationale(permissions[0])) {
                        AlertDialog.Builder(this@WebViewerActivity)
                            .setTitle(R.string.write_permission_denied_title)
                            .setMessage(R.string.write_permission_denied_message)
                            .setNegativeButton(R.string.dialog_dismiss, null)
                            .setPositiveButton(R.string.dialog_settings) { dialog, which ->
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri
                                startActivityForResult(intent, REQUEST_PERMISSION_SETTING)
                            }
                            .show()
                    }
                }
            }
        }
    }

    private var downloadUrl: String? = null
    private var downloadMimetype: String? = null

    override fun onDownloadStart(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String,
        contentLength: Long
    ) {
        if (downloadManager == null) {
            downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        }
        downloadUrl = url
        downloadMimetype = mimeType
        val hasPermission = PermissionUtils.hasPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            PermissionUtils.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
        )
        if (hasPermission) {
            mLastDownloadId = FileUtils.downloadFile(this, url, mimeType)
        }
    }

    override fun onClick(v: View) {
        presenter.onClick(v.id, webView.url ?: "", popupMenu)
    }

    override fun onRefresh() {
        webView.reload()
    }

    inner class MyWebChromeClient : WebChromeClient() {
        // For Android 3.0+
        // For Android < 3.0
        fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String) {
            filePathCallbackNormal = uploadMsg

            Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*")
                .let {
                    startActivityForResult(
                        Intent.createChooser(it, getString(R.string.select_image)),
                        REQUEST_FILE_CHOOSER
                    )
                }
        }

        // For Android 5.0+
        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            if (filePathCallbackLollipop != null) {
                filePathCallbackLollipop!!.onReceiveValue(null)
                filePathCallbackLollipop = null
            }
            filePathCallbackLollipop = filePathCallback
            Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("image/*")
                .let {
                    startActivityForResult(
                        Intent.createChooser(it, getString(R.string.select_image)),
                        REQUEST_FILE_CHOOSER_FOR_LOLLIPOP
                    )
                }

            return true
        }

        override fun onJsAlert(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            AlertDialog.Builder(this@WebViewerActivity)
                .setMessage(message)
                .setPositiveButton(R.string.yes) { _, _ -> result.confirm() }
                .create()
                .apply {
                    setCanceledOnTouchOutside(false)
                }.run {
                    show()
                }
            return true
        }

        override fun onJsConfirm(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            AlertDialog.Builder(this@WebViewerActivity)
                .setMessage(message)
                .setPositiveButton(R.string.yes) { _, _ -> result.confirm() }
                .setNegativeButton(R.string.no) { _, _ -> result.cancel() }
                .create()
                .apply {
                    setCanceledOnTouchOutside(false)
                }.run {
                    show()
                }
            return true
        }

        override fun onProgressChanged(view: WebView, progress: Int) {
            presenter.onProgressChanged(swipeRefreshLayout, progress)
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            presenter.onReceivedTitle(title, view.url ?: "")
        }
    }

    inner class MyWebViewClient : WebViewClient() {

        override fun onPageFinished(view: WebView?, url: String?) {
            val view = view ?: return
            val url = url ?: return

            presenter.onReceivedTitle(view.title ?: "", url)
            presenter.setEnabledGoBackAndGoForward(view.canGoBack(), view.canGoForward())
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            val view = view ?: return false
            val url = url ?: return false

            return when {
                url.endsWith(".mp4") -> {
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(Uri.parse(url), "video/*")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .let {
                            view.context.startActivity(it)
                        }
                    true
                }
                url.startsWith("tel:") || url.startsWith("sms:") || url.startsWith("smsto:")
                        || url.startsWith("mms:") || url.startsWith("mmsto:") || url.startsWith("mailto:") -> {

                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .let {
                            view.context.startActivity(it)
                        }
                    true
                }
                else -> super.shouldOverrideUrlLoading(view, url)
            }
        }
    }

    override fun onBackPressed() {
        presenter.onBackPressed(popupMenu, webView)
    }

    private val downloadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                if (downloadManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        val downloadedUri =
                            downloadManager!!.getUriForDownloadedFile(mLastDownloadId)
                        val mimeType =
                            downloadManager!!.getMimeTypeForDownloadedFile(mLastDownloadId)
                        NotifyDownloadedTask().execute(downloadedUri.toString(), mimeType)
                    }
                }
            } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED == action) {
                val notiIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                notiIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(notiIntent)
            }
        }
    }

    private inner class NotifyDownloadedTask : AsyncTask<String?, Void?, Array<String?>?>() {

        override fun doInBackground(vararg params: String?): Array<String?>? {
            if (params == null || params.size != 2) {
                return null
            }
            val uriStr = params[0]
            val mimeType = params[1]
            var fileName = ""
            val query = DownloadManager.Query()
            query.setFilterById(mLastDownloadId)
            val c = downloadManager!!.query(query)
            if (c.moveToFirst()) {
                val status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))
                if (DownloadManager.STATUS_SUCCESSFUL == status) {
                    fileName = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE))
                }
            }
            return arrayOf(uriStr, fileName, mimeType)
        }

        override fun onPostExecute(results: Array<String?>?) {
            if (results != null && results.size == 3) {
                val uriStr = results[0]
                val fileName = results[1]
                val mimeType = results[2]
                Snackbar.make(
                    coordinatorlayout,
                    fileName + getString(R.string.downloaded_message),
                    Snackbar.LENGTH_LONG
                ).apply {
                    duration = resources.getInteger(R.integer.snackbar_duration)
                    setAction(getString(R.string.open)) {
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(Uri.parse(uriStr), mimeType)
                            .let {
                                presenter.startActivity(it)
                            }
                    }
                }.run {
                    show()
                }
            }
        }
    }

    companion object {
        private const val TAG = "WebViewerActivity"
        private const val REQUEST_FILE_CHOOSER = 0
        private const val REQUEST_FILE_CHOOSER_FOR_LOLLIPOP = 1
        private const val REQUEST_PERMISSION_SETTING = 2
        const val EXTRA_URL = "url"
    }
}
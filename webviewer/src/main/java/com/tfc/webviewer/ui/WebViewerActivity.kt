/*
 * Copyright Fobid. All Rights Reserved.
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
import android.databinding.DataBindingUtil
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.WindowManager
import android.webkit.*
import android.webkit.WebChromeClient.FileChooserParams
import android.widget.*
import com.tfc.webviewer.R
import com.tfc.webviewer.databinding.AWebViewerBinding
import com.tfc.webviewer.databinding.PopupMenuBinding
import com.tfc.webviewer.presenter.WebViewPresenterImpl
import com.tfc.webviewer.util.ClipboardUtils
import com.tfc.webviewer.util.FileUtils
import com.tfc.webviewer.util.PermissionUtils

/**
 * author @Fobid
 */
class WebViewerActivity : AppCompatActivity(),
    WebViewPresenterImpl.View,
    View.OnClickListener,
    OnRefreshListener,
    DownloadListener,
    OnCreateContextMenuListener {

    private val presenter by lazy { WebViewPresenterImpl(this, this) }

    private val popupMenu: PopupWindow by lazy {
        PopupWindow(this)
    }

    private val binding by lazy {
        DataBindingUtil.setContentView<AWebViewerBinding>(this, R.layout.a_web_viewer)
    }

    private val popupMenuBinding by lazy {
        PopupMenuBinding.inflate(LayoutInflater.from(this))
    }

    private var url = ""
    private var filePathCallbackLollipop: ValueCallback<Array<Uri>>? = null
    private var filePathCallbackNormal: ValueCallback<Uri>? = null
    private var downloadManager: DownloadManager? = null
    private var mLastDownloadId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
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
        val result = binding.aWebViewerWv.hitTestResult
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
        with(binding) {
            aWebViewerSrl.setOnRefreshListener(this@WebViewerActivity)

            aWebViewerWv.apply {
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

            toolbarContainer.apply {
                toolbarBtnClose.setOnClickListener(this@WebViewerActivity)
                toolbarBtnMore.setOnClickListener(this@WebViewerActivity)
            }
        }

        // PopupWindow
        initPopupMenu()
    }

    private fun initPopupMenu() {
        popupMenu.apply {
            contentView = popupMenuBinding.root
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(0))
            isFocusable = true
        }

        with(popupMenuBinding) {
            popupMenuBtnBack.setOnClickListener(this@WebViewerActivity)
            popupMenuBtnForward.setOnClickListener(this@WebViewerActivity)
            popupMenuBtnRefresh.setOnClickListener(this@WebViewerActivity)
            popupMenuBtnCopyLink.setOnClickListener(this@WebViewerActivity)
            popupMenuBtnOpenWithOtherBrowser.setOnClickListener(this@WebViewerActivity)
            popupMenuBtnShare.setOnClickListener(this@WebViewerActivity)
        }
    }

    override fun loadUrl(url: String) {
        binding.aWebViewerWv.loadUrl(url)
        presenter.onReceivedTitle("", this.url)
    }

    override fun close() {
        finish()
    }

    override fun closeMenu() {
        popupMenu.dismiss()
    }

    override fun openMenu() {
        popupMenu.showAsDropDown(binding.toolbarContainer.toolbarBtnMore)
    }

    override fun setEnabledGoBackAndGoForward() {
        popupMenuBinding.popupMenuRlArrows.visibility = View.VISIBLE
    }

    override fun setDisabledGoBackAndGoForward() {
        popupMenuBinding.popupMenuRlArrows.visibility = View.GONE
    }

    override fun setEnabledGoBack() {
        popupMenuBinding.popupMenuBtnBack.isEnabled = true
    }

    override fun setDisabledGoBack() {
        popupMenuBinding.popupMenuBtnBack.isEnabled = false
    }

    override fun setEnabledGoForward() {
        popupMenuBinding.popupMenuBtnForward.isEnabled = true
    }

    override fun setDisabledGoForward() {
        popupMenuBinding.popupMenuBtnForward.isEnabled = false
    }

    override fun goBack() {
        binding.aWebViewerWv.goBack()
    }

    override fun goForward() {
        binding.aWebViewerWv.goForward()
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
            .putExtra(Intent.EXTRA_TEXT, binding.aWebViewerWv.url)
            .setType("text/plain")
            .let {
                startActivity(Intent.createChooser(it, resources.getString(R.string.menu_share)))
            }
    }

    override fun setToolbarTitle(title: String) {
        binding.toolbarContainer.toolbarTvTitle.text = title
    }

    override fun setToolbarUrl(url: String) {
        binding.toolbarContainer.toolbarTvUrl.text = url
    }

    override fun onDownloadStart(url: String) {
        onDownloadStart(url, null, null, "image/jpeg", 0)
    }

    override fun setProgressBar(progress: Int) {
        binding.aWebViewerPb.progress = progress
    }

    override fun setRefreshing(refreshing: Boolean) {
        binding.aWebViewerSrl.isRefreshing = refreshing
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
        presenter.onClick(v.id, binding.aWebViewerWv.url ?: "", popupMenu)
    }

    override fun onRefresh() {
        binding.aWebViewerWv.reload()
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
            presenter.onProgressChanged(binding.aWebViewerSrl, progress)
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
        presenter.onBackPressed(popupMenu, binding.aWebViewerWv)
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
                    binding.aWebViewerCoordinatorlayout,
                    fileName + getString(R.string.downloaded_message),
                    Snackbar.LENGTH_LONG
                )
                    .setDuration(resources.getInteger(R.integer.snackbar_duration))
                    .setAction(getString(R.string.open)) {
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(Uri.parse(uriStr), mimeType)
                            .let {
                                presenter.startActivity(it)
                            }
                    }
                    .show()
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
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
import android.content.res.Configuration
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
import android.support.v7.widget.AppCompatImageButton
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

    private val binding by lazy {
        DataBindingUtil.setContentView<AWebViewerBinding>(this, R.layout.a_web_viewer)
    }

    private var mUrl = ""
    private var filePathCallbackLollipop: ValueCallback<Array<Uri>>? = null
    private var filePathCallbackNormal: ValueCallback<Uri?>? = null
    private var mPresenter: WebViewPresenterImpl? = null
    private var mDownloadManager: DownloadManager? = null
    private var mLastDownloadId: Long = 0

    // PopupWindow
    private val mPopupMenu: PopupWindow by lazy {
        PopupWindow(this)
    }
    private var mLlControlButtons: RelativeLayout? = null
    private var mBtnBack: AppCompatImageButton? = null
    private var mBtnFoward: AppCompatImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        supportActionBar!!.hide()
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        filter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        registerReceiver(mDownloadReceiver, filter)
        mUrl = intent.getStringExtra(EXTRA_URL) ?: ""
        bindView()
        mPresenter = WebViewPresenterImpl(this, this)
        mPresenter!!.validateUrl(mUrl)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mDownloadReceiver)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val result = binding.aWebViewerWv!!.hitTestResult
        mPresenter!!.onLongClick(result)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (mUrl != null) {
            outState.putString(EXTRA_URL, mUrl)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        mUrl = savedInstanceState.getString(EXTRA_URL)
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (RESULT_OK == resultCode) {
            when (requestCode) {
                REQUEST_FILE_CHOOSER -> {
                    if (filePathCallbackNormal == null) {
                        return
                    }
                    val result = if (data == null) null else data.data
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
                    if (mDownloadUrl != null && mDownloadMimetype != null) {
                        mLastDownloadId =
                            FileUtils.downloadFile(this, mDownloadUrl!!, mDownloadMimetype!!)
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
        binding.apply {
            aWebViewerSrl.setOnRefreshListener(this@WebViewerActivity)

            aWebViewerWv.apply {
                settings.let { webSettings ->
                    webSettings.javaScriptEnabled = true
                    webSettings.javaScriptCanOpenWindowsAutomatically = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        webSettings.displayZoomControls = false
                    }
                    webSettings.builtInZoomControls = true
                    webSettings.setSupportZoom(true)
                    webSettings.domStorageEnabled = true
                }

                setWebChromeClient(MyWebChromeClient())
                setWebViewClient(MyWebViewClient())
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
        @SuppressLint("InflateParams") val view =
            LayoutInflater.from(this).inflate(R.layout.popup_menu, null)
        mPopupMenu!!.contentView = view
        mPopupMenu!!.isOutsideTouchable = true
        mPopupMenu!!.setBackgroundDrawable(ColorDrawable(0))
        mPopupMenu!!.isFocusable = true
        mLlControlButtons = view.findViewById(R.id.popup_menu_rl_arrows) as RelativeLayout
        mBtnBack = view.findViewById(R.id.popup_menu_btn_back) as AppCompatImageButton
        mBtnFoward = view.findViewById(R.id.popup_menu_btn_forward) as AppCompatImageButton
        mBtnBack!!.setOnClickListener(this)
        mBtnFoward!!.setOnClickListener(this)
        view.findViewById(R.id.popup_menu_btn_refresh).setOnClickListener(this)
        view.findViewById(R.id.popup_menu_btn_copy_link).setOnClickListener(this)
        view.findViewById(R.id.popup_menu_btn_open_with_other_browser).setOnClickListener(this)
        view.findViewById(R.id.popup_menu_btn_share).setOnClickListener(this)
    }

    override fun loadUrl(url: String) {
        binding.aWebViewerWv!!.loadUrl(url)
        mPresenter!!.onReceivedTitle("", mUrl)
    }

    override fun close() {
        finish()
    }

    override fun closeMenu() {
        mPopupMenu.dismiss()
    }

    override fun openMenu() {
        mPopupMenu.showAsDropDown(binding.toolbarContainer.toolbarBtnMore)
    }

    override fun setEnabledGoBackAndGoForward() {
        mLlControlButtons!!.visibility = View.VISIBLE
    }

    override fun setDisabledGoBackAndGoForward() {
        mLlControlButtons!!.visibility = View.GONE
    }

    override fun setEnabledGoBack() {
        mBtnBack!!.isEnabled = true
    }

    override fun setDisabledGoBack() {
        mBtnBack!!.isEnabled = false
    }

    override fun setEnabledGoForward() {
        mBtnFoward!!.isEnabled = true
    }

    override fun setDisabledGoForward() {
        mBtnFoward!!.isEnabled = false
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
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, binding.aWebViewerWv!!.url)
        intent.type = "text/plain"
        startActivity(Intent.createChooser(intent, resources.getString(R.string.menu_share)))
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
        binding.aWebViewerPb!!.progress = progress
    }

    override fun setRefreshing(refreshing: Boolean) {
        binding.aWebViewerSrl!!.isRefreshing = refreshing
    }

    override fun openEmail(email: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null))
        startActivity(Intent.createChooser(intent, getString(R.string.email)))
    }

    override fun openPopup(url: String) {
        val intent = Intent(this, WebViewerActivity::class.java)
        intent.putExtra(EXTRA_URL, url)
        startActivity(intent)
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
                if (mDownloadUrl != null && mDownloadMimetype != null) {
                    mLastDownloadId =
                        FileUtils.downloadFile(this, mDownloadUrl!!, mDownloadMimetype!!)
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

    private var mDownloadUrl: String? = null
    private var mDownloadMimetype: String? = null

    override fun onDownloadStart(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String,
        contentLength: Long
    ) {
        if (mDownloadManager == null) {
            mDownloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        }
        mDownloadUrl = url
        mDownloadMimetype = mimeType
        val hasPermission = PermissionUtils.hasPermission(
            this@WebViewerActivity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            PermissionUtils.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
        )
        if (hasPermission) {
            mLastDownloadId = FileUtils.downloadFile(this, url, mimeType)
        }
    }

    override fun onClick(v: View) {
        mPresenter!!.onClick(v.id, binding.aWebViewerWv!!.url, mPopupMenu)
    }

    override fun onRefresh() {
        binding.aWebViewerWv!!.reload()
    }

    inner class MyWebChromeClient : WebChromeClient() {
        // For Android 3.0+
        // For Android < 3.0
        @JvmOverloads
        fun openFileChooser(
            uploadMsg: ValueCallback<Uri?>?,
            acceptType: String? = ""
        ) {
            filePathCallbackNormal = uploadMsg
            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)
            i.type = "image/*"
            startActivityForResult(
                Intent.createChooser(i, getString(R.string.select_image)),
                REQUEST_FILE_CHOOSER
            )
        }

        // For Android 5.0+
        override fun onShowFileChooser(
            webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            if (filePathCallbackLollipop != null) {
                filePathCallbackLollipop!!.onReceiveValue(null)
                filePathCallbackLollipop = null
            }
            filePathCallbackLollipop = filePathCallback
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.select_image)),
                REQUEST_FILE_CHOOSER_FOR_LOLLIPOP
            )
            return true
        }

        override fun onJsAlert(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            val dialog = AlertDialog.Builder(this@WebViewerActivity)
                .setMessage(message)
                .setPositiveButton(R.string.yes) { dialog, which -> result.confirm() }
                .create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
            return true
        }

        override fun onJsConfirm(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            val dialog = AlertDialog.Builder(this@WebViewerActivity)
                .setMessage(message)
                .setPositiveButton(R.string.yes) { dialog, which -> result.confirm() }
                .setNegativeButton(R.string.no) { dialog, which -> result.cancel() }
                .create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
            return true
        }

        override fun onProgressChanged(view: WebView, progress: Int) {
            mPresenter!!.onProgressChanged(binding.aWebViewerSrl, progress)
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            mPresenter!!.onReceivedTitle(title, view.url)
        }
    }

    inner class MyWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            mPresenter!!.onReceivedTitle(view.title, url)
            mPresenter!!.setEnabledGoBackAndGoForward(view.canGoBack(), view.canGoForward())
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return if (url.endsWith(".mp4")) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(url), "video/*")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                view.context.startActivity(intent)
                true
            } else if (url.startsWith("tel:") || url.startsWith("sms:") || url.startsWith("smsto:")
                || url.startsWith("mms:") || url.startsWith("mmsto:") || url.startsWith("mailto:")
            ) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                view.context.startActivity(intent)
                true
            } else {
                super.shouldOverrideUrlLoading(view, url)
            }
        }
    }

    override fun onBackPressed() {
        mPresenter!!.onBackPressed(mPopupMenu, binding.aWebViewerWv)
    }

    private val mDownloadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                if (mDownloadManager != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        val downloadedUri =
                            mDownloadManager!!.getUriForDownloadedFile(mLastDownloadId)
                        val mimeType =
                            mDownloadManager!!.getMimeTypeForDownloadedFile(mLastDownloadId)
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
            val c = mDownloadManager!!.query(query)
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
                        val viewIntent = Intent(Intent.ACTION_VIEW)
                        viewIntent.setDataAndType(Uri.parse(uriStr), mimeType)
                        mPresenter!!.startActivity(viewIntent)
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
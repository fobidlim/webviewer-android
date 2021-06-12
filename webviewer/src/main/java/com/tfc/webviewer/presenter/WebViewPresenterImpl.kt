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
package com.tfc.webviewer.presenter

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Log
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import android.widget.PopupWindow
import android.widget.Toast
import com.tfc.webviewer.R
import com.tfc.webviewer.util.UrlUtils
import com.tfc.webviewer.vibrate
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URLEncoder

/**
 * author @Fobid
 */
class WebViewPresenterImpl(
    private val context: Context,
    private val view: View
) : IWebViewPresenter {

    private fun makeToast(text: CharSequence): Toast =
        Toast.makeText(context, text, Toast.LENGTH_LONG)

    override fun validateUrl(url: String) =
        if (URLUtil.isValidUrl(url)) {
            view.loadUrl(url)
        } else {
            if (!TextUtils.isEmpty(url)) {
                var tempUrl = url
                if (!URLUtil.isHttpUrl(url) && !URLUtil.isHttpsUrl(url)) {
                    tempUrl = "http://$url"
                }
                var host = ""
                try {
                    host = UrlUtils.getHost(tempUrl)
                } catch (e: MalformedURLException) {
                    view.setToolbarUrl(context.getString(R.string.loading))
                }
                if (URLUtil.isValidUrl(tempUrl)) {
                    view.loadUrl(tempUrl)
                    view.setToolbarTitle(host)
                } else try {
                    tempUrl = "http://www.google.com/search?q=" + URLEncoder.encode(url, "UTF-8")
                    tempUrl = UrlUtils.getHost(tempUrl)
                    view.loadUrl(tempUrl)
                    view.setToolbarTitle(tempUrl)
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                    view.showToast(makeToast(context.getString(R.string.message_invalid_url)))
                    view.close()
                } catch (e: MalformedURLException) {
                    view.setToolbarUrl(context.getString(R.string.loading))
                }
            } else {
                view.showToast(makeToast(context.getString(R.string.message_invalid_url)))
                view.close()
            }
        }

    override fun onBackPressed(menu: PopupWindow, webView: WebView) =
        when {
            menu.isShowing -> view.closeMenu()
            webView.canGoBack() -> view.goBack()
            else -> view.close()
        }

    override fun onReceivedTitle(title: String, url: String) {
        view.setToolbarTitle(title)
        try {
            var tempUrl: String? = url
            tempUrl = UrlUtils.getHost(tempUrl)
            view.setToolbarUrl(tempUrl)
        } catch (e: MalformedURLException) {
            view.setToolbarUrl(context.getString(R.string.loading))
        }
    }

    override fun onClick(resId: Int, url: String, popupWindow: PopupWindow) {
        view.closeMenu()

        when (resId) {
            R.id.toolbar_btn_close -> view.close()
            R.id.toolbar_btn_more -> {
                if (popupWindow.isShowing) {
                    view.closeMenu()
                } else {
                    view.openMenu()
                }
            }
            R.id.popup_menu_btn_back -> view.goBack()
            R.id.popup_menu_btn_forward -> view.goForward()
            R.id.popup_menu_btn_refresh -> view.onRefresh()
            R.id.popup_menu_btn_copy_link -> view.run {
                copyLink(url)
                showToast(makeToast(context.getString(R.string.message_copy_to_clipboard)))
            }
            R.id.popup_menu_btn_open_with_other_browser -> view.openBrowser(Uri.parse(url))
            R.id.popup_menu_btn_share -> view.openShare(url)
            else -> {
            }
        }
    }

    override fun setEnabledGoBackAndGoForward(enabledGoBack: Boolean, enabledGoForward: Boolean) {
        if (enabledGoBack || enabledGoForward) {
            view.setEnabledGoBackAndGoForward()
            if (enabledGoBack) {
                view.setEnabledGoBack()
            } else {
                view.setDisabledGoBack()
            }
            if (enabledGoForward) {
                view.setEnabledGoForward()
            } else {
                view.setDisabledGoForward()
            }
        } else {
            view.setDisabledGoBackAndGoForward()
        }
    }

    override fun onLongClick(result: HitTestResult) {
        context.vibrate()

        val type = result.type
        val extra = result.extra ?: return

        when (type) {
            HitTestResult.EMAIL_TYPE -> {
                val items = arrayOf<CharSequence>(
                    context.getString(R.string.send_email),
                    context.getString(R.string.copy_email),
                    context.getString(R.string.copy_link_text)
                )
                AlertDialog.Builder(context)
                    .setTitle(extra)
                    .setItems(items) { _, which ->
                        if (which == 0) {
                            view.openEmail(extra)
                        } else if (which == 1 || which == 2) {
                            view.copyLink(extra)
                            view.showToast(makeToast(context.getString(R.string.message_copy_to_clipboard)))
                        }
                    }
                    .show()
            }
            HitTestResult.GEO_TYPE -> {
                Log.d(TAG, "geo longclicked")
            }
            HitTestResult.SRC_IMAGE_ANCHOR_TYPE, HitTestResult.IMAGE_TYPE -> {
                val items = arrayOf<CharSequence>(
                    context.getString(R.string.copy_link),
                    context.getString(R.string.save_link),
                    context.getString(R.string.save_image),
                    context.getString(R.string.open_image)
                )
                AlertDialog.Builder(context)
                    .setTitle(extra)
                    .setItems(items) { _, which ->
                        when (which) {
                            0 ->
                                view.run {
                                    copyLink(extra)
                                    showToast(makeToast(context.getString(R.string.message_copy_to_clipboard)))
                                }
                            1 -> view.onDownloadStart(extra)
                            2 -> view.onDownloadStart(extra)
                            3 -> view.openPopup(extra)
                            else -> {
                            }
                        }
                    }
                    .show()
            }
            HitTestResult.PHONE_TYPE, HitTestResult.SRC_ANCHOR_TYPE -> {
                val items = arrayOf<CharSequence>(
                    context.getString(R.string.copy_link),
                    context.getString(R.string.copy_link_text),
                    context.getString(R.string.save_link)
                )
                AlertDialog.Builder(context)
                    .setTitle(extra)
                    .setItems(items) { _, which ->
                        when (which) {
                            0 -> view.run {
                                copyLink(extra)
                                showToast(makeToast(context.getString(R.string.message_copy_to_clipboard)))
                            }
                            1 -> view.run {
                                copyLink(extra)
                                showToast(makeToast(context.getString(R.string.message_copy_to_clipboard)))
                            }
                            2 -> view.onDownloadStart(extra)
                            else -> {
                            }
                        }
                    }
                    .show()
            }
        }
    }

    override fun onProgressChanged(swipeRefreshLayout: SwipeRefreshLayout, progress: Int) {
        var progress = progress
        if (swipeRefreshLayout.isRefreshing && progress == 100) {
            swipeRefreshLayout.post { view.setRefreshing(false) }
        }
        if (!swipeRefreshLayout.isRefreshing && progress != 100) {
            swipeRefreshLayout.post { view.setRefreshing(true) }
        }
        if (progress == 100) {
            progress = 0
        }
        view.setProgressBar(progress)
    }

    fun startActivity(intent: Intent) =
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            view.showToast(makeToast(context.getString(R.string.message_activity_not_found)))
        }

    interface View {
        fun loadUrl(url: String)
        fun close()
        fun closeMenu()
        fun openMenu()
        fun setEnabledGoBackAndGoForward()
        fun setDisabledGoBackAndGoForward()
        fun setEnabledGoBack()
        fun setDisabledGoBack()
        fun setEnabledGoForward()
        fun setDisabledGoForward()
        fun goBack()
        fun goForward()
        fun onRefresh()
        fun copyLink(url: String)
        fun showToast(toast: Toast)
        fun openBrowser(uri: Uri)
        fun openShare(url: String)
        fun setToolbarTitle(title: String)
        fun setToolbarUrl(url: String)
        fun onDownloadStart(url: String)
        fun setProgressBar(progress: Int)
        fun setRefreshing(refreshing: Boolean)
        fun openEmail(email: String)
        fun openPopup(url: String)
    }

    companion object {
        private const val TAG = "WebViewPresenterImpl"
    }
}
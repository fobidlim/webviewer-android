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
package com.tfc.webviewer.presenter;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.tfc.webviewer.R;
import com.tfc.webviewer.util.UrlUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * author @Fobid
 */
public class WebViewPresenterImpl implements IWebViewPresenter {

    private final Context mContext;
    private final View mView;

    public WebViewPresenterImpl(Context context, View view) {
        mContext = context;
        mView = view;
    }

    private Toast makeToast(CharSequence text) {
        return Toast.makeText(mContext, text, Toast.LENGTH_LONG);
    }

    @Override
    public void validateUrl(final String url) {
        if (URLUtil.isValidUrl(url)) {
            mView.loadUrl(url);
        } else {
            if (!TextUtils.isEmpty(url)) {
                String tempUrl = url;
                if (!URLUtil.isHttpUrl(url) && !URLUtil.isHttpsUrl(url)) {
                    tempUrl = "http://" + url;
                }
                String host = UrlUtils.getHost(tempUrl);

                if (URLUtil.isValidUrl(tempUrl)) {
                    mView.loadUrl(tempUrl);
                    mView.setToolbarTitle(host);
                } else try {
                    tempUrl = "http://www.google.com/search?q=" + URLEncoder.encode(url, "UTF-8");

                    mView.loadUrl(tempUrl);
                    mView.setToolbarTitle(UrlUtils.getHost(tempUrl));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();

                    mView.showToast(makeToast(mContext.getString(R.string.message_invalid_url)));
                    mView.close();
                }
            } else {
                mView.showToast(makeToast(mContext.getString(R.string.message_invalid_url)));
                mView.close();
            }
        }
    }

    @Override
    public void onBackPressed(PopupWindow menu, WebView webView) {
        if (menu.isShowing()) {
            mView.closeMenu();
        } else if (webView.canGoBack()) {
            mView.goBack();
        } else {
            mView.close();
        }
    }

    @Override
    public void onReceivedTitle(String title, String url) {
        mView.setToolbarTitle(title);
        mView.setToolbarUrl(UrlUtils.getHost(url));
    }

    @Override
    public void onClickMenu(PopupWindow menu) {
        if (menu.isShowing()) {
            mView.closeMenu();
        } else {
            mView.openMenu();
        }
    }

    @Override
    public void setEnabledGoBackAndGoFoward(boolean enabledGoBack, boolean enabledGoFoward) {
        if (enabledGoBack || enabledGoFoward) {
            mView.setEnabledGoBackAndGoFoward();

            if (enabledGoBack) {
                mView.setEnabledGoBack();
            } else {
                mView.setDisabledGoBack();
            }

            if (enabledGoFoward) {
                mView.setEnabledGoFoward();
            } else {
                mView.setDisabledGoFoward();
            }
        } else {
            mView.setDisabledGoBackAndGoFoward();
        }
    }

    @Override
    public void onClickClose() {
        mView.close();
    }

    @Override
    public void onClickGoBack() {
        mView.goBack();
    }

    @Override
    public void onClickGoFoward() {
        mView.goFoward();
    }

    @Override
    public void onClickCopyLink(String url) {
        mView.copyLink(url);

        mView.showToast(makeToast(mContext.getString(R.string.message_copy_to_clipboard)));
    }

    @Override
    public void onClickOpenBrowser(Uri uri) {
        mView.openBrowser(uri);
    }

    @Override
    public void onClickShare(String url) {
        mView.openShare(url);
    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
        try {
            String[] fnm = url.split("/");
            String fileName = fnm[fnm.length - 1];
            String host = fnm[2];

            DownloadManager dm = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            // mRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI); //NETWORK_MOBILE
            // mRequest.setAllowedOverRoaming(false);
            request.setTitle(fileName);
            request.setDescription(host);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            if (!downloadsDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                downloadsDir.mkdirs();
            }
            // mRequest.setMimeType(HTTP.OCTET_STREAM_TYPE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            } else {
                //noinspection deprecation
                request.setShowRunningNotification(true);
            }
            request.setVisibleInDownloadsUi(true);
            dm.enqueue(request);

            @SuppressLint("ShowToast")
            Toast toast = Toast.makeText(mContext, mContext.getString(R.string.message_download_started), Toast.LENGTH_SHORT);

            mView.showToast(toast);
        } catch (SecurityException e) {
            throw new SecurityException("No permission allowed: android.permission.WRITE_EXTERNAL_STORAGE");
        }
    }

    @Override
    public void onLongClick(WebView.HitTestResult result) {
        int type = result.getType();
        String extra = result.getExtra();

        switch (type) {
            case WebView.HitTestResult.EDIT_TEXT_TYPE: {
                break;
            }
            case WebView.HitTestResult.EMAIL_TYPE: {
                break;
            }
            case WebView.HitTestResult.GEO_TYPE: {
                break;
            }
            case WebView.HitTestResult.IMAGE_TYPE: {
                break;
            }
            case WebView.HitTestResult.PHONE_TYPE: {
                break;
            }
            case WebView.HitTestResult.SRC_ANCHOR_TYPE: {
                break;
            }
            case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE: {
                break;
            }
            case WebView.HitTestResult.UNKNOWN_TYPE: {
                break;
            }
        }
    }

    public interface View {
        void loadUrl(String url);

        void close();

        void closeMenu();

        void openMenu();

        void setEnabledGoBackAndGoFoward();

        void setDisabledGoBackAndGoFoward();

        void setEnabledGoBack();

        void setDisabledGoBack();

        void setEnabledGoFoward();

        void setDisabledGoFoward();

        void goBack();

        void goFoward();

        void copyLink(String url);

        void showToast(Toast toast);

        void openBrowser(Uri uri);

        void openShare(String url);

        void onProgressChanged(int progress);

        void setToolbarTitle(String title);

        void setToolbarUrl(String url);

        void onReceivedTouchIconUrl(String url, boolean precomposed);

        void onPageStarted(String url);

        void onPageFinished(String url);

        void onLoadResource(String url);

        void onPageCommitVisible(String url);
    }
}

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

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.tfc.webviewer.R;
import com.tfc.webviewer.util.UrlUtils;

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

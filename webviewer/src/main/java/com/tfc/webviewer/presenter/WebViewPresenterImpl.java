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

import android.webkit.WebView;
import android.widget.PopupWindow;

/**
 * author @Fobid
 */
public class WebViewPresenterImpl implements IWebViewPresenter {

    private final View mView;

    public WebViewPresenterImpl(View view) {
        mView = view;
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
        mView.setToolbarUrl(url);
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
    public void onRefresh() {
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

    public interface View {
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

        void onProgressChanged(int progress);

        void setToolbarTitle(String title);

        void setToolbarUrl(String url);

        void onReceivedTouchIconUrl(String url, boolean precomposed);

        void onPageStarted(String url);

        void onPageFinished(String url);

        void onLoadResource(String url);

        void onPageCommitVisible(String url);

        void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength);
    }
}

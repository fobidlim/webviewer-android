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
package com.tfc.webviewer.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tfc.webviewer.R;
import com.tfc.webviewer.presenter.WebViewPresenterImpl;
import com.tfc.webviewer.util.ClipboardUtils;
import com.tfc.webviewer.util.UrlUtils;

/**
 * author @Fobid
 */
public class WebViewerActivity extends AppCompatActivity implements WebViewPresenterImpl.View,
        View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    public static final String EXTRA_URL = "url";

    private String mUrl;

    private WebViewPresenterImpl mPresenter;

    // Toolbar
    private TextView mTvTitle;
    private TextView mTvUrl;

    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private WebView mWebView;

    // PopupWindow
    private PopupWindow mPopupMenu;
    private RelativeLayout mLlControlButtons;
    private AppCompatImageButton mBtnMore;
    private AppCompatImageButton mBtnBack;
    private AppCompatImageButton mBtnFoward;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //noinspection ConstantConditions
        getSupportActionBar().hide();

        mUrl = getIntent().getStringExtra(EXTRA_URL);

        setContentView(R.layout.a_web_viewer);
        bindView();

        mPresenter = new WebViewPresenterImpl(this, this);
        mPresenter.verifyAvailableUrl(mUrl);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mUrl != null) {
            outState.putString(EXTRA_URL, mUrl);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mUrl = savedInstanceState.getString(EXTRA_URL);
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void bindView() {
        // Toolbar
        mTvTitle = (TextView) findViewById(R.id.toolbar_tv_title);
        mTvUrl = (TextView) findViewById(R.id.toolbar_tv_url);

        mProgressBar = (ProgressBar) findViewById(R.id.a_web_viewer_pb);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.a_web_viewer_srl);
        mWebView = (WebView) findViewById(R.id.a_web_viewer_wv);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mWebView.setWebChromeClient(new MyWebChromeClient());
        mWebView.setWebViewClient(new MyWebViewClient());

        mBtnMore = (AppCompatImageButton) findViewById(R.id.toolbar_btn_more);

        //noinspection ConstantConditions
        findViewById(R.id.toolbar_btn_close).setOnClickListener(this);
        //noinspection ConstantConditions
        mBtnMore.setOnClickListener(this);

        // PopupWindow
        initPopupMenu();
    }


    private void initPopupMenu() {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(this).inflate(R.layout.popup_menu, null);

        mPopupMenu = new PopupWindow(this);

        mPopupMenu.setContentView(view);
        mPopupMenu.setOutsideTouchable(true);
        mPopupMenu.setFocusable(true);

        mLlControlButtons = (RelativeLayout) view.findViewById(R.id.popup_menu_rl_arrows);
        mBtnBack = (AppCompatImageButton) view.findViewById(R.id.popup_menu_btn_back);
        mBtnFoward = (AppCompatImageButton) view.findViewById(R.id.popup_menu_btn_forward);

        mBtnBack.setOnClickListener(this);
        mBtnFoward.setOnClickListener(this);
        view.findViewById(R.id.popup_menu_btn_refresh).setOnClickListener(this);
        view.findViewById(R.id.popup_menu_btn_copy_link).setOnClickListener(this);
        view.findViewById(R.id.popup_menu_btn_open_with_other_browser).setOnClickListener(this);
        view.findViewById(R.id.popup_menu_btn_share).setOnClickListener(this);
    }

    @Override
    public void loadUrl(String url) {
        mWebView.loadUrl(url);
        mPresenter.onReceivedTitle("", mUrl);
    }

    @Override
    public void close() {
        finish();
    }

    @Override
    public void closeMenu() {
        mPopupMenu.dismiss();
    }

    @Override
    public void openMenu() {
        mPopupMenu.showAsDropDown(mBtnMore);
    }

    @Override
    public void setEnabledGoBackAndGoFoward() {
        mLlControlButtons.setVisibility(View.VISIBLE);
    }

    @Override
    public void setDisabledGoBackAndGoFoward() {
        mLlControlButtons.setVisibility(View.GONE);
    }

    @Override
    public void setEnabledGoBack() {
        mBtnBack.setEnabled(true);
    }

    @Override
    public void setDisabledGoBack() {
        mBtnBack.setEnabled(false);
    }

    @Override
    public void setEnabledGoFoward() {
        mBtnFoward.setEnabled(true);
    }

    @Override
    public void setDisabledGoFoward() {
        mBtnFoward.setEnabled(false);
    }

    @Override
    public void goBack() {
        mWebView.goBack();
    }

    @Override
    public void goFoward() {
        mWebView.goForward();
    }

    @Override
    public void copyLink(String url) {
        ClipboardUtils.copyText(this, url);
    }

    @Override
    public void showToast(Toast toast) {
        toast.show();
    }

    @Override
    public void openBrowser(Uri uri) {
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    @Override
    public void openShare(String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.menu_share)));
    }

    @Override
    public void onProgressChanged(int progress) {

    }

    @Override
    public void setToolbarTitle(String title) {
        mTvTitle.setText(title);
    }

    @Override
    public void setToolbarUrl(String url) {
        mTvUrl.setText(UrlUtils.getHost(url));
    }

    @Override
    public void onReceivedTouchIconUrl(String url, boolean precomposed) {

    }

    @Override
    public void onPageStarted(String url) {

    }

    @Override
    public void onPageFinished(String url) {

    }

    @Override
    public void onLoadResource(String url) {

    }

    @Override
    public void onPageCommitVisible(String url) {

    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {

    }

    @Override
    public void onClick(View v) {
        int resId = v.getId();

        if (R.id.toolbar_btn_close == resId) {
            closeMenu();
            mPresenter.onClickClose();
        } else if (R.id.toolbar_btn_more == resId) {
            mPresenter.onClickMenu(mPopupMenu);
        } else if (R.id.popup_menu_btn_back == resId) {
            closeMenu();
            mPresenter.onClickGoBack();
        } else if (R.id.popup_menu_btn_forward == resId) {
            closeMenu();
            mPresenter.onClickGoFoward();
        } else if (R.id.popup_menu_btn_refresh == resId) {
            closeMenu();
            onRefresh();
        } else if (R.id.popup_menu_btn_copy_link == resId) {
            closeMenu();
            mPresenter.onClickCopyLink(mWebView.getUrl());
        } else if (R.id.popup_menu_btn_open_with_other_browser == resId) {
            closeMenu();
            mPresenter.onClickOpenBrowser(Uri.parse(mWebView.getUrl()));
        } else if (R.id.popup_menu_btn_share == resId) {
            closeMenu();
            mPresenter.onClickShare(mWebView.getUrl());
        }
    }

    @Override
    public void onRefresh() {
        mWebView.reload();
    }

    public class MyWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int progress) {
            if (mSwipeRefreshLayout.isRefreshing() && progress == 100) {
                mSwipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            if (!mSwipeRefreshLayout.isRefreshing() && progress != 100) {
                mSwipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(true);
                    }
                });
            }

            if (progress == 100) {
                progress = 0;
            }
            mProgressBar.setProgress(progress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            mPresenter.onReceivedTitle(title, view.getUrl());
        }

        @Override
        public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
        }
    }

    public class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (!url.contains("docs.google.com") && url.endsWith(".pdf")) {
                mWebView.loadUrl("http://docs.google.com/gview?embedded=true&url=" + url);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            mPresenter.onReceivedTitle(view.getTitle(), url);

//            requestCenterLayout();

            mPresenter.setEnabledGoBackAndGoFoward(view.canGoBack(), view.canGoForward());

//            if (injectJavaScript != null)
//                webView.loadUrl(injectJavaScript);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.endsWith(".mp4")) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), "video/*");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                view.getContext().startActivity(intent);
                // If we return true, onPageStarted, onPageFinished won't be called.
                return true;
            } else if (url.startsWith("tel:") || url.startsWith("sms:") || url.startsWith("smsto:") || url.startsWith("mms:") || url.startsWith("mmsto:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                view.getContext().startActivity(intent);
                return true; // If we return true, onPageStarted, onPageFinished won't be called.
            } else {
                return super.shouldOverrideUrlLoading(view, url);
            }
        }

        @Override
        public void onLoadResource(WebView view, String url) {
        }

        @Override
        public void onPageCommitVisible(WebView view, String url) {
        }
    }

    @Override
    public void onBackPressed() {
        mPresenter.onBackPressed(mPopupMenu, mWebView);
    }
}

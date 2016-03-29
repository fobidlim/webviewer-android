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
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tfc.webviewer.R;
import com.tfc.webviewer.presenter.WebViewPresenterImpl;
import com.tfc.webviewer.view.IWebViewerView;

/**
 * author @Fobid
 */
public class WebViewerActivity extends AppCompatActivity implements IWebViewerView,
        View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    public static final String EXTRA_URL = "url";

    private String mUrl;

    private WebViewPresenterImpl mPresenter;

    // Toolbar
    private TextView mTvTitle;
    private TextView mTvUrl;

    private ProgressBar mPb;
    private SwipeRefreshLayout mSrl;
    private WebView mWv;

    // PopupWindow
    private PopupWindow mPopupMenu;
    private LinearLayout mLlControlButtons;
    private AppCompatImageButton mBtnMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //noinspection ConstantConditions
        getSupportActionBar().hide();

        mUrl = getIntent().getStringExtra(EXTRA_URL);

        setContentView(R.layout.a_web_viewer);

        mPresenter = new WebViewPresenterImpl(this);

        bindView();

        initPopupMenu();


    }

    private void bindView() {
        // Toolbar
        mTvTitle = (TextView) findViewById(R.id.toolbar_tv_title);
        mTvUrl = (TextView) findViewById(R.id.toolbar_tv_url);

        mPresenter.onReceivedTitle("", mUrl);

        mPb = (ProgressBar) findViewById(R.id.a_web_viewer_pb);
        mSrl = (SwipeRefreshLayout) findViewById(R.id.a_web_viewer_srl);
        mWv = (WebView) findViewById(R.id.a_web_viewer_wv);
        mSrl.setOnRefreshListener(this);

        mWv.setWebChromeClient(new MyWebChromeClient());
        mWv.setWebViewClient(new MyWebViewClient());
        mWv.loadUrl(mUrl);

        // PopupWindow
        mBtnMore = (AppCompatImageButton) findViewById(R.id.toolbar_btn_more);
        //noinspection ConstantConditions
        mBtnMore.setOnClickListener(this);
    }


    private void initPopupMenu() {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(this).inflate(R.layout.popup_menu, null);

        mPopupMenu = new PopupWindow(this);

        mPopupMenu.setContentView(view);


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
    public void onProgressChanged(int progress) {

    }

    @Override
    public void setToolbarTitle(String title) {
        mTvTitle.setText(title);
    }

    @Override
    public void setToolbarUrl(String url) {
        mTvUrl.setText(url);
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
        mPresenter.onClickMenu(mPopupMenu);

//        if (R.id.more == v.getId()) {
//            mPresenter.onClickMenu(mPopupMenu);
//        }
    }

    @Override
    public void onRefresh() {
        mWv.reload();
    }

    public class MyWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int progress) {
//            BroadCastManager.onProgressChanged(FinestWebViewActivity.this, key, progress);

            if (mSrl.isRefreshing() && progress == 100) {
                mSrl.post(new Runnable() {
                    @Override
                    public void run() {
                        mSrl.setRefreshing(false);
                    }
                });
            }

            if (!mSrl.isRefreshing() && progress != 100) {
                mSrl.post(new Runnable() {
                    @Override
                    public void run() {
                        mSrl.setRefreshing(true);
                    }
                });
            }

            if (progress == 100) {
                progress = 0;
            }
            mPb.setProgress(progress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            mPresenter.onReceivedTitle(title, view.getUrl());
//            BroadCastManager.onReceivedTitle(FinestWebViewActivity.this, key, title);
        }

        @Override
        public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
//            BroadCastManager.onReceivedTouchIconUrl(FinestWebViewActivity.this, key, url, precomposed);
        }
    }

    public class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
//            BroadCastManager.onPageStarted(FinestWebViewActivity.this, key, url);
            if (!url.contains("docs.google.com") && url.endsWith(".pdf")) {
                mWv.loadUrl("http://docs.google.com/gview?embedded=true&url=" + url);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
//            BroadCastManager.onPageFinished(FinestWebViewActivity.this, key, url);

//            if (updateTitleFromHtml)
//                title.setText(view.getTitle());
//            urlTv.setText(UrlParser.getHost(url));
//            requestCenterLayout();
//
//            if (view.canGoBack() || view.canGoForward()) {
//                back.setVisibility(showIconBack ? View.VISIBLE : View.GONE);
//                forward.setVisibility(showIconForward ? View.VISIBLE : View.GONE);
//                back.setEnabled(!disableIconBack && (rtl ? view.canGoForward() : view.canGoBack()));
//                forward.setEnabled(!disableIconForward && (rtl ? view.canGoBack() : view.canGoForward()));
//            } else {
//                back.setVisibility(View.GONE);
//                forward.setVisibility(View.GONE);
//            }
//
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
//            BroadCastManager.onLoadResource(FinestWebViewActivity.this, key, url);
        }

        @Override
        public void onPageCommitVisible(WebView view, String url) {
//            BroadCastManager.onPageCommitVisible(FinestWebViewActivity.this, key, url);
        }
    }
}

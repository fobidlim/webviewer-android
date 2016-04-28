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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.tfc.webviewer.R;
import com.tfc.webviewer.ui.WebViewerActivity;
import com.tfc.webviewer.util.UrlUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;

/**
 * author @Fobid
 */
public class WebViewPresenterImpl implements IWebViewPresenter {

    private static final String TAG = "WebViewPresenterImpl";

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
                String host = "";
                try {
                    host = UrlUtils.getHost(tempUrl);
                } catch (MalformedURLException e) {
                    mView.setToolbarUrl(mContext.getString(R.string.loading));
                }

                if (URLUtil.isValidUrl(tempUrl)) {
                    mView.loadUrl(tempUrl);
                    mView.setToolbarTitle(host);
                } else try {
                    tempUrl = "http://www.google.com/search?q=" + URLEncoder.encode(url, "UTF-8");
                    tempUrl = UrlUtils.getHost(tempUrl);

                    mView.loadUrl(tempUrl);
                    mView.setToolbarTitle(tempUrl);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();

                    mView.showToast(makeToast(mContext.getString(R.string.message_invalid_url)));
                    mView.close();
                } catch (MalformedURLException e) {
                    mView.setToolbarUrl(mContext.getString(R.string.loading));
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

        try {
            String tempUrl = url;
            tempUrl = UrlUtils.getHost(tempUrl);
            mView.setToolbarUrl(tempUrl);
        } catch (MalformedURLException e) {
            mView.setToolbarUrl(mContext.getString(R.string.loading));
        }
    }

    public void onClick(int resId, String url, PopupWindow popupWindow) {
        mView.closeMenu();

        if (R.id.toolbar_btn_close == resId) {
            mView.close();
        } else if (R.id.toolbar_btn_more == resId) {
            if (popupWindow.isShowing()) {
                mView.closeMenu();
            } else {
                mView.openMenu();
            }
        } else if (R.id.popup_menu_btn_back == resId) {
            mView.goBack();
        } else if (R.id.popup_menu_btn_forward == resId) {
            mView.goFoward();
        } else if (R.id.popup_menu_btn_refresh == resId) {
            mView.onRefresh();
        } else if (R.id.popup_menu_btn_copy_link == resId) {
            mView.copyLink(url);
            mView.showToast(makeToast(mContext.getString(R.string.message_copy_to_clipboard)));
        } else if (R.id.popup_menu_btn_open_with_other_browser == resId) {
            mView.openBrowser(Uri.parse(url));
        } else if (R.id.popup_menu_btn_share == resId) {
            mView.openShare(url);
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
    public void onLongClick(WebView.HitTestResult result) {
        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(mContext.getResources().getInteger(R.integer.vibrator_duration));

        int type = result.getType();
        final String extra = result.getExtra();

        switch (type) {
            case WebView.HitTestResult.EMAIL_TYPE: {
                CharSequence[] items = new CharSequence[]{
                        mContext.getString(R.string.send_email),
                        mContext.getString(R.string.copy_email),
                        mContext.getString(R.string.copy_link_text)
                };
                AlertDialog dialog = new AlertDialog.Builder(mContext)
                        .setTitle(extra)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    mView.openEmail(extra);
                                } else if (which == 1 || which == 2) {
                                    mView.copyLink(extra);
                                    mView.showToast(makeToast(mContext.getString(R.string.message_copy_to_clipboard)));
                                }
                            }
                        })
                        .create();
                dialog.show();

                break;
            }
            case WebView.HitTestResult.GEO_TYPE: {
                Log.d(TAG, "geo longclicked");

                break;
            }
            case WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE:
            case WebView.HitTestResult.IMAGE_TYPE: {
                CharSequence[] items = new CharSequence[]{
                        mContext.getString(R.string.copy_link),
                        mContext.getString(R.string.save_link),
                        mContext.getString(R.string.save_image),
                        mContext.getString(R.string.open_image)
                };
                AlertDialog dialog = new AlertDialog.Builder(mContext)
                        .setTitle(extra)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    mView.copyLink(extra);
                                    mView.showToast(makeToast(mContext.getString(R.string.message_copy_to_clipboard)));
                                } else if (which == 1) {
                                    mView.onDownloadStart(extra);
                                } else if (which == 2) {
                                    mView.onDownloadStart(extra);
                                } else if (which == 3) {
                                    mView.openPopup(extra);
                                }
                            }
                        })
                        .create();
                dialog.show();

                break;
            }
            case WebView.HitTestResult.PHONE_TYPE:
            case WebView.HitTestResult.SRC_ANCHOR_TYPE: {
                CharSequence[] items = new CharSequence[]{
                        mContext.getString(R.string.copy_link),
                        mContext.getString(R.string.copy_link_text),
                        mContext.getString(R.string.save_link)
                };
                AlertDialog dialog = new AlertDialog.Builder(mContext)
                        .setTitle(extra)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    mView.copyLink(extra);
                                    mView.showToast(makeToast(mContext.getString(R.string.message_copy_to_clipboard)));
                                } else if (which == 1) {
                                    mView.copyLink(extra);
                                    mView.showToast(makeToast(mContext.getString(R.string.message_copy_to_clipboard)));
                                } else if (which == 2) {
                                    mView.onDownloadStart(extra);
                                }
                            }
                        })
                        .create();
                dialog.show();

                break;
            }
        }
    }

    @Override
    public void onProgressChanged(final SwipeRefreshLayout swipeRefreshLayout, int progress) {
        if (swipeRefreshLayout.isRefreshing() && progress == 100) {
            swipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mView.setRefreshing(false);
                }
            });
        }

        if (!swipeRefreshLayout.isRefreshing() && progress != 100) {
            swipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mView.setRefreshing(true);
                }
            });
        }

        if (progress == 100) {
            progress = 0;
        }
        mView.setProgressBar(progress);
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

        void onRefresh();

        void copyLink(String url);

        void showToast(Toast toast);

        void openBrowser(Uri uri);

        void openShare(String url);

        void setToolbarTitle(String title);

        void setToolbarUrl(String url);

        void onDownloadStart(String url);

        void setProgressBar(int progress);

        void setRefreshing(boolean refreshing);

        void openEmail(String email);

        void openPopup(String url);
    }
}

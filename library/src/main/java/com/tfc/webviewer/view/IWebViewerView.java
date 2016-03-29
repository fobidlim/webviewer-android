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
package com.tfc.webviewer.view;

/**
 * author @Fobid
 */
public interface IWebViewerView {
    void close();

    void closeMenu();

    void openMenu();

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

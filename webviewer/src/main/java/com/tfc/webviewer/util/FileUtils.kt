/*
 * Copyright fobidlim. All Rights Reserved.
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
package com.tfc.webviewer.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.tfc.webviewer.R

/**
 * author @fobidlim
 */
object FileUtils {

    fun downloadFile(context: Context, url: String, mimeType: String): Long {
        return try {
            val fnm = url.split("/".toRegex()).toTypedArray()
            var fileName = fnm[fnm.size - 1]
            fileName = getFileName(fileName)
            val host = fnm[2]
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            val fileNameWithExtension = "$fileName.$extension"
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)
                .setTitle(fileNameWithExtension)
                .setMimeType(mimeType)
                .setDescription(host)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileNameWithExtension
                )
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            } else {
                @Suppress("DEPRECATION")
                request.setShowRunningNotification(true)
            }
            request.setVisibleInDownloadsUi(true)
            val downloadId = dm.enqueue(request)
            Toast.makeText(
                context,
                context.getString(R.string.message_download_started),
                Toast.LENGTH_SHORT
            ).show()
            downloadId
        } catch (e: SecurityException) {
            throw SecurityException("No permission allowed: android.permission.WRITE_EXTERNAL_STORAGE")
        }
    }

    private fun getFileName(fileName: String): String {
        var fileName = fileName
        return if (!TextUtils.isEmpty(fileName)) {
            val fragment = fileName.lastIndexOf('#')
            if (fragment > 0) {
                fileName = fileName.substring(0, fragment)
            }
            val query = fileName.lastIndexOf('?')
            if (query > 0) {
                fileName = fileName.substring(0, query)
            }
            fileName
        } else {
            ""
        }
    }
}
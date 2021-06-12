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
package com.tfc.webviewer.util

import android.annotation.TargetApi
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build

/**
 * author @Fobid
 */
object ClipboardUtils {

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun copyText(context: Context, text: String?) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ClipData.newPlainText("", text)
                .let {
                    cm.setPrimaryClip(it)
                }
        } else {
            @Suppress("DEPRECATION")
            cm.text = text
        }
    }

    fun hasText(context: Context): Boolean {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            val description = cm.primaryClipDescription
            val clipData = cm.primaryClip
            clipData != null && description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
        } else {
            @Suppress("DEPRECATION")
            cm.hasText()
        }
    }

    fun getText(context: Context): CharSequence {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            val description = cm.primaryClipDescription
            val clipData = cm.primaryClip
            if (clipData != null && description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                clipData.getItemAt(0).text
            } else {
                ""
            }
        } else {
            @Suppress("DEPRECATION")
            cm.text
        }
    }
}
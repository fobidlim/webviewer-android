package com.tfc.webviewer.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by Fobid on 2016. 4. 8..
 */
public class WebViewer extends WebView {
    public WebViewer(Context context) {
        super(context);
    }

    public WebViewer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WebViewer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WebViewer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}

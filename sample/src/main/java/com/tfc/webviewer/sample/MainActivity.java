package com.tfc.webviewer.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;
import com.tfc.webviewer.ui.WebViewerActivity;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {

    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.a_main);

        mEditText = (EditText) findViewById(R.id.a_main_et_url);
        mEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (KeyEvent.ACTION_DOWN == event.getAction()) {
                    if (KeyEvent.KEYCODE_ENTER == keyCode) {
                        loadUrl(null);
                    }
                }
                return false;
            }
        });

        String url = "github.com/fobid";
        startWebViewer(url);
    }

    public void loadUrl(View view) {
        String url = mEditText.getText().toString().trim();

        if (!TextUtils.isEmpty(url)) {
            startWebViewer(url);
        }
    }

    private void startWebViewer(String url) {
        Intent intent = new Intent(this, WebViewerActivity.class);
        intent.putExtra(WebViewerActivity.EXTRA_URL, url);
        startActivity(intent);
    }
}

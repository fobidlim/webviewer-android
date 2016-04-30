package com.tfc.webviewer.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.tfc.webviewer.ui.WebViewerActivity;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, WebViewerActivity.class);
        intent.putExtra(WebViewerActivity.EXTRA_URL, "github.com/fobid");
//        intent.putExtra(WebViewerActivity.EXTRA_URL, "grap.io");
        startActivity(intent);
    }
}

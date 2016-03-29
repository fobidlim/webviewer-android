package com.tfc.webviewer.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.tfc.webviewer.ui.WebViewerActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, WebViewerActivity.class);
        intent.putExtra(WebViewerActivity.EXTRA_URL, "http://www.google.com");
        startActivity(intent);
    }
}

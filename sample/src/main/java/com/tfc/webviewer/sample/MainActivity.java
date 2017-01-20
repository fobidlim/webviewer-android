package com.tfc.webviewer.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import com.crashlytics.android.Crashlytics;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.tfc.webviewer.ui.WebViewerActivity;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {

    private InterstitialAd interstitialAd;
    private AdView mAdView;
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Instantiate an InterstitialAd object
        interstitialAd = new InterstitialAd(this, "1788929238025622_1788930368025509");
        interstitialAd.setAdListener(new InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {
                // Interstitial displayed callback
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                // Interstitial dismissed callback
            }

            @Override
            public void onError(Ad ad, AdError adError) {
                // Ad error callback
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Show the ad when it's done loading.
                interstitialAd.show();
            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
            }
        });

        // Load the interstitial ad
        interstitialAd.loadAd();

        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.a_main);

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)        // All emulators
                .addTestDevice("B05EA171BA1EACADD3DFF94B87E35314")  // An example device ID.build();
                .build();
        mAdView.loadAd(adRequest);

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

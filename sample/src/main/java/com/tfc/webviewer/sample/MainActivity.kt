package com.tfc.webviewer.sample

import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import com.tfc.webviewer.sample.databinding.AMainBinding
import com.tfc.webviewer.ui.WebViewerActivity

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        DataBindingUtil.setContentView<AMainBinding>(this, R.layout.a_main)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.aMainEtUrl.setOnKeyListener { v, keyCode, event ->
            if (KeyEvent.ACTION_DOWN == event.action) {
                if (KeyEvent.KEYCODE_ENTER == keyCode) {
                    loadUrl()
                }
            }
            false
        }
        val url = "github.com/fobidlim"
        startWebViewer(url)
    }

    fun loadUrl() {
        val url = binding.aMainEtUrl.text.toString().trim()

        if (url.isNotEmpty()) {
            startWebViewer(url)
        }
    }

    private fun startWebViewer(url: String) =
        Intent(this, WebViewerActivity::class.java)
            .putExtra(WebViewerActivity.EXTRA_URL, url)
            .let {
                startActivity(it)
            }
}

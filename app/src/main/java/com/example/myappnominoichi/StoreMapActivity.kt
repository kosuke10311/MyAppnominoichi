package com.example.myappnominoichi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class StoreMapActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_NAME = "store_name"
        private const val EXTRA_ADDRESS = "store_address"
        private const val EXTRA_LAT = "store_lat"
        private const val EXTRA_LNG = "store_lng"

        fun newIntent(context: Context, store: Store): Intent =
            Intent(context, StoreMapActivity::class.java).apply {
                putExtra(EXTRA_NAME, store.name)
                putExtra(EXTRA_ADDRESS, store.address)
                if (store.lat != null && store.lng != null) {
                    putExtra(EXTRA_LAT, store.lat)
                    putExtra(EXTRA_LNG, store.lng)
                }
            }
    }

    private lateinit var web: WebView
    private lateinit var titleView: TextView

    private var pageLoaded = false
    private var pendingMarker: Triple<Double, Double, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_map)

        web = findViewById(R.id.webMap)
        titleView = findViewById(R.id.tvTitle)

        val name = intent.getStringExtra(EXTRA_NAME) ?: getString(R.string.map_default_store_name)
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""

        title = name
        titleView.text = getString(R.string.map_title, name)

        setupWebView()

        // 座標の優先順：stores.json の lat/lng → キャッシュ → ジオコーディング
        val fromExtras = if (intent.hasExtra(EXTRA_LAT) && intent.hasExtra(EXTRA_LNG)) {
            intent.getDoubleExtra(EXTRA_LAT, 0.0) to intent.getDoubleExtra(EXTRA_LNG, 0.0)
        } else null

        val known = fromExtras ?: loadCachedLatLng(address)
        if (known != null) {
            showMarker(known.first, known.second, name)
            return
        }

        geocodeAsync(address) { result ->
            if (isFinishing || isDestroyed) return@geocodeAsync
            if (result == null) {
                Toast.makeText(this, R.string.map_geocode_failed, Toast.LENGTH_SHORT).show()
            } else {
                saveCachedLatLng(address, result.first, result.second)
                showMarker(result.first, result.second, name)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                pageLoaded = true
                // 読み込み前に届いた座標をここで反映
                pendingMarker?.let { (lat, lng, name) -> evaluateShowMarker(lat, lng, name) }
                pendingMarker = null
            }
        }
        web.webChromeClient = WebChromeClient()
        // ローカルHTMLを読み込む
        web.loadUrl("file:///android_asset/map.html")
    }

    private fun showMarker(lat: Double, lng: Double, name: String) {
        if (pageLoaded) evaluateShowMarker(lat, lng, name) else pendingMarker = Triple(lat, lng, name)
    }

    private fun evaluateShowMarker(lat: Double, lng: Double, name: String) {
        // Leaflet 側の関数を呼ぶ
        web.evaluateJavascript("showMarker($lat, $lng, ${toJsString(name)});", null)
    }

    private fun toJsString(s: String): String =
        "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'"

    /** 住所→座標（別スレッド） */
    private fun geocodeAsync(address: String, cb: (Pair<Double, Double>?) -> Unit) {
        if (address.isBlank() || !Geocoder.isPresent()) {
            cb(null)
            return
        }
        Thread {
            val res = try {
                @Suppress("DEPRECATION")
                Geocoder(this, Locale.JAPAN).getFromLocationName(address, 1)
                    ?.firstOrNull()
                    ?.let { it.latitude to it.longitude }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            runOnUiThread { cb(res) }
        }.start()
    }

    // --- 緯度経度の簡易キャッシュ ---
    private fun prefs() = getSharedPreferences("geo_cache", Context.MODE_PRIVATE)
    private fun loadCachedLatLng(address: String): Pair<Double, Double>? {
        val s = prefs().getString(address, null) ?: return null
        val parts = s.split(",")
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        return lat to lng
    }
    private fun saveCachedLatLng(address: String, lat: Double, lng: Double) {
        prefs().edit().putString(address, "$lat,$lng").apply()
    }
}

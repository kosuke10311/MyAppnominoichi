package com.example.myappnominoichi

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import android.location.Geocoder
import android.content.Context

class StoreMapActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private lateinit var titleView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_map)

        web = findViewById(R.id.webMap)
        titleView = findViewById(R.id.tvTitle)

        val name = intent.getStringExtra("store_name") ?: "店舗"
        val address = intent.getStringExtra("store_address") ?: ""

        title = name
        titleView.text = "📍 $name"

        setupWebView()

        // まずキャッシュをチェック → なければジオコーディング
        val (lat, lng) = loadCachedLatLng(this, address) ?: run {
            geocodeAsync(address) { result ->
                if (result == null) {
                    Toast.makeText(this, "住所の位置を取得できませんでした", Toast.LENGTH_SHORT).show()
                } else {
                    saveCachedLatLng(this, address, result.first, result.second)
                    showMarker(result.first, result.second, name)
                }
            }
            // 非同期なので一旦 return
            return
        }

        // キャッシュ命中時は即表示
        showMarker(lat, lng, name)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        web.webViewClient = object : WebViewClient() {}
        web.webChromeClient = WebChromeClient()
        // ローカルHTMLを読み込む
        web.loadUrl("file:///android_asset/map.html")
    }

    private fun showMarker(lat: Double, lng: Double, name: String) {
        // Leaflet 側の関数を呼ぶ
        val js = "javascript:showMarker($lat,$lng, ${toJsString(name)});"
        web.post { web.evaluateJavascript(js, null) }
    }

    private fun toJsString(s: String): String =
        "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'"

    /** 住所→座標（別スレッド） */
    private fun geocodeAsync(address: String, cb: (Pair<Double,Double>?) -> Unit) {
        Thread {
            try {
                val geocoder = Geocoder(this, Locale.JAPAN)
                val list = geocoder.getFromLocationName(address, 1)
                val res = if (list != null && list.isNotEmpty()) {
                    Pair(list[0].latitude, list[0].longitude)
                } else null
                runOnUiThread { cb(res) }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { cb(null) }
            }
        }.start()
    }

    // --- 緯度経度の簡易キャッシュ ---
    private fun prefs() = getSharedPreferences("geo_cache", Context.MODE_PRIVATE)
    private fun loadCachedLatLng(ctx: Context, address: String): Pair<Double,Double>? {
        val p = prefs()
        val s = p.getString(address, null) ?: return null
        val parts = s.split(",")
        return if (parts.size == 2) parts[0].toDoubleOrNull()?.let { lat ->
            parts[1].toDoubleOrNull()?.let { lng -> lat to lng }
        } else null
    }
    private fun saveCachedLatLng(ctx: Context, address: String, lat: Double, lng: Double) {
        prefs().edit().putString(address, "$lat,$lng").apply()
    }
}

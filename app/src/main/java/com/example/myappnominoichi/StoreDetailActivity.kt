package com.example.myappnominoichi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StoreDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STORE = "extra_store"
    }

    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_detail)

        // ---- Store を安全に受け取る ----
        val data = intent.getSerializableExtra(EXTRA_STORE)
        if (data !is Store) {
            finish()
            return
        }
        store = data

        // ---- レイアウト上の View ----
        val textName      = findViewById<TextView>(R.id.textStoreName)
        val textCategory  = findViewById<TextView>(R.id.textStoreCategory)
        val textAddress   = findViewById<TextView>(R.id.textStoreAddress)
        val textOpenHours = findViewById<TextView>(R.id.textStoreOpenHours)
        val textMenu      = findViewById<TextView>(R.id.textStoreMenu)
        val buttonMap     = findViewById<Button>(R.id.buttonOpenMap)
        val imagePhoto = findViewById<ImageView>(R.id.imageStorePhoto)


        // ---- 表示 ----
        textName.text = store.name
        textCategory.text = store.category
        textAddress.text = store.address
        textOpenHours.text =
            if (store.openHours.isNotBlank()) "営業時間：${store.openHours}" else ""
        textMenu.text =
            if (store.menu.isNotBlank()) "おすすめ：${store.menu}" else ""
        // ★ 写真を表示（stores.json の "photo": "store_i01" を使う）
        if (store.photo.isNotBlank()) {
            // photo には拡張子なしのリソース名が入っている想定
            val resId = resources.getIdentifier(store.photo, "drawable", packageName)
            if (resId != 0) {
                imagePhoto.setImageResource(resId)
            }
        }

        buttonMap.setOnClickListener { openMap() }
    }

    private fun openMap() {
        val hasLatLng = (store.lat != null && store.lng != null)

        val uri = if (hasLatLng) {
            Uri.parse("geo:${store.lat},${store.lng}?q=${store.lat},${store.lng}(${Uri.encode(store.name)})")
        } else {
            Uri.parse("geo:0,0?q=${Uri.encode(store.address)}")
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        try {
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        } catch (e: ActivityNotFoundException) {
            val browserUri = Uri.parse(
                "https://www.google.com/maps/search/?api=1&query=${Uri.encode(store.address)}"
            )
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }
}

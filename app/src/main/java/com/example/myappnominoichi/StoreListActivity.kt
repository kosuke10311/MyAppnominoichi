package com.example.myappnominoichi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.Normalizer
import java.util.Locale
import android.app.AlertDialog
import android.net.Uri

class StoreListActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StoreAdapter
    private lateinit var stampStore: StampStore

    private val CATEGORY_FOOD = "居酒屋・お食事処"
    private val CATEGORY_BAR  = "スナック・バー・ラウンジ"

    private var allStores: List<Store> = emptyList()
    private var currentCategory = CATEGORY_FOOD
    private lateinit var toolbar: MaterialToolbar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_list)

        stampStore = StampStore(this)

        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.storeRecyclerView)

        tabLayout.addTab(tabLayout.newTab().setText(CATEGORY_FOOD))
        tabLayout.addTab(tabLayout.newTab().setText(CATEGORY_BAR))
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = StoreAdapter { store ->
            showStoreActions(store)
        }

        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        )

        // assets/stores.json を読み込む
        allStores = loadStoresFromAssets()

        applyFilter()
        refreshStampUi()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentCategory = tab.text?.toString() ?: CATEGORY_FOOD
                applyFilter()
                refreshStampUi()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {
                currentCategory = tab.text?.toString() ?: CATEGORY_FOOD
                applyFilter()
                refreshStampUi()
            }
        })

        // FAB → 既存のスキャナ(MainActivity)を起動
        findViewById<FloatingActionButton>(R.id.fabScan)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // MainActivity から渡されたスキャン結果があれば即処理
        intent.getStringExtra("scanned_qr")?.let { scanned -> handleQr(scanned) }
        updateStampTitle()
    }

    private fun applyFilter() {
        val filtered = allStores.filter { it.category == currentCategory }
        adapter.submit(filtered)
    }


    private fun loadStoresFromAssets(): List<Store> {
        return try {
            val ins = assets.open("stores.json")
            val text = BufferedReader(InputStreamReader(ins)).use { it.readText() }
            val arr = JSONArray(text)
            val list = ArrayList<Store>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(parseStore(i, o))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseStore(index: Int, obj: JSONObject): Store {
        val id = obj.optString("id").ifBlank { "AUTO_${index + 1}" }
        val name = obj.optString("name", "店舗${index + 1}")
        val rawCategory = obj.optString("category", "")
        val address = obj.optString("address", "")
        val qrCode = obj.optString("qrCode","")

        // ★ 追加
        val openHours = obj.optString("openHours", "")
        val menu = obj.optString("menu", "")
        val photo = obj.optString("photo", "")

        // lat/lng は今 JSON にないので将来用
        val lat: Double? = if (obj.has("lat")) obj.optDouble("lat") else null
        val lng: Double? = if (obj.has("lng")) obj.optDouble("lng") else null

        val normalized = normalizeCategory(rawCategory)

        return Store(
            id = id,
            name = name,
            category = normalized,
            address = address,
            qrCode = qrCode,
            openHours = openHours,
            menu = menu,
            photo = photo,
            lat = lat,
            lng = lng
        )
    }


    private fun normalizeCategory(src: String): String {
        val s = src.lowercase(Locale.JAPAN)
        val foodKeys = listOf("居酒屋", "お食事", "食堂", "定食", "焼鳥", "やきとり", "寿司", "ラーメン", "うどん", "そば", "カレー", "洋食", "和食", "中華", "食事処")
        if (foodKeys.any { s.contains(it) }) return CATEGORY_FOOD
        val barKeys = listOf("バー", "bar", "スナック", "ラウンジ", "パブ", "カラオケ", "night")
        if (barKeys.any { s.contains(it) }) return CATEGORY_BAR
        return CATEGORY_FOOD
    }

    // === QRハンドラ（qrCode一致 / ID一致 / プレフィックス対応） ===
    fun handleQr(rawText: String) {
        val scanned = normalizeCode(rawText)
        // MN:STORE: のような自前プレフィックスにも対応
        val idFromPrefix = scanned.removePrefix("MN:STORE:")

        val byQr = allStores.firstOrNull { normalizeCode(it.qrCode) == scanned }
        val byId = allStores.firstOrNull {
            val nid = normalizeCode(it.id)
            nid == scanned || nid == idFromPrefix
        }

        val store = byQr ?: byId
        if (store == null) {
            toast("店舗QRが見つかりません")
            return
        }

        val already = stampStore.has(store.id)
        stampStore.add(store.id)

        applyFilter()
        refreshStampUi()

        toast(if (already) "『${store.name}』はスタンプ済み" else "『${store.name}』のスタンプを獲得！")
        updateStampTitle()
    }

    private fun normalizeCode(src: String?): String {
        if (src == null) return ""
        var s = src
        s = runCatching { java.net.URLDecoder.decode(s, "UTF-8") }.getOrNull() ?: s
        s = Normalizer.normalize(s, Normalizer.Form.NFKC)
        s = s.replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
        s = s.replace(Regex("[\\u2010-\\u2015\\u2212]"), "-")
        s = s.replace("\u3000", " ").trim()
        s = s.replace(" ", "")
        s = s.uppercase(Locale.US)
        return s
    }
    private fun showStoreActions(store: Store) {
        val options = arrayOf("地図で見る", "店舗詳細を見る")

        AlertDialog.Builder(this)
            .setTitle(store.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // 地図で見る
                        val uri = Uri.parse("geo:0,0?q=${Uri.encode(store.address)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (mapIntent.resolveActivity(packageManager) != null) {
                            startActivity(mapIntent)
                        } else {
                            Toast.makeText(this, "Google Maps アプリが見つかりません", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> { // ★ 店舗詳細へ：Store をそのまま渡す
                        val intent = Intent(this, StoreDetailActivity::class.java).apply {
                            putExtra(StoreDetailActivity.EXTRA_STORE, store)
                        }
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    /* ---------------------------
   スタンプ数をタイトルに反映
   --------------------------- */
    private fun updateStampTitle() {
        val stampedCount = stampStore.getAll().size
        val baseTitle = "参加店舗（$currentCategory）"

        val newTitle = if (stampedCount >= 3) {
            "$baseTitle   🎉 スタンプ：$stampedCount 個"
        } else {
            "$baseTitle   スタンプ：$stampedCount 個"
        }

        toolbar.title = newTitle
    }

    // スタンプ関連のUI（一覧とタイトル）をまとめて更新
    private fun refreshStampUi() {
        adapter.updateStampedIds(stampStore.getAll())
        updateStampTitle()
    }

}

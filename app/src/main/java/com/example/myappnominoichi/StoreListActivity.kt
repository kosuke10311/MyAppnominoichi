package com.example.myappnominoichi

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

class StoreListActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StoreAdapter
    private lateinit var stampStore: StampStore
    private lateinit var toolbar: MaterialToolbar

    private var allStores: List<Store> = emptyList()
    private var currentCategory = StampRules.CATEGORY_FOOD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_list)

        stampStore = StampStore(this)

        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.storeRecyclerView)

        StampRules.CATEGORIES.forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = StoreAdapter { store ->
            showStoreActions(store)
        }

        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        )

        // assets/stores.json を読み込む
        allStores = StoreRepository.getStores(this)

        applyFilter()
        refreshStampUi()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentCategory = StampRules.CATEGORIES.getOrElse(tab.position) { StampRules.CATEGORY_FOOD }
                applyFilter()
                refreshStampUi()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // FAB → スキャナ(MainActivity)を起動
        findViewById<FloatingActionButton>(R.id.fabScan)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // スキャン画面から戻った時にスタンプ表示を最新化
        refreshStampUi()
    }

    private fun applyFilter() {
        val filtered = allStores.filter { it.category == currentCategory }
        adapter.submit(filtered)
    }

    private fun showStoreActions(store: Store) {
        val options = arrayOf(
            getString(R.string.action_open_map),
            getString(R.string.action_store_detail)
        )

        AlertDialog.Builder(this)
            .setTitle(store.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(StoreMapActivity.newIntent(this, store))
                    1 -> {
                        val intent = Intent(this, StoreDetailActivity::class.java).apply {
                            putExtra(StoreDetailActivity.EXTRA_STORE, store)
                        }
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    /* ---------------------------
   スタンプ数をタイトルに反映
   --------------------------- */
    private fun updateStampTitle() {
        val stampedCount = stampStore.count()
        val titleRes = if (stampedCount >= StampRules.MAX_STAMPS) {
            R.string.store_list_title_completed
        } else {
            R.string.store_list_title
        }
        toolbar.title = getString(titleRes, currentCategory, stampedCount, StampRules.MAX_STAMPS)
    }

    // スタンプ関連のUI（一覧とタイトル）をまとめて更新
    private fun refreshStampUi() {
        adapter.updateStampedIds(stampStore.getAll())
        updateStampTitle()
    }

}

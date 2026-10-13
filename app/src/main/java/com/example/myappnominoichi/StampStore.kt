package com.example.myappnominoichi

import android.content.Context

/** 獲得済みスタンプ（店舗ID）の保存先 */
class StampStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("stamps", Context.MODE_PRIVATE)
    private val KEY = "stamped_ids"
    private val KEY_MIGRATED = "stamped_ids_migrated_v2"

    init {
        migrateIfNeeded()
    }

    fun getAll(): Set<String> = prefs.getStringSet(KEY, emptySet()) ?: emptySet()
    fun count(): Int = getAll().size
    fun has(id: String): Boolean = getAll().contains(id)
    fun add(id: String) {
        val set = getAll().toMutableSet()
        set.add(id)
        prefs.edit().putStringSet(KEY, set).apply()
    }

    /** 旧バージョンは生のQR文字列を保存していたため、店舗IDへ変換する（初回のみ） */
    private fun migrateIfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        val stores = StoreRepository.getStores(context)
        if (stores.isEmpty()) return  // 店舗データが読めない時は消さずに次回へ持ち越す
        val migrated = StampRules.migrateStampedValues(getAll(), stores)
        prefs.edit()
            .putStringSet(KEY, migrated)
            .putBoolean(KEY_MIGRATED, true)
            .apply()
    }
}

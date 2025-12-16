package com.example.myappnominoichi

import android.content.Context

class StampStore(context: Context) {
    private val prefs = context.getSharedPreferences("stamps", Context.MODE_PRIVATE)
    private val KEY = "stamped_ids"

    fun getAll(): Set<String> = prefs.getStringSet(KEY, emptySet()) ?: emptySet()
    fun has(id: String): Boolean = getAll().contains(id)
    fun add(id: String) {
        val set = getAll().toMutableSet()
        set.add(id)
        prefs.edit().putStringSet(KEY, set).apply()
    }
}

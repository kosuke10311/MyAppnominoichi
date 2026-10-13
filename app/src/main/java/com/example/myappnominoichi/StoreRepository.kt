package com.example.myappnominoichi

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * assets/stores.json から参加店舗を読み込む（一度読んだらメモリにキャッシュ）
 */
object StoreRepository {

    private const val TAG = "StoreRepository"
    private const val FILE_NAME = "stores.json"

    @Volatile
    private var cache: List<Store>? = null

    fun getStores(context: Context): List<Store> {
        cache?.let { return it }
        return synchronized(this) {
            cache ?: load(context.applicationContext).also { cache = it }
        }
    }

    private fun load(context: Context): List<Store> {
        return try {
            val text = context.assets.open(FILE_NAME).bufferedReader().use { it.readText() }
            val arr = JSONArray(text)
            (0 until arr.length()).map { i -> parseStore(i, arr.getJSONObject(i)) }
        } catch (e: Exception) {
            Log.e(TAG, "$FILE_NAME の読み込みに失敗しました", e)
            emptyList()
        }
    }

    private fun parseStore(index: Int, obj: JSONObject): Store {
        val lat: Double? = if (obj.has("lat")) obj.optDouble("lat").takeUnless { it.isNaN() } else null
        val lng: Double? = if (obj.has("lng")) obj.optDouble("lng").takeUnless { it.isNaN() } else null

        return Store(
            id = obj.optString("id").ifBlank { "AUTO_${index + 1}" },
            name = obj.optString("name", "店舗${index + 1}"),
            category = StampRules.normalizeCategory(obj.optString("category", "")),
            address = obj.optString("address", ""),
            qrCode = obj.optString("qrCode", ""),
            openHours = obj.optString("openHours", ""),
            menu = obj.optString("menu", ""),
            photo = obj.optString("photo", ""),
            lat = lat,
            lng = lng
        )
    }
}

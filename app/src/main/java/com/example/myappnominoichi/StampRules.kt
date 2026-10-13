package com.example.myappnominoichi

import java.text.Normalizer
import java.util.Locale

/**
 * スタンプラリーの判定ロジック（Android 非依存なので単体テスト可能）
 */
object StampRules {

    /** 特典達成に必要なスタンプ数 */
    const val MAX_STAMPS = 3

    const val CATEGORY_FOOD = "居酒屋・お食事処"
    const val CATEGORY_BAR = "スナック・バー・ラウンジ"
    val CATEGORIES = listOf(CATEGORY_FOOD, CATEGORY_BAR)

    /** MN:STORE:I01 のような自前プレフィックス */
    private const val STORE_PREFIX = "MN:STORE:"

    private val FOOD_KEYS = listOf(
        "居酒屋", "お食事", "食堂", "定食", "焼鳥", "やきとり", "寿司", "ラーメン",
        "うどん", "そば", "カレー", "洋食", "和食", "中華", "食事処"
    )
    private val BAR_KEYS = listOf("バー", "bar", "スナック", "ラウンジ", "パブ", "カラオケ", "night")

    /** JSON のカテゴリ表記を2分類に正規化（判別できなければお食事処扱い） */
    fun normalizeCategory(src: String): String {
        val s = src.lowercase(Locale.JAPAN)
        if (FOOD_KEYS.any { s.contains(it) }) return CATEGORY_FOOD
        if (BAR_KEYS.any { s.contains(it) }) return CATEGORY_BAR
        return CATEGORY_FOOD
    }

    /** QR文字列の表記ゆれ（全角・空白・ダッシュ類・大文字小文字・URLエンコード）を吸収 */
    fun normalizeCode(src: String?): String {
        if (src == null) return ""
        var s: String = src
        s = runCatching { java.net.URLDecoder.decode(s, "UTF-8") }.getOrNull() ?: s
        s = Normalizer.normalize(s, Normalizer.Form.NFKC)
        s = s.replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
        s = s.replace(Regex("[\\u2010-\\u2015\\u2212]"), "-")
        s = s.replace("　", " ").trim()
        s = s.replace(" ", "")
        s = s.uppercase(Locale.US)
        return s
    }

    /** 読み取ったQRに対応する店舗を返す（qrCode一致 → ID一致 / プレフィックス付きID）。該当なしは null */
    fun findStoreByQr(stores: List<Store>, rawText: String?): Store? {
        val scanned = normalizeCode(rawText)
        if (scanned.isEmpty()) return null
        val idFromPrefix = scanned.removePrefix(STORE_PREFIX)

        return stores.firstOrNull { it.qrCode.isNotBlank() && normalizeCode(it.qrCode) == scanned }
            ?: stores.firstOrNull {
                val nid = normalizeCode(it.id)
                nid == scanned || nid == idFromPrefix
            }
    }

    /**
     * 旧バージョンで保存された値（生のQR文字列など）を店舗IDに変換する。
     * どの店舗にも一致しない値は不正なスタンプとして破棄する。
     */
    fun migrateStampedValues(saved: Set<String>, stores: List<Store>): Set<String> =
        saved.mapNotNull { value -> findStoreByQr(stores, value)?.id }.toSet()
}

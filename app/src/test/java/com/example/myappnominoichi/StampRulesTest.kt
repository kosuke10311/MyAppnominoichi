package com.example.myappnominoichi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StampRulesTest {

    private val stores = listOf(
        Store(id = "I01", name = "和風ダイニング遊食三昧", category = StampRules.CATEGORY_FOOD, address = "", qrCode = "NOMI-I01-F5DW"),
        Store(id = "S01", name = "スナックA", category = StampRules.CATEGORY_BAR, address = "", qrCode = "NOMI-S01-AAAA"),
        Store(id = "I99", name = "QR未設定店", category = StampRules.CATEGORY_FOOD, address = "", qrCode = "")
    )

    // ---- normalizeCode ----

    @Test
    fun normalizeCode_absorbsWidthCaseSpacesAndDashes() {
        assertEquals("NOMI-I01-F5DW", StampRules.normalizeCode(" ｎｏｍｉ－ｉ０１\u2212 f5dw "))
        assertEquals("NOMI-I01-F5DW", StampRules.normalizeCode("nomi\u2010i01\u2014f5dw"))
        assertEquals("NOMI-I01-F5DW", StampRules.normalizeCode("NOMI-I01\u200B-F5DW"))
    }

    @Test
    fun normalizeCode_decodesUrlEncoding() {
        assertEquals("MN:STORE:I01", StampRules.normalizeCode("MN%3ASTORE%3AI01"))
    }

    @Test
    fun normalizeCode_nullOrBlankIsEmpty() {
        assertEquals("", StampRules.normalizeCode(null))
        assertEquals("", StampRules.normalizeCode(" \u3000 "))
    }

    // ---- findStoreByQr ----

    @Test
    fun findStoreByQr_matchesQrCode() {
        assertEquals("I01", StampRules.findStoreByQr(stores, "NOMI-I01-F5DW")?.id)
        assertEquals("S01", StampRules.findStoreByQr(stores, "nomi-s01-aaaa")?.id)
    }

    @Test
    fun findStoreByQr_matchesIdAndPrefixedId() {
        assertEquals("I01", StampRules.findStoreByQr(stores, "i01")?.id)
        assertEquals("I99", StampRules.findStoreByQr(stores, "MN:STORE:I99")?.id)
    }

    @Test
    fun findStoreByQr_rejectsUnknownCodes() {
        assertNull(StampRules.findStoreByQr(stores, "https://example.com/"))
        assertNull(StampRules.findStoreByQr(stores, "NOMI-I01-XXXX"))
        assertNull(StampRules.findStoreByQr(stores, ""))
        assertNull(StampRules.findStoreByQr(stores, null))
    }

    @Test
    fun findStoreByQr_blankQrCodeDoesNotMatchBlankInput() {
        assertNull(StampRules.findStoreByQr(stores, "   "))
    }

    // ---- migrateStampedValues ----

    @Test
    fun migrateStampedValues_convertsQrToIdAndDropsInvalid() {
        val saved = setOf("NOMI-I01-F5DW", "S01", "https://example.com/", "I01")
        assertEquals(setOf("I01", "S01"), StampRules.migrateStampedValues(saved, stores))
    }

    // ---- normalizeCategory ----

    @Test
    fun normalizeCategory_mapsToTwoCategories() {
        assertEquals(StampRules.CATEGORY_FOOD, StampRules.normalizeCategory("居酒屋・お食事処"))
        assertEquals(StampRules.CATEGORY_BAR, StampRules.normalizeCategory("スナック・バー・ラウンジ"))
        assertEquals(StampRules.CATEGORY_BAR, StampRules.normalizeCategory("Night BAR"))
        assertEquals(StampRules.CATEGORY_FOOD, StampRules.normalizeCategory("不明"))
    }
}

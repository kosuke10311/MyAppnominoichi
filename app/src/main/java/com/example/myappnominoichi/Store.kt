package com.example.myappnominoichi

import java.io.Serializable

data class Store(
    val id: String,        // JSONに無ければ自動採番
    val name: String,
    val category: String,  // ← 正規化された2分類で格納
    val address: String,
    val qrCode: String,
    val openHours:String = "",
    val menu: String = "",
    val photo:String = "",
    val lat:Double?=null,
    val lng:Double?=null
) : Serializable

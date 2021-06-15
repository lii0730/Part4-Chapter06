package com.example.aop_part4_chapter06.response.AirKorea.AirQuality


import com.google.gson.annotations.SerializedName

data class Body(
    @SerializedName("items")
    val items: List<Item>?,
    @SerializedName("numOfRows")
    val numOfRows: Int?,
    @SerializedName("pageNo")
    val pageNo: Int?,
    @SerializedName("totalCount")
    val totalCount: Int?
)
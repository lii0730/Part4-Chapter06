package com.example.aop_part4_chapter06.response.AirKorea.AirQuality


import com.google.gson.annotations.SerializedName

data class Response(
    @SerializedName("body")
    val body: Body?,
    @SerializedName("header")
    val header: Header?
)
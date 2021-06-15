package com.example.aop_part4_chapter06.response.AirKorea.AirQuality


import com.google.gson.annotations.SerializedName

data class AirQualityResponse(
    @SerializedName("response")
    val response: Response?
)
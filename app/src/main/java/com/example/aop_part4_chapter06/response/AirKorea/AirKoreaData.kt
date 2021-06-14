package com.example.aop_part4_chapter06.response.AirKorea

import com.google.gson.annotations.SerializedName

data class AirKoreaData(
	@SerializedName("tm") val tm : Double?,
	@SerializedName("addr") val addr : String?,
	@SerializedName("stationName") val stationName : String?
)

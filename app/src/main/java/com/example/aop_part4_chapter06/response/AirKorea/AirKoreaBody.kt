package com.example.aop_part4_chapter06.response.AirKorea

import com.google.gson.annotations.SerializedName

data class AirKoreaBody(
	@SerializedName("totalCount") val totalCount : Int?,
	@SerializedName("items") val items : List<AirKoreaData>?,
	@SerializedName("pageNo") val pageNo : Int?,
	@SerializedName("numOfRows") val numOfRows : Int?
)

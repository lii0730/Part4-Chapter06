package com.example.aop_part4_chapter06.response.AirKorea

import com.google.gson.annotations.SerializedName

data class AirKoreaResponse(
	@SerializedName("body") val body : AirKoreaBody,
	@SerializedName("header") val header: AirKoreaHeader
)

package com.example.aop_part4_chapter06.response.AirKorea

import com.google.gson.annotations.SerializedName

data class ResponseDTO(
	@SerializedName("response") val response: AirKoreaResponse
)

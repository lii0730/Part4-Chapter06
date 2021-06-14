package com.example.aop_part4_chapter06.response.Kakao

import com.google.gson.annotations.SerializedName

data class Documents(
	@SerializedName("x") val x : Double,
	@SerializedName("y") val y : Double
)

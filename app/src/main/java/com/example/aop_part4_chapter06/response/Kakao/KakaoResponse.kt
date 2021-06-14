package com.example.aop_part4_chapter06.response.Kakao

import com.google.gson.annotations.SerializedName

data class KakaoResponse(
	@SerializedName("meta") val meta: Meta,
	@SerializedName("documents")val documents : List<Documents>
)

package com.example.aop_part4_chapter06.URL

import com.example.aop_part4_chapter06.BuildConfig

object URL {

	const val AIR_BASE_URL = "http://apis.data.go.kr/"
	const val AIR_REQUEST_URL =
		"B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList" + "?returnType=json" + "&serviceKey=${BuildConfig.AIR_KOREA_API_KEY}"
	const val AIR_QUALITY_REQUEST_URL =
		"B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty" + "?returnType=json" + "&dataTerm=DAILY" + "&serviceKey=${BuildConfig.AIR_KOREA_API_KEY}" + "&ver=1.0"

	const val KAKAO_BASE_URL = "https://dapi.kakao.com/"
	const val KAKAO_REQUEST_URL = "v2/local/geo/transcoord.json"

}
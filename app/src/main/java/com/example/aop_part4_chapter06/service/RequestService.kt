package com.example.aop_part4_chapter06.service

import com.example.aop_part4_chapter06.BuildConfig
import com.example.aop_part4_chapter06.URL.URL.AIR_REQUEST_URL
import com.example.aop_part4_chapter06.URL.URL.KAKAO_REQUEST_URL
import com.example.aop_part4_chapter06.response.AirKorea.ResponseDTO
import com.example.aop_part4_chapter06.response.Kakao.KakaoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface RequestService {

	@Headers("Authorization: KakaoAK ${BuildConfig.KAKAO_API_KEY}")
	@GET(KAKAO_REQUEST_URL)
	suspend fun getTMSystemLocation(
		@Query("x") longitude: Double,
		@Query("y") latitude: Double,
		@Query("output_coord") output_coord : String
	): KakaoResponse


	@GET(AIR_REQUEST_URL)
	suspend fun getStationName(
		@Query("returnType") returnType: String,
		@Query("serviceKey") serviceKey : String,
		@Query("tmX") tmX : Double,
		@Query("tmY") tmY : Double
	) : ResponseDTO
}
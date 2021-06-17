package com.example.aop_part4_chapter06

import com.example.aop_part4_chapter06.URL.URL.AIR_BASE_URL
import com.example.aop_part4_chapter06.URL.URL.KAKAO_BASE_URL
import com.example.aop_part4_chapter06.service.AirKoreaApiService
import com.example.aop_part4_chapter06.service.KakaoApiService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

	val gson = GsonBuilder().setLenient().create()

	val kakaoAPI by lazy {
		getKaKaoRetrofit().create(KakaoApiService::class.java)
	}

	val airKoreaAPI by lazy {
		getAirKoreaRetrofit().create(AirKoreaApiService::class.java)
	}

	fun getAirKoreaRetrofit() : Retrofit = Retrofit.Builder()
			.baseUrl(AIR_BASE_URL)
			.client(buildHttpClient())
			.addConverterFactory(GsonConverterFactory.create(gson))
			.build()


	fun getKaKaoRetrofit() : Retrofit = Retrofit.Builder()
			.baseUrl(KAKAO_BASE_URL)
			.client(buildHttpClient())
			.addConverterFactory(GsonConverterFactory.create(gson))
			.build()

	private fun buildHttpClient() : OkHttpClient =
		OkHttpClient.Builder()
			.addInterceptor(
				HttpLoggingInterceptor().apply {
					level = if(BuildConfig.DEBUG) {
						HttpLoggingInterceptor.Level.BODY
					} else {
						HttpLoggingInterceptor.Level.NONE
					}
				}
			)
			.build()
}
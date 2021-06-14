package com.example.aop_part4_chapter06

import com.example.aop_part4_chapter06.URL.URL.AIR_BASE_URL
import com.example.aop_part4_chapter06.URL.URL.KAKAO_BASE_URL
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

	val gson = GsonBuilder().setLenient().create()

	fun getAirKoreaRetrofit() : Retrofit {
		return Retrofit.Builder()
			.baseUrl(AIR_BASE_URL)
			.client(OkHttpClient())
			.addConverterFactory(GsonConverterFactory.create(gson))
			.build()
	}


	fun getKaKaoRetrofit() : Retrofit {
		return Retrofit.Builder()
			.baseUrl(KAKAO_BASE_URL)
			.client(OkHttpClient())
			.addConverterFactory(GsonConverterFactory.create(gson))
			.build()
	}
}
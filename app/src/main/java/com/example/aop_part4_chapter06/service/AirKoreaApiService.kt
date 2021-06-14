package com.example.aop_part4_chapter06.service

import com.example.aop_part4_chapter06.URL.URL
import com.example.aop_part4_chapter06.response.AirKorea.ResponseDTO
import retrofit2.http.GET
import retrofit2.http.Query

interface AirKoreaApiService {
    @GET(URL.AIR_REQUEST_URL)
    suspend fun getStationName(
        @Query("tmX") tmX : Double,
        @Query("tmY") tmY : Double
    ) : ResponseDTO
}
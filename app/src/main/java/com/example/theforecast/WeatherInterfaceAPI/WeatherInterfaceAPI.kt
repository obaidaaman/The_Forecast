package com.example.theforecast.WeatherInterfaceAPI

import com.example.weatherappkotlin.WeatherData.WeatherData
import retrofit2.Call
import retrofit2.Response

import retrofit2.http.GET

import retrofit2.http.Query


interface WeatherInterfaceAPI {

@GET("weather?")
 suspend fun getWeatherData(
   @Query("lat") latitude : Double,
   @Query("lon") longitude : Double,
   @Query("appid") APIKEY :String,
   @Query("units") units : String
  ) : Response<WeatherData>

}
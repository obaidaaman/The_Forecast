package com.example.theforecast.RetrofitUtil

import com.example.theforecast.WeatherInterfaceAPI.WeatherInterfaceAPI
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    val api : WeatherInterfaceAPI by lazy {
        Retrofit.Builder()
            .baseUrl(ApiUtil.baseURl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherInterfaceAPI::class.java)
    }
}
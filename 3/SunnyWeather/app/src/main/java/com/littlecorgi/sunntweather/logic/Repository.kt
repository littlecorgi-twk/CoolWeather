package com.littlecorgi.sunntweather.logic

import android.util.Log
import androidx.lifecycle.liveData
import com.littlecorgi.sunntweather.SunnyWeatherApplication
import com.littlecorgi.sunntweather.logic.dao.PlaceDao
import com.littlecorgi.sunntweather.logic.model.Place
import com.littlecorgi.sunntweather.logic.model.Weather
import com.littlecorgi.sunntweather.logic.network.SunnyWeatherNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext

object Repository {

    fun searchPlaces(query: String) = fire(Dispatchers.IO) {
        val placeResponse = SunnyWeatherNetwork.searchPlaces(query)
        if (placeResponse.status == "ok") {
            val places = placeResponse.places
            Result.success(places)
        } else {
            Result.failure(RuntimeException("response status is ${placeResponse.status}"))
        }
    }


    fun refreshWeather(lng: String, lat: String) = fire(Dispatchers.IO) {
        coroutineScope {
            val deferredRealtime = async {
                SunnyWeatherNetwork.getRealtimeWeather(lng, lat)
            }
            val deferredDaily = async {
                SunnyWeatherNetwork.getDailyWeather(lng, lat)
            }
            Log.d(
                "SunnyWeather",
                "https://api.caiyunapp.com/v2.5/${SunnyWeatherApplication.TOKEN}/${lng},${lat}/realtime.json"
            )
            Log.d(
                "SunnyWeather",
                "https://api.caiyunapp.com/v2.5/${SunnyWeatherApplication.TOKEN}/${lng},${lat}/daily.json"
            )
            val realtimeResponse = deferredRealtime.await()
            val dailyResponse = deferredDaily.await()
            if (realtimeResponse.status == "ok" && dailyResponse.status == "ok") {
                val weather =
                    Weather(realtimeResponse.result.realtime, dailyResponse.result.daily)
                Result.success(weather)
            } else {
                Result.failure(
                    RuntimeException(
                        "realtime response status is ${realtimeResponse.status}" +
                                "daily response status is ${dailyResponse.status}"
                    )
                )
            }
        }
    }

    private fun <T> fire(context: CoroutineContext, block: suspend () -> Result<T>) =
        liveData(context) {
            val result = try {
                block()
            } catch (e: Exception) {
                Result.failure<T>(e)
            }
            emit(result)
        }

    fun savePlace(place: Place) = PlaceDao.savePlace(place)

    fun getSavedPlace() = PlaceDao.getSavedPlace()

    fun isSavedPlace() = PlaceDao.isPlaceSaved()
}
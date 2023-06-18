package com.example.theforecast

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.theforecast.RetrofitUtil.RetrofitInstance
import com.example.theforecast.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    private lateinit var fusedLocationClient: FusedLocationProviderClient

    val WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather"

    private val API_kEY: String = "21c818974f94636755b5afd134c3514e"

    private var lastFetchedTemperature: Double by Delegates.notNull()

    private lateinit var sharedPreferences: SharedPreferences

    companion object {

        val LOCATION_PERMISSION_REQUEST_CODE = 100

        private const val SHARED_PREFS_KEY = "WeatherAppPrefs"

        private const val LAST_FETCHED_TEMP_KEY = "LastFetchedTemperature"

        private const val LAST_FETCHED_TIME_KEY = "LastFetchedTime"

    }

    private lateinit var locationManager: LocationManager

    private val networkChangeReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isInternetAvailable()){
                startLocationUpdates()
            }
        }

    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val format = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val currentDate = format.format(Date())
        binding.txtDate.text = currentDate

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        sharedPreferences = getSharedPreferences(SHARED_PREFS_KEY, Context.MODE_PRIVATE)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            startLocationUpdates()
        } else {
            // Permission is already granted, check if location is enabled
            checkLocationEnabled()
        }



        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

startLocationUpdates()

    }

    override fun onResume() {
        super.onResume()
        loadLastfetchedTemperature()


        if (isInternetAvailable()){
            startLocationUpdates()
        }
        else
        {
            loadLastfetchedTemperature()
        }

        registerNetworkChangeReceiver()
    }

    override fun onPause() {
        super.onPause()
        unregisterNetworkChangeReceiver()
    }



    private fun checkLocationEnabled() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Location is not enabled, ask the user to enable it
            AlertDialog.Builder(this)
                .setTitle("Enable Location")
                .setMessage("Location is required for this app. Please enable location.")
                .setPositiveButton("Settings") { _, _ ->
                    // Open location settings
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    // Handle cancellation if needed
                }
                .show()
        }
        else{
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val latitude = location.latitude
                val longitude = location.longitude
                // Use the latitude and longitude as needed
                // Example: Display them in TextViews or pass them to other methods
                val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                val address= geocoder.getFromLocation(latitude,longitude,1) as List<Address>
                if (address.isNotEmpty()){
                    val cityName: String= address[0].locality
                   binding.txtCity.text= cityName
                    if (isInternetAvailable()){
                        getCurrentWeather(latitude, longitude)
                    } else{
                        val formattedTemperature = String.format("%.2f", lastFetchedTemperature)
                        binding.txtTemp.text = formattedTemperature
                    }
                }else {
                    binding.txtCity.text = "City not available"
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}


        }

        // Request location updates
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0f,
                locationListener
            )
        }
    }

    private fun getCurrentWeather(lattitude: Double, longitude: Double) {
        GlobalScope.launch(Dispatchers.IO) {
            val call = try {

                RetrofitInstance.api.getWeatherData(lattitude, longitude, API_kEY, "metric")


            } catch (e: IOException) {

                return@launch
            } catch (e: HttpException) {
                Looper.prepare()
                Toast.makeText(applicationContext, "http error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

if (call.isSuccessful && call.body()!= null){
    val weatherData = call.body()

    lastFetchedTemperature= weatherData?.main?.temp!!

    val pressure = weatherData.main.pressure
    val humidity = weatherData.main.humidity
    val windSpeed = weatherData.wind
    val Visibility= weatherData.visibility


    val fetchTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        withContext(Dispatchers.Main){
            binding.txtTime.text = fetchTime
            binding.txtTemp.text = lastFetchedTemperature.toString()
            binding.txtPressureData.text= pressure.toString()
            binding.txtHumidData.text= humidity.toString()
            binding.txtWindData.text=windSpeed.toString()
            binding.txtVisiData.text=Visibility.toString()

            saveLastfetchedTemperature(lastFetchedTemperature, fetchTime,pressure,humidity,windSpeed.toString(), Visibility.toString())
        }

}else{
    withContext(Dispatchers.Main){
        Toast.makeText(applicationContext, "Failed to Fetch Weather", Toast.LENGTH_SHORT).show()
    }
}

        }
    }

    private fun saveLastfetchedTemperature(
        lastFetchedTemperature: Double,
        fetchTime: String,
        pressure: Int,
        humidity: Int,
        windspeed: String,
        Visibility: String
    ) {

        val editor= sharedPreferences.edit()

        editor.putFloat(LAST_FETCHED_TEMP_KEY, lastFetchedTemperature.toFloat())
        editor.putString(LAST_FETCHED_TIME_KEY, fetchTime)
        editor.putInt("Pressure", pressure)
        editor.putInt("Humidity",humidity)
        editor.putString("WindSpeed",windspeed)
        editor.putString("Visibility",Visibility)

        editor.apply()
    }

    private fun loadLastfetchedTemperature() {
        lastFetchedTemperature = sharedPreferences.getFloat(LAST_FETCHED_TEMP_KEY, 0f).toDouble()

        val lastFetchTime = sharedPreferences.getString(LAST_FETCHED_TIME_KEY,"")

        binding.txtTime.text= lastFetchTime
        binding.txtTemp.text= lastFetchedTemperature.toString()
        binding.txtPressureData.text= sharedPreferences.getInt("Pressure",0).toString()
        binding.txtHumidData.text=sharedPreferences.getInt("Humidity",0).toString()
        binding.txtWindData.text= sharedPreferences.getString("WindSpeed", "")
        binding.txtVisiData.text=sharedPreferences.getString("Visibility", "")
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission is granted, check if location is enabled
                checkLocationEnabled()
            } else {
                // Location permission is denied, handle accordingly
            }
        }
    }


    private fun  registerNetworkChangeReceiver(){
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkChangeReceiver,filter)
    }
    private fun unregisterNetworkChangeReceiver() {
        unregisterReceiver(networkChangeReceiver)
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val activeNetwork =
            connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }




    }

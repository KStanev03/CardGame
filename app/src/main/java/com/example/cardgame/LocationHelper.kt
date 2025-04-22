package com.example.cardgame

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val TAG = "ПомощникЛокация"

    suspend fun getCurrentLocation(): LocationResult {
        if (!hasLocationPermission()) {
            return LocationResult.PermissionRequired
        }

        Log.d(TAG, "Започва търсене на местоположение")

        val lastLocation = getLastKnownLocation()
        if (lastLocation != null) {
            Log.d(TAG, "Открито последно местоположение: ${lastLocation.latitude}, ${lastLocation.longitude}")
            val locationName = getLocationName(lastLocation.latitude, lastLocation.longitude)
            return LocationResult.Success(locationName)
        } else {
            Log.d(TAG, "Няма последно местоположение, заявка за ново")
        }

        return withTimeoutOrNull(15000) {
            requestLocationUpdates()
        } ?: LocationResult.Error("Времето за търсене на местоположение изтече. Опитайте отново или проверете настройките на устройството.")
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.d(TAG, "Липсват разрешения за локация")
            return null
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                try {
                    val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (location != null) return location
                } catch (se: SecurityException) {
                    Log.e(TAG, "Грешка при получаване на GPS местоположение", se)
                }
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                try {
                    val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (location != null) return location
                } catch (se: SecurityException) {
                    Log.e(TAG, "Грешка при получаване на мрежово местоположение", se)
                }
            }

            Log.d(TAG, "Няма налично последно местоположение")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Грешка при получаване на местоположение", e)
            return null
        }
    }

    private suspend fun requestLocationUpdates(): LocationResult =
        suspendCancellableCoroutine { cont ->
            if (!hasLocationPermission()) {
                cont.resume(LocationResult.PermissionRequired)
                return@suspendCancellableCoroutine
            }

            try {
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        try {
                            locationManager.removeUpdates(this)
                        } catch (se: SecurityException) {
                            Log.e(TAG, "Грешка при премахване на слушателя", se)
                        }

                        kotlinx.coroutines.GlobalScope.launch {
                            val locationName = getLocationName(location.latitude, location.longitude)
                            if (cont.isActive) {
                                cont.resume(LocationResult.Success(locationName))
                            }
                        }
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        if (cont.isActive) {
                            cont.resume(LocationResult.Error("Локационният доставчик е деактивиран"))
                        }
                    }
                }

                val providers = try {
                    locationManager.getProviders(true)
                } catch (se: SecurityException) {
                    Log.e(TAG, "Грешка при получаване на доставчици", se)
                    emptyList<String>()
                }

                if (providers.isEmpty()) {
                    cont.resume(LocationResult.Error("Няма налични доставчици на местоположение"))
                    return@suspendCancellableCoroutine
                }

                var понеЕдинУспешен = false
                for (provider in providers) {
                    try {
                        locationManager.requestLocationUpdates(
                            provider,
                            0,
                            0f,
                            locationListener,
                            Looper.getMainLooper()
                        )
                        понеЕдинУспешен = true
                    } catch (se: SecurityException) {
                        Log.e(TAG, "Грешка при заявка за актуализация от: $provider", se)
                    }
                }

                if (!понеЕдинУспешен) {
                    cont.resume(LocationResult.Error("Неуспешна заявка за местоположение - липсва разрешение"))
                    return@suspendCancellableCoroutine
                }

                cont.invokeOnCancellation {
                    try {
                        locationManager.removeUpdates(locationListener)
                    } catch (se: SecurityException) {
                        Log.e(TAG, "Грешка при спиране на слушателя при отказ", se)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Грешка при заявка за местоположение", e)
                if (cont.isActive) {
                    cont.resume(LocationResult.Error("Грешка при заявка за местоположение: ${e.message}"))
                }
            }
        }

    private suspend fun getLocationName(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Получаване на име за координати: $latitude, $longitude")
                val geocoder = Geocoder(context, Locale.getDefault())
                val coordinateString = "%.4f, %.4f".format(latitude, longitude)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var locationName = coordinateString
                    try {
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            if (addresses.isNotEmpty()) {
                                Log.d(TAG, "Намерен адрес: ${addresses[0]}")
                                locationName = formatAddress(addresses[0]) ?: coordinateString
                            } else {
                                Log.d(TAG, "Няма адрес за тези координати")
                            }
                        }
                        kotlinx.coroutines.delay(1000)
                    } catch (e: IOException) {
                        Log.e(TAG, "Грешка от Geocoder", e)
                    }

                    Log.d(TAG, "Връщане на име: $locationName")
                    return@withContext locationName
                } else {
                    @Suppress("DEPRECATION")
                    try {
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            Log.d(TAG, "Намерен адрес: ${addresses[0]}")
                            return@withContext formatAddress(addresses[0]) ?: coordinateString
                        } else {
                            Log.d(TAG, "Няма адрес за тези координати")
                            return@withContext coordinateString
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Грешка от Geocoder", e)
                        return@withContext "Локация: $coordinateString"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Грешка при извличане на име на локация", e)
                return@withContext "Грешка при локация"
            }
        }
    }

    private fun formatAddress(address: Address): String? {
        Log.d(
            TAG, "Компоненти на адреса: " +
                    "град=${address.locality}, " +
                    "подрайон=${address.subLocality}, " +
                    "община=${address.subAdminArea}, " +
                    "област=${address.adminArea}, " +
                    "държава=${address.countryName}"
        )

        val city = address.locality ?: address.subLocality ?: address.subAdminArea ?: address.adminArea ?: ""
        val country = address.countryName ?: ""

        return when {
            city.isNotEmpty() && country.isNotEmpty() -> "$city, $country"
            city.isNotEmpty() -> city
            country.isNotEmpty() -> country
            else -> {
                Log.d(TAG, "Няма достатъчно информация за адреса")
                null
            }
        }
    }
}

sealed class LocationResult {
    data class Success(val locationName: String) : LocationResult()
    data class Error(val message: String) : LocationResult()
    object PermissionRequired : LocationResult()
}

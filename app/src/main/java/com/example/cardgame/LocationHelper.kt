package com.example.cardgame

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class LocationHelper(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    suspend fun getCurrentLocation(): LocationResult {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return LocationResult.PermissionRequired
        }

        return try {
            // Get last known location from GPS provider
            val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location != null) {
                val locationName = getLocationName(location.latitude, location.longitude)
                LocationResult.Success(locationName)
            } else {
                LocationResult.Error("Could not get current location")
            }
        } catch (e: Exception) {
            LocationResult.Error("Error fetching location: ${e.message}")
        }
    }

    private suspend fun getLocationName(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                // Handle API level-specific geocoding
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var locationName = "Unknown Location"
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            locationName = formatAddress(addresses[0])
                        }
                    }
                    locationName
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        formatAddress(addresses[0])
                    } else {
                        "Unknown Location"
                    }
                }
            } catch (e: IOException) {
                "Location Unavailable"
            }
        }
    }

    private fun formatAddress(address: Address): String {
        // Format the address to show city and country
        val city = address.locality ?: address.subAdminArea ?: ""
        val country = address.countryName ?: ""

        return when {
            city.isNotEmpty() && country.isNotEmpty() -> "$city, $country"
            city.isNotEmpty() -> city
            country.isNotEmpty() -> country
            else -> "Unknown Location"
        }
    }
}

sealed class LocationResult {
    data class Success(val locationName: String) : LocationResult()
    data class Error(val message: String) : LocationResult()
    object PermissionRequired : LocationResult()
}
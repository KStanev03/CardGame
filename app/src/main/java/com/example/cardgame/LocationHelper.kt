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
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val TAG = "LocationHelper"

    suspend fun getCurrentLocation(): LocationResult {
        // Check permissions first
        if (!hasLocationPermission()) {
            return LocationResult.PermissionRequired
        }

        // First try to get last known location (faster)
        val lastLocation = getLastKnownLocation()
        if (lastLocation != null) {
            val locationName = getLocationName(lastLocation.latitude, lastLocation.longitude)
            return LocationResult.Success(locationName)
        }

        // If last known location is not available, request a new one with timeout
        return withTimeoutOrNull(30000) { // 30 seconds timeout
            requestLocationUpdates()
        } ?: LocationResult.Error("Location request timed out")
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
        // Always check permissions before accessing location services
        if (!hasLocationPermission()) {
            Log.d(TAG, "No location permissions")
            return null
        }

        try {
            // Try GPS provider first
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                try {
                    val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (location != null) return location
                } catch (se: SecurityException) {
                    Log.e(TAG, "Security exception getting GPS location", se)
                }
            }

            // Try Network provider next
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                try {
                    val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (location != null) return location
                } catch (se: SecurityException) {
                    Log.e(TAG, "Security exception getting Network location", se)
                }
            }

            // If we're here, no location was available
            Log.d(TAG, "No last known location available")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last known location", e)
            return null
        }
    }

    private suspend fun requestLocationUpdates(): LocationResult = suspendCancellableCoroutine { cont ->
        // Check permissions again
        if (!hasLocationPermission()) {
            cont.resume(LocationResult.PermissionRequired)
            return@suspendCancellableCoroutine
        }

        try {
            // Create location listener
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // Remove updates once we have a location
                    try {
                        locationManager.removeUpdates(this)
                    } catch (se: SecurityException) {
                        Log.e(TAG, "Security exception removing location updates", se)
                    }

                    // Launch coroutine to get location name
                    kotlinx.coroutines.GlobalScope.launch {
                        val locationName = getLocationName(location.latitude, location.longitude)
                        // Resume coroutine with result if not canceled
                        if (cont.isActive) {
                            cont.resume(LocationResult.Success(locationName))
                        }
                    }
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    if (cont.isActive) {
                        cont.resume(LocationResult.Error("Location provider disabled"))
                    }
                }
            }

            // Get available providers
            val providers = try {
                locationManager.getProviders(true)
            } catch (se: SecurityException) {
                Log.e(TAG, "Security exception getting providers", se)
                emptyList<String>()
            }

            if (providers.isEmpty()) {
                cont.resume(LocationResult.Error("No location providers available"))
                return@suspendCancellableCoroutine
            }

            // Request updates from available providers
            var atLeastOneProviderSucceeded = false
            for (provider in providers) {
                try {
                    locationManager.requestLocationUpdates(
                        provider,
                        0,
                        0f,
                        locationListener,
                        Looper.getMainLooper()
                    )
                    atLeastOneProviderSucceeded = true
                } catch (se: SecurityException) {
                    Log.e(TAG, "Security exception requesting updates for provider: $provider", se)
                }
            }

            if (!atLeastOneProviderSucceeded) {
                cont.resume(LocationResult.Error("Failed to request location updates - permission denied"))
                return@suspendCancellableCoroutine
            }

            // Make sure we clean up when coroutine is cancelled
            cont.invokeOnCancellation {
                try {
                    locationManager.removeUpdates(locationListener)
                } catch (se: SecurityException) {
                    Log.e(TAG, "Security exception removing location updates on cancellation", se)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location updates", e)
            if (cont.isActive) {
                cont.resume(LocationResult.Error("Error requesting location: ${e.message}"))
            }
        }
    }

    private suspend fun getLocationName(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                // Handle API level-specific geocoding
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var locationName = "Unknown Location"

                    try {
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            if (addresses.isNotEmpty()) {
                                locationName = formatAddress(addresses[0])
                            } else {
                                Log.d(TAG, "No addresses found for location: $latitude, $longitude")
                            }
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Geocoder IO Exception", e)
                    }

                    locationName
                } else {
                    @Suppress("DEPRECATION")
                    try {
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            formatAddress(addresses[0])
                        } else {
                            Log.d(TAG, "No addresses found for location: $latitude, $longitude")
                            "Unknown Location"
                        }
                    } catch (e: IOException) {
                        Log.e(TAG, "Geocoder IO Exception", e)
                        "Location Unavailable"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in getLocationName", e)
                "Location Error"
            }
        }
    }

    private fun formatAddress(address: Address): String {
        // Enhanced address formatting
        val locality = address.locality
        val subLocality = address.subLocality
        val subAdminArea = address.subAdminArea
        val adminArea = address.adminArea
        val countryName = address.countryName

        // Try to get city name using different fields
        val city = when {
            locality != null -> locality
            subLocality != null -> subLocality
            subAdminArea != null -> subAdminArea
            adminArea != null -> adminArea
            else -> ""
        }

        val country = countryName ?: ""

        return when {
            city.isNotEmpty() && country.isNotEmpty() -> "$city, $country"
            city.isNotEmpty() -> city
            country.isNotEmpty() -> country
            else -> {
                Log.d(TAG, "No meaningful address components for: ${address.toString()}")
                "Unknown Location"
            }
        }
    }
}

sealed class LocationResult {
    data class Success(val locationName: String) : LocationResult()
    data class Error(val message: String) : LocationResult()
    object PermissionRequired : LocationResult()
}
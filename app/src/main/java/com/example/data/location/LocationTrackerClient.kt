package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.data.model.GpsLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationTrackerClient(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return runCatching {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)
    }

    /**
     * Requests a single high-accuracy GPS fix with a 10-second timeout,
     * mirroring navigator.geolocation.getCurrentPosition({ enableHighAccuracy: true, timeout: 10000 }).
     */
    @SuppressLint("MissingPermission")
    suspend fun fetchInitialLocation(timeoutMs: Long = 10_000L): GpsLocation? {
        if (!hasLocationPermission()) return null

        val freshLocation = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val tokenSource = CancellationTokenSource()
                cont.invokeOnCancellation { tokenSource.cancel() }

                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    tokenSource.token,
                ).addOnSuccessListener { location ->
                    if (cont.isActive) cont.resume(location)
                }.addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }

        if (freshLocation != null) {
            return freshLocation.toGpsLocation()
        }

        // Fall back to last known fused location if a fresh satellite fix timed out
        val lastKnown = withTimeoutOrNull(2_000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            }
        }

        return lastKnown?.toGpsLocation() ?: getSystemLastKnownLocation()
    }

    /**
     * Streams continuous high-accuracy location updates (1s target interval, maxAge 1s),
     * mirroring navigator.geolocation.watchPosition({ enableHighAccuracy: true, maximumAge: 1000 }).
     */
    @SuppressLint("MissingPermission")
    fun locationUpdatesFlow(): Flow<GpsLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted."))
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateIntervalMillis(500L)
            .setMaxUpdateAgeMillis(1_000L)
            .setWaitForAccurateLocation(false)
            .build()

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(loc.toGpsLocation())
            }
        }

        var fallbackListener: LocationListener? = null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        try {
            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
                .addOnFailureListener {
                    // Fallback to Android LocationManager if FusedLocation is unavailable
                    if (locationManager != null && fallbackListener == null) {
                        val listener = LocationListener { loc ->
                            trySend(loc.toGpsLocation())
                        }
                        fallbackListener = listener
                        runCatching {
                            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    1_000L,
                                    0f,
                                    listener,
                                    Looper.getMainLooper(),
                                )
                            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                                locationManager.requestLocationUpdates(
                                    LocationManager.NETWORK_PROVIDER,
                                    1_000L,
                                    0f,
                                    listener,
                                    Looper.getMainLooper(),
                                )
                            }
                        }
                    }
                }
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            fusedClient.removeLocationUpdates(fusedCallback)
            fallbackListener?.let { listener ->
                runCatching { locationManager?.removeUpdates(listener) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getSystemLastKnownLocation(): GpsLocation? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            val loc = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            if (loc != null) return loc.toGpsLocation()
        }
        return null
    }

    private fun Location.toGpsLocation(): GpsLocation = GpsLocation(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else 0f,
        timestampMs = if (time > 0L) time else System.currentTimeMillis(),
    )
}

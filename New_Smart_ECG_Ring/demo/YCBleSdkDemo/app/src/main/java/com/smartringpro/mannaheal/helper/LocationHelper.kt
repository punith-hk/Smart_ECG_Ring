package com.smartringpro.mannaheal.helper

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.util.Log
import com.smartringpro.mannaheal.util.UserAppDetailSender

object LocationHelper {
    fun fetchAndSaveLocation(context: Context, isBackground: Boolean) {
        Log.d("LocationHelper", "fetchAndSaveLocation called | isBackground=$isBackground")

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Check location permissions
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("LocationHelper", "Permission check failed — location permission not granted")
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("LocationHelper", "Fetched location: lat=${location.latitude}, lng=${location.longitude}")

                    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                    sharedPreferences.edit()
                        .putString("lat", "${location.latitude}")
                        .putString("lng", "${location.longitude}")
                        .apply()

                    Log.i("LocationHelper", "Location saved to preferences")

                    // Send app detail (skip duplicates if background)
                    UserAppDetailSender.sendUserAppDetail(context, isBackground)
                } else {
                    Log.w("LocationHelper", "Location is null — cannot save or send")
                }
            }.addOnFailureListener { e ->
                Log.e("LocationHelper", "Failed to get location", e)
            }
        } catch (e: SecurityException) {
            Log.e("LocationHelper", "SecurityException when accessing location", e)
        }
    }
}

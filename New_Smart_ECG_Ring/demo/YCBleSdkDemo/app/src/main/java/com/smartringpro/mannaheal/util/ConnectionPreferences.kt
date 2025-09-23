package com.smartringpro.mannaheal.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartringpro.mannaheal.api.ringApi.RingConfig

data class DeviceInfo(val mac: String, val name: String)

object ConnectionPreferences {

    private const val PREF_NAME = "connection_preferences"
    private const val KEY_IS_CONNECTED = "is_connected"
    private const val KEY_MAC_ADDRESS = "mac_address"
    private const val KEY_NAME = "name"

    private const val KEY_SAVED_DEVICES = "saved_devices"

    fun saveConnectionState(context: Context, isConnected: Boolean, macAddress: String?, name: String?,) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_IS_CONNECTED, isConnected)
        editor.putString(KEY_MAC_ADDRESS, macAddress)
        editor.putString(KEY_NAME, name)
        editor.apply()
    }

    fun getConnectionState(context: Context): Boolean {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(KEY_IS_CONNECTED, false)
    }

    fun getMacAddress(context: Context): String? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_MAC_ADDRESS, null)
    }

    fun getDeviceName(context: Context): String? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return  sharedPreferences.getString(KEY_NAME, null)
    }

    fun getSavedDevices(context: Context): MutableList<DeviceInfo> {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(KEY_SAVED_DEVICES, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<DeviceInfo>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun saveDevice(context: Context, mac: String, name: String) {
        val devices = getSavedDevices(context)

        // check if already saved
        if (devices.any { it.mac == mac }) return

        devices.add(DeviceInfo(mac, name))

        val json = Gson().toJson(devices)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SAVED_DEVICES, json).apply()
    }

    fun isDeviceSaved(context: Context, mac: String): Boolean {
        return getSavedDevices(context).any { it.mac == mac }
    }

    fun setLastRingConfig(context: Context, configs: List<RingConfig>) {
        val prefs = context.getSharedPreferences("ring_prefs", Context.MODE_PRIVATE)
        val json = Gson().toJson(configs)
        prefs.edit()
            .putString("last_ring_config", json)
            .apply()
    }

    fun getLastRingConfig(context: Context): List<RingConfig> {
        val prefs = context.getSharedPreferences("ring_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("last_ring_config", null) ?: return emptyList()

        val type = object : TypeToken<List<RingConfig>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun setLastVitalTime(context: Context, type: String, timestamp: Long, value: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong(type, timestamp)
        editor.putString("$type value", value)
        editor.apply()
    }

    fun getLastVitalTime(context: Context, type: String): Pair<Long, String?> {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return Pair(sharedPreferences.getLong(type, 0),sharedPreferences.getString("$type value", null))
    }

}
package com.smartringpro.mannaheal.util

import android.content.Context

object ApplicationPreferences {
    fun putString(context: Context, key: String, value: String, prefName: String = "AppPreferences") {
        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        prefs.edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String, default: String = "", prefName: String = "AppPreferences"): String {
        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        return prefs.getString(key, default) ?: default
    }
}
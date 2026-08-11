package com.freeguitar.data

import android.content.Context

object UpdatePrefs {
    private const val PREFS = "updates"
    private const val KEY_URL = "manifest_url"
    private const val KEY_AUTO = "auto_check"

    const val DEFAULT_URL = "https://example.com/free-guitar/manifest.json"

    fun getUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL

    fun setUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_URL, url).apply()
    }

    fun autoCheckEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO, true)

    fun setAutoCheck(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO, enabled).apply()
    }
}

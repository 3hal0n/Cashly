package com.example.cashly.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Currency preferences
    fun getCurrency(): String {
        return prefs.getString(KEY_CURRENCY, "USD") ?: "USD"
    }

    fun setCurrency(currency: String) {
        prefs.edit().putString(KEY_CURRENCY, currency).apply()
    }

    // Budget preferences
    fun getMonthlyBudget(): Double {
        return prefs.getFloat(KEY_MONTHLY_BUDGET, 0f).toDouble()
    }

    fun setMonthlyBudget(budget: Double) {
        prefs.edit().putFloat(KEY_MONTHLY_BUDGET, budget.toFloat()).apply()
    }

    // Theme preferences
    fun getTheme(): String {
        return prefs.getString(KEY_THEME, "light") ?: "light"
    }

    fun setTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    // Notification preferences
    fun getNotificationEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "cashly_preferences"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_THEME = "theme"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    }
} 
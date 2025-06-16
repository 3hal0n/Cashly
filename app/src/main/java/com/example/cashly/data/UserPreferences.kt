package com.example.cashly.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import android.util.Log
import java.util.Locale

class UserPreferences(context: Context) {
    private var prefs: SharedPreferences

    init {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyGenParameterSpec(
                    KeyGenParameterSpec.Builder(
                        "_cashly_master_key_",
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                )
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("UserPreferences", "Failed to create EncryptedSharedPreferences, falling back to regular SharedPreferences", e)
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        // Ensure a hashed PIN is always present. If not, set the hashed default PIN.
        if (!prefs.contains(KEY_PIN)) {
            val defaultHashedPin = hashPin("1234")
            prefs.edit().putString(KEY_PIN, defaultHashedPin).apply()
        }
    }

    // PIN/Password preferences
    fun getPin(): String {
        // This method now returns the stored hashed PIN, which will always be present.
        return prefs.getString(KEY_PIN, hashPin("1234")) ?: hashPin("1234") // Should not be null after init
    }

    fun setPin(pin: String) {
        val hashedPin = hashPin(pin)
        prefs.edit().putString(KEY_PIN, hashedPin).apply()
    }

    fun validatePin(pin: String): Boolean {
        val storedHashedPin = getPin() // Always get a hashed PIN from here
        return hashPin(pin) == storedHashedPin
    }

    private fun hashPin(pin: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

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

    fun getCurrencyLocale(): Locale {
        val currencyCode = getCurrency()
        return when (currencyCode) {
            "LKR" -> Locale("en", "LK")
            "USD" -> Locale.US
            "EUR" -> Locale.FRANCE // A common Eurozone locale, can be adjusted
            "GBP" -> Locale.UK
            "JPY" -> Locale.JAPAN
            "CNY" -> Locale.CHINA
            "INR" -> Locale("en", "IN") // India
            "AUD" -> Locale("en", "AU") // Australia
            "CAD" -> Locale.CANADA
            else -> Locale.getDefault() // Fallback to default locale
        }
    }

    companion object {
        private const val PREFS_NAME = "cashly_preferences"
        private const val KEY_PIN = "pin"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_THEME = "theme"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    }
} 
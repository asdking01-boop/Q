package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ParentalControlManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("asd_parental_prefs", Context.MODE_PRIVATE)

    private val _isEnabled = MutableStateFlow(prefs.getBoolean("parental_enabled", false))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _lockedCategories = MutableStateFlow(
        prefs.getStringSet("locked_categories", defaultSensitiveKeywords)?.toSet() ?: defaultSensitiveKeywords
    )
    val lockedCategories: StateFlow<Set<String>> = _lockedCategories.asStateFlow()

    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    fun setEnabled(enabled: Boolean, pin: String): Boolean {
        if (!verifyPin(pin)) return false
        prefs.edit().putBoolean("parental_enabled", enabled).apply()
        _isEnabled.value = enabled
        return true
    }

    fun setPin(currentPin: String, newPin: String): Boolean {
        val savedPin = prefs.getString("parental_pin", "0000") ?: "0000"
        if (currentPin != savedPin && _isEnabled.value) {
            return false
        }
        if (newPin.length < 4) return false
        prefs.edit().putString("parental_pin", newPin).apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val savedPin = prefs.getString("parental_pin", "0000") ?: "0000"
        val valid = pin == savedPin
        if (valid) {
            _isSessionUnlocked.value = true
        }
        return valid
    }

    fun lockSession() {
        _isSessionUnlocked.value = false
    }

    fun toggleCategoryLock(categoryName: String) {
        val current = _lockedCategories.value.toMutableSet()
        if (current.contains(categoryName)) {
            current.remove(categoryName)
        } else {
            current.add(categoryName)
        }
        prefs.edit().putStringSet("locked_categories", current).apply()
        _lockedCategories.value = current
    }

    fun isCategoryLocked(categoryName: String): Boolean {
        if (!_isEnabled.value) return false
        if (_isSessionUnlocked.value) return false

        val lower = categoryName.lowercase()
        // Check exact match in custom locked list
        if (_lockedCategories.value.any { it.equals(categoryName, ignoreCase = true) }) {
            return true
        }

        // Check common adult/sensitive keywords
        val keywords = listOf("+18", "18+", "xxx", "adult", "erotic", "للكبار", "كبار فقط")
        return keywords.any { lower.contains(it) }
    }

    companion object {
        val defaultSensitiveKeywords = setOf(
            "+18",
            "18+",
            "XXX",
            "Adult",
            "للكبار فقط",
            "For Adults"
        )

        @Volatile
        private var INSTANCE: ParentalControlManager? = null

        fun getInstance(context: Context): ParentalControlManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ParentalControlManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

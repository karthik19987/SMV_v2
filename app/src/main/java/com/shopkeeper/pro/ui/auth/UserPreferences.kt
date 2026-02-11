package com.shopkeeper.pro.ui.auth

import android.content.Context
import android.content.SharedPreferences
import com.shopkeeper.pro.data.entity.User

object UserPreferences {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_ROLE = "role"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setCurrentUser(context: Context, user: User) {
        getPreferences(context).edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_DISPLAY_NAME, user.displayName)
            .putString(KEY_ROLE, user.role)
            .apply()
    }

    fun getCurrentUserId(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_ID, null)
    }

    fun getCurrentUsername(context: Context): String? {
        return getPreferences(context).getString(KEY_USERNAME, null)
    }

    fun getCurrentUserDisplayName(context: Context): String? {
        return getPreferences(context).getString(KEY_DISPLAY_NAME, null)
    }

    fun getCurrentUserRole(context: Context): String? {
        return getPreferences(context).getString(KEY_ROLE, null)
    }

    fun clearUser(context: Context) {
        getPreferences(context).edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getCurrentUserId(context) != null
    }

    fun isAdmin(context: Context): Boolean {
        return getCurrentUserRole(context) == "admin"
    }
}
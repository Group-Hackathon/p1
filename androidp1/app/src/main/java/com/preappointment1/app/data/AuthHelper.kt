package com.preappointment1.app.data

import android.util.Log
import com.preappointment1.app.data.api.ApiClient
import com.preappointment1.app.data.model.AuthRequest

object AuthHelper {
    private const val TAG = "LPM_AUTH"
    private const val DEVICE_PASSWORD = "secret_device_password"

    suspend fun ensureAuthenticated(): Boolean {
        if (SessionManager.getToken() != null) return true

        val email = "${SessionManager.getOrCreateDeviceId()}@local.device"
        val request = AuthRequest(email = email, password = DEVICE_PASSWORD)

        return try {
            val response = ApiClient.apiService.register(request)
            SessionManager.saveToken(response.token)
            Log.d(TAG, "Registered new device account")
            true
        } catch (e: Exception) {
            try {
                val response = ApiClient.apiService.login(request)
                SessionManager.saveToken(response.token)
                Log.d(TAG, "Logged in existing device account")
                true
            } catch (loginError: Exception) {
                Log.e(TAG, "Authentication failed", loginError)
                false
            }
        }
    }

    suspend fun ensureProfile(): String? {
        SessionManager.getProfileId()?.let { return it }

        val profile = ApiClient.apiService.createProfile(
            com.preappointment1.app.data.model.ProfileRequest(
                first_name = "Patient",
                last_name = "Local",
                relation = "Self"
            )
        )
        SessionManager.saveProfileId(profile.id)
        return profile.id
    }
}

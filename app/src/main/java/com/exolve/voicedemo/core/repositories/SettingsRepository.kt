package com.exolve.voicedemo.core.repositories

import android.content.Context
import com.exolve.voicedemo.core.models.Account
import com.exolve.voicesdk.TelecomIntegrationMode
import com.google.gson.Gson

class SettingsRepository(
    context: Context,
) {

    private val PREFERENCES = "EXOLVE_PREFERENCES_FILE_KEY"
    private val ACCOUNT_KEY = "LOCAL_ACCOUNT_GSON_KEY"
    private val BACKGROUND_RUNNING_KEY = "LOCAL_BACKGROUND_RUNNING"
    private val DETECT_CALLLOCATION_KEY = "LOCAL_DETECT_CALLLOCATION"
    private val TELECOM_MANAGER_MODE_KEY = "LOCAL_TELECOM_MANAGER_MODE"

    private val dataSource = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private val editor = dataSource.edit()

    fun fetchAccountDetails(): Account {
        val jsonString =
            dataSource.getString(ACCOUNT_KEY, null)
        return if (jsonString != null) {
            Gson().fromJson(jsonString, Account::class.java)
        } else {
            Account(null, null)
        }
    }

    fun saveAccountDetails(account: Account) {
        editor
            .putString(ACCOUNT_KEY, Gson().toJson(account))
            .apply()
    }

    fun setBackgroundRunningEnabled(enabled: Boolean) {
        editor
            .putBoolean(BACKGROUND_RUNNING_KEY, enabled)
            .apply()
    }

    fun isBackgroundRunningEnabled(): Boolean {
        return dataSource.getBoolean(BACKGROUND_RUNNING_KEY, false)
    }

    fun setDetectCallLocationEnabled(enabled: Boolean) {
        editor
            .putBoolean(DETECT_CALLLOCATION_KEY, enabled)
            .apply()
    }

    fun isDetectCallLocationEnabled(): Boolean {
        return dataSource.getBoolean(DETECT_CALLLOCATION_KEY, true)
    }

    fun setTelecomManagerMode(mode: TelecomIntegrationMode) {
        editor
            .putString(TELECOM_MANAGER_MODE_KEY, mode.name)
            .apply()
    }

    fun getTelecomManagerMode(): TelecomIntegrationMode {
        val modeName = dataSource.getString(TELECOM_MANAGER_MODE_KEY, TelecomIntegrationMode.SELF_MANAGED_SERVICE.name)
        return TelecomIntegrationMode.valueOf(modeName!!)
    }
}

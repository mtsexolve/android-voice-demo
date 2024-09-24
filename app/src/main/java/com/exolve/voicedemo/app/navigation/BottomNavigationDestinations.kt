package com.exolve.voicedemo.app.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.exolve.voicedemo.R

@Immutable
sealed class BottomNavigationDestinations(
    @StringRes val titleStringId: Int,
    val icon: Int,
    @StringRes val screenRouteStringId: Int
) {
    data object Dialer : BottomNavigationDestinations(
        R.string.bottom_navigation_dialer_title,
        R.drawable.ic_dtmf_call,
        R.string.bottom_navigation_dialer_title
    )

    data object Account : BottomNavigationDestinations(
        R.string.bottom_navigation_account_title,
        R.drawable.ic_account,
        R.string.bottom_navigation_account_title
    )

    data object Settings : BottomNavigationDestinations(
        R.string.bottom_navigation_settings_title,
        R.drawable.ic_settings,
        R.string.bottom_navigation_settings_title
    )
}


package com.exolve.voicedemo.app.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.exolve.voicedemo.R
@Immutable
sealed class BottomNavigationDestinations(@StringRes val titleStringId: Int, val icon: Int, @StringRes val screenRouteStringId: Int){
    object Dialer : BottomNavigationDestinations(R.string.bottom_navigation_dialer_title, R.drawable.ic_dtmf_call, R.string.bottom_navigation_dialer_title)
    object Settings: BottomNavigationDestinations(R.string.bottom_navigation_settings_title,R.drawable.ic_settings,R.string.bottom_navigation_settings_title)
}


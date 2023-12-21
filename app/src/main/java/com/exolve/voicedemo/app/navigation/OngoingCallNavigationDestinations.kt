package com.exolve.voicedemo.app.navigation

import androidx.annotation.StringRes
import com.exolve.voicedemo.R

sealed class OngoingCallNavigationDestinations(@StringRes val screenRouteStringId: Int) {
    object Control : OngoingCallNavigationDestinations(screenRouteStringId = R.string.navigation_ongoing_call_control)
}
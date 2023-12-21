package com.exolve.voicedemo.features.call

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun ControlOngoingCallScreenDestination(
    ongoingCallViewModel: CallViewModel
) {
    OngoingCallScreen(
        ongoingCallViewModel = ongoingCallViewModel,
        onEvent = ongoingCallViewModel::setEvent
    )
}
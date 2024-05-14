package com.exolve.voicedemo.features.call

import androidx.compose.runtime.Composable

@Composable
fun ControlOngoingCallScreenDestination(
    ongoingCallViewModel: CallViewModel
) {
    OngoingCallScreen(
        ongoingCallViewModel = ongoingCallViewModel,
        onEvent = ongoingCallViewModel::setEvent
    )
}
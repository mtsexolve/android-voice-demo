package com.exolve.voicedemo.features.dialer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable

@Composable
fun DialerScreenDestination(viewModel: DialerViewModel, barPaddingValues: PaddingValues) {
    DialerScreen(
        viewModel = viewModel,
        onEvent  = viewModel::setEvent,
        barPaddingValues = barPaddingValues
    )
}
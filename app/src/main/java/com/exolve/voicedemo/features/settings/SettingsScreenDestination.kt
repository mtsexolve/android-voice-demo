package com.exolve.voicedemo.features.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable

@Composable
fun SettingsScreenDestination(viewModel: SettingsViewModel, barPaddingValues: PaddingValues) {
    SettingsScreen(
        viewModel = viewModel,
        onEvent = viewModel::setEvent,
        barPaddingValues = barPaddingValues,
    )
}
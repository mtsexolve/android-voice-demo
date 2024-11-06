package com.exolve.voicedemo.features.settings

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.exolve.voicedemo.R
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.exolve.voicedemo.core.utils.SharingProvider
import com.exolve.voicesdk.LogLevel
import com.exolve.voicesdk.TelecomIntegrationMode

private const val SETTINGS_SCREEN = "SettingsScreen"

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onEvent: (event: SettingsContract.Event) -> Unit,
    barPaddingValues: PaddingValues
) {
    val state by viewModel.uiState.collectAsState()
    Log.d(SETTINGS_SCREEN, "SettingsScreen: updated state is $state")
    SettingsContent(
        onEvent = onEvent,
        modifier = Modifier.padding(
            top = barPaddingValues.calculateTopPadding(),
            bottom = barPaddingValues.calculateBottomPadding(),
            start = 20.dp,
            end = 20.dp,
        ),
        versionDescription = state.versionDescription,
        voipBackgroundRunning = state.voipBackgroundRunning,
        detectCallLocation = state.detectCallLocation,
        telecomManagerMode = state.telecomManagerMode,
        sipTraces = state.sipTraces,
        logLevel = state.logLevel,
        useEncryption = state.useEncryption,
        needRestart = state.needRestart
    )
}

fun getBundleId(context: Context): String {
    return context.packageName
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsContent(
    onEvent: (event: SettingsContract.Event) -> Unit,
    versionDescription: String,
    voipBackgroundRunning: Boolean,
    detectCallLocation: Boolean,
    telecomManagerMode: TelecomIntegrationMode,
    sipTraces: Boolean,
    logLevel: LogLevel,
    useEncryption: Boolean,
    needRestart: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val bundleId = getBundleId(context)
    val focusManager = LocalFocusManager.current
    var shouldShowClearLogsAlert by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                }
        ) {
            SettingsView(
                onEvent = onEvent,
                voipBackgroundRunning = voipBackgroundRunning,
                detectCallLocation = detectCallLocation,
                telecomManagerMode = telecomManagerMode,
                sipTraces = sipTraces,
                logLevel = logLevel,
                useEncryption = useEncryption,
            )
        }
        val isPressed = remember { mutableStateOf(false) }

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (needRestart) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Absolute.Center
                ) {
                    Button(
                        onClick = {
                            onEvent(SettingsContract.Event.Restart)
                        },
                        modifier = Modifier
                            .semantics { testTagsAsResourceId = true }
                            .testTag("button_settings_restart"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_red)),
                    ) {
                        Text(
                            text = stringResource(id = R.string.restart_app),
                            fontSize = 17.sp,
                            style = TextStyle(
                                color = colorResource(id = R.color.white),
                                fontFamily = FontFamily(Font(R.font.mtscompact_bold)),
                            ),
                            modifier = Modifier.padding(vertical = 14.dp)
                        )
                    }
                }
            }

            Text(
                text = "${bundleId}\n${versionDescription}",
                style = TextStyle(
                    color = colorResource(id = R.color.mts_text_grey),
                    fontFamily = FontFamily(Font(R.font.mtscompact_regular))
                ),
                modifier = Modifier
                    .padding(bottom = 4.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.Right
            ) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .padding(bottom = 4.dp),
                    colors = ButtonDefaults.buttonColors(colorResource(id = R.color.white)),
                    border = BorderStroke(1.dp, colorResource(id = R.color.white)),
                    contentPadding = PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp),
                ) {
                    Text(
                        text = AnnotatedString(stringResource(id = R.string.settings_logs_button)),
                        style = TextStyle(
                            textDecoration = TextDecoration.Underline,
                            color = if (isPressed.value) colorResource(id = R.color.black)
                            else colorResource(id = R.color.mts_text_grey),
                            fontFamily = FontFamily(Font(R.font.mtscompact_regular))
                        ),
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { SharingProvider(context.applicationContext).share("Logs") },
                                onLongPress = { shouldShowClearLogsAlert = true }
                            )
                        }
                    )
                }
            }
            if (shouldShowClearLogsAlert) {
                AlertDialog(
                    onDismissRequest = { shouldShowClearLogsAlert = false },
                    title = { Text(stringResource(id = R.string.clear_logs_title)) },
                    text = { Text(stringResource(id = R.string.clear_logs_message)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                SharingProvider(context.applicationContext).removeOldFiles()
                                shouldShowClearLogsAlert = false
                            },
                            colors = ButtonDefaults.buttonColors(colorResource(id = R.color.white)),
                            border = BorderStroke(1.dp, colorResource(id = R.color.white))
                        ) {
                            Text(stringResource(id = R.string.clear_logs_confirm))
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { shouldShowClearLogsAlert = false },
                            colors = ButtonDefaults.buttonColors(colorResource(id = R.color.white)),
                            border = BorderStroke(1.dp, colorResource(id = R.color.white))
                        ) {
                            Text(stringResource(id = R.string.clear_logs_cancel))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsView(
    onEvent: (event: SettingsContract.Event) -> Unit,
    voipBackgroundRunning: Boolean,
    detectCallLocation: Boolean,
    telecomManagerMode: TelecomIntegrationMode,
    sipTraces: Boolean,
    logLevel: LogLevel,
    useEncryption: Boolean,
) {
    ConstraintLayout(
        Modifier
            .fillMaxWidth()
    ) {
        val (
            bgModeSwitchLayout,
            ldModeSwitchLayout,
            telecomIntegrationDropdownLayout,
            useSipSwitchLayout,
            logLevelDropdownLayout,
            useEncryptionSwitchLayout,
            environmentDropdownLayout
        ) = createRefs()

        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.constrainAs(bgModeSwitchLayout) {
                top.linkTo(parent.bottom, margin = 8.dp)
                start.linkTo(parent.start)
            }

        ) {
            Text(
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                    fontSize = 14.sp,
                    color = colorResource(
                        id = R.color.mts_text_grey
                    )
                ),
                text = stringResource(id = R.string.background_running)
            )

            Switch(
                modifier = Modifier
                    .semantics { testTagsAsResourceId = true }
                    .testTag("button_settings_bg_mode_switch"),
                checked = voipBackgroundRunning,
                onCheckedChange = {
                    onEvent(SettingsContract.Event.OnBackgroundRunningChanged(it))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorResource(id = R.color.mts_red),
                )
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.constrainAs(ldModeSwitchLayout) {
                top.linkTo(bgModeSwitchLayout.bottom, margin = 8.dp)
                start.linkTo(bgModeSwitchLayout.start)
            }

        ) {
            Text(
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                    fontSize = 14.sp,
                    color = colorResource(
                        id = R.color.mts_text_grey
                    )
                ),
                text = stringResource(id = R.string.calllocation_detect)
            )

            Switch(
                modifier = Modifier
                    .semantics { testTagsAsResourceId = true }
                    .testTag("button_settings_detect_call_location_switch"),
                checked = detectCallLocation,
                onCheckedChange = {
                    onEvent(SettingsContract.Event.OnCallLocationDetectChanged(it))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorResource(id = R.color.mts_red),
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.constrainAs(telecomIntegrationDropdownLayout) {
                top.linkTo(ldModeSwitchLayout.bottom, margin = 16.dp)
                start.linkTo(ldModeSwitchLayout.start)
            }
        ) {
            Text(
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.mts_text_grey)
                ),
                text = stringResource(id = R.string.telecom_framework_integration)
            )

            var expanded by remember { mutableStateOf(false) }
            var selectedOption by remember { mutableStateOf(telecomManagerMode.name) }
            val options = TelecomIntegrationMode.entries.map { it.name }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { expanded = true }
            ) {
                Text(
                    text = selectedOption,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.mts_text_grey)
                    )
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(onClick = {
                            selectedOption = option
                            expanded = false
                            onEvent(
                                SettingsContract.Event.OnTelecomManagerModeChanged(
                                    TelecomIntegrationMode.valueOf(option)
                                )
                            )
                        }) {
                            Text(
                                text = option,
                                style = TextStyle(
                                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                                    fontSize = 14.sp,
                                    color = colorResource(id = R.color.mts_text_grey)
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.constrainAs(useSipSwitchLayout) {
                top.linkTo(telecomIntegrationDropdownLayout.bottom, margin = 8.dp)
                start.linkTo(telecomIntegrationDropdownLayout.start)
            }
        ) {
            Text(
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                    fontSize = 14.sp,
                    color = colorResource(
                        id = R.color.mts_text_grey
                    )
                ),
                text = stringResource(id = R.string.sip_traces)
            )

            Switch(
                modifier = Modifier
                    .semantics { testTagsAsResourceId = true }
                    .testTag("button_settings_enable_sip_traces_switch"),
                checked = sipTraces,
                onCheckedChange = {
                    onEvent(SettingsContract.Event.OnSipTracesChanged(it))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorResource(id = R.color.mts_red),
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.constrainAs(logLevelDropdownLayout) {
                top.linkTo(useSipSwitchLayout.bottom, margin = 16.dp)
                start.linkTo(useSipSwitchLayout.start)
            }
        ) {
            Text(
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.mts_text_grey)
                ),
                text = stringResource(id = R.string.log_level)
            )

            var expanded by remember { mutableStateOf(false) }
            var selectedOption by remember { mutableStateOf(logLevel.name) }
            val options = LogLevel.entries.map { it.name }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { expanded = true }
            ) {
                Text(
                    text = selectedOption,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                        fontSize = 14.sp,
                        color = colorResource(id = R.color.mts_text_grey)
                    )
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(onClick = {
                            selectedOption = option
                            expanded = false
                            onEvent(SettingsContract.Event.OnLogLevelChanged(LogLevel.valueOf(option)))
                        }) {
                            Text(
                                text = option,
                                style = TextStyle(
                                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                                    fontSize = 14.sp,
                                    color = colorResource(id = R.color.mts_text_grey)
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.constrainAs(useEncryptionSwitchLayout) {
                top.linkTo(logLevelDropdownLayout.bottom, margin = 8.dp)
                start.linkTo(logLevelDropdownLayout.start)
            }
        ) {
            Text(
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                    fontSize = 14.sp,
                    color = colorResource(
                        id = R.color.mts_text_grey
                    )
                ),
                text = stringResource(id = R.string.use_encryption)
            )

            Switch(
                modifier = Modifier
                    .semantics { testTagsAsResourceId = true }
                    .testTag("button_settings_enable_encryption_switch"),
                checked = useEncryption,
                onCheckedChange = {
                    onEvent(SettingsContract.Event.OnUseEncryptionChanged(it))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorResource(id = R.color.mts_red),
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.constrainAs(environmentDropdownLayout) {
                top.linkTo(useEncryptionSwitchLayout.bottom, margin = 16.dp)
                start.linkTo(useEncryptionSwitchLayout.start)
            }
        ) {
            Text(
                style = TextStyle(
                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.mts_text_grey)
                ),
                text = stringResource(id = R.string.environment)
            )

            val options = TelecomManager.getInstance().availableEnvironments()
            if (options.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                var selectedOption by remember {
                    mutableStateOf(TelecomManager.getInstance().currentEnvironment())
                }

                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { expanded = true }
                ) {
                    Text(
                        text = selectedOption,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.mts_text_grey)
                        )
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(onClick = {
                                selectedOption = option
                                expanded = false
                                onEvent(SettingsContract.Event.OnEnvironmentChanged(option))
                            }) {
                                Text(
                                    text = option,
                                    style = TextStyle(
                                        fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                                        fontSize = 14.sp,
                                        color = colorResource(id = R.color.mts_text_grey)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

    }
}


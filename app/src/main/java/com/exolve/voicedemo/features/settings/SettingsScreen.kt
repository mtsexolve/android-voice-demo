package com.exolve.voicedemo.features.settings

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.exolve.voicedemo.R
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.exolve.voicedemo.core.utils.SharingProvider
import com.exolve.voicesdk.LogLevel
import com.exolve.voicesdk.TelecomIntegrationMode
import com.exolve.voicesdk.RegistrationMode

private const val SETTINGS_SCREEN = "SettingsScreen"

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onEvent: (event: SettingsContract.Event) -> Unit,
    barPaddingValues: PaddingValues
) {
    val state by viewModel.uiState.collectAsState()
    SettingsContent(
        onEvent = onEvent,
        modifier = Modifier.padding(
            top = barPaddingValues.calculateTopPadding(),
            bottom = barPaddingValues.calculateBottomPadding(),
            start = 20.dp,
            end = 20.dp,
        ),
        versionDescription = state.versionDescription,
        registrationMode = state.registrationMode,
        detectCallLocation = state.detectCallLocation,
        telecomManagerMode = state.telecomManagerMode,
        sipTraces = state.sipTraces,
        logLevel = state.logLevel,
        useEncryption = state.useEncryption,
        callContext = state.callContext,
        needRestart = state.needRestart
    )
}

fun getBundleId(context: Context): String {
    return context.packageName
}

@Composable
fun SettingsContent(
    onEvent: (event: SettingsContract.Event) -> Unit,
    versionDescription: String,
    registrationMode: RegistrationMode,
    detectCallLocation: Boolean,
    telecomManagerMode: TelecomIntegrationMode,
    sipTraces: Boolean,
    logLevel: LogLevel,
    useEncryption: Boolean,
    callContext: String,
    needRestart: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val bundleId = getBundleId(context)
    val focusManager = LocalFocusManager.current
    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .weight(1f, fill = true)
            ) {
                OptionRegistrationMode(onEvent, registrationMode)
                OptionDetectLocation(onEvent, detectCallLocation)
                OptionTelecomIntegration(onEvent, telecomManagerMode)
                OptionSipTraces(onEvent, sipTraces)
                OptionLogLevel(onEvent, logLevel)
                OptionEncryption(onEvent, useEncryption)
                OptionEnvironment(onEvent)
                Spacer(modifier = Modifier.height(16.dp))
                OptionCallContext(onEvent, callContext)
                Spacer(Modifier.fillMaxHeight())
            }

            Column(Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (needRestart) {
                    RestartButton(onEvent)
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

                SendLogsButton()
            }
        }
    }
}

@Composable
fun OptionRegistrationMode(
    onEvent: (event: SettingsContract.Event) -> Unit,
    registrationMode: RegistrationMode
) {
    Row( verticalAlignment = Alignment.CenterVertically )
    {
        Text(
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                fontSize = 14.sp,
                color = colorResource(id = R.color.mts_text_grey)
            ),
            text = stringResource(id = R.string.registration_mode)
        )

        var expanded by remember { mutableStateOf(false) }
        var selectedOption by remember { mutableStateOf(registrationMode.name) }
        val options = RegistrationMode.entries.map { it.name }

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
                            SettingsContract.Event.OnRegistrationModeChanged(
                                RegistrationMode.valueOf(option)
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
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OptionDetectLocation(
    onEvent: (event: SettingsContract.Event) -> Unit,
    detectCallLocation: Boolean
) {
    Row( verticalAlignment = Alignment.CenterVertically )
    {
        Text(
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                fontSize = 14.sp,
                color = colorResource(
                    id = R.color.mts_text_grey
                )
            ),
            text = stringResource(id = R.string.location_detect)
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
}

@Composable
fun OptionTelecomIntegration(
    onEvent: (event: SettingsContract.Event) -> Unit,
    telecomManagerMode: TelecomIntegrationMode
) {
    Row( verticalAlignment = Alignment.CenterVertically )
    {
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
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OptionSipTraces(
    onEvent: (event: SettingsContract.Event) -> Unit,
    sipTraces: Boolean
) {
    Row( verticalAlignment = Alignment.CenterVertically )
    {
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
}

@Composable
fun OptionLogLevel(
    onEvent: (event: SettingsContract.Event) -> Unit,
    logLevel: LogLevel
) {
    Row( verticalAlignment = Alignment.CenterVertically )
    {
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
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OptionEncryption(
    onEvent: (event: SettingsContract.Event) -> Unit,
    useEncryption: Boolean
) {
    Row( verticalAlignment = Alignment.CenterVertically )
    {
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
}

@Composable
fun OptionEnvironment(
    onEvent: (event: SettingsContract.Event) -> Unit,
) {
    Row( verticalAlignment = Alignment.CenterVertically )
    {
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OptionCallContext(
    onEvent: (event: SettingsContract.Event) -> Unit,
    callContext: String
) {
    Column( horizontalAlignment = Alignment.Start )
    {
        Text(
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                fontSize = 14.sp,
                color = colorResource(id = R.color.mts_text_grey)
            ),
            text = stringResource(id = R.string.call_context)
        )

        OutlinedTextField(
            value = callContext,
            onValueChange = { onEvent(SettingsContract.Event.OnCallContextChanged(it)) },
            textStyle = TextStyle(
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                fontSize = 18.sp,
                color = colorResource(id = R.color.black)
            ),
            modifier = Modifier
                .testTag("text_field_call_context")
                .semantics { testTagsAsResourceId = true }
                .fillMaxWidth(),
            maxLines = Int.MAX_VALUE,
            enabled = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = colorResource(id = R.color.mts_grey),
                unfocusedBorderColor = colorResource(id = R.color.mts_grey),
                focusedLabelColor = colorResource(id = R.color.mts_grey),
                cursorColor = colorResource(id = R.color.black),
                backgroundColor = colorResource(id = R.color.mts_bg_grey),
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RestartButton(
    onEvent: (event: SettingsContract.Event) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
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

@Composable
fun SendLogsButton() {
    val context = LocalContext.current
    var shouldShowClearLogsAlert by remember { mutableStateOf(false) }
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
            contentPadding = PaddingValues(),
        ) {
            Text(
                text = AnnotatedString(stringResource(id = R.string.settings_logs_button)),
                style = TextStyle(
                    textDecoration = TextDecoration.Underline,
                    color = colorResource(id = R.color.black),
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
        ShowClearLogsAlert( { shouldShowClearLogsAlert = false } )
    }
}

@Composable
fun ShowClearLogsAlert(onDismiss: () -> Unit)
{
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.clear_logs_title)) },
        text = { Text(stringResource(id = R.string.clear_logs_message)) },
        confirmButton = {
            Button(
                onClick = {
                    SharingProvider(context.applicationContext).removeOldFiles()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(colorResource(id = R.color.white)),
                border = BorderStroke(1.dp, colorResource(id = R.color.white))
            ) {
                Text(stringResource(id = R.string.clear_logs_confirm))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(colorResource(id = R.color.white)),
                border = BorderStroke(1.dp, colorResource(id = R.color.white))
            ) {
                Text(stringResource(id = R.string.clear_logs_cancel))
            }
        }
    )
}

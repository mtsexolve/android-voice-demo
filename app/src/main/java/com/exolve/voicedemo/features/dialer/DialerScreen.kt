package com.exolve.voicedemo.features.dialer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.exolve.voicedemo.R
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DialerScreen(
    viewModel: DialerViewModel,
    onEvent: (event: UiEvent) -> Unit,
    barPaddingValues: PaddingValues
) {
    val state by viewModel.uiState.collectAsState()
    ConstraintLayout(
        Modifier
            .fillMaxSize()
            .padding(barPaddingValues)) {
        val (
            entryNumberPanel,
            contactsButton,
            callButton,
            removeButton,
            backToCallButton,
        ) = createRefs()

        if (state.hasCurrentCall) {
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(500)
                isVisible = true
            }
            if (isVisible) {
                Button(
                    onClick = { onEvent(DialerContract.Event.OnBackToCallActivityClicked) },
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag("button_main_activity_back_to_calls_activity")
                        .constrainAs(backToCallButton) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            top.linkTo(parent.top)
                        }
                        .fillMaxWidth()
                        .height(28  .dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.white)),
                ) {
                    Text(
                        text = stringResource(id = R.string.go_to_call),
                        style = TextStyle(
                            fontFamily = FontFamily(Font(R.font.mtswide_medium)),
                            fontSize = 12.sp, color = colorResource(id = R.color.black)
                        )
                    )
                }
            }
        }

        NumberEntryPanel(
            dialerTextField = state.dialerText,
            onEvent,
            modifier = Modifier.constrainAs(entryNumberPanel) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(callButton.top, margin = 16.dp)
            }
        )

        OutlinedButton( // Contacts
            onClick = { onEvent(DialerContract.Event.OnContactsButtonClicked) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_one")
                .constrainAs(contactsButton) {
                    bottom.linkTo(parent.bottom, margin = 48.dp)
                    end.linkTo(callButton.start, margin = 24.dp)
                }
                .size(72.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_btn_contacts),
                contentDescription = "contacts",
                Modifier.size(42.dp)
            )
        }

        IconButton(
            onClick = {},
            Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_call_to_number")
                .constrainAs(callButton) {
                    bottom.linkTo(parent.bottom, margin = 48.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .size(72.dp),
            )
        {
            Image(imageVector = ImageVector.vectorResource(id = R.drawable.ic_button_call), "call",
                Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onEvent(DialerContract.Event.OnCallButtonClicked) },
                            onLongPress = { onEvent(DialerContract.Event.OnCallButtonLongPressed) },
                        )
                    }
            )
        }

        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_btn_remove_digit),
            contentDescription = "remove",
            Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_remove_digit")
                .constrainAs(removeButton) {
                    start.linkTo(callButton.end, margin = 24.dp)
                    bottom.linkTo(parent.bottom, margin = 48.dp)
                    top.linkTo(entryNumberPanel.bottom, margin = 16.dp)
                }
                .size(height = 42.dp, width = 42.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { onEvent(DialerContract.Event.OnRemoveButtonClicked(longClicked = false)) },
                        onDoubleTap = { /* Called on Double Tap */ },
                        onLongPress = { onEvent(DialerContract.Event.OnRemoveButtonClicked(longClicked = true)) },
                        onTap = { /* Called on Tap */ }
                    )
                },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NumberEntryPanel(
    dialerTextField: String,
    onEvent: (event: UiEvent) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextField(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("edit_text_dialer_entered_number"),
            value = dialerTextField,
            onValueChange = { },
            readOnly = true,
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = colorResource(id = R.color.white),
                unfocusedBorderColor = colorResource(id = R.color.white),
                focusedLabelColor = colorResource(id = R.color.white)
            ),
            textStyle = TextStyle(fontFamily = FontFamily(Font(R.font.mtswide_medium)), fontSize = 32.sp, textAlign = TextAlign.Center)

        )
        DialerDigitButtons(
            onEvent = onEvent,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DialerDigitButtons(
    onEvent: (event: DialerContract.Event) -> Unit
) {
    val buttonLabels = remember { mutableStateListOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#") }
    ConstraintLayout(
        modifier = Modifier,
    ) {
        val (one, two, three, four, five, six, seven, eight, nine, star, zero, lattice) = createRefs()
        val horizontalPadding = 24.dp
        val verticalPadding = 16.dp
        val screenEdgePadding = 0.dp
        // 1
        OutlinedButton(
            onClick = { onEvent(DialerContract.Event.OnDigitButtonClicked(buttonLabels[1])) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_one")
                .constrainAs(one) {
                    start.linkTo(parent.start, margin = screenEdgePadding)
                    top.linkTo(parent.top)
                }
                .size(72.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
                border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))
        ) {
            Text(
                text = buttonLabels[1],
                color = colorResource(id = R.color.black),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.mtswide_medium)),
            )
        }
        // 2
        OutlinedButton(
            onClick = { onEvent(DialerContract.Event.OnDigitButtonClicked(buttonLabels[2])) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_two")
                .constrainAs(two) {
                    start.linkTo(one.end, margin = horizontalPadding)
                    top.linkTo(parent.top)
                }
                .size(72.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))
        ) {
            Text(
                text = buttonLabels[2],
                color = colorResource(id = R.color.black),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.mtswide_medium)),
            )
        }
        // 3
        OutlinedButton(
            onClick = { onEvent(DialerContract.Event.OnDigitButtonClicked(buttonLabels[3])) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_three")
                .constrainAs(three) {
                    start.linkTo(two.end, margin = horizontalPadding)
                    top.linkTo(parent.top)
                    end.linkTo(parent.end, margin = screenEdgePadding)
                }
                .size(72.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))

        ) {
            Text(
                text = buttonLabels[3],
                color = colorResource(id = R.color.black),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.mtswide_medium)),
            )
        }
        // 4
        OutlinedButton(
            onClick = { onEvent(DialerContract.Event.OnDigitButtonClicked(buttonLabels[4])) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_four")
                .constrainAs(four) {
                    start.linkTo(parent.start, margin = screenEdgePadding)
                    top.linkTo(one.bottom, margin = verticalPadding)
                }
                .size(72.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))
        ) {
            Text(
                text = buttonLabels[4],
                color = colorResource(id = R.color.black),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.mtswide_medium)),
            )
        }
        // 5
        OutlinedButton(
            onClick = { onEvent(DialerContract.Event.OnDigitButtonClicked(buttonLabels[5])) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_five")
                .constrainAs(five) {
                    start.linkTo(four.end, margin = horizontalPadding)
                    top.linkTo(two.bottom, margin = verticalPadding)
                }
                .size(72.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))
        ) {
            Text(
                text = buttonLabels[5],
                color = colorResource(id = R.color.black),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.mtswide_medium)),
            )
        }
        // 6
        OutlinedButton(
            onClick = { onEvent(DialerContract.Event.OnDigitButtonClicked(buttonLabels[6])) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_six")
                .constrainAs(six) {
                    start.linkTo(five.end, margin = horizontalPadding)
                    top.linkTo(three.bottom, margin = verticalPadding)
                    end.linkTo(parent.end, margin = screenEdgePadding)
                }
                .size(72.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))
        ) {
            Text(
                text = buttonLabels[6],
                color = colorResource(id = R.color.black),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.mtswide_medium)),
            )
        }
        // 7
        OutlinedButton(
            onClick = { onEvent(DialerContract.Event.OnDigitButtonClicked(buttonLabels[7])) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_seven")
                .constrainAs(seven) {
                    start.linkTo(parent.start, margin = screenEdgePadding)
                    top.linkTo(four.bottom, margin = verticalPadding)
                }
                .size(72.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))

        ) {
            Text(
                text = buttonLabels[7],
                color = colorResource(id = R.color.black),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.mtswide_medium)),
            )
        }
        // 8
        OutlinedButton(
            onClick = { onEvent(DialerContract.Event.OnDigitButtonClicked(buttonLabels[8])) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_eight")
                .constrainAs(eight) {
                    start.linkTo(seven.end, margin = horizontalPadding)
                    top.linkTo(five.bottom, margin = verticalPadding)
                }
                .size(72.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))
        ) {
            Text(
                text = buttonLabels[8],
                color = colorResource(id = R.color.black),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.mtswide_medium)),
            )
        }
        // 9
        OutlinedButton(
            onClick = { onEvent(DialerContract.Event.OnDigitButtonClicked(buttonLabels[9])) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_nine")
                .constrainAs(nine) {
                    start.linkTo(eight.end, margin = horizontalPadding)
                    top.linkTo(six.bottom, margin = verticalPadding)
                    end.linkTo(parent.end, margin = screenEdgePadding)
                }
                .size(72.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))
        ) {
            Text(
                text = buttonLabels[9],
                color = colorResource(id = R.color.black),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.mtswide_medium)),
            )
        }
        // *
        OutlinedButton(
            onClick = { onEvent(DialerContract.Event.OnDigitButtonClicked(buttonLabels[10])) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_asterisk")
                .constrainAs(star) {
                    start.linkTo(parent.start, margin = screenEdgePadding)
                    top.linkTo(seven.bottom, margin = verticalPadding)
                }
                .size(72.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))
        ) {
            Text(
                text = buttonLabels[10],
                color = colorResource(id = R.color.black),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.mtswide_medium)),
            )
        }
        // 0
        OutlinedButton(
            onClick = { onEvent(DialerContract.Event.OnDigitButtonClicked(buttonLabels[0])) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_zero")
                .constrainAs(zero) {
                    start.linkTo(star.end, margin = horizontalPadding)
                    top.linkTo(eight.bottom, margin = verticalPadding)
                }
                .size(72.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))
        ) {
            Text(
                text = buttonLabels[0],
                color = colorResource(id = R.color.black),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.mtswide_medium)),
            )
        }
        // #
        OutlinedButton(
            onClick = { onEvent(DialerContract.Event.OnDigitButtonClicked(buttonLabels[11])) },
            shape = CircleShape,
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_dialer_digit_hash")
                .constrainAs(lattice) {
                    start.linkTo(zero.end, margin = horizontalPadding)
                    top.linkTo(nine.bottom, margin = verticalPadding)
                    end.linkTo(parent.end, margin = screenEdgePadding)
                }
                .size(72.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(0.dp, colorResource(id = R.color.mts_bg_grey))
        ) {
            Text(
                text = buttonLabels[11],
                color = colorResource(id = R.color.black),
                fontSize = 32.sp,
                fontFamily = FontFamily(Font(R.font.mtswide_medium)),
            )
        }
    }
}


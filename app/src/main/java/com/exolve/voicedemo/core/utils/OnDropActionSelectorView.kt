package com.exolve.voicedemo.core.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.exolve.voicedemo.R
import com.exolve.voicedemo.core.telecom.TelecomManager
import com.exolve.voicedemo.features.call.CallContract.State

data class OnDropData(
    val first: State.CallItemState,
    val second: State.CallItemState)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OnDropActionSelectorView(data: OnDropData, modifier: Modifier, closeCallback: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        BoxWithConstraints(modifier.fillMaxWidth()) {
            var padding = 0.dp
            if (maxWidth >= 400.dp) {
                padding = 24.dp
            }

            ConstraintLayout(
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = padding)
                    .background(Color.White, shape = RoundedCornerShape(corner = CornerSize(8.dp)))
                    .border(
                        width = 0.5.dp,
                        color = Color.Gray,
                        shape = RoundedCornerShape(corner = CornerSize(8.dp))
                    )
            ) {
                val (
                    iconFirst,
                    textFirst,
                    spacer,
                    iconSecond,
                    textSecond,
                    buttonConference,
                    buttonTransfer
                ) = createRefs()

                Icon(Icons.Outlined.Person, contentDescription = "",
                    modifier = Modifier.size(17.dp)
                    .constrainAs(iconFirst) {
                        top.linkTo(parent.top, margin = 16.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                    }
                )

                val textStyle = TextStyle(
                    fontSize = 17.sp,
                    color = Color.Black,
                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                    //background = Color.Cyan
                )

                Text(
                    text = data.first.formattedNumber,
                    style = textStyle,
                    modifier = Modifier.constrainAs(textFirst) {
                        bottom.linkTo(iconFirst.bottom)
                        start.linkTo(iconFirst.end)
                    }
                )

                Icon(Icons.Outlined.Person, contentDescription = "",
                    modifier = Modifier.size(17.dp)
                    .constrainAs(iconSecond) {
                        top.linkTo(parent.top, margin = 16.dp)
                        end.linkTo(parent.end, margin = 16.dp)
                    }
                )

                Text(
                    text = data.second.formattedNumber,
                    style = textStyle,
                    modifier = Modifier.constrainAs(textSecond) {
                        bottom.linkTo(iconSecond.bottom)
                        end.linkTo(iconSecond.start)
                    }
                )

                Spacer(
                    modifier = Modifier.constrainAs(spacer) {
                        start.linkTo(textFirst.end)
                        end.linkTo(textSecond.start)
                    }
                )

                val bar = createBottomBarrier(iconFirst, textFirst, iconSecond, textSecond)

                OutlinedButton(
                    onClick = {
                        TelecomManager.getInstance()
                            .startConference(data.first.callsId, data.second.callsId)
                        closeCallback()
                    },
                    shape = RoundedCornerShape(corner = CornerSize(16.dp)),
                    border = BorderStroke(0.dp, Color.Transparent),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.mts_grey)),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 12.dp
                    ),
                    modifier = Modifier
                        .constrainAs(buttonConference) {
                        top.linkTo(bar, margin = 24.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                        end.linkTo(parent.end, margin = 16.dp)
                        }
                        .semantics { testTagsAsResourceId = true }
                        .testTag("button_create_a_conference")

                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.9f)) {
                        Text(
                            stringResource(id = R.string.calls_action_conference),
                            fontSize = 20.sp,
                            style = TextStyle(
                                //color = colorResource(id = R.color.white),
                                fontFamily = FontFamily(Font(R.font.mtscompact_medium)),
                            ),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            stringResource(id = R.string.calls_action_conference_hint),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        TelecomManager.getInstance()
                            .transferToCall(data.first.callsId, data.second.callsId)
                        closeCallback()
                    },
                    shape = RoundedCornerShape(corner = CornerSize(16.dp)),
                    border = BorderStroke(0.dp, Color.Transparent),
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.mts_grey)),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 12.dp
                    ),
                    modifier = Modifier
                        .constrainAs(buttonTransfer) {
                        top.linkTo(buttonConference.bottom, margin = 16.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                        end.linkTo(parent.end, margin = 16.dp)
                        bottom.linkTo(parent.bottom, margin = 16.dp)
                        }
                        .semantics { testTagsAsResourceId = true }
                        .testTag("button_transfer_call")
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.9f)) {
                        Text(
                            stringResource(id = R.string.calls_action_transfer),
                            fontSize = 20.sp,
                            style = TextStyle(
                                fontFamily = FontFamily(Font(R.font.mtscompact_medium)),
                            ),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            stringResource(id = R.string.calls_action_transfer_hint),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}
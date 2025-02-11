package com.exolve.voicedemo.features.call

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.exolve.voicedemo.core.uiCommons.interfaces.UiEvent
import com.exolve.voicedemo.features.dialer.NumberEntryPanel
import com.exolve.voicedemo.features.call.CallContract.State.CallItemState
import com.exolve.voicedemo.R
import com.exolve.voicedemo.core.utils.OnDropActionSelectorView
import com.exolve.voicesdk.AudioRouteData
import com.exolve.voicesdk.CallState
import com.exolve.voicesdk.AudioRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val CALL_SCREEN = "CallScreen"
@Composable
fun OngoingCallScreen(
    ongoingCallViewModel: CallViewModel,
    onEvent: (event: UiEvent) -> Unit
) {
    val state =  ongoingCallViewModel.uiState.collectAsState()
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (
            controlPanel,
            numberEntryPanel,
            callsInformationPanel,
        ) = createRefs()
        AnimatedVisibility(
            visible = state.value.isDtmfPressed,
            modifier = Modifier.constrainAs(numberEntryPanel) {
                bottom.linkTo(controlPanel.top, margin = 24.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            enter = slideInVertically { 3000 },
            exit = slideOutVertically { 3000 }
        ) {
            DtmfView(state.value.dialerText, onEvent)
        }
        AnimatedVisibility(
            visible = !state.value.isDtmfPressed,
            enter = slideInVertically { -3000 },
            exit = slideOutVertically { -3000 },
            modifier = Modifier
                .padding(horizontal = dimensionResource(id = R.dimen.margin_horizontal_big))
                .constrainAs(callsInformationPanel) {
                    top.linkTo(parent.top)
                    bottom.linkTo(controlPanel.top)
                    end.linkTo(parent.end)
                    start.linkTo(parent.start)
                    height = Dimension.fillToConstraints
                }
                .fillMaxHeight(),

        ) {
            CallsInformationPanel(
                onEvent = onEvent,
                callsList = state.value.calls,
                hasConference = state.value.hasConference
            )
        }
        ControlPanel(
            modifier = Modifier
                .constrainAs(controlPanel) {
                    bottom.linkTo(parent.bottom, margin = 28.dp)
                    start.linkTo(parent.start, margin = 56.dp)
                    end.linkTo(parent.end, margin = 56.dp)
                },
            onEvent = onEvent,
            isMuted = isMuted(state),
            currentCallId = state.value.currentCallId,
            state.value.audioRoutes,
            state.value.selectedAudioRoute
        )
        if (state.value.onDropData != null) {
            if (!state.value.calls.contains(state.value.onDropData!!.first)
                || !state.value.calls.contains(state.value.onDropData!!.second)) {
                onEvent(CallContract.Event.OnReleaseDropData)
            }
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
                .zIndex(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { onEvent(CallContract.Event.OnReleaseDropData) },
                    )
                }
            ) {
                OnDropActionSelectorView(
                    data = state.value.onDropData!!,
                    modifier = Modifier.align(Alignment.Center)
                ) { onEvent(CallContract.Event.OnReleaseDropData) }
            }
        }

    }
}

fun isMuted(state: State<CallContract.State>): Boolean {
    if (!state.value.hasConference) {
        val callId = state.value.currentCallId
        val call = state.value.calls.find { it.callsId == callId }
        return call?.let { call.isMuted}?: false
    }

    val conferenceCalls = state.value.calls.filter { it.isInConference }
    return conferenceCalls.fold(false) { mu, call -> mu || call.isMuted }
}

@Composable
private fun DtmfView(
    dialerTextField: String,
    onEvent: (event: UiEvent) -> Unit
) {
    Column {
        NumberEntryPanel(
            dialerTextField,
            onEvent = onEvent,
            modifier = Modifier,
        )
    }
}

@Composable
private fun CallsInformationPanel(
    hasConference: Boolean,
    callsList: List<CallItemState>,
    onEvent: (event: UiEvent) -> Unit,
 ) {
    Column(
        modifier = Modifier
            .padding(vertical = 24.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CurrentCallTextField(
                calls = callsList,
                hasConference = hasConference
            )
            CallLineList(
                onEvent = onEvent,
                list = callsList,
                hasConference = hasConference
            )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun CurrentCallTextField(
    calls: List<CallItemState>,
    hasConference: Boolean,
) {
    Text(
        text = if (hasConference) {
            stringResource(id = R.string.call_screen_current_conference) + StringBuilder().apply {
                calls.forEach { call ->
                    call.takeIf { it.isInConference }
                        ?.also {conferenceCall ->
                            this.append(
                                " ",
                                conferenceCall.formattedNumber,
                                ","
                            )
                        }
                }
                deleteCharAt(lastIndex)
            }.toString()
        } else if(calls.isNotEmpty()) {
            if(calls.find{ it.status == CallState.LOST_CONNECTION } != null){
                stringResource(id = R.string.call_screen_current_no_connection)
            } else {
                calls.find {
                    it.status == CallState.CONNECTED ||
                            it.status == CallState.ERROR ||
                            it.status == CallState.NEW
                }?.formattedNumber
                    ?: stringResource(id = R.string.call_screen_current_all_on_hold)
            }
        } else {
               stringResource(id = R.string.call_screen_current_call_ended)
        },
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTagsAsResourceId = true }
            .testTag("text_view_callscreen_current_number")
            .basicMarquee(),
        fontFamily = FontFamily(Font(R.font.mtswide_bold)),
        fontSize = 32.sp,
        style = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center)
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CallLineList(
    onEvent: (event: UiEvent) -> Unit,
    list: List<CallItemState>,
    hasConference: Boolean,
) {
    val listState: LazyListState = rememberLazyListState()
    val rememberedListState = rememberUpdatedState(newValue = listState)
    var draggedDistance by remember {mutableStateOf(0f)}
    var actualIndexOfDraggedItem by remember { mutableStateOf<Int?>(null) }
    var isUnderTheAreaOfItem by remember { mutableStateOf(false) }
    var hoveredOn by remember { mutableStateOf< LazyListItemInfo?>(null) }
    var overScrollJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val currentList = rememberUpdatedState(newValue = list)

    Log.d(CALL_SCREEN, "has conference = $hasConference")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(items = list) { index, item ->
            Log.d(CALL_SCREEN, "item.inconf = ${item.isInConference}")
            if (item.isInConference) {
                ConferenceLineItem(
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag("conference_item_callscreen_${item.indexForUiTest}")
                        .fillMaxWidth()
                        .graphicsLayer(translationY = draggedDistance.takeIf {
                            index == actualIndexOfDraggedItem
                        } ?: 0f
                        ),
                    item = list[index],
                    onEvent = onEvent,
                )
            }
        }
    }
    LazyColumn(
        state = rememberedListState.value,
        verticalArrangement = Arrangement.spacedBy( 1.dp),
        modifier = Modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        Log.d(
                            CALL_SCREEN,
                            "onDragStart ${rememberedListState.value.layoutInfo.visibleItemsInfo.size}"
                        )
                        rememberedListState.value.layoutInfo.visibleItemsInfo
                            .takeIf { currentList.value.find { it.isInConference } == null && currentList.value.size > 1 }
                            ?.firstOrNull {
                                offset.y.toInt() in it.offset..it.offset + it.size
                            }
                            ?.also {
                                actualIndexOfDraggedItem = it.index
                                Log.d(
                                    CALL_SCREEN,
                                    "indexOfDraggedItem = it.index = ${it.index}"
                                )
                            }
                        Log.d(
                            CALL_SCREEN,
                            "onDragStart, rememberedDraggedIndex = $actualIndexOfDraggedItem and " +
                                    "firstVisibleItemIndex is ${rememberedListState.value.firstVisibleItemIndex} "
                        )
                    },
                    onDrag = { change, dragAmount ->
                        rememberedListState.value.layoutInfo.visibleItemsInfo
                            .getOrNull(
                                (actualIndexOfDraggedItem
                                    ?: 0) - rememberedListState.value.firstVisibleItemIndex
                            )
                            ?.run {
                                val startOffset = offset + draggedDistance
                                val endOffset = offset + size + draggedDistance
                                hoveredOn = null
                                isUnderTheAreaOfItem = false

                                rememberedListState.value.layoutInfo.visibleItemsInfo
                                    .onEach { item ->
                                        Log.d(
                                            CALL_SCREEN,
                                            "onDrag: actual = $actualIndexOfDraggedItem " +
                                                    "for it.index = ${item.index} abs = ${
                                                        (abs(
                                                            startOffset - item.offset
                                                        ))
                                                    } < ${(item.size / 2)} " +
                                                    "&& item{ it.index != actualIndexOfDraggedItem }"
                                        )
                                        if ((abs(startOffset - item.offset) < item.size / 2) && item.index != actualIndexOfDraggedItem) {
                                            hoveredOn = item
                                            isUnderTheAreaOfItem = true
                                            Log.d(
                                                CALL_SCREEN,
                                                "hovered on ${hoveredOn?.index}"
                                            )
                                        }
                                    }
                                if (overScrollJob?.isActive == true) return@detectDragGesturesAfterLongPress
                                checkOverseasOfList(
                                    draggedDistance,
                                    endOffset,
                                    rememberedListState.value,
                                    startOffset
                                )
                                    .takeIf { it != 0f }
                                    ?.let {
                                        overScrollJob =
                                            scope.launch { rememberedListState.value.scrollBy(it) }
                                    }
                                    ?: run { overScrollJob?.cancel() }
                                Log.d(
                                    CALL_SCREEN,
                                    "rememberedIsUnderTheAreaOfItem = ${isUnderTheAreaOfItem}, rememberedHoveredOn = ${hoveredOn?.index}"
                                )

                                checkOverseasOfList(
                                    draggedDistance,
                                    endOffset,
                                    rememberedListState.value,
                                    startOffset
                                )
                                    .takeIf { it != 0f }
                                    ?.also {
                                        Log.d(
                                            CALL_SCREEN,
                                            "onDrag: ,checkOverseas when drag draggedDistance = $draggedDistance, borderoffset = $it"
                                        )
                                        draggedDistance -= it
                                    }
                            }
                        change.consume()
                        draggedDistance = draggedDistance.plus(dragAmount.y)
                        // Start autoscrolling if position is out of bounds
                    },
                    onDragEnd = {
                        if (hoveredOn != null) {
                            Log.d(
                                CALL_SCREEN,
                                "onDragEnd isDropOnElement = ${isUnderTheAreaOfItem}, drop on element = ${hoveredOn?.index}"
                            )
                            onEvent(
                                CallContract.Event.OnItemDroppedOnItem(
                                    actualIndexOfDraggedItem,
                                    hoveredOn?.index
                                )
                            )
                        } else {
                            Log.d(CALL_SCREEN, "onDragEnd")
                        }
                        draggedDistance = 0f
                        actualIndexOfDraggedItem = null
                    },
                )
            },
    ) {

        itemsIndexed(items = list) { index, item ->
            if (!item.isInConference) {
                CallLineItem(
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag("callscreen_list_item_${item.indexForUiTest}")
                        .padding(vertical = 8.dp)
                        .zIndex(if (index == actualIndexOfDraggedItem) 1f else 0f)
                        .fillMaxWidth()
                        .graphicsLayer(translationY = draggedDistance.takeIf { index == actualIndexOfDraggedItem }
                            ?: 0f)
                        .shadow(elevation = if (index == actualIndexOfDraggedItem) 16.dp else 0.dp),

                    item = list[index],
                    onEvent = onEvent,
                    hasConference = hasConference,
                    conferenceSize = list.filter { it.isInConference }.size
                )
            }
        }
    }
}

private fun checkOverseasOfList(
    draggedDistance: Float,
    endOffset: Float,
    listState: LazyListState,
    startOffset: Float
) = when {
    draggedDistance > 0 -> {
        (endOffset - listState.layoutInfo.viewportEndOffset).takeIf { it > 0 }
    }
    draggedDistance < 0 -> {
        (startOffset - listState.layoutInfo.viewportStartOffset).takeIf { it < 0 }
    }
    else -> 0f
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CallLineItem(
    modifier: Modifier,
    onEvent: (event: UiEvent) -> Unit,
    item: CallItemState,
    hasConference: Boolean,
    conferenceSize: Int,
) {
    val color = colorResource(id = R.color.mts_bg_grey)
    Card(
        colors = CardColors(
            contentColor = color,
            containerColor = color,
            disabledContainerColor = color,
            disabledContentColor = color
        ),
        modifier = modifier
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    item.formattedNumber + " ${item.status.toString().lowercase()}",
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag("text_view_callscreen_list_item_number_${item.indexForUiTest}")
                        .padding(vertical = 8.dp)
                        .weight(1f),
                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                    fontSize = 17.sp,
                    color = colorResource(id = R.color.call_card_number_text)
                )
                if (item.status == CallState.LOST_CONNECTION) {
                    Image(
                            painter = painterResource(id = R.drawable.no_wifi_icon),
                            contentDescription = "Lost connection",
                            modifier = Modifier.size(19.dp)
                                .align(Alignment.CenterVertically),
                        )
                }
                if (item.status == CallState.CONNECTED) {
                    //Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier.size(10.dp)
                            .align(Alignment.CenterVertically)
                            .clip(CircleShape)
                            .background(
                                when {
                                    item.qualityRating > 4.2 -> {
                                        Color.Green
                                    }
                                    item.qualityRating > 2.5 -> {
                                        Color.Yellow
                                    }
                                    item.qualityRating >= 0.0 -> {
                                        Color.Red
                                    }
                                    else -> Color.Transparent
                                }
                            )
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (item.isActive()) {
                    Text(
                        text = formatDuration(item.duration),
                        fontSize = 16.sp,
                        style = TextStyle(fontFamily = FontFamily(Font(R.font.mtscompact_regular))),
                        color = Color.Gray
                    )
                }
                Spacer(Modifier.weight(1f))
                when {
                    // On Hold
                    item.status == CallState.ON_HOLD -> {
                        val newColor = colorResource(id = R.color.mts_bg_grey)
                        if (hasConference && (conferenceSize < 5)) ButtonAddToConference(onEvent = onEvent, item = item)
                        ButtonTerminateSelectedRegularCall(onEvent = onEvent, item = item)
                        OutlinedButton(
                            onClick = { onEvent(CallContract.Event.OnResumeButtonClicked(item.callsId)) },
                            modifier = Modifier
                                .semantics { testTagsAsResourceId = true }
                                .testTag("button_callscreen_list_item_resume_${item.indexForUiTest}"),
                            border = BorderStroke(1.dp, colorResource(id = R.color.mts_bg_grey)),
                            colors = ButtonColors(
                                containerColor = newColor,
                                contentColor = newColor,
                                disabledContainerColor = newColor,
                                disabledContentColor = newColor
                            ),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_call),
                                contentDescription = "Resume button",
                                tint = colorResource(id = R.color.call_card_button_accept_mts_green),
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(id = R.string.calls_card_resume_call),
                                Modifier.padding(horizontal = 8.dp),
                                style = TextStyle(
                                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                                    color = colorResource(id = R.color.mts_text_grey),
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                    // Incoming call
                    (item.status != CallState.CONNECTED) and
                    (item.status != CallState.LOST_CONNECTION) and
                    !item.isCallOutgoing -> {
                        val newColor = colorResource(id = R.color.mts_bg_grey)
                        OutlinedButton(
                            onClick = { onEvent(CallContract.Event.OnAcceptCallButtonClicked(item.callsId)) },
                            //shape = CircleShape,
                            modifier = Modifier
                                .semantics { testTagsAsResourceId = true }
                                .testTag("button_callscreen_list_item_accept_call_${item.indexForUiTest}"),
                            border = BorderStroke(1.dp, colorResource(id = R.color.mts_bg_grey)),
                            colors = ButtonColors(
                                containerColor = newColor,
                                contentColor = newColor,
                                disabledContainerColor = newColor,
                                disabledContentColor = newColor
                            ),

                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_call),
                                contentDescription = "Accept button",
                                tint = colorResource(id = R.color.call_card_button_accept_mts_green),
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(id = R.string.calls_card_accet_call),
                                Modifier.padding(horizontal = 8.dp),
                                style = TextStyle(
                                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                                    color = colorResource(id = R.color.mts_text_grey),
                                    fontSize = 14.sp
                                )
                            )

                        }
                        ButtonTerminateSelectedRegularCall(onEvent = onEvent, item = item)
                    }
                    // Ongoing call, resumed
                    (item.status == CallState.CONNECTED) -> {
                        val newColor = colorResource(id = R.color.mts_bg_grey)
                        ButtonTerminateSelectedRegularCall(onEvent = onEvent, item = item)
                        OutlinedButton(
                            onClick = { onEvent(CallContract.Event.OnHoldButtonClicked(item.callsId)) },
                            //shape = CircleShape,
                            modifier = Modifier
                                .semantics { testTagsAsResourceId = true }
                                .testTag("button_callscreen_list_item_hold_${item.indexForUiTest}"),
                            border = BorderStroke(1.dp, colorResource(id = R.color.mts_bg_grey)),
                            colors = ButtonColors(
                                containerColor = newColor,
                                contentColor = newColor,
                                disabledContainerColor = newColor,
                                disabledContentColor = newColor
                            ),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_hold_call_basic),
                                contentDescription = "Hold button",
                                tint = colorResource(id = R.color.call_card_button_accept_mts_green),
                                modifier = Modifier.size(22.dp),

                            )
                            Text(
                                text = "Hold", Modifier.padding(horizontal = 8.dp),
                                style = TextStyle(
                                    fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                                    color = colorResource(id = R.color.mts_text_grey),
                                    fontSize = 14.sp
                                )
                            )

                        }
                    }
                    else -> ButtonTerminateSelectedRegularCall(onEvent, item)
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConferenceLineItem(
    modifier: Modifier,
    onEvent: (event: UiEvent) -> Unit,
    item: CallItemState
) {
    val color = colorResource(id = R.color.mts_bg_grey)
    Card(
        colors = CardColors(
            contentColor = color,
            containerColor = color,
            disabledContainerColor = color,
            disabledContentColor = color
        ),
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
    ) {
        colorResource(id = R.color.mts_bg_grey)//if (!item.isInConference) colorResource(id = R.color.mts_bg_grey) else colorResource(id = R.color.mts_text_grey)
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 0.dp)) {
            Text(
                item.formattedNumber,
                modifier = Modifier
                    .semantics { testTagsAsResourceId = true }
                    .testTag("text_view_callscreen_list_item_number_${item.indexForUiTest}")
                    .padding(vertical = 8.dp),
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                fontSize = 17.sp,
                color = colorResource(id = R.color.mts_text_grey)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                ButtonRemoveFromConference(onEvent = onEvent, item = item )
                ButtonTerminateSelectedConferenceCall(onEvent, item)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ButtonTerminateSelectedRegularCall(
    onEvent: (event: UiEvent) -> Unit,
    item: CallItemState
) {
    val color = colorResource(id = R.color.mts_bg_grey)//if (!item.isInConference) colorResource(id = R.color.mts_bg_grey) else colorResource(id = R.color.mts_text_grey)
    OutlinedButton(
        onClick = { onEvent(CallContract.Event.OnTerminateCurrentButtonClicked(item.callsId)) },
        //shape = CircleShape,
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("button_callscreen_list_item_terminate_selected_call_${item.indexForUiTest}"),
        border = BorderStroke(1.dp, color),
        colors = ButtonColors(
            containerColor = color,
            contentColor = color,
            disabledContainerColor = color,
            disabledContentColor = color
        ),
        contentPadding = PaddingValues(0.dp),

        ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_terminate_call),
            contentDescription = "Terminate button",
            tint = colorResource(id = R.color.mts_red),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "Terminate", Modifier.padding(horizontal = 8.dp),
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                color = colorResource(id = R.color.mts_text_grey),
                fontSize = 14.sp
            )
        )
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ButtonTerminateSelectedConferenceCall(
    onEvent: (event: UiEvent) -> Unit,
    item: CallItemState
) {
    colorResource(id = R.color.mts_bg_grey)//if (!item.isInConference) colorResource(id = R.color.mts_bg_grey) else colorResource(id = R.color.mts_text_grey)
    IconButton(
        onClick = { onEvent(CallContract.Event.OnTerminateCurrentButtonClicked(item.callsId)) },
        //shape = CircleShape,
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("button_callscreen_list_item_terminate_selected_conference_call__${item.indexForUiTest}"),
        ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_terminate_call),
            contentDescription = "Terminate button",
            tint = colorResource(id = R.color.mts_red),
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ButtonRemoveFromConference(
    onEvent: (event: UiEvent) -> Unit,
    item: CallItemState
) {
    IconButton(
        onClick = { onEvent(CallContract.Event.OnRemoveCallFromConference(item.callsId)) },
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("button_callscreen_list_item_remove_from_conference_${item.indexForUiTest}"),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_remove_from_conference),
            contentDescription = "Remove from conference",
            tint = colorResource(id = R.color.black),
            modifier = Modifier.size(20.dp)
        )
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ButtonAddToConference(
    onEvent: (event: UiEvent) -> Unit,
    item: CallItemState
) {
    colorResource(id = R.color.mts_bg_grey)
    IconButton(
        onClick = { onEvent(CallContract.Event.OnAddCallToConference(item.callsId)) },
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("button_callscreen_list_item_remove_from_conference_${item.indexForUiTest}"),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_add_call_to_conference),
            contentDescription = "Remove from conference",
            tint = colorResource(id = R.color.black),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ControlPanel(
    modifier: Modifier,
    onEvent: (event: UiEvent) -> Unit,
    isMuted: Boolean,
    currentCallId: String,
    routes: List<AudioRouteData>,
    selectedRoute: AudioRoute
) {
    ConstraintLayout(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(colorResource(id = R.color.white))

    ) {
        val (
            endCallButton,
            speakerButton,
            dtmfButton,
            muteButton,
            addCallButton,
            transferButton,
        ) = createRefs()
        // Mute
        ButtonMute(
            onEvent = onEvent,
            isMuted = isMuted,
            modifier = Modifier.constrainAs(muteButton) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
            },
        )
        // Dtmf
        ButtonDtmf(
            Modifier.constrainAs(dtmfButton) {
                top.linkTo(parent.top)
                start.linkTo(muteButton.end, margin = 24.dp)
            },
            onEvent,
        )
        // Speaker
        ButtonSpeaker(
            Modifier.constrainAs(speakerButton) {
                top.linkTo(parent.top)
                start.linkTo(dtmfButton.end, margin = 24.dp)
            },
            routes,
            onEvent,
            selectedRoute == AudioRoute.UNKNOWN
        )
        // New Call add
        ButtonNewCall(
            Modifier.constrainAs(addCallButton) {
                top.linkTo(muteButton.bottom, margin = 16.dp)
                start.linkTo(parent.start)
            },
            onEvent,
        )
        // Terminate
        ButtonTerminate(
            Modifier.constrainAs(endCallButton) {
                top.linkTo(dtmfButton.bottom, margin = 16.dp)
                start.linkTo(addCallButton.end, margin = 24.dp)
            },
            onEvent
        )
        // Transfer
        ButtonTransfer(
            Modifier.constrainAs(transferButton) {
                top.linkTo(speakerButton.bottom, margin = 16.dp)
                start.linkTo(endCallButton.end, margin = 24.dp)
            },
            onEvent,
            currentCallId,
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun ButtonTransfer(
    modifier: Modifier,
    onEvent: (event: UiEvent) -> Unit,
    currentCallId: String,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = { onEvent(CallContract.Event.OnTransferButtonClicked(currentCallId)) },
            Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_callscreen_transfer")
                .size(72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(1.dp, colorResource(id = R.color.mts_bg_grey)),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_transfer),
                tint = Color.Black,
                contentDescription = "Transfer",
                modifier = Modifier.size(width = 26.dp, height = 28.dp),
            )
        }
        Text(
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                color = colorResource(id = R.color.call_control_button_mts)
            ),
            text = stringResource(id = R.string.button_transfer_call)
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun ButtonTerminate(
    modifier: Modifier,
    onEvent: (event: UiEvent) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = { onEvent(CallContract.Event.OnTerminateAllButtonClicked) },
            Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_callscreen_terminate_all")
                .size(72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(colorResource(id = R.color.mts_red)),
            border = BorderStroke(1.dp, colorResource(id = R.color.mts_bg_grey)),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_end_calls),
                tint = colorResource(id = R.color.white),
                contentDescription = "Terminate",
                modifier = Modifier.size(width = 37.dp, height = 18.dp),
            )
        }
        Text(
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                color = colorResource(id = R.color.call_control_button_mts)
            ),
            text = stringResource(id = R.string.button_end_call)
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun ButtonNewCall(
    modifier: Modifier,
    onEvent: (event: UiEvent) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = { onEvent(CallContract.Event.OnNewCallButtonClicked) },
            Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_callscreen_add_new_call")
                .size(72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(1.dp, colorResource(id = R.color.mts_bg_grey)),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_new_call),
                tint = Color.Black,
                contentDescription = "New Call",
                modifier = Modifier.size(width = 28.dp, height = 28.dp),
            )
        }
        Text(
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                color = colorResource(id = R.color.call_control_button_mts)
            ),
            text = stringResource(id = R.string.button_new_call_add)
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun ButtonSpeaker(
    modifier: Modifier,
    routes: List<AudioRouteData>,
    onEvent: (event: UiEvent) -> Unit,
    enabled: Boolean
) {
    var showRouteSelector by remember { mutableStateOf(false) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var iconId = R.drawable.ic_route_earpiece
        val activeRoute = routes.firstOrNull { it.isActive }
        if (activeRoute != null) {
            Log.d(CALL_SCREEN, "active route ${activeRoute.name}")
            iconId = when (activeRoute.route) {
                AudioRoute.SPEAKER -> R.drawable.ic_route_speaker
                AudioRoute.HEADSET -> R.drawable.ic_route_headset
                AudioRoute.HEADPHONES -> R.drawable.ic_route_headphones
                AudioRoute.BLUETOOTH -> R.drawable.ic_route_bluetooth
                else -> R.drawable.ic_route_earpiece
            }
        } else {
            Log.d(CALL_SCREEN, "no active route")
        }

        if (routes.size <= 2) {
            showRouteSelector = false
        }

        OutlinedButton(
            onClick = {
                onEvent(CallContract.Event.OnSpeakerButtonClicked)
                showRouteSelector = routes.size > 2
            },
            Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_callscreen_speaker")
                .size(72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(1.dp, colorResource(id = R.color.mts_bg_grey)),
            enabled = enabled
        ) {
            Icon(
                painter = painterResource(id = iconId),
                tint = Color.Black,
                contentDescription = "Speaker",
                modifier = Modifier.size(width = 26.dp, height = 28.dp),
            )
        }
        Text(
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.mtscompact_regular)),
                color = colorResource(id = R.color.call_control_button_mts)
            ),
            text = stringResource(id = R.string.button_speaker)
        )
        DropdownMenu(expanded = showRouteSelector,
            onDismissRequest = { showRouteSelector = false }
        ) {
                routes.forEach {
                    DropdownMenuItem(
                        text = { Text(it.name) },
                        onClick = {
                            if (!it.isActive) {
                                onEvent(CallContract.Event.OnAudioRouteSelect(it.route))
                            }
                            showRouteSelector = false
                        },
                        leadingIcon = {
                            val (icon, description) = when (it.route) {
                                AudioRoute.SPEAKER -> Pair(R.drawable.ic_route_speaker, stringResource(com.exolve.voicesdk.R.string.audio_route_speaker))
                                AudioRoute.HEADSET -> Pair(R.drawable.ic_route_headset, stringResource(com.exolve.voicesdk.R.string.audio_route_headset))
                                AudioRoute.HEADPHONES -> Pair(R.drawable.ic_route_headphones, stringResource(com.exolve.voicesdk.R.string.audio_route_headphones))
                                AudioRoute.BLUETOOTH -> Pair(R.drawable.ic_route_bluetooth, "Bluetooth")
                                else -> Pair(R.drawable.ic_route_earpiece, stringResource(com.exolve.voicesdk.R.string.audio_route_earpiece))
                            }
                            Icon(
                                painter = painterResource(id = icon),
                                contentDescription = description,
                                modifier = Modifier.size(width = 30.dp, height = 31.dp),
                            )
                        },
                        trailingIcon = {
                            if (it.isActive) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_route_current),
                                    contentDescription = "Active Route",
                                    modifier = Modifier.size(width = 30.dp, height = 31.dp),
                                )
                            }
                        }
                    )
                }
            }
        }
    }


@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun ButtonDtmf(modifier: Modifier, onEvent: (event: UiEvent) -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(
            onClick = { onEvent(CallContract.Event.OnDtmfButtonClicked) },
            Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_callscreen_dtmf")
                .size(72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(1.dp, colorResource(id = R.color.mts_bg_grey)),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_dtmf_call),
                tint = Color.Black,
                contentDescription = "DTMF",
                modifier = Modifier.size(width = 30.dp, height = 31.dp),
            )
        }
        Text(
            style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily(Font(R.font.mtscompact_regular)), color = colorResource(id = R.color.call_control_button_mts)),
            text = stringResource(id = R.string.button_dtmf)
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun ButtonMute(
    modifier: Modifier,
    onEvent: (event: UiEvent) -> Unit,
    isMuted: Boolean
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedButton(
            onClick = { onEvent(CallContract.Event.OnMuteButtonClicked) },
            Modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("button_callscreen_mute")
                .size(72.dp),
            shape = CircleShape,
            colors = if(isMuted)ButtonDefaults.buttonColors(colorResource(id = R.color.mts_grey))
            else ButtonDefaults.buttonColors(colorResource(id = R.color.mts_bg_grey)),
            border = BorderStroke(1.dp, colorResource(id = R.color.mts_bg_grey)),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_mute_call),
                contentDescription = "Mute",
                modifier = Modifier.size(width = 28.dp, height = 36.dp),
                tint = if (isMuted) colorResource(id = R.color.mts_red)
                    else colorResource(id = R.color.black)
            )
        }
        Text(
            style = TextStyle(fontSize = 12.sp, fontFamily = FontFamily(Font(R.font.mtscompact_regular)), color = colorResource(id = R.color.call_control_button_mts)),
            text = stringResource(id = R.string.button_mute_call)
        )
    }
}

private fun formatDuration(duration: UInt): String  {
    val t = duration.toInt()
    if (t == 0) {
        return "00:00"
    }

    val h = t / 3600
    val m = (t - h * 3600) / 60
    val s = t % 60
    if (h == 0) {
        return String.format("%02d:%02d", m, s)
    }

    return String.format("%02d:%02d:%02d", h, m, s)
}

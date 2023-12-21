package com.exolve.voicedemo.core.uiCommons

class ActivitiesInstantiatorContract {
    data class State(
        val hasActivityInForeground: Boolean = false,
        val isCallsActivityInForeground: Boolean = false,
    )

    sealed class Event {
        object SomeActivityInForeground : Event()
        object AllActivitiesInBackground : Event()
        object CallActivityMustFinished : Event()
    }
}
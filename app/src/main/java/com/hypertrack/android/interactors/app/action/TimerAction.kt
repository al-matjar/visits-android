package com.hypertrack.android.interactors.app.action

import com.hypertrack.android.interactors.app.Timer
import kotlinx.coroutines.Job

sealed class TimerAction
data class TimerStartedAction(val timer: Timer, val job: Job) : TimerAction()
data class TimerEndedAction(val timer: Timer) : TimerAction()

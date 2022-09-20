package com.hypertrack.android.interactors.app.reducer

import com.hypertrack.android.interactors.app.AppEffect
import com.hypertrack.android.interactors.app.AppEventEffect
import com.hypertrack.android.interactors.app.Timer
import com.hypertrack.android.interactors.app.action.TimerAction
import com.hypertrack.android.interactors.app.action.TimerEndedAction
import com.hypertrack.android.interactors.app.action.TimerStartedAction
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.withEffects
import kotlinx.coroutines.Job

class TimerReducer {

    fun reduce(action: TimerAction, state: AppState): ReducerResult<out AppState, out AppEffect> {
        return when (state) {
            is AppInitialized -> {
                state.copy(timerJobs = reduce(action, state.timerJobs)).withEffects()
            }
            is AppNotInitialized -> {
                state.copy(timerJobs = reduce(action, state.timerJobs)).withEffects()
            }
        }
    }

    private fun reduce(action: TimerAction, jobs: Map<Timer, Job>): Map<Timer, Job> {
        return when (action) {
            is TimerStartedAction -> {
                jobs + (action.timer to action.job)
            }
            is TimerEndedAction -> {
                jobs - (action.timer)
            }
        }
    }

}

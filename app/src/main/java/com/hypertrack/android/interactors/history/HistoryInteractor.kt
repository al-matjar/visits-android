package com.hypertrack.android.interactors.history

import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.api.graphql.DayRange
import com.hypertrack.android.api.graphql.GraphQlApiClient
import com.hypertrack.android.api.graphql.models.GraphQlHistory
import com.hypertrack.android.models.local.LocalHistory
import com.hypertrack.android.ui.common.delegates.address.GraphQlGeofenceVisitAddressDelegate
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.DeviceId
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Loading
import com.hypertrack.android.utils.LoadingFailure
import com.hypertrack.android.utils.LoadingSuccess
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.ReducerResult
import com.hypertrack.android.utils.StateMachine
import com.hypertrack.android.utils.Success
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.CoroutineContext

interface HistoryInteractor {
    val history: MutableLiveData<HistoryState>
    fun loadHistory(date: LocalDate)
}

class GraphQlHistoryInteractor(
    private val deviceId: DeviceId,
    private val graphQlApiClient: GraphQlApiClient,
    private val osUtilsProvider: OsUtilsProvider,
    private val crashReportsProvider: CrashReportsProvider,
    private val moshi: Moshi,
    private val scope: CoroutineScope,
    private val stateMachineContext: CoroutineContext,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : HistoryInteractor {

    private val visitAddressDelegate = GraphQlGeofenceVisitAddressDelegate(osUtilsProvider)

    override val history = MutableLiveData<HistoryState>(HistoryState(mapOf()))

    private val stateMachine = StateMachine<Action, HistoryState, Effect>(
        javaClass.simpleName,
        HistoryState(mapOf()),
        scope,
        stateMachineContext,
        this::reduce,
        this::applyEffects,
        this::stateChangeEffects
    )

    override fun loadHistory(date: LocalDate) {
        stateMachine.handleAction(StartDayHistoryLoading(date))
    }

    private fun applyEffects(effects: Set<Effect>) {
        effects.forEach { effect ->
            when (effect) {
                is LoadHistoryEffect -> {
                    scope.launch {
                        graphQlApiClient.getHistoryForDay(
                            DayRange(effect.date, zoneId)
                        ).let { result ->
                            when (result) {
                                is Success -> stateMachine.handleAction(
                                    DayHistoryLoadedAction(
                                        effect.date,
                                        createLocalHistory(effect, result)
                                    )
                                )
                                is Failure -> stateMachine.handleAction(
                                    DayHistoryErrorAction(
                                        effect.date,
                                        result.exception
                                    )
                                )
                            } as Any?
                        }
                    } as Any?
                }
                is UpdateHistoryStateEffect -> {
                    history.postValue(effect.historyState)
                }
            } as Any?
        }
    }

    private fun reduce(
        oldState: HistoryState,
        action: Action
    ): ReducerResult<HistoryState, Effect> {
        return when (action) {
            is StartDayHistoryLoading -> {
                oldState
                    .withDay(action.day, Loading())
                    .withEffects(LoadHistoryEffect(action.day))
            }
            is DayHistoryLoadedAction -> {
                oldState
                    .withDay(action.day, LoadingSuccess(action.history))
                    .asReducerResult()
            }
            is DayHistoryErrorAction -> {
                oldState
                    .withDay(action.day, LoadingFailure(action.exception))
                    .asReducerResult()
            }
        }
    }

    private fun stateChangeEffects(newState: HistoryState): Set<Effect> {
        return setOf(UpdateHistoryStateEffect(newState))
    }

    private fun createLocalHistory(
        effect: LoadHistoryEffect,
        result: Success<GraphQlHistory>
    ): LocalHistory {
        return LocalHistory.fromGraphQl(
            effect.date,
            result.data,
            deviceId,
            visitAddressDelegate,
            crashReportsProvider,
            osUtilsProvider,
            moshi
        )
    }
}

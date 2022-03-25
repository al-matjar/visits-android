package com.hypertrack.android.interactors.history

import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.api.graphql.DayRange
import com.hypertrack.android.api.graphql.GraphQlApiClient
import com.hypertrack.android.api.graphql.models.GraphQlGeofenceVisit
import com.hypertrack.android.api.graphql.models.GraphQlHistory
import com.hypertrack.android.models.local.DeviceId
import com.hypertrack.android.models.local.DeviceStatusMarker
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.models.local.Geotag
import com.hypertrack.android.models.local.LocalGeofenceVisit
import com.hypertrack.android.models.local.LocalHistory
import com.hypertrack.android.ui.common.delegates.address.GraphQlGeofenceVisitAddressDelegate
import com.hypertrack.android.ui.screens.visits_management.tabs.places.PlaceItem
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Loading
import com.hypertrack.android.utils.LoadingFailure
import com.hypertrack.android.utils.LoadingSuccess
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.ReducerResult
import com.hypertrack.android.utils.StateMachine
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.datetime.toSeconds
import com.hypertrack.android.utils.toMeters
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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
    private val crashReportsProvider: CrashReportsProvider,
    private val visitAddressDelegate: GraphQlGeofenceVisitAddressDelegate,
    private val moshi: Moshi,
    private val scope: CoroutineScope,
    private val stateMachineContext: CoroutineContext,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : HistoryInteractor {

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

    private suspend fun createLocalHistory(
        effect: LoadHistoryEffect,
        result: Success<GraphQlHistory>
    ): LocalHistory {
        return mapRemoteHistory(
            effect.date,
            result.data,
            deviceId,
            crashReportsProvider,
            moshi
        )
    }

    private suspend fun mapRemoteHistory(
        date: LocalDate,
        gqlHistory: GraphQlHistory,
        deviceId: DeviceId,
        crashReportsProvider: CrashReportsProvider,
        moshi: Moshi
    ): LocalHistory {
        return LocalHistory(
            date,
            getGeocodingAddresses(gqlHistory.visits).let { addresses ->
                gqlHistory.visits.map {
                    LocalGeofenceVisit.fromGraphQlVisit(
                        it,
                        deviceId,
                        crashReportsProvider,
                        addresses[it],
                        moshi
                    )
                }
            },
            gqlHistory.geotagMarkers.map {
                Geotag.fromGraphQlGeotagMarker(it, moshi)
            },
            gqlHistory.locations
                .sortedBy { it.recordedAt }
                .map { it.coordinate.toLatLng() },
            gqlHistory.deviceStatusMarkers.map {
                DeviceStatusMarker.fromGraphQl(it)
            },
            gqlHistory.totalDriveDistanceMeters.toMeters(),
            gqlHistory.totalDriveDurationMinutes.toSeconds()
        )
    }

    private suspend fun getGeocodingAddresses(
        visits: List<GraphQlGeofenceVisit>
    ): Map<GraphQlGeofenceVisit, String?> {
        val addresses = mutableMapOf<GraphQlGeofenceVisit, String?>()
        try {
            withTimeout(5000L) {
                //todo parallelize
                visits.forEach { visit ->
                    addresses[visit] = visitAddressDelegate.displayAddress(visit)
                }
            }
        } catch (e: TimeoutCancellationException) {
        }
        return addresses
    }
}

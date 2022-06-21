package com.hypertrack.android.interactors.app.effect

import com.hypertrack.android.di.AppScope
import com.hypertrack.android.interactors.app.AppAction
import com.hypertrack.android.interactors.app.HistoryAppAction
import com.hypertrack.android.interactors.app.HistoryViewEffect
import com.hypertrack.android.interactors.app.LoadHistoryEffect
import com.hypertrack.android.interactors.app.action.DayHistoryErrorAction
import com.hypertrack.android.interactors.app.action.DayHistoryLoadedAction
import com.hypertrack.android.ui.common.use_case.get_error_message.ExceptionError
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.common.util.updateConsumableAsFlow
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.ui.screens.visits_management.tabs.history.Effect
import com.hypertrack.android.ui.screens.visits_management.tabs.history.GeofenceVisitDialog
import com.hypertrack.android.ui.screens.visits_management.tabs.history.GeotagDialog
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OpenGeofenceVisitInfoDialogEffect
import com.hypertrack.android.ui.screens.visits_management.tabs.history.OpenGeotagInfoDialogEffect
import com.hypertrack.android.ui.screens.visits_management.tabs.history.ShowGeofenceVisitDialogEvent
import com.hypertrack.android.ui.screens.visits_management.tabs.history.ShowGeotagDialogEvent
import com.hypertrack.android.ui.screens.visits_management.tabs.history.ViewEventEffect
import com.hypertrack.android.use_case.app.UseCases
import com.hypertrack.android.use_case.history.LoadHistoryUseCase
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.toAddressString
import com.hypertrack.android.utils.toFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

@Suppress("OPT_IN_USAGE")
class HistoryEffectHandler(
    private val appScope: AppScope,
    private val useCases: UseCases
) {

    fun applyEffect(effect: HistoryViewEffect): Flow<AppAction?> {
        return effectFlow(effect.effect).map { null }
    }

    fun applyEffect(effect: LoadHistoryEffect): Flow<AppAction?> {
        return getEffectFlow(effect)
    }

    private fun getEffectFlow(effect: LoadHistoryEffect): Flow<AppAction?> {
        return LoadHistoryUseCase(
            effect.userScope.graphQlApiClient,
            appScope.graphQlGeofenceVisitAddressDelegate,
            appScope.moshi
        ).execute(effect.date, effect.userScope.deviceId)
            .flatMapConcat { result ->
                when (result) {
                    is Success -> {
                        DayHistoryLoadedAction(effect.date, result.data).toFlow()
                    }
                    is Failure -> {
                        useCases.getErrorMessageUseCase.execute(ExceptionError(result.exception))
                            .map {
                                DayHistoryErrorAction(effect.date, result.exception, it)
                            }
                    }
                }.map { HistoryAppAction(it) }
            }
    }

    private fun effectFlow(effect: Effect): Flow<Unit> {
        return when (effect) {
            is OpenGeofenceVisitInfoDialogEffect -> {
                suspend {
                    effect.visit.id?.let {
                        effect.viewEventHandle.postValue(
                            GeofenceVisitDialog(
                                visitId = effect.visit.id,
                                geofenceId = effect.visit.geofenceId,
                                geofenceName = appScope.geofenceVisitDisplayDelegate.getGeofenceName(
                                    effect.visit
                                ),
                                geofenceDescription = appScope.geofenceVisitDisplayDelegate.getGeofenceDescription(
                                    effect.visit
                                ),
                                integrationName = effect.visit.metadata?.integration?.name,
                                address = appScope.geofenceVisitAddressDelegate.shortAddress(effect.visit),
                                durationText = appScope.geofenceVisitDisplayDelegate.getDurationText(
                                    effect.visit
                                ),
                                routeToText = appScope.geofenceVisitDisplayDelegate.getRouteToText(
                                    effect.visit
                                )
                            ).let { ShowGeofenceVisitDialogEvent(it) }
                        )
                    }
                    Unit
                }.asFlow()
            }
            is OpenGeotagInfoDialogEffect -> {
                suspend {
                    effect.viewEventHandle.postValue(
                        GeotagDialog(
                            geotagId = effect.geotag.id,
                            title = appScope.geotagDisplayDelegate.getDescription(effect.geotag),
                            metadataString = appScope.geotagDisplayDelegate.formatMetadata(effect.geotag),
                            routeToText = appScope.geotagDisplayDelegate.getRouteToText(effect.geotag),
                            address = effect.geotag.address
                                ?: appScope.geocodingInteractor.getPlaceFromCoordinates(effect.geotag.location)
                                    ?.toAddressString(strictMode = false)
                        ).let {
                            ShowGeotagDialogEvent(it)
                        }
                    )
                }.asFlow()
            }
            is ViewEventEffect -> {
                effect.viewEventHandle.updateConsumableAsFlow(effect.viewEvent)
            }
        }
    }

}

package com.hypertrack.android.ui.common.map_state

import com.hypertrack.android.interactors.app.AppAction
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.GeofencesForMapAppAction
import com.hypertrack.android.interactors.app.action.LoadGeofencesForMapAction
import com.hypertrack.android.interactors.app.noAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Suppress("EXPERIMENTAL_API_USAGE")
class MapUiEffectHandler(
    private val appInteractor: AppInteractor
) {

    fun getEffectFlow(effect: MapUiEffect): Flow<MapUiAction?> {
        return when (effect) {
            is AddGeofencesOnMapEffect -> {
                {
                    effect.geofences.forEach { effect.map.addGeofence(it) }
                }.asFlow().flowOn(Dispatchers.Main).noAction()
            }
            is TriggerLoadingGeofencesEffect -> {
                appInteractor.handleActionFlow(
                    GeofencesForMapAppAction(
                        LoadGeofencesForMapAction(
                            effect.latLng
                        )
                    )
                ).noAction()
            }
            is TriggerLoadingGeofencesForMapPositionEffect -> {
                { effect.map.cameraPosition }.asFlow().flowOn(Dispatchers.Main)
                    .flatMapConcat {
                        appInteractor.handleActionFlow(
                            GeofencesForMapAppAction(
                                LoadGeofencesForMapAction(it)
                            )
                        )
                    }.noAction()
            }
            is UpdateMapStateEffect -> {
                {
                    effect.map.clear()
                }.asFlow()
                    .flatMapConcat {
                        getEffectFlow(
                            AddGeofencesOnMapEffect(
                                effect.map,
                                effect.mapUiState.geofences.toList()
                            )
                        )
                    }
                    .flatMapConcat {
                        {
                            effect.mapUiState.trip?.let {
                                effect.map.addTrip(it)
                            }
                            effect.map.addUserLocation(effect.mapUiState.userLocation)
                            Unit
                        }.asFlow()
                    }.flowOn(Dispatchers.Main).noAction()
            }
            is UpdateGeofenceForDetailsEffect -> {
                {
                    effect.oldGeofenceForDetails?.forEach { it.remove() }
                    effect.geofenceForDetails?.let { geofenceForDetails ->
                        effect.map.add(geofenceForDetails).let {
                            OnGeofenceForDetailsUpdatedMapUiAction(it)
                        }
                    }
                }.asFlow().flowOn(Dispatchers.Main)
            }
        }
    }

}

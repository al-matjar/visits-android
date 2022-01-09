package com.hypertrack.android.ui.screens.add_geotag

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.interactors.Error
import com.hypertrack.android.interactors.GeotagCreationError
import com.hypertrack.android.interactors.GeotagCreationException
import com.hypertrack.android.interactors.GeotagCreationResult
import com.hypertrack.android.interactors.GeotagCreationSuccess
import com.hypertrack.android.interactors.GeotagsInteractor
import com.hypertrack.android.interactors.LatestLocation
import com.hypertrack.android.interactors.LatestLocationResult
import com.hypertrack.android.interactors.Outage
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.common.adapters.EditableKeyValueItem
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.util.ErrorMessage
import com.hypertrack.android.utils.IllegalActionException
import com.hypertrack.android.utils.ReducerResult
import com.hypertrack.android.utils.StateMachine
import com.hypertrack.logistics.android.github.R
import java.util.*

class AddGeotagViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val geotagsInteractor: GeotagsInteractor
) : BaseViewModel(baseDependencies) {

    private val stateMachine = StateMachine<Action, State, Effect>(
        this::class.java.simpleName,
        viewModelScope,
        InitialState,
        this::handleAction,
        this::applyEffects,
        this::stateChangeEffects
    )

    private val errorHint = osUtilsProvider.stringFromResource(R.string.add_geotag_error_hint)

    val viewState = MutableLiveData<ViewState>()

    init {
        geotagsInteractor.getLatestLocation().let {
            stateMachine.handleAction(LatestLocationResultReceivedAction(it))
        }
    }

    fun onMapReady(googleMap: GoogleMap) {
        HypertrackMapWrapper(
            googleMap, osUtilsProvider, crashReportsProvider, MapParams(
                enableMyLocationButton = false,
                enableMyLocationIndicator = false,
                enableScroll = false,
                enableZoomKeys = true
            )
        ).let { mapWrapper ->
            stateMachine.handleAction(MapReadyAction(mapWrapper))
        }
    }

    private fun handleAction(state: State, action: Action): ReducerResult<State, Effect> {
        return when (action) {
            is LatestLocationResultReceivedAction -> {
                when (state) {
                    InitialState -> {
                        action.result.let { result ->
                            when (result) {
                                is LatestLocation -> HasLatestLocation(result.latLng)
                                is Error -> OutageState(
                                    ErrorMessage(
                                        errorHint,
                                        result.exception.toString()
                                    )
                                )
                                is Outage -> OutageState(
                                    ErrorMessage(
                                        errorHint,
                                        formatUnderscoreName(result.reason.name)
                                    )
                                )
                            }.asReducerResult()
                        }
                    }
                    else -> throw IllegalActionException(action, state)
                }
            }
            is MapReadyAction -> {
                when (state) {
                    InitialState -> throw IllegalActionException(action, state)
                    is HasLatestLocation -> ReadyForCreation(
                        action.map,
                        state.latestLocation
                    ).withEffects(
                        ShowOnMapEffect(action.map, state.latestLocation)
                    )
                    is OutageState -> state.asReducerResult()
                    is ReadyForCreation -> ReadyForCreation(
                        action.map,
                        state.latestLocation
                    ).withEffects(
                        ShowOnMapEffect(state.map, state.latestLocation)
                    )
                }
            }
            is GeotagResultAction -> {
                when (action.result) {
                    GeotagCreationSuccess -> state.withEffects(GoBackEffect)
                    is GeotagCreationError -> OutageState(
                        ErrorMessage(
                            errorHint,
                            formatUnderscoreName(action.result.reason.name)
                        )
                    ).asReducerResult()
                    is GeotagCreationException -> OutageState(
                        ErrorMessage(
                            errorHint,
                            action.result.exception.toString()
                        )
                    ).asReducerResult()
                }
            }
            is CreateButtonClickAction -> {
                when (state) {
                    is ReadyForCreation -> {
                        state.withEffects(
                            when {
                                action.metadata.isEmpty() -> {
                                    ShowErrorMessageEffect(
                                        osUtilsProvider.stringFromResource(
                                            R.string.add_geotag_validation_empty_metadata
                                        )
                                    )
                                }
                                action.metadata.any { it.key.isEmpty() || it.value.isEmpty() } -> {
                                    ShowErrorMessageEffect(
                                        osUtilsProvider.stringFromResource(
                                            R.string.add_geotag_validation_empty_key_or_value
                                        )
                                    )
                                }
                                else -> {
                                    CreateGeotag(action.metadata)
                                }
                            }
                        )
                    }
                    is HasLatestLocation -> {
                        state.withEffects(
                            ShowErrorMessageEffect(
                                osUtilsProvider.stringFromResource(
                                    R.string.map_is_not_ready_yet
                                )
                            )
                        )
                    }
                    is OutageState, InitialState -> throw IllegalActionException(action, state)
                }
            }
        }
    }

    private fun stateChangeEffects(newState: State): Set<Effect> {
        return setOf(SetViewStateEffect(newState.viewState))
    }

    private fun applyEffects(effects: Set<Effect>) {
        for (effect in effects) {
            when (effect) {
                is SetViewStateEffect -> {
                    viewState.postValue(effect.viewState)
                }
                is ShowOnMapEffect -> {
                    effect.map.let {
                        it.addGeotagMarker(effect.latestLocation)
                        it.moveCamera(effect.latestLocation)
                    }
                }
                GoBackEffect -> {
                    popBackStack.postValue(true.toConsumable())
                }
                is ShowErrorMessageEffect -> {
                    snackbar.postValue(effect.text.toConsumable())
                }
                is CreateGeotag -> {
                    geotagsInteractor.createGeotag(effect.metadata).let {
                        stateMachine.handleAction(GeotagResultAction(it))
                    }
                }
            } as Unit
        }
    }

    fun onCreateClick(items: MutableList<EditableKeyValueItem>) {
        stateMachine.handleAction(CreateButtonClickAction(items.map { it.key to it.value }.toMap()))
    }

    private fun formatUnderscoreName(name: String): String {
        return name
            .replace("_", " ")
            .toLowerCase(Locale.getDefault())
            .capitalize(Locale.getDefault())
    }

}

private fun State.asReducerResult(): ReducerResult<State, Effect> {
    return ReducerResult(this)
}

private fun State.withEffects(effects: Set<Effect>): ReducerResult<State, Effect> {
    return ReducerResult(this, effects)
}

private fun State.withEffects(vararg effect: Effect): ReducerResult<State, Effect> {
    return ReducerResult(
        this,
        effect.toMutableSet()
    )
}

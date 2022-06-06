package com.hypertrack.android.ui.screens.add_geotag

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.hypertrack.android.interactors.Error
import com.hypertrack.android.interactors.GeotagCreationError
import com.hypertrack.android.interactors.GeotagCreationException
import com.hypertrack.android.interactors.GeotagCreationSuccess
import com.hypertrack.android.interactors.GeotagsInteractor
import com.hypertrack.android.interactors.LatestLocation
import com.hypertrack.android.interactors.Outage
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.toConsumable
import com.hypertrack.android.ui.common.adapters.EditableKeyValueItem
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.ui.screens.add_place_info.ShowErrorMessageEffect
import com.hypertrack.android.utils.ErrorMessage
import com.hypertrack.android.utils.IllegalActionException
import com.hypertrack.android.utils.ReducerResult
import com.hypertrack.android.utils.StateMachine
import com.hypertrack.android.utils.format
import com.hypertrack.android.utils.mapToJson
import com.hypertrack.android.utils.prettifyJson
import com.hypertrack.logistics.android.github.R
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import java.util.*

class AddGeotagViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val geotagsInteractor: GeotagsInteractor
) : BaseViewModel(baseDependencies) {

    private val emptyStringPlaceholder = resourceProvider.stringFromResource(
        R.string.add_geotag_empty_placeholder
    )

    private val stateMachine = StateMachine<Action, State, Effect>(
        this::class.java.simpleName,
        crashReportsProvider,
        InitialState,
        viewModelScope,
        Dispatchers.Main,
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
                    GeotagCreationSuccess -> {
                        state.withEffects(
                            GoBackEffect,
                            ShowToastEffect(R.string.add_geotag_success)
                        )
                    }
                    is GeotagCreationError -> {
                        OutageState(
                            ErrorMessage(
                                errorHint,
                                formatUnderscoreName(action.result.reason.name)
                            )
                        ).asReducerResult()
                    }
                    is GeotagCreationException -> {
                        OutageState(
                            ErrorMessage(
                                errorHint,
                                action.result.exception.toString()
                            )
                        ).asReducerResult()
                    }
                }
            }
            is CreateButtonClickAction -> {
                when (state) {
                    is ReadyForCreation -> {
                        state.withEffects(
                            when {
                                action.metadata.isEmpty() -> {
                                    ShowMetadataErrorEffect(action.metadata, NoMetadata)
                                }
                                action.metadata.any { it.first.isBlank() && it.second.isBlank() } -> {
                                    ShowMetadataErrorEffect(action.metadata, EmptyMetadata)
                                }
                                // has duplicate keys
                                action.metadata.distinctBy { it.first }.size != action.metadata.size -> {
                                    ShowMetadataErrorEffect(
                                        action.metadata,
                                        DuplicateKeys(
                                            action.metadata.groupingBy { it.first }
                                                .eachCount()
                                                .filter { it.value > 1 }
                                                .map { it.key }
                                        ))
                                }
                                action.metadata.any { it.first.isBlank() } -> {
                                    ShowMetadataErrorEffect(
                                        action.metadata,
                                        EmptyKeys(
                                            values = action.metadata.filter { it.first.isBlank() }
                                                .map { it.second }
                                        ))
                                }
                                action.metadata.any { it.second.isBlank() } -> {
                                    ShowMetadataErrorEffect(
                                        action.metadata,
                                        EmptyValues(
                                            keys = action.metadata.filter { it.second.isBlank() }
                                                .map { it.first }
                                        ))
                                }
                                else -> {
                                    CreateGeotag(action.metadata.toMap())
                                }
                            }
                        )
                    }
                    is HasLatestLocation -> {
                        state.withEffects(ShowMapNotReadyErrorEffect)
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
        try {
            for (effect in effects) {
                applyEffect(effect)
            }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
            snackbar.postValue(e.format().toConsumable())
        }
    }

    private fun applyEffect(effect: Effect) {
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
            is CreateGeotag -> {
                geotagsInteractor.createGeotag(effect.metadata).let {
                    stateMachine.handleAction(GeotagResultAction(it))
                }
            }
            is ShowToastEffect -> {
                osUtilsProvider.makeToast(effect.stringResource)
            }
            ShowMapNotReadyErrorEffect -> {
                snackbar.postValue(
                    osUtilsProvider.stringFromResource(
                        R.string.map_is_not_ready_yet
                    ).toConsumable()
                )
            }
            is ShowMetadataErrorEffect -> {
                val metadataString = effect.metadataItems.joinToString("\n") {
                    val key = it.first.nullIfBlank() ?: emptyStringPlaceholder
                    val value = it.second.nullIfBlank() ?: emptyStringPlaceholder
                    "$key: $value"
                }

                when (effect.metadataError) {
                    NoMetadata -> {
                        resourceProvider.stringFromResource(
                            R.string.add_geotag_validation_no_metadata
                        )
                    }
                    EmptyMetadata -> {
                        resourceProvider.stringFromResource(
                            R.string.add_geotag_validation_empty_metadata
                        ).let {
                            resourceProvider.stringFromResource(
                                R.string.add_geotag_validation_wrong_metadata,
                                metadataString,
                                it
                            )
                        }
                    }
                    is DuplicateKeys -> {
                        resourceProvider.stringFromResource(
                            R.string.add_geotag_validation_duplicate_keys,
                            effect.metadataError.keys.joinToString("\n")
                        ).let {
                            resourceProvider.stringFromResource(
                                R.string.add_geotag_validation_wrong_metadata,
                                metadataString,
                                it
                            )
                        }
                    }
                    is EmptyKeys -> {
                        resourceProvider.stringFromResource(
                            R.string.add_geotag_validation_empty_keys,
                            effect.metadataError.values.joinToString("\n")
                        ).let {
                            resourceProvider.stringFromResource(
                                R.string.add_geotag_validation_wrong_metadata,
                                metadataString,
                                it
                            )
                        }
                    }
                    is EmptyValues -> {
                        resourceProvider.stringFromResource(
                            R.string.add_geotag_validation_empty_values,
                            effect.metadataError.keys.joinToString("\n")
                        ).let {
                            resourceProvider.stringFromResource(
                                R.string.add_geotag_validation_wrong_metadata,
                                metadataString,
                                it
                            )
                        }
                    }
                }.let {
                    snackbar.postValue(it.toConsumable())
                }
            }
        } as Any?
    }

    fun onCreateClick(items: MutableList<EditableKeyValueItem>) {
        stateMachine.handleAction(CreateButtonClickAction(items.map { it.key to it.value }))
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

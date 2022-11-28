package com.hypertrack.android.ui.screens.add_geotag

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import com.hypertrack.android.hypertrack_sdk.LatestLocation
import com.hypertrack.android.hypertrack_sdk.Outage
import com.hypertrack.android.interactors.GeotagCreationError
import com.hypertrack.android.interactors.GeotagCreationSuccess
import com.hypertrack.android.interactors.GeotagsInteractor
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.adapters.EditableKeyValueItem
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.map.MapParams
import com.hypertrack.android.ui.common.use_case.get_error_message.ComplexTextError
import com.hypertrack.android.ui.common.use_case.get_error_message.asError
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.ui.common.util.updateAsFlow
import com.hypertrack.android.ui.common.util.updateConsumableAsFlow
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.android.utils.state_machine.ReducerResult
import com.hypertrack.android.utils.StateMachine
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.catchException
import com.hypertrack.android.utils.toFlow
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.*

@Suppress("OPT_IN_USAGE")
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
        actionsScope,
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
                                is Outage -> OutageState(formatUnderscoreName(result.reason.name))
                            }.withEffects()
                        }
                    }
                    else -> throw IllegalActionException(action, state)
                }
            }
            is MapReadyAction -> {
                when (state) {
                    InitialState -> {
                        throw IllegalActionException(action, state)
                    }
                    is HasLatestLocation -> {
                        ReadyForCreation(
                            action.map,
                            state.latestLocation
                        ).withEffects(
                            ShowOnMapEffect(action.map, state.latestLocation)
                        )
                    }
                    is OutageState -> {
                        state.withEffects()
                    }
                    is ReadyForCreation -> {
                        ReadyForCreation(
                            action.map,
                            state.latestLocation
                        ).withEffects(
                            ShowOnMapEffect(state.map, state.latestLocation)
                        )
                    }
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
                        OutageState(formatUnderscoreName(action.result.reason.name)).asReducerResult()
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
        return setOf(UpdateViewStateEffect(newState))
    }

    private fun applyEffects(effects: Set<Effect>) {
        try {
            for (effect in effects) {
                runInVmEffectsScope {
                    getEffectFlow(effect).catchException {
                        onError(it)
                    }.collect()
                }
            }
        } catch (e: Exception) {
            showExceptionMessageAndReport(e)
        }
    }

    private fun getEffectFlow(effect: Effect): Flow<Unit> {
        return when (effect) {
            is UpdateViewStateEffect -> {
                viewState.updateAsFlow(getViewState(effect.state))
            }
            is ShowOnMapEffect -> {
                {
                    effect.map.addGeotagMarker(effect.latestLocation)
                    effect.map.moveCamera(effect.latestLocation)
                }.asFlow().flowOn(Dispatchers.Main)
            }
            GoBackEffect -> {
                popBackStack.updateConsumableAsFlow(true)
            }
            is CreateGeotag -> {
                suspend {
                    geotagsInteractor.createGeotag(effect.metadata).let {
                        when (it) {
                            is Success -> stateMachine.handleAction(GeotagResultAction(it.data))
                            is Failure -> showExceptionMessageAndReport(it.exception)
                        }
                    }
                }.asFlow()
            }
            is ShowToastEffect -> {
                {
                    osUtilsProvider.makeToast(effect.stringResource)
                }.asFlow().flowOn(Dispatchers.Main)
            }
            ShowMapNotReadyErrorEffect -> {
                showErrorFlow(R.string.map_is_not_ready_yet.asError())
            }
            is ShowMetadataErrorEffect -> {
                showMetadataErrorFlow(effect)
            }
        }
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

    private fun showMetadataErrorFlow(effect: ShowMetadataErrorEffect): Flow<Unit> {
        return effect.metadataItems.joinToString("\n") {
            val key = it.first.nullIfBlank() ?: emptyStringPlaceholder
            val value = it.second.nullIfBlank() ?: emptyStringPlaceholder
            "$key: $value"
        }.toFlow().map { metadataString ->
            when (effect.metadataError) {
                NoMetadata -> {
                    R.string.add_geotag_validation_no_metadata.asError()
                }
                EmptyMetadata -> {
                    ComplexTextError(
                        R.string.add_geotag_validation_wrong_metadata,
                        arrayOf(
                            metadataString,
                            resourceProvider.stringFromResource(
                                R.string.add_geotag_validation_empty_metadata
                            )
                        )
                    )
                }
                is DuplicateKeys -> {
                    ComplexTextError(
                        R.string.add_geotag_validation_wrong_metadata,
                        arrayOf(
                            metadataString,
                            resourceProvider.stringFromResource(
                                R.string.add_geotag_validation_duplicate_keys,
                                effect.metadataError.keys.joinToString("\n")
                            )
                        )
                    )
                }
                is EmptyKeys -> {
                    ComplexTextError(
                        R.string.add_geotag_validation_wrong_metadata,
                        arrayOf(
                            metadataString,
                            resourceProvider.stringFromResource(
                                R.string.add_geotag_validation_empty_keys,
                                effect.metadataError.values.joinToString("\n")
                            )
                        )
                    )
                }
                is EmptyValues -> {
                    ComplexTextError(
                        R.string.add_geotag_validation_wrong_metadata,
                        arrayOf(
                            metadataString,
                            resourceProvider.stringFromResource(
                                R.string.add_geotag_validation_empty_values,
                                effect.metadataError.keys.joinToString("\n")
                            )
                        )
                    )
                }
            }
        }.showErrorMessage()
    }

    private fun getViewState(state: State): ViewState {
        return when (state) {
            InitialState -> {
                InitialViewState
            }
            is ReadyForCreation -> {
                GeotagCreationViewState()
            }
            is HasLatestLocation -> {
                GeotagCreationViewState()
            }
            is OutageState -> {
                ErrorViewState(state.outageText)
            }
        }
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

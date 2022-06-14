package com.hypertrack.android.ui.screens.visits_management

import androidx.lifecycle.*
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.AppState
import com.hypertrack.android.interactors.app.NotInitialized
import com.hypertrack.android.interactors.app.Initialized
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.interactors.app.UserNotLoggedIn
import com.hypertrack.android.repository.preferences.PreferencesRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.use_case.sdk.DeviceDeleted
import com.hypertrack.android.use_case.sdk.LocationServicesDisabled
import com.hypertrack.android.use_case.sdk.NewTrackingState
import com.hypertrack.android.use_case.sdk.PermissionsDenied
import com.hypertrack.android.use_case.sdk.TrackingFailure
import com.hypertrack.android.use_case.sdk.TrackingStarted
import com.hypertrack.android.use_case.sdk.TrackingStateUnknown
import com.hypertrack.android.use_case.sdk.TrackingStopped
import com.hypertrack.android.utils.*
import com.hypertrack.android.utils.exception.IllegalActionException
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf

@Suppress("OPT_IN_USAGE")
class VisitsManagementViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val appState: LiveData<AppState>,
    private val preferencesRepository: PreferencesRepository,
) : BaseViewModel(baseDependencies) {

    val showProgressbar = Transformations.map(appState) {
        it.isProgressbarVisible()
    }

    val trackingIndicatorState = MutableLiveData<TrackingIndicatorState>()

    init {
        appState.observeManaged {
            handleAppStateChangedAction(it)
        }
    }

    fun handleAction(action: Action) {
        when (val state = appState.requireValue()) {
            is NotInitialized -> {
                flowOf(Failure(IllegalActionException(action, state)))
            }
            is Initialized -> when (state.userState) {
                UserNotLoggedIn -> {
                    flowOf(Failure(IllegalActionException(action, state)))
                }
                is UserLoggedIn -> {
                    val userScope = state.userState.userScope
                    when (action) {
                        OnViewCreatedAction -> {
                            init(userScope)
                        }
                        is TrackingSwitchClickedAction -> {
                            switchTracking(
                                action,
                                trackingIndicatorState,
                                userScope.hyperTrackService
                            )
                        }
                        RefreshHistoryAction -> {
                            tryAsResult {
                                userScope.historyInteractorLegacy.refreshTodayHistory()
                            }.toFlow()
                        }
                    }
                }
            }
        }.flatMapConcat {
            when (it) {
                is Success -> {
                }
                is Failure -> {
//                    crashReportsProvider.logException(it.exception)
                    showExceptionMessageAndReport(it.exception)
                }
            }.toFlow()
        }.let { handleEffect(it) }
    }

    private fun handleEffect(effectFlow: Flow<Unit>) {
        runInVmEffectsScope {
            effectFlow.catchException {
//                crashReportsProvider.logException(it)
                showExceptionMessageAndReport(it)
            }.collect()
        }
    }

    private fun init(userScope: UserScope): Flow<Result<Unit>> {
        return tryAsResult {
            userScope.historyInteractorLegacy.errorFlow.asLiveData()
                .observeManaged { consumable ->
                    consumable.consume {
                        showExceptionMessageAndReport(it)
                    }
                }

            userScope.tripsInteractor.errorFlow.asLiveData()
                .observeManaged { consumable ->
                    consumable.consume {
                        showExceptionMessageAndReport(it)
                    }
                }

            // todo unit test
            preferencesRepository.trackingStartedOnFirstLaunch.load().let {
                when (it) {
                    is Success -> {
                        if (it.data != false) {
                            userScope.hyperTrackService.startTracking()
                            preferencesRepository.trackingStartedOnFirstLaunch.save(false)
                        }
                    }
                    is Failure -> {
                        showExceptionMessageAndReport(it.exception)
                    }
                }
            }
        }.toFlow()
    }

    private fun switchTracking(
        action: TrackingSwitchClickedAction,
        indicatorState: MutableLiveData<TrackingIndicatorState>,
        hyperTrackService: HyperTrackService
    ): Flow<Result<Unit>> {
        return tryAsResult {
            if (action.isChecked != trackingIndicatorState.requireValue().isTracking) {
                if (trackingIndicatorState.requireValue().isTracking) {
                    hyperTrackService.stopTracking()
                } else {
                    hyperTrackService.startTracking()
                }
            }
        }.toFlow()
    }

    private fun handleAppStateChangedAction(appState: AppState) {
        when (appState) {
            is Initialized -> {
                when (appState.userState) {
                    is UserLoggedIn -> {
                        appState.userState.trackingState.let { trackingState ->
                            getTrackingIndicatorState(trackingState).let {
                                trackingIndicatorState.postValue(it)
                            }
                        }
                    }
                    UserNotLoggedIn -> {
                        showExceptionMessageAndReport(
                            IllegalArgumentException(appState.toString())
                        )
                    }
                }
            }
            is NotInitialized -> {
                showExceptionMessageAndReport(
                    IllegalArgumentException(appState.toString())
                )
            }
        } as Any?
    }

    private fun getTrackingIndicatorState(trackingState: NewTrackingState): TrackingIndicatorState {
        return when (trackingState) {
            TrackingStarted -> {
                TrackingIndicatorState(
                    color = R.color.colorTrackingActive,
                    statusMessageResource = R.string.visits_management_clocked_in,
                    trackingMessageResource = getTrackingMessageResource(true),
                    isTracking = true,
                )
            }
            TrackingStopped -> {
                TrackingIndicatorState(
                    color = R.color.colorTrackingStopped,
                    statusMessageResource = R.string.visits_management_clocked_out,
                    trackingMessageResource = getTrackingMessageResource(false),
                    isTracking = false,
                )
            }
            is TrackingFailure -> {
                TrackingIndicatorState(
                    color = R.color.colorTrackingActive,
                    statusMessageResource = R.string.visits_management_generic_tracking_error,
                    trackingMessageResource = getTrackingMessageResource(false),
                    isTracking = false,
                )
            }
            TrackingStateUnknown -> {
                TrackingIndicatorState(
                    color = R.color.colorTrackingStopped,
                    statusMessageResource = R.string.empty_string,
                    trackingMessageResource = getTrackingMessageResource(false),
                    isTracking = false,
                )
            }
            DeviceDeleted -> {
                TrackingIndicatorState(
                    color = R.color.colorTrackingError,
                    statusMessageResource = R.string.visits_management_device_deleted,
                    trackingMessageResource = getTrackingMessageResource(false),
                    isTracking = false,
                )
            }
            PermissionsDenied -> {
                TrackingIndicatorState(
                    color = R.color.colorTrackingError,
                    statusMessageResource = R.string.visits_management_permissions_not_granted,
                    trackingMessageResource = getTrackingMessageResource(false),
                    isTracking = false,
                )
            }
            LocationServicesDisabled -> {
                TrackingIndicatorState(
                    color = R.color.colorTrackingError,
                    statusMessageResource = R.string.visits_management_location_services_disabled,
                    trackingMessageResource = getTrackingMessageResource(false),
                    isTracking = false,
                )
            }
        }
    }

    private fun getTrackingMessageResource(isTracking: Boolean): Int {
        return if (isTracking) {
            R.string.clock_hint_tracking_on
        } else {
            R.string.clock_hint_tracking_off
        }
    }

}

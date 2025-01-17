package com.hypertrack.android.ui.screens.visits_management

import androidx.lifecycle.*
import com.hypertrack.android.di.UserScope
import com.hypertrack.android.interactors.app.state.AppState
import com.hypertrack.android.interactors.app.state.AppNotInitialized
import com.hypertrack.android.interactors.app.state.AppInitialized
import com.hypertrack.android.interactors.app.state.UserLoggedIn
import com.hypertrack.android.interactors.app.state.UserNotLoggedIn
import com.hypertrack.android.repository.preferences.PreferencesRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.Tab
import com.hypertrack.android.ui.common.util.requireValue
import com.hypertrack.android.use_case.sdk.DeviceDeleted
import com.hypertrack.android.use_case.sdk.LocationServicesDisabled
import com.hypertrack.android.use_case.sdk.TrackingState
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
            is AppNotInitialized -> {
                flowOf(Failure(IllegalActionException(action, state)))
            }
            is AppInitialized -> when (state.userState) {
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
                                trackingIndicatorState.requireValue(),
                                userScope.hyperTrackService
                            )
                        }
                    }
                }
            }
        }.flatMapConcat {
            when (it) {
                is Success -> {
                }
                is Failure -> {
                    showExceptionMessageAndReport(it.exception)
                }
            }.toFlow()
        }.let { handleEffect(it) }
    }

    private fun handleEffect(effectFlow: Flow<Unit>) {
        runInVmEffectsScope {
            effectFlow.catchException {
                showExceptionMessageAndReport(it)
            }.collect()
        }
    }

    private fun init(userScope: UserScope): Flow<Result<Unit>> {
        return tryAsResult {
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
                            crashReportsProvider.log("initial_start_tracking")
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
        indicatorState: TrackingIndicatorState,
        hyperTrackService: HyperTrackService
    ): Flow<Result<Unit>> {
        return tryAsResult {
            if (action.isChecked != indicatorState.isTracking) {
                if (indicatorState.isTracking) {
                    crashReportsProvider.log("stop_tracking_pressed")
                    hyperTrackService.stopTracking()
                } else {
                    crashReportsProvider.log("start_tracking_pressed")
                    hyperTrackService.startTracking()
                }
            }
        }.toFlow()
    }

    private fun handleAppStateChangedAction(appState: AppState) {
        when (appState) {
            is AppInitialized -> {
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
            is AppNotInitialized -> {
                showExceptionMessageAndReport(
                    IllegalArgumentException(appState.toString())
                )
            }
        } as Any?
    }

    private fun getTrackingIndicatorState(trackingState: TrackingState): TrackingIndicatorState {
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

    fun onTabSelected(tab: Tab) {
//        when (tab) {
//            Tab.CURRENT_TRIP ->
//            Tab.HISTORY ->
//            Tab.ORDERS ->
//            Tab.PLACES ->
//            Tab.SUMMARY ->
//            Tab.PROFILE ->
//        }.let {
//            Injector.provideAppInteractor().handleAction(RegisterScreenAction(it))
//        }
    }

}

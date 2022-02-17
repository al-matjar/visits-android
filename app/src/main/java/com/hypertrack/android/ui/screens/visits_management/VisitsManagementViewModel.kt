package com.hypertrack.android.ui.screens.visits_management

import androidx.lifecycle.*
import com.hypertrack.android.interactors.HistoryInteractorImpl
import com.hypertrack.android.interactors.app.AppState
import com.hypertrack.android.interactors.app.NotInitialized
import com.hypertrack.android.interactors.app.Initialized
import com.hypertrack.android.interactors.app.UserLoggedIn
import com.hypertrack.android.interactors.app.UserNotLoggedIn
import com.hypertrack.android.repository.preferences.PreferencesRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.ErrorHandler
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
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class VisitsManagementViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val appState: LiveData<AppState>,
    private val historyInteractor: HistoryInteractorImpl,
    private val hyperTrackService: HyperTrackService,
    private val preferencesRepository: PreferencesRepository,
) : BaseViewModel(baseDependencies) {

    val showProgressbar = Transformations.map(appState) {
        it.isProgressbarVisible()
    }

    override val errorHandler = ErrorHandler(
        osUtilsProvider,
        crashReportsProvider,
        historyInteractor.errorFlow.asLiveData()
    )

    val trackingIndicatorState = MutableLiveData<TrackingIndicatorState>()

    init {
        appState.observeManaged {
            handleAppStateChangedAction(it)
        }

        // todo unit test
        preferencesRepository.trackingStartedOnFirstLaunch.load().let {
            when (it) {
                is Success -> {
                    if (it.data != false) {
                        hyperTrackService.startTracking()
                        preferencesRepository.trackingStartedOnFirstLaunch.save(false)
                    }
                }
                is Failure -> {
                    errorHandler.postException(it.exception)
                }
            }
        }
    }

    fun onTrackingSwitchClicked(isChecked: Boolean) {
        if (isChecked != trackingIndicatorState.requireValue().isTracking) {
            switchTracking()
        }
    }

    fun refreshHistory() {
        MainScope().launch {
            historyInteractor.refreshTodayHistory()
        }
    }

    private fun switchTracking() {
        viewModelScope.launch {
            if (trackingIndicatorState.requireValue().isTracking) {
                hyperTrackService.stopTracking()
            } else {
                hyperTrackService.startTracking()
            }
        }
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
                        errorHandler.postException(
                            IllegalArgumentException(appState.toString())
                        )
                    }
                }
            }
            is NotInitialized -> {
                errorHandler.postException(
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

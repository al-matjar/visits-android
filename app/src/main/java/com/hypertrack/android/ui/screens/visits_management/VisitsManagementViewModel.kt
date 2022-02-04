package com.hypertrack.android.ui.screens.visits_management

import android.annotation.SuppressLint
import androidx.lifecycle.*
import com.hypertrack.android.interactors.HistoryInteractorImpl
import com.hypertrack.android.repository.AccessTokenRepository
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.ErrorHandler
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Provider

@SuppressLint("NullSafeMutableLiveData")
class VisitsManagementViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val historyInteractor: HistoryInteractorImpl,
    private val accountRepositoryProvider: Provider<AccountRepository>,
    private val hyperTrackService: HyperTrackService,
    accessTokenRepository: AccessTokenRepository
) : BaseViewModel(baseDependencies) {

    override val errorHandler = ErrorHandler(
        osUtilsProvider,
        crashReportsProvider,
        historyInteractor.errorFlow.asLiveData()
    )

    val isTracking = Transformations.map(hyperTrackService.state) {
        it == TrackingStateValue.TRACKING
    }

    init {
        if (accountRepositoryProvider.get().shouldStartTracking) {
            hyperTrackService.startTracking()
            accountRepositoryProvider.get().shouldStartTracking = false
        }
    }

    private val _showSpinner = MutableLiveData(false)
    val showSpinner: LiveData<Boolean>
        get() = _showSpinner

    private val _statusBarColor = MediatorLiveData<Int?>()

    init {
        _statusBarColor.addSource(hyperTrackService.state) {
            when (it) {
                TrackingStateValue.TRACKING -> _statusBarColor.postValue(R.color.colorTrackingActive)
                TrackingStateValue.STOP -> _statusBarColor.postValue(R.color.colorTrackingStopped)
                TrackingStateValue.DEVICE_DELETED,
                TrackingStateValue.ERROR,
                TrackingStateValue.PERMISIONS_DENIED -> {
                    _statusBarColor.postValue(
                        R.color.colorTrackingError
                    )
                }
                else -> _statusBarColor.postValue(null)
            }
        }
    }

    val statusBarColor: LiveData<Int?>
        get() = _statusBarColor

    val statusBarMessage = MediatorLiveData<StatusMessage>()

    init {
        statusBarMessage.addSource(hyperTrackService.state) {
            statusBarMessage.postValue(
                when (it) {
                    TrackingStateValue.DEVICE_DELETED -> StatusString(R.string.device_deleted)
                    TrackingStateValue.ERROR -> StatusString(R.string.generic_tracking_error)
                    TrackingStateValue.TRACKING -> StatusString(R.string.clocked_in)
                    TrackingStateValue.STOP -> StatusString(R.string.clocked_out)
                    TrackingStateValue.PERMISIONS_DENIED -> StatusString(R.string.permissions_not_granted)
                    else -> StatusString(R.string.unknown_error)
                }
            )
        }
    }

    fun refreshHistory() {
        MainScope().launch {
            historyInteractor.refreshTodayHistory()
        }
    }

    fun switchTracking() {
        _showSpinner.postValue(true)
        viewModelScope.launch {
            if (isTracking.value == true) {
                hyperTrackService.stopTracking()
            } else {
                hyperTrackService.startTracking()
            }
            _showSpinner.postValue(false)
        }
    }

    companion object {
        const val TAG = "VisitsManagementVM"
    }

}

sealed class StatusMessage
class StatusString(val stringId: Int) : StatusMessage()

enum class LocalVisitCtaLabel {
    CHECK_IN, CHECK_OUT
}

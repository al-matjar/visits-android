package com.hypertrack.android.view_models

import android.util.Log
import androidx.lifecycle.*
import com.hypertrack.android.repository.AccessTokenRepository
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.repository.VisitsRepository
import com.hypertrack.android.utils.CrashReportsProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class VisitsManagementViewModel(
    private val visitsRepository: VisitsRepository,
    accountRepository: AccountRepository,
    private val accessTokenRepository: AccessTokenRepository,
    private val crashReportsProvider: CrashReportsProvider
) : ViewModel() {

    private val _clockInButtonText = MediatorLiveData<CharSequence>()
    init {
        _clockInButtonText.addSource(visitsRepository.isTracking) { tracking ->
            _clockInButtonText.postValue(if (tracking) "Clock Out" else "Clock In")
        }
    }
    val clockInButtonText: LiveData<CharSequence>
        get() = _clockInButtonText

    private val _checkInButtonText = MediatorLiveData<CharSequence>()
    init {
        if (accountRepository.isManualCheckInAllowed) {
            _checkInButtonText.addSource(visitsRepository.hasOngoingLocalVisit) { hasVisit ->
                _checkInButtonText.postValue(if (hasVisit) "CheckOut" else "CheckIn")
            }
        }
    }
    val checkInButtonText: LiveData<CharSequence>
        get() = _checkInButtonText

    private val _showSpinner = MutableLiveData(false)
    val showSpinner: LiveData<Boolean>
        get() = _showSpinner

    private val _showToast = MutableLiveData("")
    val showToast: LiveData<String>
        get() = _showToast

    private val _enableCheckIn = MediatorLiveData<Boolean>()
    init {
        if (accountRepository.isManualCheckInAllowed) {
            _enableCheckIn.addSource(visitsRepository.isTracking) { _enableCheckIn.postValue(it) }
        } else {
            _enableCheckIn.postValue(false)
        }
    }
    val enableCheckIn: LiveData<Boolean>
        get() = _enableCheckIn

    val showCheckIn: Boolean = accountRepository.isManualCheckInAllowed

    val deviceHistoryWebViewUrl = accessTokenRepository.deviceHistoryWebViewUrl

    fun refreshVisits() {
        if (_showSpinner.value == true) return

        _showSpinner.postValue(true)

         val coroutineExceptionHandler = CoroutineExceptionHandler{_ , throwable ->
            Log.e(TAG, "Got error $throwable in coroutine")
        }
        MainScope().launch(Dispatchers.IO + coroutineExceptionHandler) {
            try {
                visitsRepository.refreshVisits()
            } catch (e: Throwable) {
                Log.e(TAG, "Got error $e refreshing visits")
                crashReportsProvider.logException(e)
                _showToast.postValue("Can't refresh visits")
            } finally {
                _showSpinner.postValue(false)
            }
        }
    }

    fun switchTracking() {
        // Log.v(TAG, "switchTracking")
        _showSpinner.postValue(true)
        viewModelScope.launch {
            visitsRepository.switchTracking()
            _showSpinner.postValue(false)

        }
    }

    fun checkIn() {
        // Log.v(TAG, "checkin")
        visitsRepository.processLocalVisit()
    }

    fun possibleLocalVisitCompletion() {
        // Local visit change affects Check In/ Check Out state
        visitsRepository.checkLocalVisitCompleted()
    }

    val visits = visitsRepository.visitListItems
    val statusLabel = visitsRepository.statusLabel

    companion object {
        const val TAG = "VisitsManagementVM"
    }

}


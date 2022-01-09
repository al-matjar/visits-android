package com.hypertrack.android.ui.screens.visits_management.tabs.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.messaging.VisitsMessagingService
import com.hypertrack.android.repository.AccountRepository
import com.hypertrack.android.repository.DriverRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.base.postValue
import com.hypertrack.android.ui.common.adapters.KeyValueItem
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.launch

class ProfileViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val driverRepository: DriverRepository,
    private val hyperTrackService: HyperTrackService,
    private val accountRepository: AccountRepository,
) : BaseViewModel(baseDependencies) {

    val profile = MutableLiveData<List<KeyValueItem>>()

    init {
        viewModelScope.launch {
            updateProfileItems()
        }
    }

    fun onCopyItemClick(txt: String) {
        osUtilsProvider.copyToClipboard(txt)
    }

    fun onReportAnIssueClick() {
        destination.postValue(VisitsManagementFragmentDirections.actionVisitManagementFragmentToSendFeedbackFragment())
    }

    private suspend fun updateProfileItems() {
        val items = mutableListOf<KeyValueItem>()

        val firebaseToken = VisitsMessagingService.getFirebaseToken()

        items.apply {
            driverRepository.user?.let { user ->
                user.email?.let {
                    add(
                        KeyValueItem(
                            osUtilsProvider.stringFromResource(R.string.email),
                            it
                        )
                    )
                }
                user.phoneNumber?.let {
                    add(
                        KeyValueItem(
                            osUtilsProvider.stringFromResource(R.string.phone_number),
                            it
                        )
                    )
                }
                user.driverId?.let {
                    add(
                        KeyValueItem(
                            osUtilsProvider.stringFromResource(R.string.driver_id),
                            it
                        )
                    )
                }
            }

            add(
                KeyValueItem(
                    osUtilsProvider.stringFromResource(R.string.device_id),
                    hyperTrackService.deviceId ?: ""
                )
            )

            osUtilsProvider.getBuildVersion()?.let {
                add(
                    KeyValueItem(
                        osUtilsProvider.stringFromResource(R.string.app_version),
                        it
                    )
                )
            }

            if (BuildConfig.DEBUG) {
                add(KeyValueItem("Firebase token", firebaseToken))

                add(
                    KeyValueItem(
                        "Publishable key (debug)",
                        accountRepository.publishableKey
                    )
                )
            }
        }

        profile.postValue(items)
    }

}
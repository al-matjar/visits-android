package com.hypertrack.android.ui.screens.visits_management.tabs.profile

import android.text.TextUtils.split
import android.view.Gravity.apply
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
import com.hypertrack.android.utils.formatters.DistanceFormatter
import com.hypertrack.logistics.android.github.BuildConfig
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.launch
import java.util.*

class ProfileViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val driverRepository: DriverRepository,
    private val hyperTrackService: HyperTrackService,
    private val accountRepository: AccountRepository,
    private val distanceFormatter: DistanceFormatter
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

            add(
                KeyValueItem(
                    "Locale",
                    Locale.getDefault().displayName
                )
            )

            add(
                KeyValueItem(
                    "Distance units",
                    distanceFormatter.formatDistance(EXAMPLE_DISTANCE)
                )
            )

            if (MyApplication.DEBUG_MODE) {
                add(KeyValueItem("Firebase token (debug)", firebaseToken))

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

    companion object {
        val EXAMPLE_DISTANCE = 2010.toMeters()
    }

}

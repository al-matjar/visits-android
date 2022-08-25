package com.hypertrack.android.ui.screens.visits_management.tabs.profile

import android.app.Activity
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hypertrack.android.messaging.VisitsMessagingService
import com.hypertrack.android.models.Imperial
import com.hypertrack.android.models.Metric
import com.hypertrack.android.models.Unspecified
import com.hypertrack.android.repository.MeasurementUnitsRepository
import com.hypertrack.android.repository.PublishableKeyRepository
import com.hypertrack.android.repository.access_token.AccessTokenRepository
import com.hypertrack.android.repository.access_token.UserAccessToken
import com.hypertrack.android.repository.user.UserRepository
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.ui.common.util.postValue
import com.hypertrack.android.ui.common.adapters.KeyValueItem
import com.hypertrack.android.ui.screens.visits_management.VisitsManagementFragmentDirections
import com.hypertrack.android.utils.*
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.launch
import java.util.*

class ProfileViewModel(
    baseDependencies: BaseViewModelDependencies,
    private val measurementUnitsRepository: MeasurementUnitsRepository,
    private val userRepository: UserRepository,
    private val publishableKeyRepository: PublishableKeyRepository,
    private val hyperTrackService: HyperTrackService,
    private val accessTokenRepository: AccessTokenRepository
) : BaseViewModel(baseDependencies) {

    private val measurementUnitsOptions = mapOf(
        Unspecified to resourceProvider.stringFromResource(R.string.profile_measurement_units_unspecified),
        Metric to resourceProvider.stringFromResource(R.string.profile_measurement_units_metric),
        Imperial to resourceProvider.stringFromResource(R.string.profile_measurement_units_imperial),
    )

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

        items.apply {
            userRepository.userData.load().toNullable()?.let { user ->
                user.email?.let {
                    add(
                        KeyValueItem(
                            osUtilsProvider.stringFromResource(R.string.email),
                            it.value
                        )
                    )
                }
                user.phone?.let {
                    add(
                        KeyValueItem(
                            osUtilsProvider.stringFromResource(R.string.phone_number),
                            it.value
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

            if (MyApplication.DEBUG_MODE) {
                val firebaseToken = VisitsMessagingService.getFirebaseToken().let {
                    when (it) {
                        is Success -> it.data
                        is Failure -> it.exception.format()
                    }
                }

                add(KeyValueItem("Firebase token (debug)", firebaseToken))

                add(
                    KeyValueItem(
                        "Publishable key (debug)",
                        publishableKeyRepository.publishableKey.load().toNullable()?.value.orEmpty()
                    )
                )
            }
        }

        profile.postValue(items)
    }

    fun createMeasurementUnitsAdapter(activity: Activity): SpinnerAdapter {
        return ArrayAdapter(
            activity,
            R.layout.item_spinner,
            measurementUnitsOptions.values.toList()
        )
    }

    fun getInitialMeasurementUnitItemIndex(): Int {
        return measurementUnitsOptions.keys.indexOf(
            measurementUnitsRepository.getMeasurementUnits()
        )
    }

    fun onMeasurementUnitItemSelected(index: Int) {
        measurementUnitsRepository.setMeasurementUnits(
            measurementUnitsOptions.keys.toList()[index]
        )
    }

    fun onOpenDontkillmyappClicked(activity: Activity) {
        osUtilsProvider.openUrl(
            activity,
            resourceProvider.stringFromResource(R.string.dontkillmyapp_url)
        )
    }

    fun onShowOnDashboardClick(activity: Activity) {
        val deviceId = hyperTrackService.deviceId
        val publishableKey = publishableKeyRepository.publishableKey
        osUtilsProvider.openUrl(
            activity,
            "https://dashboard.hypertrack.com/tracking/$deviceId?view=devices&publishable_key=$publishableKey&on_demand=l1"
        )
    }

    fun onInvalidateAccessTokenClick() {
        accessTokenRepository.accessToken = UserAccessToken("invalid_access_token")
    }

}

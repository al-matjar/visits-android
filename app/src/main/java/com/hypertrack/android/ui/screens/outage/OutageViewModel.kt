package com.hypertrack.android.ui.screens.outage

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.models.OutageType
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.use_case.handle_push.OutageNotification
import com.hypertrack.logistics.android.github.R

class OutageViewModel(
    baseViewModelDependencies: BaseViewModelDependencies
) : BaseViewModel(baseViewModelDependencies) {

    val viewState = MutableLiveData<ViewState>()

    fun init(outageNotification: OutageNotification) {
        viewState.postValue(
            ViewState(
                title = outageNotification.outageDisplayName,
                description = listOf(
                    outageNotification.outageDeveloperDescription,
                    outageNotification.userActionRequired
                ).filter { it.isNotBlank() }.joinToString("\n\n"),
                showOpenDontkillmyappButton = OutageType.SERVICE_TERMINATED_GROUP
                    .map { it.name }
                    .contains(outageNotification.outageType)
            )
        )
    }

    fun onOpenDontkillmyappClicked(activity: Activity) {
        osUtilsProvider.openUrl(
            activity,
            resourceProvider.stringFromResource(R.string.dontkillmyapp_url)
        )
    }

}

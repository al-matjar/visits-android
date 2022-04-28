package com.hypertrack.android.ui.screens.outage

import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.ui.base.BaseViewModel
import com.hypertrack.android.ui.base.BaseViewModelDependencies
import com.hypertrack.android.use_case.OutageNotification

class OutageViewModel(
    baseViewModelDependencies: BaseViewModelDependencies
) : BaseViewModel(baseViewModelDependencies) {

    val viewState = MutableLiveData<ViewState>()

    fun init(outageNotification: OutageNotification) {
        viewState.postValue(
            ViewState(
                title = outageNotification.outageDisplayName,
                description = listOf(
                    outageNotification.outageDescription,
                    outageNotification.userActionRequired
                ).filter { it.isNotBlank() }.joinToString("\n\n")
            )
        )
    }

    fun onError(e: Exception) {
        crashReportsProvider.logException(e)
    }

}

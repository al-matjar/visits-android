package com.hypertrack.android.ui.activity.use_case

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.mapSuccess
import com.hypertrack.android.utils.toSuspendCoroutine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Suppress("OPT_IN_USAGE", "EXPERIMENTAL_API_USAGE")
class CheckForUpdatesUseCase {

    fun execute(context: Context): Flow<Result<UpdatesResult>> {
        return suspend {
            AppUpdateManagerFactory.create(context).appUpdateInfo.toSuspendCoroutine()
        }.asFlow().mapSuccess { appUpdateInfo ->
            if (
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                !MyApplication.DEBUG_MODE
            ) {
                UpdateAvailable(
                    appUpdateInfo,
                    if (
                        appUpdateInfo.updatePriority() == UPDATE_PRIORITY_HIGH &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                    ) {
                        Immediate
                    } else {
                        Flexible
                    }
                )
            } else {
                NoUpdates
            }
        }
    }

    companion object {
        const val UPDATE_PRIORITY_HIGH = 5
        const val UPDATE_PRIORITY_LOW = 0
    }

}


package com.hypertrack.android.ui.activity.use_case

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.hypertrack.android.interactors.app.AppAction
import com.hypertrack.android.interactors.app.AppErrorAction
import com.hypertrack.android.ui.MainActivity
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Suppress("OPT_IN_USAGE")
class RequestUpdateIfAvailableUseCase(
    private val checkForUpdatesUseCase: CheckForUpdatesUseCase
) {

    fun execute(activity: Activity): Flow<AppAction?> {
        return checkForUpdatesUseCase.execute(activity).map { result ->
            when (result) {
                is Success -> {
                    val update = result.data
                    when (update) {
                        is UpdateAvailable -> {
                            AppUpdateManagerFactory.create(activity).startUpdateFlowForResult(
                                update.appUpdateInfo,
                                update.priority.value,
                                activity,
                                MainActivity.REQUEST_CODE_UPDATE
                            )
                        }
                        NoUpdates -> {
                        }
                    }
                    null
                }
                is Failure -> {
                    AppErrorAction(result.exception)
                }
            }

        }
    }

}


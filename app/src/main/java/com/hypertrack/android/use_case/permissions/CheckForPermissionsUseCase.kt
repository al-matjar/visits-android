package com.hypertrack.android.use_case.permissions

import com.hypertrack.android.interactors.PermissionsInteractor
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.tryAsResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class CheckForPermissionsUseCase(
    private val permissionsInteractor: PermissionsInteractor
) {
    fun execute(): Flow<Result<PermissionsResult>> {
        return suspend {
            tryAsResult {
                with(permissionsInteractor.checkPermissionsState()) {
                    when {
                        !foregroundLocationGranted || !activityTrackingGranted -> LocationOrActivityNotGranted
                        !backgroundLocationGranted -> BackgroundLocationNotGranted
                        else -> AllGranted
                    }
                }
            }
        }.asFlow()
    }
}

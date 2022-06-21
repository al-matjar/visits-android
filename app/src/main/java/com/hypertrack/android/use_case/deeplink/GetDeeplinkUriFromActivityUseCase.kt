package com.hypertrack.android.use_case.deeplink

import android.app.Activity
import android.net.Uri
import com.hypertrack.android.use_case.error.LogMessageToCrashlyticsUseCase
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Success
import com.hypertrack.android.utils.asSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

//todo log deeplink data
@Suppress("OPT_IN_USAGE")
class GetDeeplinkUriFromActivityUseCase(
    private val logMessageToCrashlyticsUseCase: LogMessageToCrashlyticsUseCase
) {

    // uri is not null when there is deeplink
    fun execute(activity: Activity): Flow<Result<Uri?>> {
        return flowOf(activity.intent)
            .flatMapConcat { intent ->
                intent?.let {
                    logMessageToCrashlyticsUseCase.execute(
                        "Deeplink intent received: ${intent.data}"
                    ).flatMapConcat {
                        intent.extras?.get(BRANCH_DATA_KEY)?.let {
                            logMessageToCrashlyticsUseCase.execute("Branch data: $it").map {
                                intent
                            }
                        } ?: flowOf(intent)
                    }.map {
                        it.data.asSuccess()
                    }
                } ?: flowOf(Success(null))
            }
    }

    companion object {
        private const val BRANCH_DATA_KEY = "branch_data"
    }

}

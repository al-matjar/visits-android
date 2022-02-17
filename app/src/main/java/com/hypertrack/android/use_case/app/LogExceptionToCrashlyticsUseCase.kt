package com.hypertrack.android.use_case.app

import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf

@Suppress("EXPERIMENTAL_API_USAGE", "OPT_IN_USAGE")
class LogExceptionToCrashlyticsUseCase(
    private val crashReportsProvider: CrashReportsProvider
) {

    fun execute(
        exception: Exception,
        metadata: Map<String, String> = mapOf()
    ): Flow<Unit> {
        return {
            crashReportsProvider.logException(exception, metadata)
        }.asFlow()
    }

}

//todo test
@Suppress("OPT_IN_USAGE")
fun <T> Flow<Result<T>>.logIfFailure(useCase: LogExceptionToCrashlyticsUseCase): Flow<T?> {
    return this.flatMapConcat {
        when (it) {
            is Success -> flowOf(it.data)
            is Failure -> {
                useCase.execute(it.exception)
                flowOf(null)
            }
        }
    }
}

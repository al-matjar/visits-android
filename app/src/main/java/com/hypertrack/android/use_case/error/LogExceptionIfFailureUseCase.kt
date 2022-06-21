package com.hypertrack.android.use_case.error

import com.hypertrack.android.utils.Failure
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class LogExceptionIfFailureUseCase(
    private val logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase
) {

    fun <T> execute(result: Result<T>): Flow<Unit> {
        return when (result) {
            is Success -> flowOf(Unit)
            is Failure -> logExceptionToCrashlyticsUseCase.execute(result.exception)
        }
    }

}

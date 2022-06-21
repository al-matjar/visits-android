package com.hypertrack.android.ui.screens.sign_in.use_case

import com.hypertrack.android.ui.common.use_case.ShowErrorUseCase
import com.hypertrack.android.use_case.error.LogExceptionToCrashlyticsUseCase
import com.hypertrack.android.use_case.deeplink.DeeplinkFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@Suppress("OPT_IN_USAGE")
class HandleDeeplinkFailureUseCase(
    private val logExceptionToCrashlyticsUseCase: LogExceptionToCrashlyticsUseCase,
    private val showErrorUseCase: ShowErrorUseCase
) {

    fun execute(failure: DeeplinkFailure): Flow<Unit> {
        return flowOf(Unit).onEach {
            logExceptionToCrashlyticsUseCase.execute(failure.toException())
        }.map {
            failure.toTextError()
        }.flatMapConcat {
            showErrorUseCase.execute(it)
        }
    }

}

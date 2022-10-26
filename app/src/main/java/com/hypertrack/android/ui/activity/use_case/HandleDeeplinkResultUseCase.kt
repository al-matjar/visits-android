package com.hypertrack.android.ui.activity.use_case

import com.hypertrack.android.deeplink.BranchErrorException
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.deeplink.NoDeeplink
import com.hypertrack.android.di.Injector.crashReportsProvider
import com.hypertrack.android.interactors.app.DeeplinkCheckedAction
import com.hypertrack.android.use_case.deeplink.GetBranchDataFromAppBackendUseCase
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.toFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

@Suppress("OPT_IN_USAGE")
class HandleDeeplinkResultUseCase(
    private val getBranchDataFromAppBackendUseCase: GetBranchDataFromAppBackendUseCase
) {

    fun execute(deeplinkResult: DeeplinkResult): Flow<DeeplinkCheckedAction> {
        return if (deeplinkResult is DeeplinkError
            && deeplinkResult.exception is BranchErrorException
            && deeplinkResult.exception.isBranchConnectionError
        ) {
            if (deeplinkResult.deeplinkUri != null) {
                getBranchDataFromAppBackendUseCase.execute(deeplinkResult.deeplinkUri)
                    .map { DeeplinkCheckedAction(it) }
            } else {
                DeeplinkCheckedAction(deeplinkResult).toFlow()
            }
        } else {
            DeeplinkCheckedAction(deeplinkResult).toFlow()
        }
    }

}

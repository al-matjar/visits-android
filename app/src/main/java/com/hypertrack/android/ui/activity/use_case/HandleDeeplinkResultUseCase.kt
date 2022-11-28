package com.hypertrack.android.ui.activity.use_case

import com.hypertrack.android.deeplink.BranchErrorException
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.interactors.app.DeeplinkCheckedAction
import com.hypertrack.android.ui.screens.sign_in.use_case.result.BranchReferrer
import com.hypertrack.android.ui.screens.sign_in.use_case.result.InvalidDeeplink
import com.hypertrack.android.ui.screens.sign_in.use_case.result.ValidDeeplink
import com.hypertrack.android.use_case.deeplink.GetBranchDataFromAppBackendUseCase
import com.hypertrack.android.use_case.deeplink.ValidateDeeplinkUrlUseCase
import com.hypertrack.android.use_case.deeplink.exception.DeeplinkUriUnsuitableForBackendException
import com.hypertrack.android.use_case.deeplink.exception.InvalidDeeplinkUrlException
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.flatMapSuccess
import com.hypertrack.android.utils.mapSuccess
import com.hypertrack.android.utils.toFlow
import kotlinx.coroutines.flow.Flow
import com.hypertrack.android.utils.Result

@Suppress("OPT_IN_USAGE")
class HandleDeeplinkResultUseCase(
    private val getBranchDataFromAppBackendUseCase: GetBranchDataFromAppBackendUseCase,
    private val validateDeeplinkUrlUseCase: ValidateDeeplinkUrlUseCase,
) {

    fun execute(deeplinkResult: DeeplinkResult): Flow<Result<DeeplinkCheckedAction>> {
        return if (deeplinkResult is DeeplinkError
            && deeplinkResult.exception is BranchErrorException
            && deeplinkResult.exception.isBranchConnectionError
        ) {
            if (deeplinkResult.deeplinkUri != null) {
                validateDeeplinkUrlUseCase.execute(deeplinkResult.deeplinkUri.toString())
                    .flatMapSuccess {
                        when (it) {
                            is ValidDeeplink -> {
                                getBranchDataFromAppBackendUseCase.execute(it)
                            }
                            is BranchReferrer -> {
                                DeeplinkError(
                                    DeeplinkUriUnsuitableForBackendException(),
                                    null
                                ).asSuccess().toFlow()
                            }
                            is InvalidDeeplink -> {
                                DeeplinkError(InvalidDeeplinkUrlException(), null).asSuccess()
                                    .toFlow()
                            }
                        }
                    }.mapSuccess { DeeplinkCheckedAction(it) }
            } else {
                DeeplinkCheckedAction(deeplinkResult).asSuccess().toFlow()
            }
        } else {
            DeeplinkCheckedAction(deeplinkResult).asSuccess().toFlow()
        }
    }

}

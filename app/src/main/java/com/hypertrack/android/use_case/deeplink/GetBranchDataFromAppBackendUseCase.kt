package com.hypertrack.android.use_case.deeplink

import android.net.Uri
import com.hypertrack.android.api.api_interface.AppBackendApi
import com.hypertrack.android.api.models.BranchLinkBody
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.ui.screens.sign_in.use_case.result.ValidDeeplink
import com.hypertrack.android.use_case.error.LogMessageToCrashlyticsUseCase
import com.hypertrack.android.utils.asFailure
import com.hypertrack.android.utils.asSuccess
import com.hypertrack.android.utils.createAnyMapAdapter
import com.hypertrack.android.utils.exception.SimpleException
import com.hypertrack.android.utils.flatMapSuccess
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import retrofit2.HttpException
import com.hypertrack.android.utils.Result


@Suppress("OPT_IN_USAGE", "BlockingMethodInNonBlockingContext")
class GetBranchDataFromAppBackendUseCase(
    private val appBackendApi: AppBackendApi,
    private val logMessageToCrashlyticsUseCase: LogMessageToCrashlyticsUseCase,
    private val moshi: Moshi
) {
    fun execute(validDeeplink: ValidDeeplink): Flow<Result<DeeplinkParams>> {
        return logMessageToCrashlyticsUseCase.execute(
            "Getting Branch data from App Backend"
        ).flatMapConcat {
            suspend {
                try {
                    appBackendApi.getBranchData(
                        BranchLinkBody(linkUrl = validDeeplink.url)
                    ).let { response ->
                        if (response.isSuccessful) {
                            response.body()?.let { responseString ->
                                logMessageToCrashlyticsUseCase.execute(responseString)
                                moshi.createAnyMapAdapter().fromJson(responseString)
                                    ?.let { responseMap ->
                                        try {
                                            val branchData = responseMap[KEY_DATA]
                                                    as Map<String, Any>
                                            DeeplinkParams(branchData).asSuccess()
                                        } catch (e: Exception) {
                                            SimpleException(
                                                "Invalid branch response: $validDeeplink $response $responseString"
                                            ).asFailure()
                                        }
                                    } ?: NullPointerException().asFailure()
                            } ?: NullPointerException().asFailure()
                        } else {
                            HttpException(response).asFailure()
                        }
                    }
                } catch (e: Exception) {
                    e.asFailure()
                }
            }.asFlow()
        }
    }

    companion object {
        private const val KEY_DATA = "data"
    }
}

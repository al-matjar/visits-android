package com.hypertrack.android.use_case.deeplink

import android.net.Uri
import com.hypertrack.android.api.api_interface.AppBackendApi
import com.hypertrack.android.api.models.BranchLinkBody
import com.hypertrack.android.deeplink.DeeplinkError
import com.hypertrack.android.deeplink.DeeplinkParams
import com.hypertrack.android.deeplink.DeeplinkResult
import com.hypertrack.android.use_case.app.LogMessageToCrashlyticsUseCase
import com.hypertrack.android.utils.createAnyMapAdapter
import com.hypertrack.android.utils.exception.SimpleException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import retrofit2.HttpException


@Suppress("OPT_IN_USAGE", "BlockingMethodInNonBlockingContext")
class GetBranchDataFromAppBackendUseCase(
    private val appBackendApi: AppBackendApi,
    private val logMessageToCrashlyticsUseCase: LogMessageToCrashlyticsUseCase,
    private val moshi: Moshi
) {
    fun execute(deeplinkUri: Uri): Flow<DeeplinkResult> {
        return logMessageToCrashlyticsUseCase.execute("Getting Branch data from App Backend")
            .flatMapConcat {
                suspend {
                    try {
                        appBackendApi.getBranchData(
                            BranchLinkBody(linkUrl = deeplinkUri.toString())
                        ).let { response ->
                            if (response.isSuccessful) {
                                response.body()?.let { responseString ->
                                    logMessageToCrashlyticsUseCase.execute(responseString)
                                    moshi.createAnyMapAdapter().fromJson(responseString)
                                        ?.let { responseMap ->
                                            try {
                                                val branchData = responseMap[KEY_DATA]
                                                        as Map<String, Any>
                                                DeeplinkParams(branchData)
                                            } catch (e: Exception) {
                                                DeeplinkError(
                                                    SimpleException(
                                                        "Invalid branch response: $response"
                                                    ),
                                                    deeplinkUri
                                                )
                                            }
                                        } ?: DeeplinkError(NullPointerException(), deeplinkUri)
                                } ?: DeeplinkError(NullPointerException(), deeplinkUri)
                            } else {
                                DeeplinkError(HttpException(response), deeplinkUri)
                            }
                        }
                    } catch (e: Exception) {
                        DeeplinkError(e, deeplinkUri)
                    }
                }.asFlow()
            }
    }

//    // todo change to SLAB
//    private suspend fun getBranchData(): String {
//        val request = Request.Builder()
//            .url("https://api2.branch.io/v1/url?branch_key=key_live_cjSN7Pt0Xej1hTb1Ew2AybjbAqaVS636&url=https%3A%2F%2Fhypertrack-logistics.app.link%2F1oF0VcDvYgb")
//            .build()
//        return OkHttpClient.Builder().build().newCall(request).execute().body!!.string()
//    }

    companion object {
        const val KEY_DATA = "data"
    }
}

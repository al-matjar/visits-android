package com.hypertrack.android.api.api_interface

import com.hypertrack.android.api.models.BranchLinkBody
import com.hypertrack.android.use_case.app.CreateUserScopeUseCase.Companion.BASE_URL
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface AppBackendApi {

    @POST("branch_redirect")
    suspend fun getBranchData(
        @Body body: BranchLinkBody,
    ): Response<String>

    companion object {
        const val APP_BACKEND_URL = BASE_URL
    }

}

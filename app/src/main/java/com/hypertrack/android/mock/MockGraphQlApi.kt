package com.hypertrack.android.mock

import com.hypertrack.android.api.GraphQlApi

class MockGraphQlApi(private val remoteApi: GraphQlApi) : GraphQlApi by remoteApi {

//    override suspend fun getPlacesVisits(body: GraphQlApi.PlacesVisitsBody): Response<GraphQlApi.GraphQlResponse<Map<String, Any>>> {
//        return Response.success(GraphQlApi.GraphQlResponse(mapOf()))
//    }
}

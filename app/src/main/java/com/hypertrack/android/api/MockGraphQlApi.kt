package com.hypertrack.android.api

import retrofit2.Response

class MockGraphQlApi(private val remoteApi: GraphQlApi) : GraphQlApi by remoteApi {

//    override suspend fun getPlacesVisits(body: GraphQlApi.PlacesVisitsBody): Response<GraphQlApi.GraphQlResponse<Map<String, Any>>> {
//        return Response.success(GraphQlApi.GraphQlResponse(mapOf()))
//    }
}
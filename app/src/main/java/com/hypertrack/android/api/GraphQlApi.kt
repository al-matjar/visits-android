package com.hypertrack.android.api

import com.hypertrack.android.utils.MyApplication
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GraphQlApi {

    @POST("graphql")
    @Headers("X-Api-Key: ${MyApplication.GRAPHQL_API_KEY}")
    suspend fun getPlacesVisits(@Body body: PlacesVisitsBody): Response<PlaceVisitsResponse>

    @JsonClass(generateAdapter = true)
    class PlacesVisitsBody(query: String) : QueryBody(query, mapOf())

    @JsonClass(generateAdapter = true)
    class PlaceVisitsResponse(val data: Map<String, RemoteDayVisitsStats>)

    @JsonClass(generateAdapter = true)
    open class QueryBody(
        val query: String,
        val variables: Map<String, Any>,
    )

}
package com.hypertrack.android.api.graphql

import com.hypertrack.android.api.graphql.models.GraphQlHistory
import com.hypertrack.android.api.graphql.models.GraphQlPlaceVisits
import com.hypertrack.android.api.graphql.queries.HistoryQuery
import com.hypertrack.android.api.graphql.queries.PlacesVisitsQuery
import com.hypertrack.android.api.graphql.queries.QueryBody
import com.hypertrack.android.utils.MyApplication
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface GraphQlApi {

    @POST("{graphql}")
    @Headers("X-Api-Key: ${MyApplication.GRAPHQL_API_KEY}")
    suspend fun getPlacesVisits(
        @Body body: QueryBody<PlacesVisitsQuery>,
        @Path("graphql") graphql: String = "graphql"
    ): Response<GraphQlResponse<GraphQlPlaceVisits>>

    @POST("{graphql}")
    @Headers("X-Api-Key: ${MyApplication.GRAPHQL_API_KEY}")
    suspend fun getHistory(
        @Body body: QueryBody<HistoryQuery>,
        @Path("graphql") graphql: String = "graphql"
    ): Response<GraphQlResponse<GraphQlDataWrapper<GraphQlHistory>>>

    @JsonClass(generateAdapter = true)
    class GraphQlResponse<T>(
        val data: T?,
        val errors: List<GraphQlError>?
    )

    @JsonClass(generateAdapter = true)
    class GraphQlError(
        val message: String,
        val errorType: String?,
        val path: List<String>,
    ) {
        override fun toString(): String {
            val path = path.joinToString(",")
            return "$path: $errorType - $message"
        }
    }

    @JsonClass(generateAdapter = true)
    class GraphQlDataWrapper<T>(
        @field:Json(name = "result") val result: T
    )

}

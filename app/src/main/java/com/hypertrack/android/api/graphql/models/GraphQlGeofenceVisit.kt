package com.hypertrack.android.api.graphql.models

import com.hypertrack.android.api.Arrival
import com.hypertrack.android.api.Exit
import com.hypertrack.android.api.RouteTo
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GraphQlGeofenceVisit(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "arrival") val arrival: Arrival,
    @field:Json(name = "exit") val exit: Exit?,
    @field:Json(name = "route_to") val routeTo: RouteTo?,
    @field:Json(name = "geofence") val geofence: GraphQlGeofence,
    @field:Json(name = "address") val address: GraphQlAddress?,
) {
    companion object {
        const val FIELDS_QUERY = """
            id 
            arrival {
                recorded_at
            }
            exit {
                recorded_at
            }
            route_to {
                distance 
                duration
            }
            geofence {
                geofence_id
                geometry {
                    type
                    center
                    vertices
                }
                metadata
            }
            address {
                address
                place
            }
        """
    }
}

package com.hypertrack.android.api

import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.*
import com.hypertrack.android.utils.toBase64
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

//todo move api entities elsewhere
interface ApiInterface {

    @POST("client/devices/{device_id}/start")
    suspend fun clockIn(@Path("device_id") deviceId: String)

    @POST("client/devices/{device_id}/stop")
    suspend fun clockOut(@Path("device_id") deviceId: String)

    @POST("client/devices/{device_id}/image")
    suspend fun persistImage(
        @Path("device_id") deviceId: String,
        @Body encodedImage: EncodedImage,
    ): Response<ImageResponse>

    @GET("client/devices/{device_id}/image/{image_id}")
    suspend fun getImage(
        @Path("device_id") deviceId: String,
        @Path("image_id") imageId: String,
    ): Response<EncodedImage>

//    @GET("client/geofences?include_markers=true")
//    suspend fun getGeofences(
//        @Query("pagination_token") paginationToken: String?,
//        @Query("device_id") deviceId: String,
//        @Query("include_archived") includeArchived: Boolean = false,
//        @Query("sort_nearest") sortNearest: Boolean = true,
//    ): Response<GeofenceResponse>

    @GET("client/devices/{device_id}/geofences")
    suspend fun getDeviceGeofences(
        @Path("device_id") deviceId: String,
        @Query("geohash") geohash: String? = null,
        @Query("pagination_token") paginationToken: String? = null,
        @Query("include_archived") includeArchived: Boolean = false,
        @Query("include_markers") includeMarkers: Boolean = true,
        @Query("sort_nearest") sortNearest: Boolean = true,
    ): Response<GeofenceResponse>

    @GET("client/geofences/{geofence_id}")
    suspend fun getGeofenceMetadata(
        @Path("geofence_id") geofenceId: String,
    ): Response<Geofence>

    @POST("client/devices/{device_id}/geofences")
    suspend fun createGeofences(
        @Path("device_id") deviceId: String,
        @Body params: GeofenceParams
    ): Response<List<Geofence>>

    @DELETE("client/geofences/{geofence_id}")
    suspend fun deleteGeofence(@Path("geofence_id") geofence_id: String): Response<Unit>

    @GET("client/geofences/visits")
    suspend fun getAllGeofencesVisits(
        @Query("device_id") deviceId: String,
        @Query("pagination_token") paginationToken: String? = null,
    ): Response<VisitsResponse>

    @GET("client/geofences/visits")
    suspend fun getGeofenceVisits(
        @Query("geofence_id") geofenceId: String,
    ): Response<VisitsResponse>

    @GET("client/trips")
    suspend fun getTrips(
        @Query("device_id") deviceId: String,
        @Query("pagination_token") paginationToken: String
    ): Response<TripResponse>

    @POST("client/trips/")
    suspend fun createTrip(@Body params: TripParams): Response<Trip>

    @POST("client/trips/{trip_id}/complete")
    suspend fun completeTrip(@Path("trip_id") tripId: String): Response<Unit>

    @POST("client/trips/{trip_id}/orders")
    suspend fun addOrderToTrip(
        @Path("trip_id") tripId: String,
        @Body addOrderBody: AddOrderBody,
    ): Response<Trip>

    @POST("client/trips/{trip_id}/orders/{order_id}/complete")
    suspend fun completeOrder(
        @Path("trip_id") tripId: String,
        @Path("order_id") orderId: String,
    ): Response<Void>

    @POST("client/trips/{trip_id}/orders/{order_id}/cancel")
    suspend fun cancelOrder(
        @Path("trip_id") tripId: String,
        @Path("order_id") orderId: String,
    ): Response<Void>

    @POST("client/trips/{trip_id}/orders/{order_id}/disable")
    suspend fun snoozeOrder(
        @Path("trip_id") tripId: String,
        @Path("order_id") orderId: String,
    ): Response<Void>

    @POST("client/trips/{trip_id}/orders/{order_id}/enable")
    suspend fun unsnoozeOrder(
        @Path("trip_id") tripId: String,
        @Path("order_id") orderId: String,
    ): Response<Void>

    @PATCH("client/trips/{trip_id}/orders/{order_id}")
    suspend fun updateOrder(
        @Path("trip_id") tripId: String,
        @Path("order_id") orderId: String,
        @Body order: OrderBody
    ): Response<Trip>

    /**
     * client/devices/A24BA1B4-1234-36F7-8DD7-15D97C3FD912/history/2021-02-05?timezone=Europe%2FZaporozhye
     */
    @GET("client/devices/{device_id}/history/{day}")
    suspend fun getHistory(
        @Path("device_id") deviceId: String,
        @Path("day") day: String,
        @Query("timezone") timezone: String
    ): Response<HistoryResponse>

    @GET("client/devices/{device_id}/history")
    suspend fun getHistoryForPeriod(
        @Path("device_id") deviceId: String,
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<HistoryResponse>

    @GET("client/get_entity_data")
    suspend fun getIntegrations(
        @Query("search_string") query: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<IntegrationsResponse>
}

@JsonClass(generateAdapter = true)
data class AddOrderBody(
    @field:Json(name = "device_id") val deviceId: String,
    @field:Json(name = "orders") val orderCreationParams: List<OrderCreationParams>,
)

@JsonClass(generateAdapter = true)
data class OrderCreationParams(
    @field:Json(name = "order_id") val id: String,
    @field:Json(name = "destination") val destination: TripDestination,
)

@JsonClass(generateAdapter = true)
data class IntegrationsResponse(
    val data: List<Integration>,
)

@JsonClass(generateAdapter = true)
data class OrderBody(
    val metadata: Map<String, Any>,
)

@JsonClass(generateAdapter = true)
data class GeofenceParams(
    @field:Json(name = "geofences") val geofences: Set<GeofenceProperties>,
    @field:Json(name = "device_id") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class GeofenceProperties(
    @field:Json(name = "geometry") val geometry: Geometry,
    @field:Json(name = "metadata") val metadata: Map<String, Any>,
    @field:Json(name = "radius") val radius: Int?
)

@JsonClass(generateAdapter = true)
data class EncodedImage(
    @field:Json(name = "file_name") val filename: String?,
    @field:Json(name = "data") val data: String
) {
    constructor(filename: String, bitmap: Bitmap) : this(
        filename = filename,
        data = bitmap.toBase64()
    )
}

@JsonClass(generateAdapter = true)
data class TripResponse(
        @field:Json(name = "data") val trips: List<Trip>,
        @field:Json(name = "pagination_token") val paginationToken: String?
)

@JsonClass(generateAdapter = true)
data class GeofenceMarkersResponse(
    @field:Json(name = "data") val visits: List<GeofenceVisit>,
    @field:Json(name = "pagination_token") val next: String?
)

@JsonClass(generateAdapter = true)
data class GeofenceResponse(
    @field:Json(name = "data") val geofences: List<Geofence>,
    @field:Json(name = "pagination_token") val paginationToken: String?
)

@JsonClass(generateAdapter = true)
data class VisitsResponse(
    @field:Json(name = "data") val visits: List<GeofenceVisit>,
    @field:Json(name = "links") val links: PaginationTokenLinks?
) {
    val paginationToken: String? = links?.token
}

@JsonClass(generateAdapter = true)
class PaginationTokenLinks(
    @field:Json(name = "next") val nextUrl: String?
) {
    val token: String? by lazy {
        nextUrl?.let {
            Uri.parse(nextUrl).let { uri ->
                try {
                    uri.getQueryParameter("pagination_token")
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class ImageResponse(
    @field:Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class Trip(
    @field:Json(name = "trip_id") val id: String?,
    @field:Json(name = "views") val views: Views?,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "started_at") val createdAt: String,
    @field:Json(name = "metadata") val metadata: Map<String, Any>?,
    @field:Json(name = "destination") val destination: TripDestination?,
    @field:Json(name = "estimate") val estimate: RemoteEstimate?,
    @field:Json(name = "orders") val orders: List<Order>?,
)

@JsonClass(generateAdapter = true)
data class TripDestination(
    @field:Json(name = "address") val address: String? = null,
    @field:Json(name = "geometry") val geometry: Geometry,
    @field:Json(name = "radius") val radius: Int? = null,
    @field:Json(name = "arrived_at") val arrivedAt: String? = null
) {
    constructor(latLng: LatLng, address: String?) : this(
        address,
        Point(latitude = latLng.latitude, longitude = latLng.longitude),
        100,
        null
    )
}

@JsonClass(generateAdapter = true)
data class Views(
        @field:Json(name = "share_url") val shareUrl: String,
        @field:Json(name = "embed_url") val embedUrl: String?
)

@JsonClass(generateAdapter = true)
data class Geofence(
    @field:Json(name = "geofence_id") val geofence_id: String,
    @field:Json(name = "device_id") val deviceId: String?,
    @field:Json(name = "created_at") val created_at: String,
    @field:Json(name = "metadata") val metadata: Map<String, Any>?,
    @field:Json(name = "geometry") val geometry: Geometry,
    @field:Json(name = "markers") val marker: GeofenceMarkersResponse?,
    @field:Json(name = "radius") val radius: Int?,
    @field:Json(name = "address") val address: String?,
    @field:Json(name = "archived") val archived: Boolean?,
) {

    val latitude: Double
        get() = geometry.latitude
    val longitude: Double
        get() = geometry.longitude
    val visitedAt: String
        get() = marker?.visits?.first()?.arrival?.recordedAt ?: ""

    val type: String
        get() = geometry.type
}

class Point(
        @field:Json(name = "coordinates") override val coordinates: List<Double>
) : Geometry() {
    constructor(latitude: Double, longitude: Double) : this(listOf(longitude, latitude))

    override val type: String
        get() = "Point"

    override val latitude: Double
        get() = coordinates[1]

    override val longitude: Double
        get() = coordinates[0]
}

data class Polygon(
    @field:Json(name = "coordinates") override val coordinates: List<List<List<Double>>>
) : Geometry() {
    override val type: String
        get() = "Polygon"
    override val latitude: Double
        get() {
            return coordinates[0].map { it[1] }.average()
        }
    override val longitude: Double
        get() {
            return coordinates[0].map { it[0] }.average()
        }
}

abstract class Geometry {
    abstract val coordinates: List<*>
    abstract val type: String
    abstract val latitude: Double
    abstract val longitude: Double
}

@JsonClass(generateAdapter = true)
data class GeofenceVisit(
    @field:Json(name = "geofence_id") val geofenceId: String,
    //todo why null? and non-null on graphQl schema
    @field:Json(name = "marker_id") val markerId: String?,
    @field:Json(name = "device_id") val deviceId: String,
    @field:Json(name = "arrival") val arrival: Arrival,
    @field:Json(name = "exit") val exit: Exit?,
    @field:Json(name = "geometry") val geometry: Geometry?,
    @field:Json(name = "route_to") val routeTo: RouteTo?,
    @field:Json(name = "duration") val duration: Int?,
    @field:Json(name = "address") val address: String?,
    @field:Json(name = "metadata") val metadata: GeofenceMetadata?
)

@JsonClass(generateAdapter = true)
data class Arrival(@field:Json(name = "recorded_at") val recordedAt: String = "")

@JsonClass(generateAdapter = true)
data class Exit(@field:Json(name = "recorded_at") val recordedAt: String = "")

@JsonClass(generateAdapter = true)
data class RouteTo(
    @field:Json(name = "idle_time") val idleTime: Int?,
    @field:Json(name = "distance") val distance: Int?,
    @field:Json(name = "duration") val duration: Int?
)

@JsonClass(generateAdapter = true)
data class HistoryResponse(
    @field:Json(name = "locations") val locations: Locations,
    @field:Json(name = "markers") val markers: List<HistoryMarker>,
    @field:Json(name = "active_duration") val activeDuration: Int,
    @field:Json(name = "distance") val distance: Int,
    @field:Json(name = "duration") val duration: Int,
    @field:Json(name = "geofences_visited") val geofencesCount: Int,
    @field:Json(name = "geotags") val geotagsCount: Int,
    @field:Json(name = "inactive_duration") val inactiveDuration: Int,
    // Skip reasons for now as constants aren't deserialized automatically but their usage is questionable
//    @field:Json(name = "inactive_reasons") val inactiveReasons: List<String>,
    @field:Json(name = "stop_duration") val stopDuration: Int,
    @field:Json(name = "tracking_rate") val trackingRate: Double,
    @field:Json(name = "trips") val tripsCount: Int,
    @field:Json(name = "walk_duration") val walkDuration: Int,
    @field:Json(name = "drive_duration") val driveDuration: Int?,
    @field:Json(name = "steps") val stepsCount: Int?,
    @field:Json(name = "trips_destinations_visited") val tripsDestinationsVisited: Int?,
    @field:Json(name = "trips_destinations_visited_duration") val tripsDestinationsVisitedDuration: Int?,
    @field:Json(name = "trips_on_time") val tripsOnTime: Int?,
    @field:Json(name = "trips_estimated_distance") val tripsEstimatedDistance: Int?,
    @field:Json(name = "geotags_route_to_duration") val geotagsRouteToDuration: Int?,
    @field:Json(name = "geofences_visited_duration") val geofecesVisitedDuration: Int?,
    @field:Json(name = "geofences_route_to_duration") val geofecesRouteToDuration: Int?,
    @field:Json(name = "geofences_route_to_idle_duration") val geofecesRouteToIdleDuration: Int?,
)

interface HistoryMarker {
    val markerId: String?
    val type: String
    val data: Any
}

@JsonClass(generateAdapter = true)
data class HistoryStatusMarker(
    @field:Json(name = "marker_id") override val markerId: String?,
    @field:Json(name = "type") override val type: String = "device_status",
    @field:Json(name = "data") override val data: HistoryStatusMarkerData
) : HistoryMarker

@JsonClass(generateAdapter = true)
data class HistoryStatusMarkerData(
        @field:Json(name = "value") val value: String,
        @field:Json(name = "activity") val activity: String?,
        @field:Json(name = "reason") val reason: String?,
        @field:Json(name = "duration") val duration: Int,
        @field:Json(name = "start") val start: MarkerTerminal,
        @field:Json(name = "end") val end: MarkerTerminal,
        @field:Json(name = "steps") val steps: Int?,
        @field:Json(name = "distance") val distance: Int?,
        @field:Json(name = "address") val address: String?,

)

// Do not be misguided by name. It's a geotag.
@JsonClass(generateAdapter = true)
data class HistoryTripMarker(
    @field:Json(name = "marker_id") override val markerId: String?,
    @field:Json(name = "type") override val type: String = "trip_marker",
    @field:Json(name = "data") override val data: HistoryTripMarkerData,
) : HistoryMarker

@JsonClass(generateAdapter = true)
data class HistoryTripMarkerData(
        @field:Json(name = "recorded_at") val recordedAt: String,
        @field:Json(name = "metadata") val metadata: Map<String, Any>?,
        @field:Json(name = "location") val location: HistoryTripMarkerLocation?,
        @field:Json(name = "route_to") val routeTo: MarkerRoute?,
)

@JsonClass(generateAdapter = true)
data class HistoryTripMarkerLocation(
    //[long, lat]
    val coordinates: List<Double>
)

@JsonClass(generateAdapter = true)
data class HistoryGeofenceMarker(
    @field:Json(name = "marker_id") override val markerId: String?,
    @field:Json(name = "type") override val type: String = "geofence",
    @field:Json(name = "data") override val data: HistoryGeofenceMarkerData,
) : HistoryMarker

@JsonClass(generateAdapter = true)
data class HistoryGeofenceMarkerData(
        @field:Json(name = "duration") val duration: Int?,
        @field:Json(name = "arrival") val arrival: HistoryGeofenceMarkerArrival,
        @field:Json(name = "exit") val exit: HistoryGeofenceMarkerArrival?,
        @field:Json(name = "geofence") val geofence: HistoryGeofenceMarkerGeofence,
)

@JsonClass(generateAdapter = true)
data class HistoryGeofenceMarkerGeofence(
    @field:Json(name = "geofence_id") val geofenceId: String,
    @field:Json(name = "metadata") val metadata: Map<String, Any>?,
)

@JsonClass(generateAdapter = true)
data class HistoryGeofenceMarkerArrival(@field:Json(name = "location") val location: HistoryGeofenceArrivalLocation)

@JsonClass(generateAdapter = true)
data class HistoryGeofenceArrivalLocation(
        @field:Json(name = "geometry") val geometry: Geometry?,
        @field:Json(name = "recorded_at") val recordedAt: String,
)

@JsonClass(generateAdapter = true)
data class MarkerRoute(
        @field:Json(name = "distance") val distance: Int,
        @field:Json(name = "duration") val duration: Int,
)

@JsonClass(generateAdapter = true)
data class MarkerTerminal(
        @field:Json(name = "location") val location: MarkerLocation?,
        @field:Json(name = "recorded_at") val recordedAt: String,
)

@JsonClass(generateAdapter = true)
data class MarkerLocation(
    @field:Json(name = "geometry") val geometry: Geometry,
    @field:Json(name = "recorded_at") val recordedAt: String,
)

@JsonClass(generateAdapter = true)
data class Locations(
        @field:Json(name = "coordinates") val coordinates: List<HistoryCoordinate>,
        @field:Json(name = "type") val type: String
)

class HistoryCoordinate(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val timestamp: String,
)

@JsonClass(generateAdapter = true)
class TripParams(
    @field:Json(name = "device_id") val deviceId: String,
    @field:Json(name = "destination") val destination: TripDestination? = null,
    @field:Json(name = "orders") val orders: List<OrderParams>? = null
) {
    constructor(deviceId: String) : this(deviceId, null)
    constructor(deviceId: String, latitude: Double, longitude: Double) : this(
        deviceId, TripDestination(null, Point(listOf(longitude, latitude)), null)
    )
}

@JsonClass(generateAdapter = true)
class OrderParams(
    @field:Json(name = "order_id") val orderId: String,
    @field:Json(name = "destination") val destination: TripDestination?,
) {
}

package com.hypertrack.android.mock

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.graphql.models.GraphQlGeotagMarker
import com.hypertrack.android.api.graphql.models.GraphQlHistory
import com.hypertrack.android.api.models.Coordinate
import com.hypertrack.android.api.models.RemoteLocation
import com.hypertrack.android.mock.GeofenceVisitMockData.createGraphQlGeofenceVisit
import com.hypertrack.android.ui.common.util.copy
import com.hypertrack.android.use_case.app.AppCreationUseCase
import com.hypertrack.android.utils.datetime.toIso
import java.time.ZonedDateTime

// don't delete unusable code here, it can be used in main sources to mock responses
object MockData {

    val moshi = AppCreationUseCase.createMoshi()

    val PALO_ALTO_LAT_LNG = LatLng(37.381451, -122.063616)

    fun createGraphQlHistory(): GraphQlHistory {
        return GraphQlHistory(
            listOf(
                createGraphQlGeofenceVisit(
                    ZonedDateTime.now(), LatLng(
                        PALO_ALTO_LAT_LNG.latitude + 0.01,
                        PALO_ALTO_LAT_LNG.longitude
                    )
                ),
                createGraphQlGeofenceVisit(
                    ZonedDateTime.now(), LatLng(
                        PALO_ALTO_LAT_LNG.latitude,
                        PALO_ALTO_LAT_LNG.longitude + 0.01
                    )
                ),
            ),
            listOf(),
            listOf(
                GraphQlGeotagMarker(
                    "1",
                    ZonedDateTime.now().toIso(),
                    createRemoteLocation(
                        PALO_ALTO_LAT_LNG.copy(longitude = PALO_ALTO_LAT_LNG.longitude - 0.01)
                    ),
                    "{}"
                )
            ),
            listOf(
                createRemoteLocation(
                    PALO_ALTO_LAT_LNG.copy(latitude = PALO_ALTO_LAT_LNG.latitude - 0.01)
                ),
                createRemoteLocation(
                    PALO_ALTO_LAT_LNG.copy(latitude = PALO_ALTO_LAT_LNG.latitude - 0.02)
                )
            ),
            12354,
            22354,
            32345,
            4235,
            5345234,
//            64523,
//            72354
        )
    }


    private fun createRemoteLocation(latLng: LatLng): RemoteLocation {
        return RemoteLocation(
            Coordinate(
                latLng.latitude,
                latLng.longitude
            ), ZonedDateTime.now().toIso()
        )
    }

}

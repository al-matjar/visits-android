package com.hypertrack.android.interactors.app.state

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.mock.MockData.PALO_ALTO_LAT_LNG
import com.hypertrack.android.ui.common.util.getGeoHash
import junit.framework.TestCase

class GeofencesForMapStateTest {
    companion object {
        fun geofencesForMapState(
            status: GeoCacheItemStatus,
            location: LatLng = PALO_ALTO_LAT_LNG
        ): GeofencesForMapState {
            return GeofencesForMapState(
                mapOf(
                    location.getGeoHash() to GeoCacheItem(
                        location.getGeoHash(),
                        status
                    )
                )
            )
        }
    }
}

package com.hypertrack.android.use_case.map

import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.screens.visits_management.tabs.history.MapData
import com.hypertrack.android.utils.Result
import com.hypertrack.android.utils.toFlow
import com.hypertrack.android.utils.tryAsFlow
import com.hypertrack.android.utils.tryAsResult
import kotlinx.coroutines.flow.Flow

class UpdateMapDataUseCase {

    // have to be executed on the main thread
    fun execute(map: HypertrackMapWrapper, data: MapData): Flow<Result<Unit>> {
        return tryAsFlow {
            map.clear()
            data.userLocation?.let {
                map.addUserLocation(it)
            }
            map.addPolyline(data.historyPolyline.polylineOptions)
            data.geotags.forEach { map.addMarker(it.markerOptions) }
            data.geofenceVisits.forEach { map.addMarker(it.markerOptions) }
        }
    }

}

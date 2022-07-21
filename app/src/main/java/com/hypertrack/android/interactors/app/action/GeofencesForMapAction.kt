package com.hypertrack.android.interactors.app.action

import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.ui.common.DataPage
import com.hypertrack.android.use_case.geofences.GeofencesPageResult
import com.hypertrack.android.utils.MyApplication
import com.hypertrack.android.utils.Result
import kotlinx.coroutines.Job

sealed class GeofencesForMapAction
data class GeofencesForMapLoadedAction(
    val geoHash: GeoHash,
    val result: GeofencesPageResult
) : GeofencesForMapAction()

data class LoadGeofencesForMapAction(val location: LatLng) : GeofencesForMapAction()
object ClearGeofencesForMapAction : GeofencesForMapAction()

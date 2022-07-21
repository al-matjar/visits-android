package com.hypertrack.android.use_case.geofences

import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.utils.MyApplication

sealed class GeofencesPageResult
data class PageFailure(
    val exception: Exception,
    val loaded: List<Geofence>,
    val nextPageToken: String?
) : GeofencesPageResult() {
    override fun toString(): String {
        return "${javaClass.simpleName}(loaded=${loaded.size}, nextPageToken=$nextPageToken, exception=$exception)"
    }
}

data class PageSuccess(val geofences: List<Geofence>) : GeofencesPageResult() {
    override fun toString(): String {
        return "${javaClass.simpleName}(geofences=${geofences.size})"
    }
}

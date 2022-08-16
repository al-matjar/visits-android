package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.models.RemoteEstimate
import com.hypertrack.android.utils.datetime.dateTimeFromString
import com.squareup.moshi.JsonClass
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
data class Estimate(
    val arriveAt: ZonedDateTime?,
    val route: List<LatLng>?
) {
    override fun toString(): String {
        return "${javaClass.simpleName}(arriveAt=$arriveAt, route=${route?.size})"
    }

    companion object {
        fun fromRemote(
            estimate: RemoteEstimate?,
        ): Estimate? {
            return estimate?.let {
                Estimate(estimate.arriveAt?.let { dateTimeFromString(it) },
                    estimate.route?.polyline?.coordinates?.map { LatLng(it[1], it[0]) })
            }
        }
    }
}

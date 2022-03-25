package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.interactors.PhotoForUpload
import com.hypertrack.android.api.models.RemoteEstimate
import com.hypertrack.android.models.Metadata
import com.hypertrack.android.api.models.RemoteOrder
import com.hypertrack.android.utils.datetime.dateTimeFromString
import com.squareup.moshi.JsonClass
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@JsonClass(generateAdapter = true)
data class Order(
    val id: String,
    val destinationLatLng: LatLng,
    val shortAddress: String?,
    val fullAddress: String?,
    val scheduledAt: ZonedDateTime?,
    val completedAt: ZonedDateTime?,
    val estimate: Estimate?,
    val _metadata: Metadata?,
    var status: OrderStatus,
    // persisted in local database
    var note: String? = null,
    //todo we should make it set of string, whole photos is stored here until we'll enable retrieving them from s3
    var photos: MutableSet<PhotoForUpload> = mutableSetOf(),
) {

    val metadata: Map<String, String>
        get() = _metadata?.otherMetadata ?: mapOf()

    val eta: ZonedDateTime?
        get() = estimate?.arriveAt

    val awaySeconds: Long?
        get() {
            return estimate?.let {
                it.arriveAt?.let { arriveAt ->
                    ChronoUnit.SECONDS.between(
                        ZonedDateTime.now(),
                        arriveAt
                    ).let {
                        if (it < 0) null else it
                    }
                }
            }
        }

    val metadataNote: String?
        get() = _metadata?.visitsAppMetadata?.note

    val routeToPolyline: List<LatLng>?
        get() = estimate?.route

    companion object {
        fun fromRemote(
            order: RemoteOrder,
            metadata: Metadata?,
            shortAddress: String?,
            fullAddress: String?,
            photos: MutableSet<PhotoForUpload> = mutableSetOf(),
            status: OrderStatus? = null,
            note: String? = null,
        ): Order {
            return Order(
                id = order.id,
                destinationLatLng = order.destination.geometry.let {
                    LatLng(it.latitude, it.longitude)
                },
                status = status ?: OrderStatus.fromString(order._status),
                scheduledAt = order.scheduledAt?.let { dateTimeFromString(it) },
                completedAt = order.completedAt?.let { dateTimeFromString(it) },
                estimate = Estimate.fromRemote(order.estimate),
                _metadata = metadata,
                note = note,
                photos = photos,
                shortAddress = shortAddress,
                fullAddress = fullAddress
            )
        }

        const val VISIT_NOTE_KEY = "visit_note"
        const val VISIT_PHOTOS_KEY = "_visit_photos"
    }

}

enum class OrderStatus(val value: String) {
    ONGOING("ongoing"),
    COMPLETED("completed"),
    CANCELED("cancelled"),
    SNOOZED("snoozed"),
    UNKNOWN("");

    companion object {
        fun fromString(str: String?): OrderStatus {
            for (i in values()) {
                if (str == i.value) {
                    return i
                }
            }
            return UNKNOWN
        }
    }
}

@JsonClass(generateAdapter = true)
data class Estimate(
    val arriveAt: ZonedDateTime?,
    val route: List<LatLng>?
) {
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

package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.TripDestination
import com.hypertrack.android.interactors.PhotoForUpload
import com.hypertrack.android.models.RemoteEstimate
import com.hypertrack.android.models.Metadata
import com.hypertrack.android.models.Order
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.utils.datetime.dateTimeFromString
import com.squareup.moshi.JsonClass
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@JsonClass(generateAdapter = true)
data class LocalOrder(
    val id: String,
    val destination: TripDestination,
    val scheduledAt: ZonedDateTime?,
    val completedAt: ZonedDateTime?,
    val estimate: Estimate?,
    val _metadata: Metadata?,
    var status: OrderStatus,
    //local
    //todo remove
    var isPickedUp: Boolean = true,
    var note: String? = null,
    //todo we should make it set of string, whole photos is stored here until we'll enable retrieving them from s3
    var photos: MutableSet<PhotoForUpload> = mutableSetOf(),
    val legacy: Boolean = false
) {

    val metadata: Map<String, String>
        get() = _metadata?.otherMetadata ?: mapOf()

    val destinationLatLng: LatLng
        get() = LatLng(destination.geometry.latitude, destination.geometry.longitude)

    //use address delegate
    val destinationAddress: String?
        get() = destination.address.nullIfBlank()

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
            order: Order,
            isPickedUp: Boolean = true,
            note: String? = null,
            metadata: Metadata?,
            legacy: Boolean = false,
            photos: MutableSet<PhotoForUpload> = mutableSetOf(),
            status: OrderStatus? = null,
        ): LocalOrder {
            return LocalOrder(
                id = order.id,
                destination = order.destination,
                status = status ?: OrderStatus.fromString(order._status),
                scheduledAt = order.scheduledAt?.let { dateTimeFromString(it) },
                completedAt = order.completedAt?.let { dateTimeFromString(it) },
                estimate = Estimate.fromRemote(order.estimate),
                _metadata = metadata,
                note = note,
                legacy = legacy,
                isPickedUp = isPickedUp,
                photos = photos,
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

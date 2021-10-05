package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.TripDestination
import com.hypertrack.android.interactors.PhotoForUpload
import com.hypertrack.android.models.Estimate
import com.hypertrack.android.models.Metadata
import com.hypertrack.android.models.Order
import com.hypertrack.android.ui.common.util.nullIfBlank
import com.hypertrack.android.utils.datetimeFromString
import com.squareup.moshi.JsonClass
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@JsonClass(generateAdapter = true)
data class LocalOrder(
    val id: String,
    val destination: TripDestination,
    val scheduledAt: ZonedDateTime?,
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

    @Suppress("UNCHECKED_CAST")
    constructor(
        order: Order,
        isPickedUp: Boolean = true,
        note: String? = null,
        metadata: Metadata?,
        legacy: Boolean = false,
        photos: MutableSet<PhotoForUpload> = mutableSetOf(),
        status: OrderStatus? = null
    ) : this(
        id = order.id,
        destination = order.destination,
        status = status ?: OrderStatus.fromString(order._status),
        scheduledAt = order.scheduledAt?.let { datetimeFromString(it) },
        estimate = order.estimate,
        _metadata = metadata,
        note = note,
        legacy = legacy,
        isPickedUp = isPickedUp,
        photos = photos,
    )

    val metadata: Map<String, String>
        get() = _metadata?.otherMetadata ?: mapOf()

    val destinationLatLng: LatLng
        get() = LatLng(destination.geometry.latitude, destination.geometry.longitude)

    //use address delegate
    val destinationAddress: String?
        get() = destination.address.nullIfBlank()

    val eta: ZonedDateTime?
        get() = estimate?.let {
            it.arriveAt?.let { arriveAt ->
                datetimeFromString(arriveAt)
            }
        }

    val awaySeconds: Long?
        get() = estimate?.let {
            it.arriveAt?.let { arriveAt ->
                ChronoUnit.SECONDS.between(
                    ZonedDateTime.now(),
                    datetimeFromString(arriveAt)
                ).let {
                    if (it < 0) null else it
                }
            }
        }

    val metadataNote: String?
        get() = _metadata?.visitsAppMetadata?.note

    val metadataPhotoIds: List<String>
        get() = _metadata?.visitsAppMetadata?.photos ?: listOf()

    companion object {
        const val VISIT_NOTE_KEY = "visit_note"
        const val VISIT_PHOTOS_KEY = "_visit_photos"
    }

}

enum class OrderStatus(val value: String) {
    ONGOING("ongoing"), COMPLETED("completed"), CANCELED("cancelled"), UNKNOWN("");

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
package com.hypertrack.android.models.local

import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.api.models.RemoteOrder
import com.hypertrack.android.interactors.PhotoForUpload
import com.hypertrack.android.models.Metadata
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
    }

}


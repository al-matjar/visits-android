package com.hypertrack.android.ui.common.map

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.TypedValue
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.models.History
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.models.local.Order
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R

class HypertrackMapWrapper(
    val googleMap: GoogleMap,
    private val osUtilsProvider: OsUtilsProvider,
    private val crashReportsProvider: CrashReportsProvider,
    private val params: MapParams
) {
    init {
        googleMap.uiSettings.apply {
            isScrollGesturesEnabled = params.enableScroll
            isZoomControlsEnabled = params.enableZoomKeys
            isMyLocationButtonEnabled = params.enableMyLocationButton
            isCompassEnabled = false
            isMapToolbarEnabled = false
            isRotateGesturesEnabled = false
        }

        try {
            @SuppressLint("MissingPermission")
            googleMap.isMyLocationEnabled = params.enableMyLocationIndicator
        } catch (_: Exception) {
        }
    }

    val cameraPosition: LatLng
        get() = googleMap.viewportPosition

    val geofenceMarkerIcon: BitmapDescriptor by lazy {
        BitmapDescriptorFactory.fromBitmap(
            osUtilsProvider.bitmapFromResource(
                R.drawable.ic_ht_departure_active
            )
        )
    }
    private val tripStartIcon = osUtilsProvider.bitmapDescriptorFromResource(
        R.drawable.starting_position
    )
    private val activeOrderIcon = osUtilsProvider.bitmapDescriptorFromResource(
        R.drawable.destination
    )
    private val completedOrderIcon = osUtilsProvider.bitmapDescriptorFromVectorResource(
        R.drawable.ic_order_completed
    )
    private val canceledOrderIcon = osUtilsProvider.bitmapDescriptorFromVectorResource(
        R.drawable.ic_order_canceled
    )
    private val userLocationIcon = osUtilsProvider.bitmapDescriptorFromVectorResource(
        R.drawable.ic_user_location
    )
    private val tripRouteColor = osUtilsProvider.colorFromResource(R.color.colorHyperTrackGreen)
    private val tripRouteWidth by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 3f,
            osUtilsProvider.getDisplayMetrics()
        )
    }
    private val tripLinePattern = listOf(
        Dash(tripRouteWidth * 2),
        Gap(tripRouteWidth)
    )

    fun setOnCameraMovedListener(listener: (LatLng) -> Unit) {
        googleMap.setOnCameraIdleListener { listener.invoke(googleMap.viewportPosition) }
    }

    fun addMarker(markerOptions: MarkerOptions): Marker? {
        return googleMap.addMarker(markerOptions)
    }

    fun addGeofenceShape(geofence: Geofence) {
        if (geofence.isPolygon) {
            googleMap.addPolygon(
                PolygonOptions()
                    .addAll(geofence.polygon!!)
                    .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                    .strokeColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                    .strokeWidth(3f)
                    .visible(true)
            )
        } else {
            geofence.radius.let { radius ->
                googleMap.addCircle(
                    CircleOptions()
                        .center(geofence.latLng)
                        .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                        .strokeColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                        .strokeWidth(3f)
                        .radius(radius.toDouble())
                        .visible(true)
                )
            }
            googleMap.addCircle(
                CircleOptions()
                    .center(geofence.latLng)
                    .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                    .strokeColor(Color.TRANSPARENT)
                    .radius(30.0)
                    .visible(true)
            )
        }
    }

    fun addGeofenceShape(latLng: LatLng, radius: Int): List<Circle> {
        val res = mutableListOf<Circle>()
        googleMap.addCircle(
            CircleOptions()
                .center(latLng)
                .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                .strokeColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                .strokeWidth(3f)
                .radius(radius.toDouble())
                .visible(true)
        ).also {
            res.add(it)
        }
        googleMap.addCircle(
            CircleOptions()
                .center(latLng)
                .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                .strokeColor(Color.TRANSPARENT)
                .radius((radius / 10).toDouble())
                .visible(true)
        ).also {
            res.add(it)
        }
        return res
    }

    fun addGeofenceMarker(geofence: Geofence) {
        val it = geofence
        if (geofence.isPolygon) {
            googleMap.addPolygon(
                PolygonOptions()
                    .addAll(geofence.polygon!!)
                    .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                    .strokeColor(osUtilsProvider.colorFromResource(R.color.colorGeofence))
                    .strokeWidth(3f)
                    .visible(true)
            )
        } else {
            it.radius?.let { radius ->
                googleMap.addCircle(
                    CircleOptions()
                        .radius(radius.toDouble())
                        .center(it.latLng)
                        .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                        .strokeColor(
                            osUtilsProvider.colorFromResource(
                                R.color.colorHyperTrackGreenSemitransparent
                            )
                        )
                )
            }
        }

        googleMap.addMarker(
            MarkerOptions()
                .icon(geofenceMarkerIcon)
                .snippet(it.id)
                .position(it.latLng)
                .anchor(0.5f, 0.5f)
        )
    }

    fun addNewGeofenceRadius(latLng: LatLng, radius: Int): Circle {
        return googleMap.addCircle(
            CircleOptions()
                .radius(radius.toDouble())
                .center(latLng)
                .strokePattern(listOf(Dash(30f), Gap(20f)))
                .fillColor(osUtilsProvider.colorFromResource(R.color.colorGeofenceFill))
                .strokeColor(
                    osUtilsProvider.colorFromResource(
                        R.color.colorHyperTrackGreenSemitransparent
                    )
                )
        )
    }

    fun addTrip(trip: LocalTrip) {
        val map = googleMap
        val tripStart =
            trip.orders.firstOrNull()?.estimate?.route?.firstOrNull()

        tripStart?.let {
            map.addMarker(
                MarkerOptions()
                    .position(it)
                    .anchor(0.5f, 0.5f)
                    .icon(tripStartIcon)
                    .zIndex(100f)
            )
        }

        trip.orders.forEach { order ->
            when (order.status) {
                OrderStatus.ONGOING, OrderStatus.COMPLETED -> {
                    order.estimate?.route?.let {
                        val options = if (order.status == OrderStatus.ONGOING) {
                            PolylineOptions()
                                .width(tripRouteWidth)
                                .color(tripRouteColor)
                                .pattern(tripLinePattern)
                        } else {
                            PolylineOptions()
                                .width(tripRouteWidth)
                                .color(tripRouteColor)
                                .pattern(tripLinePattern)
                        }

                        map.addPolyline(options.addAll(it))
                    }
                }
                else -> {
                }
            }

            map.addMarker(
                MarkerOptions()
                    .anchor(0.5f, 0.5f)
                    .icon(
                        when (order.status) {
                            OrderStatus.ONGOING -> {
                                activeOrderIcon
                            }
                            OrderStatus.COMPLETED -> {
                                completedOrderIcon
                            }
                            OrderStatus.CANCELED, OrderStatus.UNKNOWN, OrderStatus.SNOOZED -> {
                                canceledOrderIcon
                            }
                        }
                    )
                    .snippet(order.id)
                    .position(order.destinationLatLng)
                    .zIndex(100f)
            )
        }
    }

    fun animateCameraToTrip(trip: LocalTrip, userLocation: LatLng? = null) {
        try {
            val tripStart =
                trip.orders.firstOrNull()?.estimate?.route?.firstOrNull()

            if (trip.ongoingOrders.isNotEmpty()) {
                val bounds = LatLngBounds.builder().apply {
                    trip.ongoingOrders.forEach { order ->
                        include(order.destinationLatLng)
                        order.estimate?.route?.forEach {
                            include(it)
                        }
                    }
                    tripStart?.let { include(it) }
                    userLocation?.let { include(it) }
                }.build()
                //newLatLngBounds can cause crash if called before layout without map size
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
    }

    fun moveCamera(latLng: LatLng, zoom: Float? = null) {
        googleMap.moveCamera(latLng, zoom)
    }

    fun animateCamera(latLng: LatLng, zoom: Float? = null) {
        googleMap.animateCamera(latLng, zoom)
    }

    fun moveCamera(cameraUpdate: CameraUpdate) {
        googleMap.animateCamera(cameraUpdate)
    }

    fun setOnMapClickListener(listener: () -> Unit) {
        googleMap.setOnMapClickListener { listener.invoke() }
    }

    override fun toString(): String {
        return javaClass.simpleName
    }

    fun clear() {
        googleMap.clear()
    }

    fun addPolyline(polylineOptions: PolylineOptions): Polyline {
        return googleMap.addPolyline(polylineOptions)
    }

    fun setPadding(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
        googleMap.setPadding(left, top, right, bottom)
    }

    fun addOrder(order: Order) {
        googleMap.addMarker(
            MarkerOptions()
                .icon(geofenceMarkerIcon)
                .anchor(0.5f, 0.5f)
                .position(order.destinationLatLng)
                .title(order.shortAddress)
        )
    }

    fun addGeotagMarker(location: LatLng) {
        googleMap.addMarker(
            MarkerOptions()
                .icon(geofenceMarkerIcon)
                .position(location)
                .anchor(0.5f, 0.5f)
        )
    }

    fun animateCameraToBounds(bounds: LatLngBounds) {
        try {
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    bounds,
                    BOUNDS_CAMERA_PADDING
                )
            )
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
    }

    private fun History.asPolylineOptions(): PolylineOptions = this
        .locationTimePoints
        .map { it.first }
        .fold(PolylineOptions()) { options, point ->
            options.add(LatLng(point.latitude, point.longitude))
        }

    fun addUserLocation(latLng: LatLng?): Marker? {
        return latLng?.let {
            return googleMap.addMarker(
                MarkerOptions()
                    .icon(userLocationIcon)
                    .position(latLng)
                    .zIndex(Float.MAX_VALUE)
                    .anchor(0.5f, 0.5f)
            )
        }
    }

    companion object {
        const val DEFAULT_ZOOM = 13f
        const val BOUNDS_CAMERA_PADDING = 100
    }

    private class StyleAttrs {
        var tripRouteWidth = 0f
        var tripRouteColor = 0
    }

}

class MapParams(
    val enableScroll: Boolean,
    val enableZoomKeys: Boolean,
    val enableMyLocationButton: Boolean,
    val enableMyLocationIndicator: Boolean
)

fun GoogleMap.moveCamera(latLng: LatLng, zoom: Float?) {
    moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom ?: HypertrackMapWrapper.DEFAULT_ZOOM))
}

fun GoogleMap.animateCamera(latLng: LatLng, zoom: Float?) {
    animateCamera(
        CameraUpdateFactory.newLatLngZoom(
            latLng,
            zoom ?: HypertrackMapWrapper.DEFAULT_ZOOM
        )
    )
}

val GoogleMap.viewportPosition: LatLng
    get() {
        return cameraPosition.target
    }

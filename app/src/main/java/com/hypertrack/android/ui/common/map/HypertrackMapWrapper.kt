package com.hypertrack.android.ui.common

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.models.History
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.models.local.LocalOrder
import com.hypertrack.android.models.local.LocalTrip
import com.hypertrack.android.models.local.OrderStatus
import com.hypertrack.android.ui.common.delegates.OrderAddressDelegate
import com.hypertrack.android.ui.screens.visits_management.tabs.history.HistoryStyle
import com.hypertrack.android.utils.CrashReportsProvider
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

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

    val tripStartIcon = osUtilsProvider.bitmapDescriptorFromResource(
        com.hypertrack.maps.google.R.drawable.starting_position
    )
    val activeOrderIcon = osUtilsProvider.bitmapDescriptorFromResource(
        com.hypertrack.maps.google.R.drawable.destination
    )
    val completedOrderIcon = osUtilsProvider.bitmapDescriptorFromVectorResource(
        R.drawable.ic_order_completed, R.color.colorHyperTrackGreen
    )
    val canceledOrderIcon = osUtilsProvider.bitmapDescriptorFromVectorResource(
        R.drawable.ic_order_canceled, R.color.colorHyperTrackGreen
    )

    private val tripStyleAttrs by lazy {
        StyleAttrs().let { tripStyleAttrs ->
            tripStyleAttrs.tripRouteWidth = tripRouteWidth
            tripStyleAttrs.tripRouteColor =
                osUtilsProvider.colorFromResource(com.hypertrack.maps.google.R.color.ht_route)
            tripStyleAttrs
        }
    }

    private val tripRouteWidth by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 3f,
            osUtilsProvider.getDisplayMetrics()
        )
    }

    fun setOnCameraMovedListener(listener: (LatLng) -> Unit) {
        googleMap.setOnCameraIdleListener { listener.invoke(googleMap.viewportPosition) }
    }

    fun addGeofenceShape(geofence: LocalGeofence) {
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
            geofence.radius?.let { radius ->
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

    fun addGeofenceMarker(geofence: LocalGeofence) {
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
            trip.ongoingOrders.firstOrNull()?.estimate?.route?.polyline?.getPolylinePoints()
                ?.firstOrNull()

        tripStart?.let {
            map.addMarker(
                MarkerOptions()
                    .position(it)
                    .anchor(0.5f, 0.5f)
                    .icon(tripStartIcon)
                    .zIndex(100f)
            )
        }

        trip.ongoingOrders.forEach { order ->
            order.estimate?.route?.polyline?.getPolylinePoints()?.let {
                val options = if (order.status == OrderStatus.ONGOING) {
                    PolylineOptions()
                        .width(tripStyleAttrs.tripRouteWidth)
                        .color(tripStyleAttrs.tripRouteColor)
                        .pattern(
                            Arrays.asList(
                                Dash(tripStyleAttrs.tripRouteWidth * 2),
                                Gap(tripStyleAttrs.tripRouteWidth)
                            )
                        )
                } else {
                    PolylineOptions()
                        .width(tripStyleAttrs.tripRouteWidth)
                        .color(tripStyleAttrs.tripRouteColor)
                        .pattern(
                            Arrays.asList(
                                Dash(tripStyleAttrs.tripRouteWidth * 2),
                                Gap(tripStyleAttrs.tripRouteWidth)
                            )
                        )
                }

                map.addPolyline(options.addAll(it))
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
                    .position(order.destinationLatLng)
                    .zIndex(100f)
            )
        }
    }

    fun showHistory(history: History, style: HistoryStyle) {
        addPolyline(history.asPolylineOptions().color(style.activeColor))
    }

    fun animateCameraToTrip(trip: LocalTrip, userLocation: LatLng? = null) {
        val map = googleMap
        try {
            val tripStart =
                trip.orders.firstOrNull()?.estimate?.route?.polyline?.getPolylinePoints()
                    ?.firstOrNull()

            if (trip.ongoingOrders.isNotEmpty()) {
                val bounds = LatLngBounds.builder().apply {
                    trip.ongoingOrders.forEach { order ->
                        include(order.destinationLatLng)
                        order.estimate?.route?.polyline?.getPolylinePoints()?.forEach {
                            include(it)
                        }
                    }
                    tripStart?.let { include(it) }
                    userLocation?.let { include(it) }
                }.build()
                //newLatLngBounds can cause crash if called before layout without map size
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            }
        } catch (e: Exception) {
            crashReportsProvider.logException(e)
        }
    }

    fun moveCamera(latLng: LatLng, zoom: Float? = null) {
        GlobalScope.launch(Dispatchers.Main) {
            googleMap.moveCamera(latLng, zoom)
        }
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

    fun addPolyline(polylineOptions: PolylineOptions) {
        googleMap.addPolyline(polylineOptions)
    }

    fun setPadding(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) {
        googleMap.setPadding(left, top, right, bottom)
    }

    fun addOrder(order: LocalOrder, orderAddressDelegate: OrderAddressDelegate) {
        googleMap.addMarker(
            MarkerOptions()
                .icon(geofenceMarkerIcon)
                .anchor(0.5f, 0.5f)
                .position(order.destinationLatLng)
                .title(orderAddressDelegate.shortAddress(order))
        )
    }

    private fun History.asPolylineOptions(): PolylineOptions = this
        .locationTimePoints
        .map { it.first }
        .fold(PolylineOptions()) { options, point ->
            options.add(LatLng(point.latitude, point.longitude))
        }

    companion object {
        const val DEFAULT_ZOOM = 13f
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

val GoogleMap.viewportPosition: LatLng
    get() {
        return cameraPosition.target
    }

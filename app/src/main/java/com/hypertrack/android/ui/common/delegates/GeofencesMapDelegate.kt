package com.hypertrack.android.ui.common.delegates

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractorImpl
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.ui.common.map.HypertrackMapWrapper
import com.hypertrack.android.ui.common.ManagedObserver
import com.hypertrack.android.ui.common.util.toLatLng
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.TypeWrapper

open class GeofencesMapDelegate(
    private val context: Context,
    private val mapWrapper: HypertrackMapWrapper,
    private val placesInteractor: PlacesInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val onMarkerClickListener: (GeofenceId) -> Unit
) {

    private val managedObserver = ManagedObserver()

    init {
        placesInteractor.geofences.value?.let {
            updateGeofencesOnMap(mapWrapper, it.values.toList())
        }

        managedObserver.observeManaged(placesInteractor.geofences) {
            updateGeofencesOnMap(mapWrapper, it.values.toList())
        }

//        (placesInteractor as PlacesInteractorImpl).debugCacheState.observeManaged {
//            map.value?.let {
//                showMapDebugData()
//            }
//        }
    }

    open fun onCameraIdle() {
        placesInteractor.loadGeofencesForMap(mapWrapper.googleMap.cameraPosition.target)
    }

    protected open fun updateGeofencesOnMap(
        mapWrapper: HypertrackMapWrapper,
        geofences: List<Geofence>
    ) {
        //todo filter regarding to viewport
        mapWrapper.googleMap.clear()
        geofences.forEach {
            mapWrapper.addGeofenceMarker(it)
        }
        // todo check conflicts with other map click listeners
        mapWrapper.googleMap.setOnMarkerClickListener {
            it.snippet?.let { geofenceId ->
                onMarkerClickListener.invoke(GeofenceId(geofenceId))
            }
            return@setOnMarkerClickListener true
        }
    }

    private fun showMapDebugData(googleMap: GoogleMap) {
        (placesInteractor as PlacesInteractorImpl).debugCacheState.value?.let { items ->
            items.forEach {
                val text = it.let { item ->
                    when (item.status) {
                        PlacesInteractorImpl.Status.COMPLETED -> "completed"
                        PlacesInteractorImpl.Status.LOADING -> item.pageToken
                            ?: "loading"
                        PlacesInteractorImpl.Status.ERROR -> "error"
                    }
                }
                googleMap.addMarker(
                    MarkerOptions().anchor(0.5f, 0.5f).position(
                        it.gh.toLocation().toLatLng()
                    )
                        .icon(createPureTextIcon(text))
                )

                it.gh.boundingBox
                    .let { bb ->
                        listOf(
                            bb.bottomLeft,
                            bb.bottomRight,
                            bb.topRight,
                            bb.topLeft,
                            bb.bottomLeft
                        ).map {
                            it.toLatLng()
                        }
                    }
                    .let {
                        googleMap.addPolygon(PolygonOptions().strokeWidth(1f).addAll(it))
                    }
            }
        }
    }

    private fun createPureTextIcon(text: String?): BitmapDescriptor? {
        val textPaint = Paint().apply {
            textSize = 50f
        }
        val textWidth: Float = textPaint.measureText(text)
        val textHeight: Float = textPaint.getTextSize()
        val width = textWidth.toInt()
        val height = textHeight.toInt()
        val image: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.translate(0f, height.toFloat())
        canvas.drawText(text ?: "null", 0f, 0f, textPaint)
        return BitmapDescriptorFactory.fromBitmap(image)
    }

    fun onCleared() {
        managedObserver.onCleared()
    }

}

class GeofenceId(id: String) : TypeWrapper<String>(id)

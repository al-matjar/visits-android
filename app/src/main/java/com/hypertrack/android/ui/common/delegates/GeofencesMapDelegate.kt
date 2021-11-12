package com.hypertrack.android.ui.common.delegates

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.hypertrack.android.interactors.PlacesInteractor
import com.hypertrack.android.interactors.PlacesInteractorImpl
import com.hypertrack.android.models.local.LocalGeofence
import com.hypertrack.android.ui.common.HypertrackMapWrapper
import com.hypertrack.android.ui.common.ManagedObserver
import com.hypertrack.android.ui.common.util.toLatLng
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import net.sharewire.googlemapsclustering.*

open class GeofencesMapDelegate(
    private val context: Context,
    private val mapWrapper: HypertrackMapWrapper,
    private val placesInteractor: PlacesInteractor,
    private val osUtilsProvider: OsUtilsProvider,
    private val onMarkerClickListener: (GeofenceClusterItem) -> Unit
) {

    private val managedObserver = ManagedObserver()

    private val clusterIcon = GeofenceClusterItem.createClusterIcon(osUtilsProvider)
    private val clusterManager = createClusterManager()

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
        clusterManager.onCameraIdle()
    }

    protected open fun updateGeofencesOnMap(
        mapWrapper: HypertrackMapWrapper,
        geofences: List<LocalGeofence>
    ) {
        //todo filter regarding to viewport
        mapWrapper.googleMap.clear()
        geofences.forEach {
            mapWrapper.addGeofenceMarker(it)
        }
        mapWrapper.googleMap.setOnMarkerClickListener {
            it.snippet?.let {
                placesInteractor.geofences.value?.get(it)?.let {
                    onMarkerClickListener.invoke(GeofenceClusterItem(it))
                }
            }
            return@setOnMarkerClickListener true
        }

//        clusterManager.setItems(geofences.map {
//            GeofenceClusterItem(it)
//        })

//        if (BuildConfig.DEBUG.not() || !SHOW_DEBUG_DATA) {
//            clusterManager.setItems(placesInteractor.geofences.value!!.values.map {
//                GeofenceClusterItem(
//                    it
//                )
//            })
//        } else {
//            googleMap.clear()
//            showMapDebugData(googleMap)
//            placesInteractor.geofences.value!!.values.forEach {
//                googleMap.addMarker(
//                    MarkerOptions().icon(
//                        icon
//                    ).position(it.latLng)
//                )
//            }
//        }
    }

    private fun createClusterManager(): ClusterManager<GeofenceClusterItem> {
        return ClusterManager<GeofenceClusterItem>(
            context,
            mapWrapper.googleMap,
            object : ClusterRenderer<GeofenceClusterItem>(context, mapWrapper.googleMap) {

            }).apply {
            setMinClusterSize(10)
            setIconGenerator(object : IconGenerator<GeofenceClusterItem> {
                override fun getClusterIcon(cluster: Cluster<GeofenceClusterItem>): BitmapDescriptor {
                    return clusterIcon
                }

                override fun getClusterItemIcon(clusterItem: GeofenceClusterItem): BitmapDescriptor {
                    return mapWrapper.geofenceMarkerIcon
                }
            })
            setCallbacks(object : ClusterManager.Callbacks<GeofenceClusterItem> {
                override fun onClusterClick(cluster: Cluster<GeofenceClusterItem>): Boolean {
                    //todo
                    return true
                }

                override fun onClusterItemClick(clusterItem: GeofenceClusterItem): Boolean {
                    onMarkerClickListener.invoke(clusterItem)
                    return true
                }
            })
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
        } // Adapt to your needs
        val textWidth: Float = textPaint.measureText(text)
        val textHeight: Float = textPaint.getTextSize()
        val width = textWidth.toInt()
        val height = textHeight.toInt()
        val image: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image)
        canvas.translate(0f, height.toFloat())
//        canvas.drawColor(Color.LTGRAY)
        canvas.drawText(text ?: "null", 0f, 0f, textPaint)
        return BitmapDescriptorFactory.fromBitmap(image)
    }

    fun onCleared() {
        managedObserver.onCleared()
    }

}

class GeofenceClusterItem(
    private val geofence: LocalGeofence
) : ClusterItem {

    override fun getLatitude(): Double = geofence.latLng.latitude

    override fun getLongitude(): Double = geofence.latLng.longitude

    override fun getTitle() = geofence.name

    override fun getSnippet() = geofence.id

    companion object {
        fun createClusterIcon(osUtilsProvider: OsUtilsProvider): BitmapDescriptor {
            return BitmapDescriptorFactory.fromBitmap(
                osUtilsProvider.bitmapFromResource(
                    R.drawable.ic_cluster
                )
            )
        }
    }
}


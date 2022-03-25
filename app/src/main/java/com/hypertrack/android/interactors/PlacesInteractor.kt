package com.hypertrack.android.interactors

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fonfon.kgeohash.GeoHash
import com.google.android.gms.maps.model.LatLng
import com.hypertrack.android.ui.common.delegates.GeofenceNameDelegate
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.DataPage
import com.hypertrack.android.utils.Intersect
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.android.utils.formatters.DateTimeFormatter
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.lang.RuntimeException

interface PlacesInteractor {
    val errorFlow: MutableSharedFlow<Consumable<Exception>>
    val geofences: LiveData<Map<String, Geofence>>
    val geofencesDiff: Flow<List<Geofence>>
    val isLoadingForLocation: MutableLiveData<Boolean>

    fun loadGeofencesForMap(center: LatLng)
    suspend fun getGeofence(geofenceId: String): GeofenceResult
    fun invalidateCache()
    suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        name: String?,
        address: String?,
        radius: Int?,
        description: String?,
        integration: Integration?
    ): CreateGeofenceResult

    suspend fun loadPage(pageToken: String?): DataPage<Geofence>

    val adjacentGeofencesAllowed: Boolean
    suspend fun hasAdjacentGeofence(latLng: LatLng, radius: Int): Boolean
    suspend fun blockingHasAdjacentGeofence(latLng: LatLng, radius: Int): Boolean

    companion object {
        //meters
        const val MIN_RADIUS = 50
        const val MAX_RADIUS = 200
        const val DEFAULT_RADIUS = MIN_RADIUS
    }
}

class PlacesInteractorImpl(
    private val placesRepository: PlacesRepository,
    private val integrationsRepository: IntegrationsRepository,
    private val osUtilsProvider: OsUtilsProvider,
    private val geofenceNameDelegate: GeofenceNameDelegate,
    private val intersect: Intersect,
    private val globalScope: CoroutineScope
) : PlacesInteractor {

    private var pendingCreatedGeofences = mutableListOf<Geofence>()

    override val errorFlow = MutableSharedFlow<Consumable<Exception>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    //key - geofence id
    private val _geofences = mutableMapOf<String, Geofence>()
    override val geofences = MutableLiveData<Map<String, Geofence>>(mapOf())
    override val geofencesDiff = MutableSharedFlow<List<Geofence>>(
        replay = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val pageCache = mutableMapOf<String?, List<Geofence>>()
    private val geoCache = GeoCache()

    val debugCacheState = MutableLiveData<List<GeoCacheItem>>()

    override val isLoadingForLocation = MutableLiveData<Boolean>(false)
    private var firstPageJob: Deferred<DataPage<Geofence>>? = null

    override val adjacentGeofencesAllowed: Boolean = false

    init {
        firstPageJob = globalScope.async {
            loadPlacesPage(null, true)
        }
    }

    override suspend fun loadPage(pageToken: String?): DataPage<Geofence> {
        return loadPlacesPage(pageToken, false)
    }

    override fun loadGeofencesForMap(center: LatLng) {
        val gh = center.getGeohash()
        globalScope.launch(Dispatchers.Main) {
            loadRegion(gh)
            gh.adjacent.forEach { loadRegion(it) }
        }
    }

    override suspend fun getGeofence(geofenceId: String): GeofenceResult {
        val cached = geofences.value?.get(geofenceId)
        return if (cached != null) {
            GeofenceSuccess(cached)
        } else {
            placesRepository.getGeofence(geofenceId).also {
                if (it is GeofenceSuccess) {
                    addGeofencesToCache(listOf(it.geofence))
                }
            }
        }
    }

    override suspend fun hasAdjacentGeofence(latLng: LatLng, radius: Int): Boolean {
        return withContext(Dispatchers.Default) {
            checkIntersection(latLng, radius, geofences.value!!.values.toList())
        }
    }

    override suspend fun blockingHasAdjacentGeofence(latLng: LatLng, radius: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val gh = latLng.getGeohash()
            loadRegion(gh)
            //wait for all ghs to load
            gh.adjacent.forEach { agh ->
                geoCache.getLoadedItem(agh)
            }

            return@withContext hasAdjacentGeofence(latLng, radius)
        }
    }

    override fun invalidateCache() {
        invalidatePageCache()
        integrationsRepository.invalidateCache()
        _geofences.clear()
        geofences.postValue(_geofences)
        geoCache.clear()
    }

    override suspend fun createGeofence(
        latitude: Double,
        longitude: Double,
        name: String?,
        address: String?,
        radius: Int?,
        description: String?,
        integration: Integration?
    ): CreateGeofenceResult {
        return withContext(globalScope.coroutineContext) {
            placesRepository.createGeofence(
                latitude = latitude,
                longitude = longitude,
                name = name,
                address = address,
                radius = radius ?: PlacesInteractor.DEFAULT_RADIUS,
                description = description,
                integration = integration
            ).apply {
                if (this is CreateGeofenceSuccess) {
                    addGeofencesToCache(listOf(geofence))
                    pendingCreatedGeofences.add(geofence)
                }
            }
        }
    }

    private fun invalidatePageCache() {
        pendingCreatedGeofences.clear()
        firstPageJob?.cancel()
        firstPageJob = null
        pageCache.clear()
    }

    private suspend fun loadPlacesPage(
        pageToken: String?,
        initial: Boolean
    ): DataPage<Geofence> {
        if (pageCache.containsKey(pageToken)) {
//            Log.v("hypertrack-verbose", "cached: ${pageToken.hashCode()}")
            return DataPage(
                pageCache.getValue(pageToken).let {
                    if (pageToken == null && pendingCreatedGeofences.isNotEmpty()) {
                        pendingCreatedGeofences.map {
                            it.copy(
                                name = "${geofenceNameDelegate.getName(it)} (${
                                    osUtilsProvider.stringFromResource(
                                        R.string.places_recently_created
                                    )
                                })"
                            )
                        }.plus(it)
                    } else it
                },
                pageToken
            )
        } else {
            val res =
                if ((firstPageJob?.let { it.isActive || it.isCompleted } == true) && !initial) {
//                    Log.v("hypertrack-verbose", "waiting first job: ${pageToken.hashCode()}")
                    firstPageJob!!.await()
                } else {
//                    Log.v("hypertrack-verbose", "loading: ${pageToken.hashCode()}")
                    placesRepository.loadGeofencesPage(pageToken).also {
                        pageCache[pageToken] = it.items
                        addGeofencesToCache(it.items)
                    }.apply {
//                    Log.v("hypertrack-verbose", "--loaded: ${pageToken.hashCode()}")
                    }
                }
            if (pageToken == null) {
                pendingCreatedGeofences.clear()
            }
            return res
        }
    }

    private fun loadRegion(gh: GeoHash) {
        val item = geoCache.getItems()[gh]
//        Log.v(
//            "hypertrack-verbose",
//            geoCache.getItems().values.map { "${it.gh} ${it.status}" }.toString()
//        )
        when (item?.status) {
            Status.COMPLETED, Status.LOADING -> {
            }
            null -> {
//                Log.v("hypertrack-verbose", "loading: ${gh}")
                isLoadingForLocation.postValue(true)
                geoCache.add(gh)
            }
            Status.ERROR -> {
//                Log.v("hypertrack-verbose", "retrying: ${gh}")
                isLoadingForLocation.postValue(true)
                item.retry()
            }
        }
    }

    private fun addGeofencesToCache(newPack: List<Geofence>) {
        globalScope.launch(Dispatchers.Main) {
            newPack.forEach {
                _geofences.put(it.id, it)
            }
            geofences.postValue(_geofences)
            updateDebugCacheState()
            geofencesDiff.emit(newPack)
        }
    }

    private fun updateDebugCacheState() {
        debugCacheState.postValue(geoCache.getItems().values.toList())
    }

    private fun checkIntersection(
        center: LatLng,
        radius: Int,
        allGeofences: List<Geofence>
    ): Boolean {
        val gh = center.getGeohash(5)
        allGeofences.filter {
            val checkGh = it.latLng.getGeohash(5)
            gh == checkGh || checkGh in gh.adjacent
        }.forEach {
            val intersects = if (it.isPolygon) {
                intersect.areCircleAndPolygonIntersects(center, radius, it.polygon!!)
            } else {
                intersect.areTwoCirclesIntersects(center, radius, it.latLng, it.radius)
            }
            if (intersects) return true
        }
        return false
    }

    inner class GeoCache {
        private val items = mutableMapOf<GeoHash, GeoCacheItem>()

        fun getItems(): Map<GeoHash, GeoCacheItem> = items

        fun clear() {
            items.forEach { it.value.job.cancel() }
            items.clear()
        }

        fun contains(gh: GeoHash): Boolean {
            return items.contains(gh)
        }

        fun add(gh: GeoHash) {
            items[gh] = GeoCacheItem(gh)
            updateDebugCacheState()
        }

        fun isLoading(): Boolean {
            return items.values.any { it.status == Status.LOADING }
        }

        //throws
        suspend fun getLoadedItem(gh: GeoHash): GeoCacheItem {
            check(contains(gh))
            val item = items[gh]!!
            when (item.status) {
                Status.COMPLETED -> {
                    return item
                }
                Status.LOADING -> {
                    item.job.join()
                    return item
                }
                Status.ERROR -> {
                    throw RuntimeException("Error loading geofences in region $gh")
                }
            }
        }

    }

    inner class GeoCacheItem(
        val gh: GeoHash
    ) {
        lateinit var job: Job
        var pageToken: String? = null
        lateinit var status: Status

        init {
            load()
        }

        private fun load() {
            status = Status.LOADING
            job = globalScope.launch(Dispatchers.Main) {
                try {
                    do {
                        val res = placesRepository.loadGeofencesPageForMap(gh, pageToken)
                        pageToken = res.paginationToken
                        addGeofencesToCache(res.items)
                    } while (pageToken != null)

                    status = Status.COMPLETED
                    isLoadingForLocation.postValue(geoCache.isLoading())
//                    Log.v(
//                        "hypertrack-verbose",
//                        "${gh}: loaded"
//                    )
                } catch (e: Exception) {
//                    Log.v("hypertrack-verbose", "error for ${gh} :  ${e.format()}")
                    status = Status.ERROR
                    isLoadingForLocation.postValue(geoCache.isLoading())
                    errorFlow.emit(Consumable(e))
                }
            }
        }

        fun retry() {
            load()
        }
    }

    enum class Status {
        COMPLETED, LOADING, ERROR
    }

}

private fun LatLng.getGeohash(charsCount: Int = 4): GeoHash {
    return GeoHash(latitude, longitude, charsCount)
}


sealed class GeofenceResult
class GeofenceSuccess(val geofence: Geofence) : GeofenceResult()
class GeofenceError(val e: Exception) : GeofenceResult()

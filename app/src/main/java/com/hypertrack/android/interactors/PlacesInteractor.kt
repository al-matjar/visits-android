package com.hypertrack.android.interactors

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hypertrack.android.interactors.app.AppInteractor
import com.hypertrack.android.interactors.app.GeofencesForMapAppAction
import com.hypertrack.android.interactors.app.action.ClearGeofencesForMapAction
import com.hypertrack.android.ui.common.delegates.GeofenceNameDelegate
import com.hypertrack.android.models.Integration
import com.hypertrack.android.models.local.Geofence
import com.hypertrack.android.repository.*
import com.hypertrack.android.ui.base.Consumable
import com.hypertrack.android.ui.common.DataPage
import com.hypertrack.android.utils.Intersect
import com.hypertrack.android.utils.OsUtilsProvider
import com.hypertrack.logistics.android.github.R
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

interface PlacesInteractor {
    val errorFlow: MutableSharedFlow<Consumable<Exception>>
    val geofences: LiveData<Map<String, Geofence>>
    val isLoadingForLocation: MutableLiveData<Boolean>

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

    companion object {
        //meters
        const val MIN_RADIUS = 50
        const val MAX_RADIUS = 200
        const val DEFAULT_RADIUS = MIN_RADIUS
    }
}

class PlacesInteractorImpl(
    private val appInteractor: AppInteractor,
    private val placesRepository: PlacesRepository,
    private val integrationsRepository: IntegrationsRepository,
    private val osUtilsProvider: OsUtilsProvider,
    private val geofenceNameDelegate: GeofenceNameDelegate,
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
    private val pageCache = mutableMapOf<String?, List<Geofence>>()

    override val isLoadingForLocation = MutableLiveData<Boolean>(false)
    private var firstPageJob: Deferred<DataPage<Geofence>>? = null

    init {
        firstPageJob = globalScope.async {
            loadPlacesPage(null, true)
        }
    }

    override suspend fun loadPage(pageToken: String?): DataPage<Geofence> {
        return loadPlacesPage(pageToken, false)
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

    override fun invalidateCache() {
        // todo clear map cache
        invalidatePageCache()
        integrationsRepository.invalidateCache()
        _geofences.clear()
        geofences.postValue(_geofences)
        appInteractor.handleAction(GeofencesForMapAppAction(ClearGeofencesForMapAction))
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

    private fun addGeofencesToCache(newPack: List<Geofence>) {
        globalScope.launch(Dispatchers.Main) {
            newPack.forEach {
                _geofences.put(it.id, it)
            }
            geofences.postValue(_geofences)
        }
    }

}
